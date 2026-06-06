# Phase 6 Session 2773 Completion

## Unit of Work

[Phase 6][UOW-2773] Send legion add-member packet on invite acceptance

## Runtime Progress Gate

- Deferred/live behavior advanced: live guild invite acceptance persisted and mutated the responder, but online legion members received C#'s existing `SM_LEGION_UPDATE_MEMBER` approximation instead of Java's `SM_LEGION_ADD_MEMBER` fanout.
- Java source of truth: `LegionService.addLegionMember` broadcast path and `SM_LEGION_ADD_MEMBER.writeImpl`.
- C# runtime artifact wired/fixed: new `SmLegionAddMember` server packet and `GameServerConnection.AcceptLegionInviteAsync` online-legion fanout.
- Client-visible/state/persistence effect changed: clients on the live invite-acceptance path now receive packet opcode `111` with Java add-member fields for the joining player, including name, rank, new-member flag, class id, level, map id, game-server id, message id, and text.
- Why this is not preview-only/test-only/documentation-only: it sends a real server packet from live invite-acceptance code and changes the client-visible packet contract for online legion members.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_ADD_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## C# Runtime Changes

- Added `SmLegionAddMember` with Java opcode `111`.
- Serialized the Java packet fields in `SM_LEGION_ADD_MEMBER.writeImpl` order.
- Swapped invite-acceptance fanout from `SmLegionUpdateMember` to `SmLegionAddMember`.
- Preserved Java invite semantics: `isMember` is `false`, message id is `1300260`, and text is the joining player's name.

## Validation Decision

- Changed surface: server-packet serialization and live invite-acceptance packet fanout.
- Specific behavior/contract: Java `LegionService.addLegionMember` broadcasts `new SM_LEGION_ADD_MEMBER(player, false, 1300260, player.getName())` to the legion after sending the joining player `SM_LEGION_INFO`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionAddMember" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `SM_LEGION_ADD_MEMBER` serialization.
- Broad-validation trigger: live packet fanout was changed.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly asserted the new packet shape plus live invite-acceptance fanout.
- Why this scope is sufficient: the change is isolated to one packet class and one live fanout call; the focused tests assert the Java field order and the live handler's packet selection.

## Validation Result

- Focused C# result: Passed, 86 total, 0 failed, 0 skipped.
- First focused run failed at compile time because the direct packet test attempted to mutate init-only `WorldPosition.WorldId`; the fixture was corrected to assign a new `WorldPosition` and the same command passed.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmLegionAddMember_WritesJavaInviteAcceptanceShape` | Unit | `SM_LEGION_ADD_MEMBER.writeImpl` | Opcode payload fields for object id, name, rank id, new-member flag, class id, live player level, map id, game-server id, message id, and text. | Packet serialization assertions derived from Java source review. | No Java golden fixture. |
| `HandleQuestionResponseAsync_LegionInviteAcceptPersistsMemberMutatesStateAndBroadcastsLikeJava` | Unit | `LegionService.addLegionMember` invite fanout | Invite acceptance now fans out `SmLegionAddMember` to online legion members and no longer sends `SmLegionUpdateMember` from this path. | Live handler and packet-type assertions. | Member-list update and emblem fanout remain separate gaps. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_ADD_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionAddMember` | Server Packet | Complete for modeled fields | Unit Tested | Partial Parity | Field order and invite-acceptance constructor values are tested. No Java golden packet fixture. |
| `com.aionemu.gameserver.services.LegionService.addLegionMember` add-member broadcast | `GameServerConnection.AcceptLegionInviteAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Live invite acceptance now sends `SmLegionAddMember`; member-list update, emblem fanout, aggregate member cache, member-limit behavior, and bonus behavior remain gaps. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` opcode `111` | `SmLegionAddMember.PacketOpCode` | Packet Opcode | Complete for this packet | Unit Tested through serialization | Partial Parity | Opcode is wired through the packet class; no generated opcode registry check. |

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported or advanced in this UOW: 3 packet/runtime artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked/not-started artifacts: 4 acceptance-path gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still does not send Java's invite-acceptance `SM_LEGION_UPDATE_EMBLEM` fanout.
- C# still lacks Java's `updateLegionMemberList(player, false, player.getObjectId())` behavior for invite acceptance.
- C# still lacks a full Java-like `Legion` aggregate/member-list update and member-limit check for acceptance.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- No Java golden packet fixture was generated for `SM_LEGION_ADD_MEMBER`.
