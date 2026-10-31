import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { before, beforeEach, after, test } from 'node:test';
import {
  assertFails, assertSucceeds, initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { initializeApp, deleteApp } from 'firebase/app';
import {
  connectAuthEmulator, createUserWithEmailAndPassword, getAuth,
  signInWithEmailAndPassword, signOut,
} from 'firebase/auth';
import {
  Timestamp, and, collection, connectFirestoreEmulator, deleteDoc,
  doc, documentId, getDoc, getDocs, getFirestore, limit, or, orderBy,
  query, serverTimestamp, setDoc, updateDoc, where, writeBatch,
} from 'firebase/firestore';

// A missing restriction must fail a denial assertion against the real emulator.
// Never permit this suite to fall back to a live Firebase endpoint.
const projectId = 'demo-linkme';
const host = process.env.FIRESTORE_EMULATOR_HOST;
const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
if (host !== '127.0.0.1:8080' || authHost !== '127.0.0.1:9099') {
  throw new Error('Run through npm test: isolated Auth and Firestore emulators are required.');
}
const rulesPath = process.env.LINKME_RULES_MODE === 'open'
  ? new URL('./fixtures/open.rules', import.meta.url)
  : new URL('../firestore.rules', import.meta.url);
const oldTime = Timestamp.fromMillis(1_700_000_000_000);
let env;

function canonicalId(first, second) {
  const pair = [first, second].sort();
  return createHash('sha256').update(pair.map((uid) => `${uid.length}:${uid}`).join(''), 'utf8').digest('hex');
}

function dbAs(uid) {
  return uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
}

function publicProfile(uid, overrides = {}) {
  return {
    uid, username: uid.toUpperCase(), bio: 'Enjoys music and games',
    interests: ['Music', 'Games'], discoverable: true, updatedAt: oldTime,
    ...overrides,
  };
}

function chatData(overrides = {}) {
  return {
    members: ['alice', 'bob'], lastMessage: 'Hello Bob',
    lastSenderId: 'alice', updatedAt: serverTimestamp(), lastMessageId: 'new-message', ...overrides,
  };
}

function messageData(overrides = {}) {
  return {
    senderId: 'alice', receiveId: 'bob', content: 'Hello Bob',
    timestamp: serverTimestamp(), conversationId: canonicalId('alice', 'bob'),
    members: ['alice', 'bob'], ...overrides,
  };
}

function sendBatch(db, messageOverrides = {}, chatOverrides = {}, messageId = 'new-message') {
  const message = messageData(messageOverrides);
  const batch = writeBatch(db);
  batch.set(doc(db, 'Chats', message.conversationId), chatData({ lastMessageId: messageId, ...chatOverrides }));
  batch.set(doc(db, 'Messages', messageId), message);
  return batch.commit();
}

async function seed() {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all(['alice', 'bob', 'carol', 'dan'].flatMap((uid) => [
      setDoc(doc(db, 'Users', uid), {
        uid, email: `${uid}@example.test`, phone: '+15555550123', gender: 'Not Specified',
        username: uid.toUpperCase(), interests: ['Music', 'Games'],
      }),
      setDoc(doc(db, 'PublicProfiles', uid), publicProfile(uid)),
    ]));
    const oldSummary = chatData({ updatedAt: oldTime });
    delete oldSummary.lastMessageId;
    await Promise.all([
      setDoc(doc(db, 'Chats', canonicalId('alice', 'bob')), oldSummary),
      setDoc(doc(db, 'Messages', 'legacy-alice-bob'), {
        senderId: 'alice', receiveId: 'bob', content: 'An existing legacy message', timestamp: oldTime,
      }),
      setDoc(doc(db, 'Messages', 'legacy-carol-bob'), {
        senderId: 'carol', receiveId: 'bob', content: 'A different conversation', timestamp: oldTime,
      }),
      setDoc(doc(db, 'Messages', 'legacy-carol-dan'), {
        senderId: 'carol', receiveId: 'dan', content: 'Unrelated private history', timestamp: oldTime,
      }),
    ]);
  });
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { host: '127.0.0.1', port: 8080, rules: await readFile(rulesPath, 'utf8') },
  });
});
beforeEach(async () => { await env.clearFirestore(); await seed(); });
after(async () => { await env?.cleanup(); });

for (const path of [
  'Users/alice', 'PublicProfiles/alice', 'Messages/legacy-alice-bob',
  `Chats/${canonicalId('alice', 'bob')}`, 'Users/alice/Blocked/bob',
]) {
  test(`unauthenticated client cannot read ${path}`, async () => {
    await assertFails(getDoc(doc(dbAs(null), path)));
  });
}

test('owner can read and merge a private legacy profile without losing contact fields', async () => {
  const db = dbAs('alice');
  await assertSucceeds(setDoc(doc(db, 'Users', 'alice'), {
    bio: 'Updated bio', discoverable: true, updatedAt: serverTimestamp(),
  }, { merge: true }));
  const result = await assertSucceeds(getDoc(doc(db, 'Users', 'alice')));
  assert.equal(result.data().email, 'alice@example.test');
  assert.equal(result.data().phone, '+15555550123');
  assert.equal(result.data().bio, 'Updated bio');
});

test('another user cannot read a private profile', async () => {
  await assertFails(getDoc(doc(dbAs('bob'), 'Users', 'alice')));
});

test('another user cannot change a private profile', async () => {
  await assertFails(updateDoc(doc(dbAs('bob'), 'Users', 'alice'), { username: 'Impersonated' }));
});

test('private profiles cannot be listed, even when the query selects only the owner', async () => {
  const db = dbAs('alice');
  await assertFails(getDocs(query(collection(db, 'Users'), where(documentId(), '==', 'alice'), limit(1))));
});

test('safe owner public profile publication succeeds with an acknowledged server timestamp', async () => {
  const db = dbAs('alice');
  await assertSucceeds(setDoc(doc(db, 'PublicProfiles', 'alice'), publicProfile('alice', {
    username: 'n'.repeat(40), bio: '', interests: ['Sport', 'Food', 'Dating', 'Games', 'Music', 'Anime', 'Movie', 'Other'],
    updatedAt: serverTimestamp(),
  })));
  await assertSucceeds(updateDoc(doc(db, 'PublicProfiles', 'alice'), {
    bio: 'b'.repeat(240), updatedAt: serverTimestamp(),
  }));
  const result = await getDoc(doc(db, 'PublicProfiles', 'alice'));
  assert.deepEqual(Object.keys(result.data()).sort(),
    ['uid', 'username', 'bio', 'interests', 'discoverable', 'updatedAt'].sort());
});

for (const [name, overrides] of [
  ['contact email leak', { email: 'private@example.test' }],
  ['contact phone leak', { phone: '+15555550123' }],
  ['unrecognized public field', { role: 'admin' }],
  ['forged public uid', { uid: 'bob' }],
  ['empty username', { username: '' }],
  ['oversized username', { username: 'n'.repeat(41) }],
  ['oversized bio', { bio: 'x'.repeat(241) }],
  ['empty interests', { interests: [] }],
  ['duplicate interests', { interests: ['Music', 'Music'] }],
  ['unknown interest', { interests: ['Cryptocurrency'] }],
  ['non-list interests', { interests: 'Music' }],
  ['invalid visibility type', { discoverable: 'true' }],
  ['spoofed publication timestamp', { updatedAt: oldTime }],
]) {
  test(`public profile rejects ${name}`, async () => {
    const db = dbAs('alice');
    await assertFails(setDoc(doc(db, 'PublicProfiles', 'alice'), publicProfile('alice', {
      updatedAt: serverTimestamp(), ...overrides,
    })));
  });
}

test('public schema rejects a missing required field', async () => {
  const profile = publicProfile('alice', { updatedAt: serverTimestamp() });
  delete profile.bio;
  await assertFails(setDoc(doc(dbAs('alice'), 'PublicProfiles', 'alice'), profile));
});

test('another user cannot publish a profile under an existing owner uid', async () => {
  await assertFails(setDoc(doc(dbAs('bob'), 'PublicProfiles', 'alice'), publicProfile('alice', {
    updatedAt: serverTimestamp(),
  })));
});

test('discovery permits the bounded visible interest query and exposes no contact fields', async () => {
  const db = dbAs('alice');
  const result = await assertSucceeds(getDocs(query(collection(db, 'PublicProfiles'),
    where('discoverable', '==', true), where('interests', 'array-contains-any', ['Music', 'Movie']), limit(60))));
  assert.equal(result.size, 4);
  for (const snapshot of result.docs) {
    assert.equal('email' in snapshot.data(), false);
    assert.equal('phone' in snapshot.data(), false);
  }
});

test('hidden profile is owner-readable and not readable by another account', async () => {
  await env.withSecurityRulesDisabled((context) => updateDoc(
    doc(context.firestore(), 'PublicProfiles', 'bob'), { discoverable: false }));
  await assertSucceeds(getDoc(doc(dbAs('bob'), 'PublicProfiles', 'bob')));
  await assertFails(getDoc(doc(dbAs('alice'), 'PublicProfiles', 'bob')));
});

test('discovery rejects a query that can include hidden profiles', async () => {
  const db = dbAs('alice');
  await assertFails(getDocs(query(collection(db, 'PublicProfiles'),
    where('interests', 'array-contains-any', ['Music']), limit(60))));
});

for (const queryLimit of [null, 61]) {
  test(`discovery rejects ${queryLimit === null ? 'an unbounded' : 'an oversized'} list`, async () => {
    const db = dbAs('alice');
    const filters = [where('discoverable', '==', true)];
    if (queryLimit !== null) filters.push(limit(queryLimit));
    await assertFails(getDocs(query(collection(db, 'PublicProfiles'), ...filters)));
  });
}

test('a participant can atomically send a message and update the two-person summary', async () => {
  const db = dbAs('alice');
  await assertSucceeds(sendBatch(db));
  const message = await assertSucceeds(getDoc(doc(dbAs('bob'), 'Messages', 'new-message')));
  const chat = await assertSucceeds(getDoc(doc(dbAs('bob'), 'Chats', canonicalId('alice', 'bob'))));
  assert.equal(message.data().content, 'Hello Bob');
  assert.equal(chat.data().lastMessage, 'Hello Bob');
  assert.equal(message.data().timestamp.isEqual(chat.data().updatedAt), true);
  const boundaryText = 'H\n' + 'x'.repeat(1998);
  await assertSucceeds(sendBatch(db, { content: boundaryText }, { lastMessage: boundaryText }, 'boundary-message'));
});

test('a first message can create its summary and message in the same batch', async () => {
  const db = dbAs('alice');
  await assertSucceeds(sendBatch(db, {
    receiveId: 'carol', conversationId: canonicalId('alice', 'carol'), members: ['alice', 'carol'],
  }, { members: ['alice', 'carol'] }));
});

for (const [name, overrides] of [
  ['a forged sender', { senderId: 'carol' }],
  ['self-addressed messages', { receiveId: 'alice' }],
  ['an empty recipient', { receiveId: '' }],
  ['an unpublished recipient', { receiveId: 'missing-user', members: ['alice', 'missing-user'] }],
  ['mismatched message members', { members: ['alice', 'carol'] }],
  ['duplicate message members', { members: ['alice', 'alice'] }],
  ['a third message participant', { members: ['alice', 'bob', 'carol'] }],
  ['non-string content', { content: 7 }],
  ['empty content', { content: '' }],
  ['whitespace-only content', { content: ' \n\t ' }],
  ['oversized content', { content: 'x'.repeat(2001) }],
  ['a spoofed timestamp', { timestamp: oldTime }],
  ['an extra message field', { email: 'private@example.test' }],
]) {
  test(`message creation rejects ${name}`, async () => {
    await assertFails(sendBatch(dbAs('alice'), overrides));
  });
}

test('message creation rejects a missing required field', async () => {
  const db = dbAs('alice');
  const message = messageData();
  delete message.members;
  const batch = writeBatch(db);
  batch.set(doc(db, 'Chats', canonicalId('alice', 'bob')), chatData());
  batch.set(doc(db, 'Messages', 'incomplete-message'), message);
  await assertFails(batch.commit());
});

test('a message cannot be created without a matching conversation summary', async () => {
  await assertFails(setDoc(doc(dbAs('alice'), 'Messages', 'no-chat'), messageData({
    receiveId: 'carol', members: ['alice', 'carol'], conversationId: 'missing-chat',
  })));
});

test('a message cannot use an existing but stale summary without a summary update', async () => {
  await assertFails(setDoc(doc(dbAs('alice'), 'Messages', 'stale-chat'), messageData()));
});

test('a message cannot disagree with the summary text in its batch', async () => {
  await assertFails(sendBatch(dbAs('alice'), { content: 'Different content' }));
});

test('participants cannot edit or delete already-sent messages', async () => {
  const db = dbAs('alice');
  await assertFails(updateDoc(doc(db, 'Messages', 'legacy-alice-bob'), { content: 'Changed history' }));
  await assertFails(deleteDoc(doc(db, 'Messages', 'legacy-alice-bob')));
});

test('legacy message is readable only by sender and recipient', async () => {
  await assertSucceeds(getDoc(doc(dbAs('alice'), 'Messages', 'legacy-alice-bob')));
  await assertSucceeds(getDoc(doc(dbAs('bob'), 'Messages', 'legacy-alice-bob')));
  await assertFails(getDoc(doc(dbAs('carol'), 'Messages', 'legacy-alice-bob')));
});

test('bounded conversation OR query includes legacy history in either direction', async () => {
  const db = dbAs('alice');
  await assertSucceeds(sendBatch(dbAs('bob'), {
    senderId: 'bob', receiveId: 'alice', content: 'Hello Alice',
  }, { lastSenderId: 'bob', lastMessage: 'Hello Alice' }));
  const result = await assertSucceeds(getDocs(query(collection(db, 'Messages'),
    or(and(where('senderId', '==', 'alice'), where('receiveId', '==', 'bob')),
      and(where('senderId', '==', 'bob'), where('receiveId', '==', 'alice'))),
    orderBy('timestamp', 'desc'), orderBy(documentId(), 'desc'), limit(100))));
  assert.deepEqual(result.docs.map((snapshot) => snapshot.id).sort(), ['legacy-alice-bob', 'new-message']);
});

test('bounded legacy inbox OR query returns only the current user history', async () => {
  const db = dbAs('bob');
  const result = await assertSucceeds(getDocs(query(collection(db, 'Messages'),
    or(where('senderId', '==', 'bob'), where('receiveId', '==', 'bob')),
    orderBy('timestamp', 'desc'), orderBy(documentId(), 'desc'), limit(100))));
  assert.deepEqual(result.docs.map((snapshot) => snapshot.id).sort(), ['legacy-alice-bob', 'legacy-carol-bob']);
});

for (const queryLimit of [null, 101]) {
  test(`message history rejects ${queryLimit === null ? 'an unbounded' : 'an oversized'} list`, async () => {
    const db = dbAs('alice');
    const constraints = [where('senderId', '==', 'alice'), orderBy('timestamp', 'desc')];
    if (queryLimit !== null) constraints.push(limit(queryLimit));
    await assertFails(getDocs(query(collection(db, 'Messages'), ...constraints)));
  });
}

test('an unrelated account cannot query somebody else conversation', async () => {
  const db = dbAs('carol');
  await assertFails(getDocs(query(collection(db, 'Messages'),
    where('senderId', '==', 'alice'), where('receiveId', '==', 'bob'), limit(100))));
});

test('chat membership cannot add or replace a participant', async () => {
  const db = dbAs('alice');
  await assertFails(setDoc(doc(db, 'Chats', canonicalId('alice', 'bob')), chatData({ members: ['alice', 'bob', 'carol'] })));
  await assertFails(setDoc(doc(db, 'Chats', canonicalId('alice', 'bob')), chatData({ members: ['alice', 'carol'] })));
});

test('new chat rejects duplicate members, malformed summaries, and forged sender', async () => {
  const db = dbAs('alice');
  for (const overrides of [
    { members: ['alice', 'alice'] }, { members: ['alice', 7] },
    { lastMessage: ' \n\t ' }, { lastMessage: 'x'.repeat(2001) },
    { lastSenderId: 'bob' }, { updatedAt: oldTime }, { phone: '+15555550123' },
  ]) {
    await assertFails(setDoc(doc(db, 'Chats', 'new-chat'), chatData(overrides)));
  }
});

test('unrelated accounts cannot get chats and members cannot delete history summaries', async () => {
  await assertFails(getDoc(doc(dbAs('carol'), 'Chats', canonicalId('alice', 'bob'))));
  await assertFails(deleteDoc(doc(dbAs('alice'), 'Chats', canonicalId('alice', 'bob'))));
});

test('a bounded member chat query succeeds and unbounded or unrelated chat queries fail', async () => {
  const db = dbAs('alice');
  await assertSucceeds(getDocs(query(collection(db, 'Chats'),
    where('members', 'array-contains', 'alice'), orderBy('updatedAt', 'desc'), limit(100))));
  await assertFails(getDocs(query(collection(db, 'Chats'), where('members', 'array-contains', 'alice'))));
  await assertFails(getDocs(query(collection(db, 'Chats'), where('members', 'array-contains', 'alice'), limit(101))));
  await assertFails(getDocs(query(collection(db, 'Chats'),
    where('members', 'array-contains', 'bob'), limit(100))));
});

test('owner can create, inspect, list, and delete their own block entries', async () => {
  const db = dbAs('alice');
  const ref = doc(db, 'Users', 'alice', 'Blocked', 'bob');
  await assertSucceeds(setDoc(ref, { createdAt: serverTimestamp() }));
  await assertSucceeds(getDoc(ref));
  const result = await assertSucceeds(getDocs(collection(db, 'Users', 'alice', 'Blocked')));
  assert.equal(result.size, 1);
  await assertSucceeds(deleteDoc(ref));
});

test('block entries reject forged owners, extra fields, self-blocking, and clock spoofing', async () => {
  const db = dbAs('alice');
  await assertFails(setDoc(doc(db, 'Users', 'bob', 'Blocked', 'carol'), { createdAt: serverTimestamp() }));
  await assertFails(setDoc(doc(db, 'Users', 'alice', 'Blocked', 'bob'), { createdAt: oldTime }));
  await assertFails(setDoc(doc(db, 'Users', 'alice', 'Blocked', 'bob'), {
    createdAt: serverTimestamp(), phone: '+15555550123',
  }));
  await assertFails(setDoc(doc(db, 'Users', 'alice', 'Blocked', 'alice'), { createdAt: serverTimestamp() }));
});

test('block lists and entries are private to their owner', async () => {
  await assertSucceeds(setDoc(doc(dbAs('alice'), 'Users', 'alice', 'Blocked', 'bob'), { createdAt: serverTimestamp() }));
  const db = dbAs('bob');
  await assertFails(getDoc(doc(db, 'Users', 'alice', 'Blocked', 'bob')));
  await assertFails(getDocs(collection(db, 'Users', 'alice', 'Blocked')));
  await assertFails(deleteDoc(doc(db, 'Users', 'alice', 'Blocked', 'bob')));
});

for (const blocker of ['alice', 'bob']) {
  for (const sender of ['alice', 'bob']) {
    test(`${blocker} blocking the peer denies a new send from ${sender}`, async () => {
      const peer = blocker === 'alice' ? 'bob' : 'alice';
      await assertSucceeds(setDoc(doc(dbAs(blocker), 'Users', blocker, 'Blocked', peer), { createdAt: serverTimestamp() }));
      const reverse = sender === 'bob';
      await assertFails(sendBatch(dbAs(sender), {
        senderId: sender, receiveId: reverse ? 'alice' : 'bob',
      }, { lastSenderId: sender }));
    });
  }
}

test('a block also prevents summary-only writes but preserves participant access to legacy history', async () => {
  await assertSucceeds(setDoc(doc(dbAs('alice'), 'Users', 'alice', 'Blocked', 'bob'), { createdAt: serverTimestamp() }));
  await assertFails(setDoc(doc(dbAs('bob'), 'Chats', canonicalId('alice', 'bob')), chatData({ lastSenderId: 'bob' })));
  await assertSucceeds(getDoc(doc(dbAs('bob'), 'Messages', 'legacy-alice-bob')));
});

test('email/password authentication can create, sign out, sign back in and save a private profile locally', async () => {
  const app = initializeApp({ projectId, apiKey: 'demo-api-key', authDomain: `${projectId}.firebaseapp.com` }, 'auth-smoke');
  try {
    const auth = getAuth(app);
    connectAuthEmulator(auth, `http://${authHost}`, { disableWarnings: true });
    const db = getFirestore(app);
    connectFirestoreEmulator(db, '127.0.0.1', 8080);
    const email = `emulator-user-${Date.now()}@example.test`;
    const credential = await createUserWithEmailAndPassword(auth, email, 'A-local-password1');
    const uid = credential.user.uid;
    await assertSucceeds(setDoc(doc(db, 'Users', uid), { uid, email }));
    await signOut(auth);
    await assertFails(getDoc(doc(db, 'Users', uid)));
    await signInWithEmailAndPassword(auth, email, 'A-local-password1');
    const profile = await assertSucceeds(getDoc(doc(db, 'Users', uid)));
    assert.equal(profile.data().uid, uid);
  } finally {
    await deleteApp(app);
  }
});


// These exploits passed the previous member-only summary policy. The denial
// assertions must fail before canonical identity and fresh-message linkage exist.
test('canonical security: another participant cannot squat on the alice-bob canonical pair ID', async () => {
  await env.withSecurityRulesDisabled((context) => deleteDoc(
    doc(context.firestore(), 'Chats', canonicalId('alice', 'bob'))));
  await assertFails(sendBatch(dbAs('carol'), {
    senderId: 'carol', receiveId: 'alice', members: ['alice', 'carol'],
    conversationId: canonicalId('alice', 'bob'),
  }, { members: ['alice', 'carol'], lastSenderId: 'carol' }));
});

test('canonical security: the same pair cannot create summaries under arbitrary duplicate IDs', async () => {
  await assertFails(sendBatch(dbAs('alice'), { conversationId: 'attacker-chosen-duplicate-pair' }));
});

test('canonical security: summary-only creation cannot invent an inbox preview', async () => {
  await assertFails(setDoc(doc(dbAs('alice'), 'Chats', canonicalId('alice', 'carol')),
    chatData({ members: ['alice', 'carol'] })));
});

test('canonical security: summary-only update cannot invent a newer preview', async () => {
  await env.withSecurityRulesDisabled((context) => setDoc(
    doc(context.firestore(), 'Chats', canonicalId('alice', 'bob')), chatData({ updatedAt: oldTime })));
  await assertFails(setDoc(doc(dbAs('alice'), 'Chats', canonicalId('alice', 'bob')), chatData()));
});

test('canonical security: an existing message cannot be reused to authorize a new summary', async () => {
  await env.withSecurityRulesDisabled((context) => setDoc(
    doc(context.firestore(), 'Messages', 'old-v2-message'), messageData({ timestamp: oldTime })));
  await assertFails(setDoc(doc(dbAs('alice'), 'Chats', canonicalId('alice', 'bob')),
    chatData({ lastMessageId: 'old-v2-message' })));
});

test('canonical security: one summary cannot authorize two new message document IDs', async () => {
  const db = dbAs('alice');
  const batch = writeBatch(db);
  const id = canonicalId('alice', 'bob');
  batch.set(doc(db, 'Chats', id), chatData());
  batch.set(doc(db, 'Messages', 'new-message'), messageData({ conversationId: id }));
  batch.set(doc(db, 'Messages', 'extra-message'), messageData({ conversationId: id }));
  await assertFails(batch.commit());
});


for (const [first, second] of [['z', 'A'], ['é', '中'], ['😀abc', 'bob'], ['😀', '\uE000']]) {
  test(`canonical key matches Android UTF-16 sort/length and lowercase UTF-8 hash for ${JSON.stringify([first, second])}`, async () => {
    const members = [first, second].sort();
    await env.withSecurityRulesDisabled((context) => Promise.all(members.map((uid) =>
      setDoc(doc(context.firestore(), 'PublicProfiles', uid), publicProfile(uid)))));
    const db = dbAs(first);
    const id = canonicalId(first, second);
    await assertSucceeds(sendBatch(db, {
      senderId: first, receiveId: second, members, conversationId: id,
    }, { members, lastSenderId: first }));
    const summary = await assertSucceeds(getDoc(doc(dbAs(second), 'Chats', id)));
    assert.equal(summary.data().lastMessageId, 'new-message');
  });
}
