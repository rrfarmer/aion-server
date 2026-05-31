# Phase 6 Session 1798 Completion - Add Craft Runtime Packet Surface

Date: 2026-05-30
Unit of Work: UOW-1798
Status: Complete

## Scope

Port the missing Java craft runtime packet surface and the narrow non-live `CraftingTask` packet-sequence boundary without widening into scheduler, opcode, XP, or cooldown work.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCraftAnimation.cs`:
  - mirrors Java `SM_CRAFT_ANIMATION`
  - uses opcode `180`
  - serializes `playerObjId`, `targetObjectId`, `skillId`, `action`
- Added `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCraftUpdate.cs`:
  - mirrors Java `SM_CRAFT_UPDATE`
  - uses opcode `181`
  - serializes action-specific message ids and localized item-name payloads
  - preserves the Java morph delay override forcing `1000ms` when `skillId == 40009`
- Added `dotnetConversion/src/Aion.GameServer/Services/CraftingTaskPacketPlanService.cs`:
  - added non-live start, progress, abort, failure-finish, and non-crit success-finish packet planners
  - preserved Java self/broadcast packet ordering and Java branch-specific action ids
- Added `dotnetConversion/tests/Aion.GameServer.Tests/CraftingTaskPacketPlanServiceTests.cs`:
  - verifies initial start ordering
  - verifies combo restart action `3`
  - verifies progress-action timing forwarding
  - verifies abort/failure/success finish packet branches
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`:
  - added direct serialization coverage for `SmCraftAnimation`
  - added direct serialization coverage for `SmCraftUpdate`
  - asserted Java action/message mapping and morph delay override

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftingTaskPacketPlanServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_ExtractMissingRewardTemplateFailsWithoutMutation" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TargetReachedAsync_SchedulesBroadcastAfterRestTime|FullyQualifiedName~HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Result:

- Focused craft packet/planner validation passed with 257 tests.
- The first full-suite attempt hit the command timeout boundary before a final result was produced.
- The second full-suite attempt failed in unrelated `HandleUseItemAsync_ExtractMissingRewardTemplateFailsWithoutMutation`; that isolated rerun passed with 1 test.
- The third full-suite attempt failed in unrelated `TargetReachedAsync_SchedulesBroadcastAfterRestTime` and `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag`; their isolated rerun passed with 2 tests.
- The final full-suite rerun passed cleanly with 4798 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4591` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.skillengine.task.CraftingTask`
- `com.aionemu.gameserver.skillengine.task.AbstractCraftTask`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_UPDATE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_ANIMATION`
- `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes`

## Migration Parity Table - UOW-1798

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `SM_CRAFT_ANIMATION` | `SmCraftAnimation` | Packet / Serialization | Complete | Regression Tested | Verified Parity | Opcode `180` and payload order are directly represented and packet-tested. |
| `SM_CRAFT_UPDATE` | `SmCraftUpdate` | Packet / Serialization | Complete | Regression Tested | Verified Parity | Opcode `181`, message mapping, localized item-name payloads, and morph delay override are directly represented and packet-tested. |
| `CraftingTask.onInteractionStart` | `CraftingTaskPacketPlanService.CreateInteractionStartPlan` | Deterministic Packet-Sequence Planner | Partial | Unit Tested | Partial Parity | Start ordering and combo restart action `3` are represented, but no live task runtime exists yet. |
| `CraftingTask.sendInteractionUpdate` | `CraftingTaskPacketPlanService.CreateProgressUpdatePlan` | Deterministic Packet-Sequence Planner | Partial | Unit Tested | Partial Parity | Progress action ids and timing fields are forwarded, but no live callback wiring exists yet. |
| `CraftingTask.onInteractionAbort` / `onFailureFinish` / non-crit `onSuccessFinish` | `CraftingTaskPacketPlanService.CreateAbortPlan` / `CreateFailureFinishPlan` / `CreateSuccessFinishPlan` | Deterministic Packet-Sequence Planner | Partial | Unit Tested | Partial Parity | Java packet branches are source-shaped and tested, but still not consumed by a live craft runtime shell. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreateInteractionStartPlan_UsesJavaInitOrderingForFirstCraftStep` | First-step start packets preserve Java self/broadcast order. | Java `CraftingTask.onInteractionStart` | Unit | No live task dispatch. |
| `CreateInteractionStartPlan_UsesCritProcActionForComboRestart` | Combo restart uses Java action `3`. | Java `CraftingTask.onInteractionStart` combo branch | Unit | No crit-roll/runtime loop. |
| `CreateProgressUpdatePlan_UsesProvidedProgressActionAndTimings` | Progress action ids and timing fields flow into `SM_CRAFT_UPDATE`. | Java `CraftingTask.sendInteractionUpdate` | Unit | No live callback evidence. |
| `CreateAbortAndFinishPlans_MirrorJavaPacketBranches` | Abort, failure, and non-crit success packet branches match Java. | Java `CraftingTask.onInteractionAbort`, `onFailureFinish`, `onSuccessFinish` | Unit | Does not cover crit continuation into a new craft step. |
| `CharacterSelectionServerPackets_WriteJavaShapedPayloads` craft additions | `SmCraftAnimation` and `SmCraftUpdate` serialize Java-shaped payloads, including morph delay override. | Java `SM_CRAFT_ANIMATION`, `SM_CRAFT_UPDATE` | Regression | Does not prove live ordering. |

## Risks / Gaps

- C# still lacks live `CM_CRAFT` / `CraftingTask` runtime wiring.
- The crit-roll continuation branch is only represented as deterministic packet sequencing, not as a full random/runtime loop.
- Craft XP, player XP, recipe deletion, cooldown persistence, and craft logging remain unported.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 2 craft runtime packet classes, 1 packet-sequence planner, and 5 focused tests/regressions.
- Total artifacts with verified parity: 2 grouped rows.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: live craft runtime wiring and the broader `finishCrafting` XP/cooldown/log branches.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the smallest live Java crafting runtime shell that can consume `CraftingTaskPacketPlanService` and `CraftService.CreateFinishRewardPlan`, ideally the completion/abort/start packet application boundary before widening into skill XP or craft cooldown persistence.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `DropRegistrationService.calculateBoostDropRate`
  - return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCraftAnimation.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCraftUpdate.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftingTaskPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftingTaskPacketPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1798-Completion.md`
- `docs/Phase-6-Session-1798-Handoff.md`
