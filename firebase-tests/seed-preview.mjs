import { initializeApp, deleteApp } from 'firebase/app';
import { getAuth, connectAuthEmulator, createUserWithEmailAndPassword, signInWithEmailAndPassword, inMemoryPersistence, setPersistence } from 'firebase/auth';
import { getFirestore, connectFirestoreEmulator, doc, setDoc, serverTimestamp, writeBatch, terminate, deleteDoc, collection, query, where, limit, getDocs } from 'firebase/firestore';
import { createHash } from 'node:crypto';

// Synthetic preview only. Explicit emulator endpoints and demo project prevent production writes.
const options = { projectId: 'demo-linkme', apiKey: 'demo-api-key', appId: '1:1234567890:android:demo-linkme' };
const password = ' linkme-preview-pass ';
if(process.argv.includes('--reset')) {
 for(const url of ['http://127.0.0.1:8080/emulator/v1/projects/demo-linkme/databases/(default)/documents','http://127.0.0.1:9099/emulator/v1/projects/demo-linkme/accounts']) {
  const response=await fetch(url,{method:'DELETE'});if(!response.ok)throw new Error(`Local fixture reset failed: ${response.status}`);
 }
}
const samples = [
 ['alex','Alex Morgan','Always up for a new playlist and a good co-op game.',['Music','Games','Food']],
 ['maya','Maya Chen','Looking for a concert buddy and someone to swap indie game recommendations with.',['Music','Games','Movie']],
 ['leo','Leo Rivera','Collecting new recipes and old records. Tell me what you are listening to.',['Music','Food']],
 ['nora','Nora Patel','Weekend hikes, film nights, and finding the best noodles in town.',['Sport','Movie','Food']],
 ['sam','Sam Taylor','A new album on repeat, a sketchbook nearby, and one more episode.',['Music','Anime']],
 ['riley','Riley Brooks','Happy to talk about a great film long after the credits end.',['Movie']],
 ['hidden','Private Preview','This profile must not appear in discovery.',['Music','Games']]
];
const clients = new Map();
for (const [key, username, bio, interests] of samples) {
 const app = initializeApp(options, `preview-${key}`);
 const auth = getAuth(app); connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true}); await setPersistence(auth,inMemoryPersistence);
 const db = getFirestore(app); connectFirestoreEmulator(db,'127.0.0.1',8080);
 const email = `${key}@linkme.example.test`;
 let result;
 try { result = await createUserWithEmailAndPassword(auth,email,password); }
 catch (error) { if(error.code !== 'auth/email-already-in-use') throw error; result = await signInWithEmailAndPassword(auth,email,password); }
 const uid = result.user.uid;
 const profile = {uid,username,bio,interests,discoverable:key!=='hidden',updatedAt:serverTimestamp()};
 await setDoc(doc(db,'Users',uid),{...profile,email,phone:'202-555-0100',gender:'Not specified'},{merge:true});
 await setDoc(doc(db,'PublicProfiles',uid),profile);
 clients.set(key,{app,auth,db,uid});
}
await deleteDoc(doc(clients.get('alex').db,'Users',clients.get('alex').uid,'Blocked',clients.get('maya').uid));
for(const [senderKey,receiverKey,content] of [
 ['maya','alex','Hey Alex! Found a new co-op game we should try.'],
 ['alex','maya','I am in. What have you been playing lately?'],
 ['nora','alex','Any good film recommendations for the weekend?']
]) {
 const sender = clients.get(senderKey), receiver = clients.get(receiverKey);
 const members=[sender.uid,receiver.uid].sort();
 const id=createHash('sha256').update(members.map(v=>`${v.length}:${v}`).join('')).digest('hex');
 const existing=await getDocs(query(collection(sender.db,'Messages'),where('senderId','==',sender.uid),where('receiveId','==',receiver.uid),where('content','==',content),limit(1)));
 if(!existing.empty)continue;
 const messageId=`preview-${senderKey}-${receiverKey}`;
 const batch=writeBatch(sender.db);
 batch.set(doc(sender.db,'Chats',id),{members,lastMessage:content,lastSenderId:sender.uid,updatedAt:serverTimestamp(),lastMessageId:messageId});
 batch.set(doc(sender.db,'Messages',messageId),{members,conversationId:id,senderId:sender.uid,receiveId:receiver.uid,content,timestamp:serverTimestamp()});
 await batch.commit();
}
for(const c of clients.values()){await terminate(c.db);await deleteApp(c.app);}
console.log('Seeded 7 synthetic local preview profiles and 2 conversations in demo-linkme.');
