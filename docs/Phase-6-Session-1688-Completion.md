# Phase 6 Session 1688 Completion - Summon Release Notification Planner

Date: 2026-05-30
Unit of Work: UOW-1688
Status: Complete

## Scope

Add the missing non-live release notification planner for Java `SummonsService.ReleaseSummonTask.run`, including the summon-release `SM_SYSTEM_MESSAGE` factories that precede the already ported release packet sequence.

## Completed Work

- Added `SmSystemMessage.SkillSummonUnsummonByTooDistance()`.
- Added `SmSystemMessage.SkillSummonUnsummoned(string)`.
- Added `dotnetConversion/src/Aion.GameServer/Services/SummonReleaseNotificationPlanService.cs`.
- Added `SummonReleaseNotificationPlan`.
- Added `SummonReleaseNotificationPlanStatus`.
- Modeled Java release notification order:
  - `DISTANCE` -> `STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE()` then `SM_SUMMON_PANEL_REMOVE` then `SM_SUMMON_OWNER_REMOVE`
  - `COMMAND` and `UNSPECIFIED` -> `STR_SKILL_SUMMON_UNSUMMONED(summon.getL10n())` then `SM_SUMMON_PANEL_REMOVE` then `SM_SUMMON_OWNER_REMOVE`
  - `LOGOUT` -> skips the notification and packet trio
- Reused `SummonReleasePacketSequencePlanService` so the composed planner inherits the existing packet-order and primitive guards.
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/SummonReleaseNotificationPlanServiceTests.cs`.
- Extended `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` with the new summon-release system-message factories.

## Validation

Executed:

Focused Aion.GameServer test run covering `SummonReleaseNotificationPlanServiceTests`, `SummonReleasePacketSequencePlanServiceTests`, `SmSummonPanelRemovePacketTests`, `SmSummonOwnerRemovePacketTests`, and `GamePacketTests`.

Result:

- 255 tests passed.
- Build succeeded for the focused test slice.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.summons.SummonsService.ReleaseSummonTask.run`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMONED`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_PANEL_REMOVE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_OWNER_REMOVE`

## Migration Parity Table - UOW-1688

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.summons.SummonsService.ReleaseSummonTask.run` | `Aion.GameServer.Services.SummonReleaseNotificationPlanService.CreatePlan` | Service Boundary | Partial | Unit Tested boundary only | Partial Parity | C# now models the non-live release notification branch ahead of the existing packet sequence for `DISTANCE`, `COMMAND`, and `UNSPECIFIED`, and skips the trio for `LOGOUT`. It still does not delete live summon/NPC objects, clear `master.summon`, set cooldowns, schedule hate transfer, or run inside live release scheduling. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.SkillSummonUnsummonByTooDistance` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover message id `1300073` and the no-parameter packet payload. No Java runtime/encrypted frame capture was produced. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMONED` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.SkillSummonUnsummoned` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover message id `1200006` and the summon-name parameter payload. No Java runtime/encrypted frame capture was produced. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `SummonReleaseNotificationPlan.PacketsInOrder` | Utility Boundary | Partial | Unit Tested boundary only | Needs Verification | C# records ordered send intent only. Recipient socket behavior, dispatch, encryption, exception handling, and runtime ordering relative to live release scheduling remain unverified. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_DistanceComposesTooDistanceMessageAheadOfReleasePackets` | `DISTANCE` branch emits the distance release message ahead of the panel-remove and owner-remove packets. | Reviewed Java `SummonsService.ReleaseSummonTask.run` | Unit | Does not execute live release task or socket dispatch |
| `CreatePlan_CommandLikeBranchesComposeUnsummonedMessageAheadOfReleasePackets` | `COMMAND` and `UNSPECIFIED` branches emit the summon-name release message ahead of the packet pair. | Reviewed Java `SummonsService.ReleaseSummonTask.run` | Unit | Does not execute live release task or localized summon-name generation |
| `CreatePlan_LogoutSkipsNotificationAndReleasePackets` | `LOGOUT` branch produces no notification or release packets. | Reviewed Java `SummonsService.ReleaseSummonTask.run` | Unit | Does not cover live logout cleanup |
| `CreatePlan_CommandLikeBranchesBlockEmptySummonName` | Blank summon names block notification planning for the Java parameterized message branch. | C# safety boundary around `summon.getL10n()` snapshot input | Unit | Java uses live localized summon names instead of primitive validation |
| `CreatePlan_BlocksNegativeSkillIdBeforeCompositeSend` | Negative skill ids block the composite send before packet emission. | C# safety boundary via existing release packet-sequence planner | Unit | Java relies on live summon state rather than primitive validation |
| `GamePacketTests` updated `SmSystemMessage_WritesDialogTooFarMessages` | New summon release system-message factories serialize the expected ids and parameter counts. | Reviewed Java `SM_SYSTEM_MESSAGE` factories | Regression | No Java runtime/encrypted frame capture |

## Risks / Gaps

- No live `SummonsService.release` or `ReleaseSummonTask` integration was added.
- The `scheduleOrRun()` `COMMAND` branch that sends `STR_SKILL_SUMMON_UNSUMMON_FOLLOWER(summon.getL10n())` and `SM_SUMMON_UPDATE` before delayed release is still not modeled in C#.
- Summon deletion, transformed NPC deletion, master summon clearing, cooldown mutation, scheduler delay, and hate-transfer behavior are not ported here.
- No Java runtime/encrypted frame capture was produced for the combined release notification + packet sequence.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 2 `SmSystemMessage` factories, 1 notification planner, 1 plan record, 1 status enum, and 6 focused regressions/updates.
- Total artifacts with verified parity: 2 grouped rows (`STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE`, `STR_SKILL_SUMMON_UNSUMMONED` factories).
- Total artifacts needing verification: 1 grouped row explicitly marked Needs Verification; the main release boundary remains Partial Parity because live workflow integration and `scheduleOrRun()` follow-up behavior are intentionally deferred.
- Total blocked artifacts: live summon release integration, `scheduleOrRun()` command-release notification/update modeling, live packet dispatch/order verification, Java runtime/encrypted packet capture, scheduler/cooldown/hate-transfer behavior.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Add `SmSystemMessage.STR_SKILL_SUMMON_UNSUMMON_FOLLOWER(summon.getL10n())` parity and a non-live `scheduleOrRun()` `COMMAND` release planner that composes that warning message ahead of the existing `SM_SUMMON_UPDATE` send-to-master intent.
- Otherwise capture Java runtime/golden vectors for the release notification + packet sequence if a deterministic harness is available.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SummonReleaseNotificationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SummonReleaseNotificationPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1688-Completion.md`
- `docs/Phase-6-Session-1688-Handoff.md`