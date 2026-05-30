# Phase 6 Session 1690 Completion - Summon Mode Change Planner

Date: 2026-05-30
Unit of Work: UOW-1690
Status: Complete

## Scope

Add non-live `SummonModeChangePlanService` covering Java `SummonsService.restMode`, `guardMode`, `attackMode`, and `setUnkMode` packet composition, plus the four missing `SmSystemMessage` factories those methods require.

## Completed Work

- Added `SmSystemMessage.SkillSummonAttackMode(string)` (messageId `1200008`, `%0 is in Attack mode.`).
- Added `SmSystemMessage.SkillSummonGuardMode(string)` (messageId `1200009`, `%0 is in Guard mode.`).
- Added `SmSystemMessage.SkillSummonRestMode(string)` (messageId `1200010`, `%0 is in Resting mode.`).
- Added `SmSystemMessage.SkillSummonAlreadyHaveAFollower()` (messageId `1300072`, `You already have a spirit following you.`).
- Added `dotnetConversion/src/Aion.GameServer/Services/SummonModeChangePlanService.cs`.
- Added `SummonModeChangePlan`, `SummonModeChangeType`, `SummonModeChangePlanStatus`.
- Modeled Java mode-change packet order:
  - `REST` → `STR_SKILL_SUMMON_REST_MODE(L10n)` then `SM_SUMMON_UPDATE`
  - `GUARD` → `STR_SKILL_SUMMON_GUARD_MODE(L10n)` then `SM_SUMMON_UPDATE`
  - `ATTACK` → `STR_SKILL_SUMMON_ATTACK_MODE(L10n)` then `SM_SUMMON_UPDATE`
  - `UNK` → `SM_SUMMON_UPDATE` only (no system message, no summon-name requirement)
- Documented deferred live side effects in Java source breadcrumbs: `cancelCurrentSkill(null)`, `setMode(...)`, `triggerRestoreTask()` (REST/GUARD), `cancelRestoreTask()` (ATTACK).
- Reused `SummonUpdatePacketPlanService` for the `SM_SUMMON_UPDATE` portion so existing validation guards propagate.
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/SummonModeChangePlanServiceTests.cs`.
- Extended `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` with the four new system-message factory assertions.

## Validation

Executed:

Focused test run covering `SummonModeChangePlanServiceTests`, `SummonCommandReleaseSchedulePlanServiceTests`, and `GamePacketTests`.

Result:

- 257 tests passed.
- Build succeeded for the focused test slice.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.summons.SummonsService.restMode`
- `com.aionemu.gameserver.services.summons.SummonsService.guardMode`
- `com.aionemu.gameserver.services.summons.SummonsService.attackMode`
- `com.aionemu.gameserver.services.summons.SummonsService.setUnkMode`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ATTACK_MODE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_GUARD_MODE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_REST_MODE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER`

## Migration Parity Table - UOW-1690

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.summons.SummonsService.restMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Rest, ...)` | Service Boundary | Partial | Unit Tested boundary only | Partial Parity | C# models the non-live REST message → `SM_SUMMON_UPDATE` intent. Live `cancelCurrentSkill`, `setMode(REST)`, and `triggerRestoreTask` remain deferred. |
| `com.aionemu.gameserver.services.summons.SummonsService.guardMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Guard, ...)` | Service Boundary | Partial | Unit Tested boundary only | Partial Parity | C# models the non-live GUARD message → `SM_SUMMON_UPDATE` intent. Live `cancelCurrentSkill`, `setMode(GUARD)`, and `triggerRestoreTask` remain deferred. |
| `com.aionemu.gameserver.services.summons.SummonsService.attackMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Attack, ...)` | Service Boundary | Partial | Unit Tested boundary only | Partial Parity | C# models the non-live ATTACK message → `SM_SUMMON_UPDATE` intent. Live `setMode(ATTACK)` and `cancelRestoreTask` remain deferred. |
| `com.aionemu.gameserver.services.summons.SummonsService.setUnkMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Unk, ...)` | Service Boundary | Partial | Unit Tested boundary only | Partial Parity | C# models the non-live UNK `SM_SUMMON_UPDATE` intent. Live `setMode(UNK)` remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ATTACK_MODE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.SkillSummonAttackMode` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; messageId `1200008` and one string parameter. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_GUARD_MODE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.SkillSummonGuardMode` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; messageId `1200009` and one string parameter. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_REST_MODE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.SkillSummonRestMode` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; messageId `1200010` and one string parameter. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.SkillSummonAlreadyHaveAFollower` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; messageId `1300072`, no parameters. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_ComposesModeModeMessageAheadOfSummonUpdate` (theory for REST/GUARD/ATTACK) | Correct message id, correct packet order, correct `ShouldSendToMaster` for each mode-change type. | Reviewed Java `SummonsService.restMode/guardMode/attackMode` | Unit | Does not execute live mode change, `setMode`, life-stats tasks, or socket dispatch |
| `CreatePlan_UnkModeSkipsModeMessageAndSendsOnlySummonUpdate` | UNK produces only `SM_SUMMON_UPDATE`, no system message, no summon-name requirement. | Reviewed Java `SummonsService.setUnkMode` | Unit | Does not cover live `setMode(UNK)` |
| `CreatePlan_BlocksEmptySummonNameForModesThatRequireIt` (theory for REST/GUARD/ATTACK) | Empty summon names block planning for modes that need a localized name. | C# safety guard matching Java `summon.getL10n()` requirement | Unit | Java uses live localized names |
| `CreatePlan_BlocksInvalidSummonUpdateSnapshotBeforeCompositeSend` (theory for all modes) | Invalid snapshot blocks planning before any packet emission. | C# safety guard via existing `SummonUpdatePacketPlanService` | Unit | Java relies on live summon state |
| `CreatePlan_RecordsDeferredLifeStatsCallInJavaSource` (theory for REST/GUARD/ATTACK) | Java source breadcrumbs name the deferred `triggerRestoreTask`/`cancelRestoreTask` calls. | Reviewed Java `SummonsService.restMode/guardMode/attackMode` | Unit | No live life-stats integration |
| `GamePacketTests` new `SmSystemMessage` assertions | All four new factories serialize expected message ids and parameter counts. | Reviewed Java `SM_SYSTEM_MESSAGE` source | Regression | No Java runtime/encrypted frame capture |

## Risks / Gaps

- Live mode dispatch (`cancelCurrentSkill`, `setMode`, `triggerRestoreTask`, `cancelRestoreTask`) is not ported.
- `SummonsService.doMode` dispatch and `summon.cancelReleaseTask()` for non-release commands are not modeled.
- `SummonsService.createSummon` spawn + packet sequence is not ported.
- No Java runtime/encrypted frame capture was produced for any mode-change packet sequence.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped rows in this unit.
- Total artifacts ported: 4 system message factories, 1 mode change planner, 1 plan record, 2 enums, 10 focused tests/regressions.
- Total artifacts with verified parity: 4 grouped rows (all system message factories).
- Total artifacts needing verification: 4 grouped rows (service boundaries, Partial Parity).
- Total blocked artifacts: live mode/life-stats dispatch, `doMode` dispatch, `createSummon` spawn flow.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Add a non-live `SummonDoModePlanService` covering Java `SummonsService.doMode` dispatch: dead-guard, null-master guard, `cancelReleaseTask` for non-release COMMAND transitions, and mode-dispatch routing to the existing mode planners.
- Alternatively, model Java `SummonsService.createSummon` spawn packet sequence: `STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER` guard, `SM_SUMMON_PANEL`, `SM_EMOTION(CHANGE_SPEED)` broadcast, and `SM_SUMMON_UPDATE` broadcast — still non-live (no `VisibleObjectSpawner`).
- Or continue the next independent non-live boundary from any other gameplay area (e.g., broker live-readiness isolation, bind-point live Kinah deduction, or a NPC dialog packet boundary).

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SummonModeChangePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SummonModeChangePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1690-Completion.md`
- `docs/Phase-6-Session-1690-Handoff.md`
