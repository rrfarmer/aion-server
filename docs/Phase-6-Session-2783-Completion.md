# Phase 6 Session 2783 Completion

## Unit of Work

[Phase 6][UOW-2783] Wire legion bonus deactivation on member loss

## Runtime Progress Gate

- Deferred/live behavior advanced: C# now executes Java's legion bonus removal path for explicit live leave and kick member-loss flows.
- Java source of truth: `Legion.removeBonus`, `LegionService.removeLegionMember`, `LegionService.leaveLegion`, and `LegionService.kickMember`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionLeaveAsync`, `HandleLegionKickMemberAsync`, `RemoveLegionBonusIfEligibleAsync`, and active-player-aware legion bonus packet fanout.
- Client-visible/state/persistence effect changed: when leave or kick drops online legion membership below ten, C# clears runtime bonus state and sends `SM_ICON_INFO(1, false)` to affected online members.
- Why this is not preview-only/test-only/documentation-only: it mutates live legion runtime state and emits real icon-off server packets from live leave/kick handlers.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ICON_INFO.java`

## C# Runtime Changes

- Added bonus deactivation after successful self-leave and kick flows.
- Sent the Java-equivalent icon-off packet to the leaving/kicked online player when the runtime legion bonus is active.
- Added active-player-aware online legion member collection so activation/deactivation counts and packet fanout include the active connection even when it is not represented in the registry enumeration.
- Reused `LegionBonusRuntime.TryDeactivate` to clear state once online membership falls below the Java threshold.

## Validation Decision

- Changed surface: live legion leave/kick handlers, shared legion bonus runtime state, and `SmIconInfo` fanout.
- Specific behavior/contract: Java `Legion.removeBonus` clears bonus below ten online members and sends `SM_ICON_INFO(1, false)` to remaining online members after member reset; Java also sends icon-off to an online removed member if the bonus was active.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GamePacket" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java fixture exists for `Legion.removeBonus`.
- Broad-validation trigger: live packet and runtime state mutation.
- Broad .NET decision: skipped. The focused command compiles the affected project and exercises packet shape plus live invite, leave, and kick bonus paths.

## Validation Result

- First focused C# run: failed 1 test because the prior activation test expected the active player's icon packet in registry captures; after active-player-aware fanout, the packet is correctly sent through the active connection observer.
- Final focused C# result: Passed, 383 total, 0 failed, 0 skipped.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_KickOnlineMemberDeactivatesLegionBonusBelowJavaThreshold` | Unit | `LegionService.kickMember`, `removeLegionMember`, and `Legion.removeBonus` | Kicking an online member below the threshold sends icon-off to the kicked member, active kicker, remaining members, and clears runtime state. | Live handler side-effect assertions and runtime-state assertion. | Logout deactivation remains open. |
| `HandleInfrastructurePacketAsync_LeaveDeactivatesLegionBonusBelowJavaThreshold` | Unit | `LegionService.leaveLegion`, `removeLegionMember`, and `Legion.removeBonus` | Self-leave below the threshold sends icon-off to the leaving player and remaining online members, then clears runtime state. | Live handler side-effect assertions and runtime-state assertion. | Logout deactivation remains open. |
| `HandleQuestionResponseAsync_LegionInviteAcceptActivatesOnlineBonusAtJavaThreshold` | Unit Update | `Legion.addBonus` | Active player icon-on delivery now reflects active connection fanout instead of registry-only delivery. | Live handler side-effect assertions. | Login sync remains open. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.legion.Legion.removeBonus` | `Aion.GameServer.Network.Aion.GameServerConnection.RemoveLegionBonusIfEligibleAsync` plus `LegionBonusRuntime.TryDeactivate` | Runtime State/Packet Fanout | Partial | Unit Tested | Partial Parity | Explicit live leave/kick deactivation is wired; logout deactivation is still missing. |
| `com.aionemu.gameserver.services.LegionService.removeLegionMember` | `GameServerConnection.HandleLegionLeaveAsync` and `HandleLegionKickMemberAsync` | Live Handler | Partial | Unit Tested | Partial Parity | Icon-off ordering/state reset is modeled for explicit leave/kick; full Java aggregate/cache behavior remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ICON_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmIconInfo` | Server Packet | Ported | Unit Tested | Partial Parity | Icon-on and icon-off packet fanout use the existing packet; no Java golden fixture. |

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported or advanced in this UOW: 4 runtime/handler/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked/not-started artifacts: 2 legion bonus runtime paths remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java `LegionService.onLogout` bonus deactivation is not yet wired.
- Java `LegionService.onLogin` icon sync and activation-on-login are not yet wired.
- `Rates.calcXpRate` XP bonus consumption is not yet connected to C# runtime bonus state.
- No Java golden or runtime comparison fixture was generated for `SM_ICON_INFO`.
