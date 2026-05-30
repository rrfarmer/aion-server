# Phase 6 Session 1687 Completion - Summon Release Packet Sequence Planner

Date: 2026-05-30
Unit of Work: UOW-1687
Status: Complete

## Scope

Add a conservative non-live planner for Java `SummonsService.ReleaseSummonTask.run` that composes the already ported release packets in Java order and mirrors the `LOGOUT` skip branch.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/SummonReleasePacketSequencePlanService.cs`.
- Added `SummonReleasePacketSequencePlan`.
- Added `SummonReleaseUnsummonType`.
- Added `SummonReleasePacketSequencePlanStatus`.
- Modeled Java release packet order:
  - `COMMAND`, `DISTANCE`, and `UNSPECIFIED` -> `SM_SYSTEM_MESSAGE` then `SM_SUMMON_PANEL_REMOVE` then `SM_SUMMON_OWNER_REMOVE`
  - `LOGOUT` -> skips the master packet pair
- Reused `SummonPanelRemovePacketPlanService` and `SummonOwnerRemovePacketPlanService` so the composed planner inherits the existing packet-shape safety guards.
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/SummonReleasePacketSequencePlanServiceTests.cs`.

## Validation

Executed:

Focused Aion.GameServer test run covering `SummonReleasePacketSequencePlanServiceTests`, `SmSummonPanelRemovePacketTests`, and `SmSummonOwnerRemovePacketTests`.

Result:

- 9 tests passed.
- Build succeeded for the focused test slice.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.summons.SummonsService.ReleaseSummonTask.run`
- `com.aionemu.gameserver.model.summons.UnsummonType`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_PANEL_REMOVE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_OWNER_REMOVE`
- `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket`

## Migration Parity Table - UOW-1687

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.summons.SummonsService.ReleaseSummonTask.run` | `Aion.GameServer.Services.SummonReleasePacketSequencePlanService.CreatePlan` | Service Boundary | Partial | Unit Tested boundary only | Partial Parity | C# models the non-live packet order branch for `COMMAND`, `DISTANCE`, and `UNSPECIFIED`, and skips the pair for `LOGOUT`. It still does not create the preceding system message, delete live summon/NPC objects, clear `master.summon`, set cooldowns, schedule hate transfer, or run inside live release scheduling. |
| `com.aionemu.gameserver.model.summons.UnsummonType` | `Aion.GameServer.Services.SummonReleaseUnsummonType` | Enum / Branch Control | Partial | Unit Tested boundary only | Needs Verification | C# mirrors the Java branch labels used by `ReleaseSummonTask.run`, but live callers, serialization, and controller integration remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `SummonReleasePacketSequencePlan.PacketsInOrder` | Utility Boundary | Partial | Unit Tested boundary only | Needs Verification | C# records ordered send intent only. Recipient socket behavior, packet dispatch, encryption, exception handling, and runtime ordering relative to system messages remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_PANEL_REMOVE` + `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_OWNER_REMOVE` | `SummonReleasePacketSequencePlan.PacketsInOrder` | Packet Composition | Complete inputs reused | Unit Tested composition only | Needs Verification | This unit reuses already ported packet shapes and verifies that the composed non-live sequence preserves Java order. No Java runtime capture of the combined release sequence was produced. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_ComposesPanelRemoveThenOwnerRemoveInJavaOrder` | Planner emits `SM_SUMMON_PANEL_REMOVE` before `SM_SUMMON_OWNER_REMOVE` for `COMMAND`, `DISTANCE`, and `UNSPECIFIED`. | Reviewed Java `SummonsService.ReleaseSummonTask.run` | Unit | Does not execute live release task, system message send, or socket dispatch |
| `CreatePlan_LogoutSkipsReleasePackets` | `LOGOUT` branch produces no master packet pair. | Reviewed Java `SummonsService.ReleaseSummonTask.run` | Unit | Does not cover live logout cleanup |
| `CreatePlan_BlocksNegativeSkillIdBeforeSequenceCreation` | Negative skill ids block sequence creation before packet ordering. | C# safety boundary via existing panel-remove planner | Unit | Java relies on live summon state rather than primitive validation |
| `CreatePlan_BlocksInvalidSummonObjectIdBeforeSequenceCreation` | Non-positive summon object ids block sequence creation. | C# safety boundary via existing owner-remove planner | Unit | Java requires a live summon with a real object id |

## Risks / Gaps

- No live `SummonsService.release` or `ReleaseSummonTask` integration was added.
- The preceding `SM_SYSTEM_MESSAGE` branch for distance vs non-distance release is still not modeled in C#.
- Summon deletion, transformed NPC deletion, master summon clearing, cooldown mutation, scheduler delay, and hate-transfer behavior are not ported here.
- `PacketSendUtility.sendPacket` behavior remains intent-only and unverified.
- No Java runtime/encrypted frame capture was produced for the combined release packet sequence.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 packet-sequence planner, 1 plan record, 2 enums, and 4 focused regressions.
- Total artifacts with verified parity: 0 new grouped rows in this unit.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; the main release boundary remains Partial Parity because live workflow integration and system-message composition are intentionally deferred.
- Total blocked artifacts: live summon release integration, release system-message modeling, live packet dispatch/order verification, Java runtime/encrypted packet capture, scheduler/cooldown/hate-transfer behavior.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Add `SmSystemMessage` factories and a non-live release notification planner for `STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE()` vs `STR_SKILL_SUMMON_UNSUMMONED(summon.getL10n())`, composed ahead of the existing release packet sequence.
- Otherwise capture Java runtime/golden vectors for the summon release packet order if a deterministic harness is available.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/SummonReleasePacketSequencePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SummonReleasePacketSequencePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1687-Completion.md`
- `docs/Phase-6-Session-1687-Handoff.md`