# Phase 6 Session 2785 Completion

## Completed UOW

[Phase 6][UOW-2785] Wire legion bonus deactivation on logout

## Runtime Progress Gate

- Deferred/live behavior advanced: live disconnect/leave-world now mirrors Java `LegionService.onLogout` by evaluating `Legion.removeBonus()` after the leaving player is no longer counted online.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/LegionService.java` `onLogout(Player)` and `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java` `removeBonus()`.
- C# runtime artifact wired: `GameServerConnection.LeavePlayerWorldAsync` after visible delete broadcast and connection-registry unregister.
- Client-visible/state effect: when the leaving member drops a legion below ten online members, remaining online legion members receive `SmIconInfo(1, false)` and `LegionBonusRuntime` clears the bonus state.
- Why this is runtime progress: this mutates live runtime state and sends real server packets from the live logout path.

## Implementation

- Added logout invocation of `RemoveLegionBonusIfEligibleAsync(player.LegionId, player.ObjectId)` after unregistering the active player connection.
- Extended the online-member collector with an optional excluded player id so logout parity counts remaining online members, while invite/login/leave/kick behavior keeps its existing active-player inclusion semantics.
- Reused the existing `SmIconInfo(1, false)` fanout path and `LegionBonusRuntime.TryDeactivate`.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeaveActivePlayerAsync_DeactivatesLegionBonusBelowJavaOnlineThresholdOnLogout` | Regression | Java source review: `LegionService.onLogout`, `Legion.removeBonus` | Live connection leave path excludes the leaving player, clears bonus below ten remaining online members, and sends icon-off to remaining members only | C# filtered runtime-path test | No Java golden fixture for `SM_ICON_INFO` |
| `LeaveActivePlayerAsync_KeepsLegionBonusWhenJavaOnlineThresholdRemainsAfterLogout` | Regression | Java source review: `Legion.removeBonus` threshold branch | Remaining count of ten keeps bonus active and sends no icon-off packets | C# filtered runtime-path test | No Java runtime comparison |

## Validation Decision

- Changed surface: live connection dispatch, packet fanout, runtime state.
- Specific behavior/contract: Java `Legion.removeBonus()` logout threshold and icon-off fanout.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorld" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; no narrow Java fixture exists for `SM_ICON_INFO` logout fanout.
- Broad-validation trigger: live disconnect packet fanout and runtime state mutation.
- Broad .NET decision: skipped unfiltered project/solution validation; the filtered command covers the edited connection test class and adjacent enter/leave-world service surface.
- Why this scope is sufficient: the tests invoke the actual connection leave helper used by shutdown and assert packet/state effects against the Java threshold branches.

Result: Passed, 212 total, 0 failed, 0 skipped. Existing nullable/analyzer warnings remain outside this UOW.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.LegionService` `onLogout(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.LeavePlayerWorldAsync` | Service/runtime path | Partial | Regression Tested | Partial Parity | Logout now triggers bonus removal after unregister; broader Java logout member-info/store ordering and all legion side effects are not fully verified here. |
| `com.aionemu.gameserver.model.team.legion.Legion` `removeBonus()` | `Aion.GameServer.Network.Aion.GameServerConnection.RemoveLegionBonusIfEligibleAsync` and `Aion.GameServer.Services.LegionBonusRuntime` | Runtime state/packet fanout | Partial | Regression Tested | Partial Parity | Threshold and icon-off fanout covered for leave/kick/logout; Java concurrency semantics use `compareAndSet`, C# runtime uses its own thread-safe state but full concurrent parity is not runtime-compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ICON_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmIconInfo` | Packet | Complete | Regression Tested | Partial Parity | Existing packet writer is used by live paths; no Java golden fixture exists for this packet. |

## Summary Metrics

- Total Java artifacts touched/discovered: 3.
- Total artifacts ported or wired this UOW: 2 runtime paths plus existing packet usage.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; this is a narrow Phase 6 runtime slice.

## Remaining Gaps

- XP-rate consumption of active legion bonus state remains missing.
- No Java golden fixture exists for `SM_ICON_INFO`.
- Full Java/C# concurrency parity for bonus activation/deactivation has not been runtime-compared.

