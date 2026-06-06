# Phase 6 Session 2774 Completion

## Unit of Work

[Phase 6][UOW-2774] Broadcast legion emblem on invite acceptance

## Runtime Progress Gate

- Deferred/live behavior advanced: live guild invite acceptance sent `SM_LEGION_ADD_MEMBER`, but still skipped Java's immediate visible `SM_LEGION_UPDATE_EMBLEM` fanout.
- Java source of truth: `LegionService.addLegionMember` visible broadcast of `SM_LEGION_UPDATE_EMBLEM` and `SM_LEGION_UPDATE_EMBLEM.writeImpl`.
- C# runtime artifact wired/fixed: existing `SmLegionUpdateEmblem`, `GameServerConnection.AcceptLegionInviteAsync`, copied responder legion emblem fields, and visible-player broadcast registry path.
- Client-visible/state/persistence effect changed: after invite acceptance, visible clients including the accepted player now receive the Java-shaped emblem update packet carrying legion id, emblem id, emblem type, and color bytes.
- Why this is not preview-only/test-only/documentation-only: it sends a real server packet from live invite-acceptance code and changes visible client emblem state.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_EMBLEM.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`

## C# Runtime Changes

- Added `BroadcastLegionInviteEmblemAsync` to send `SmLegionUpdateEmblem` around the accepted player with `includeSourcePlayer: true`.
- Wired the emblem broadcast immediately after `SmLegionAddMember` fanout and before edit/title updates in the invite-acceptance path.
- Used responder legion emblem fields after `BindLegionInviteAcceptedPlayer` copies them from the inviter.

## Validation Decision

- Changed surface: live invite-acceptance visible packet fanout.
- Specific behavior/contract: Java `LegionService.addLegionMember` broadcasts `new SM_LEGION_UPDATE_EMBLEM(legion.getLegionId(), legionEmblem)` around the newly added player with source included.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~CmLegionSendEmblemInfoTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this acceptance broadcast.
- Broad-validation trigger: live packet fanout was changed.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly asserted the live handler broadcast plus existing emblem packet serialization.
- Why this scope is sufficient: the change is isolated to one live broadcast call using an already ported packet whose payload is covered by adjacent emblem tests.

## Validation Result

- Focused C# result: Passed, 101 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_LegionInviteAcceptPersistsMemberMutatesStateAndBroadcastsLikeJava` | Unit | `LegionService.addLegionMember` emblem broadcast and `SM_LEGION_UPDATE_EMBLEM.writeImpl` | Invite acceptance now emits a visible `SmLegionUpdateEmblem` broadcast sourced from the accepted player with source included and copied legion emblem bytes. | Live handler assertion plus packet payload reader. | Does not prove real known-list recipient selection beyond the registry contract. |
| `SmLegionUpdateEmblem_WritesJavaPayload` | Unit | `SM_LEGION_UPDATE_EMBLEM.writeImpl` | Existing adjacent test covers emblem packet field order. | Packet serialization assertions. | No Java golden fixture. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_EMBLEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionUpdateEmblem` | Server Packet | Complete for modeled fields | Unit Tested | Partial Parity | Packet field order is tested; no Java golden fixture. |
| `com.aionemu.gameserver.services.LegionService.addLegionMember` emblem broadcast | `GameServerConnection.AcceptLegionInviteAsync` / `BroadcastLegionInviteEmblemAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Invite acceptance now sends add-member, emblem, edit, and title packets. Member-list, aggregate cache, member limit, and bonus behavior remain gaps. |

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported or advanced in this UOW: 2 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 2
- Total blocked/not-started artifacts: 3 acceptance-path gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still lacks Java's `updateLegionMemberList(player, false, player.getObjectId())` behavior for invite acceptance.
- C# still lacks a full Java-like `Legion` aggregate/member-list update and member-limit check for acceptance.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- No Java golden packet fixture was generated for `SM_LEGION_UPDATE_EMBLEM`.
