# Phase 6 Session 2775 Completion

## Unit of Work

[Phase 6][UOW-2775] Send legion member list on invite acceptance

## Runtime Progress Gate

- Deferred/live behavior advanced: live guild invite acceptance sent info/add-member/emblem/edit/title packets, but still skipped Java's `updateLegionMemberList(player, false, player.getObjectId())` packet to populate the accepted player's roster.
- Java source of truth: `LegionService.addLegionMember`, `LegionService.updateLegionMemberList`, and `SM_LEGION_MEMBERLIST.writeImpl`.
- C# runtime artifact wired/fixed: new `SmLegionMemberList`, `GameServerConnection.AcceptLegionInviteAsync`, current online same-legion member rows, and the accepted player's direct packet path.
- Client-visible/state/persistence effect changed: the accepted player now receives a real `SM_LEGION_MEMBERLIST` packet for currently online same-legion members before the add-member fanout completes their own roster entry.
- Why this is not preview-only/test-only/documentation-only: it sends a real server packet from live invite-acceptance code and changes the accepted client's visible legion roster state.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`

## C# Runtime Changes

- Added `SmLegionMemberList` with Java opcode `157`.
- Serialized first/last flags, signed count, and member rows in Java `SM_LEGION_MEMBERLIST.writeImpl` order.
- Sent a first/last member-list chunk directly to the accepted player after `SM_LEGION_INFO`.
- Excluded the accepted player's own row, matching Java's `updateLegionMemberList(player, false, player.getObjectId())` call.
- Used currently online same-legion players available from the live connection registry; offline repository-backed roster loading remains a documented gap.

## Validation Decision

- Changed surface: server-packet serialization and live invite-acceptance packet fanout.
- Specific behavior/contract: Java sends `SM_LEGION_MEMBERLIST` to the accepted player after `SM_LEGION_INFO`, excluding that player's object id, with the last chunk count written as `-size`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionMemberList" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `SM_LEGION_MEMBERLIST` serialization.
- Broad-validation trigger: live packet fanout was changed.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly asserted packet shape plus live invite-acceptance roster fanout.
- Why this scope is sufficient: the change is isolated to one packet class and one live send call; the focused tests assert every modeled Java field and the accepted-player exclusion.

## Validation Result

- Focused C# result: Passed, 87 total, 0 failed, 0 skipped.
- First focused run failed at compile time because the test fixture tried to set init-only `PlayerClass` after construction; the fixture was changed to use an object initializer and the same command passed.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmLegionMemberList_WritesJavaLastChunkPayload` | Unit | `SM_LEGION_MEMBERLIST.writeImpl` | First flag, negative last-chunk count, and per-member row fields in Java order. | Packet serialization assertions from Java source review. | No Java golden fixture. |
| `HandleQuestionResponseAsync_LegionInviteAcceptPersistsMemberMutatesStateAndBroadcastsLikeJava` | Unit | `LegionService.addLegionMember` member-list call | Accepted player receives a member-list packet containing online same-legion members and excluding the accepted player. | Live handler and packet payload assertions. | Online registry slice only; offline/cache roster and 80-row chunking remain gaps. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionMemberList` | Server Packet | Partial | Unit Tested | Partial Parity | Modeled row fields and first/last count behavior are tested. Housing ids default to zero and no Java golden fixture exists. |
| `com.aionemu.gameserver.services.LegionService.updateLegionMemberList` invite path | `GameServerConnection.SendLegionInviteMemberListAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Sends one current-online roster chunk to the accepted player and excludes the accepted player. Full Java roster cache, offline members, and 80-row split behavior are not ported. |
| `com.aionemu.gameserver.model.team.legion.Legion.canAddMember` | no C# invite-acceptance member-limit guard yet | Runtime State Guard | Not Started | No Tests | Unknown | Java checks max members by legion level before adding; C# still persists first without this guard. |

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported or advanced in this UOW: 2 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 2
- Total blocked/not-started artifacts: 2 acceptance-path gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Member list currently includes only online same-legion players visible through the C# connection registry; Java uses the full legion member cache including offline members.
- Java splits member lists into chunks of 80; this C# invite path currently sends a single chunk.
- Member-list house address and door-state ids are currently zero because the C# invite path does not query active houses for roster rows.
- C# still lacks Java's `Legion.canAddMember` max-member guard before invite-acceptance persistence.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- No Java golden packet fixture was generated for `SM_LEGION_MEMBERLIST`.
