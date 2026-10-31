# LinkMe v2 design

User request: find or clone fuarthur/linkme, review and repair it, improve UI/UX, evolve the same idea and use only a free backend.

## Approach

Keep the native Kotlin / Android Views architecture and original Firebase project. A browser rewrite would abandon the Android app; replacing Firebase would add migration without addressing an observed outage. The existing Spark project is active. This is an implementation choice under the requested review and upgrade; campus-only and group-community expansions remain optional directions.

## User flow

Login or register -> set a name, short bio and 1–8 interests -> Discover people with shared interests -> open a conversation -> exchange real-time text -> return to a persistent inbox. Three native navigation destinations: Discover, Chats, Profile. Profile edits load existing values and wait for save acknowledgement. Login restores a valid session and supports password recovery. Empty, loading, offline and error states have clear copy and retry actions.

## Data and privacy

Users/{uid} remains private, owner readable/writable, preserving legacy email/phone fields. PublicProfiles/{uid} contains only uid, username, bio, interests, discoverable and updatedAt. Owners publish safe public fields on profile save; old accounts publish only after editing/saving their profile. No blanket migration of private data.

Messages remains the backwards-compatible text-message collection with senderId, receiveId, content and timestamp. New messages additionally use a deterministic conversationId made from sorted UID values and members with exactly those two UIDs. Chats/{conversationId} contains immutable sorted members plus lastMessage, lastSenderId, updatedAt and lastMessageId; the lastMessageId must reference the fresh matching message in the same atomic write. Rules independently validate the canonical conversation hash. Sending writes message and chat summary atomically. Security rules validate participants, sender identity, field types/lengths and timestamps. Existing legacy messages can be read only by their participants; they remain accessible in chat and inbox queries without rewriting historical data.

Discovery queries PublicProfiles where discoverable == true and interests array-contains-any selected interests, limit 60. Rank the bounded result locally by shared-interest count then stable name/UID. Never display fabricated people, match percentages, proximity or online status.

## UI

Improve the original purple identity through Material 3, warm neutral surfaces, dark theme, readable typography and native navigation. Use initial avatars to avoid paid file storage. Main area has shared-interest chips, readable people rows and a clear conversation action. Chat has a named toolbar, bounded real-time history, timestamps, a safe composer, visible failures and draft preservation. Forms scroll at narrow sizes, landscape and increased font sizes. Respect system/keyboard insets and native Back.

## Quality and delivery

Fix registration/profile races, launch routing, missing intent/auth guards, stale query results, duplicate sends and draft clearing. Remove unused dependencies. Unit tests cover discovery ranking, normalization, conversation identity, validation and draft completion. Emulator security tests cover unauthorized access, forged senders, membership changes and private fields. Build, lint, instrumented UI checks and emulator screenshots precede delivery. Supply a local debug APK and a review report. Any live rule change must be verified and keep Spark active. No repository push or store release is requested.
