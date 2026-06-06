# Phase 6 Session 2778 Completion

## Unit of Work

[Phase 6][UOW-2778] Chunk legion invite member list packets

## Runtime Progress Gate

- Deferred/live behavior advanced: live legion invite acceptance loaded persisted roster rows but still sent them as one `SM_LEGION_MEMBERLIST` packet.
- Java source of truth: `LegionService.updateLegionMemberList`, `FixedElementCountSplitList`, `ListPart.isFirst/isLast`, and `SM_LEGION_MEMBERLIST.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.SendLegionInviteMemberListAsync` packet fanout and focused invite acceptance tests.
- Client-visible/state/persistence effect changed: accepted clients with more than 80 visible roster rows now receive multiple Java-shaped member-list packets with correct first/last flags and signed counts.
- Why this is not preview-only/test-only/documentation-only: it changes the live server-packet fanout from invite acceptance and sends real roster packets from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/utils/collections/FixedElementCountSplitList.java`
- `game-server/src/com/aionemu/gameserver/utils/collections/SplitList.java`
- `game-server/src/com/aionemu/gameserver/utils/collections/ListPart.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`

## C# Runtime Changes

- Added Java-sized 80-row chunking to the invite acceptance member-list send path.
- Preserved Java's one empty first/last packet behavior when the roster is empty.
- Preserved `SM_LEGION_MEMBERLIST` signed count semantics: non-last chunks write a positive count and last chunks write a negative count.
- Added a live handler test with 82 roster rows after accepted-player exclusion, proving an 80-row first chunk and a 2-row last chunk.

## Validation Decision

- Changed surface: live invite-acceptance packet fanout.
- Specific behavior/contract: Java uses `FixedElementCountSplitList<>(allMembers, true, 80)` and sends each part as `SM_LEGION_MEMBERLIST(part, part.isFirst(), part.isLast())`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionMemberList" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this chunking path.
- Broad-validation trigger: none. The change is isolated to a live packet fanout loop and does not alter shared packet serialization primitives.
- Broad .NET decision: skipped because the focused command compiles the touched project and asserts the live handler packet split behavior directly.
- Why this scope is sufficient: the test exercises the live invite acceptance path, packet filtering, chunk counts, first/last flags, and serialized row payloads.

## Validation Result

- Focused C# result: Passed, 89 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_LegionInviteAcceptChunksMemberListAtJavaSize` | Unit | `LegionService.updateLegionMemberList`, `FixedElementCountSplitList`, and `SM_LEGION_MEMBERLIST.writeImpl` | Live invite acceptance sends 80-row first chunk and 2-row last chunk after accepted-player exclusion, with Java signed-count behavior. | Live handler packet assertions from Java source review. | Does not include active-house fields; no Java golden fixture. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.updateLegionMemberList` | `Aion.GameServer.Network.Aion.GameServerConnection.SendLegionInviteMemberListAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Uses persisted roster rows, accepted-player exclusion, and Java 80-row chunking. House lookup remains missing. |
| `com.aionemu.gameserver.utils.collections.FixedElementCountSplitList` | `GameServerConnection.SplitLegionMemberList` | Utility Behavior | Partial | Unit Tested through live handler | Partial Parity | Models the 80-row and empty-list behavior needed by live legion member-list sends only; not a general reusable splitter port. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionMemberList` | Server Packet | Partial | Unit Tested | Partial Parity | First/last count behavior is exercised through chunked live sends. Housing row fields remain zero. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 3 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked/not-started artifacts: 3 roster/bonus/invite gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Member-list house address and door-state ids are still zero because C# does not query active houses for roster rows here.
- C# still does not model Java's full in-memory `Legion` aggregate and `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Invite eligibility still has broader Java gaps such as other-faction invite config and `DeniedStatus.GUILD` behavior.
- No Java golden fixture was generated for chunked `SM_LEGION_MEMBERLIST` packets.
