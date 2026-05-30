# Phase 6 Session 1777 Completion - CM_TUNE Runtime Decision Planner

Date: 2026-05-30
Unit of Work: UOW-1777
Status: Complete

## Scope

Port the narrow Java `CM_TUNE.runImpl` runtime decision boundary as a planner-only C# slice that consumes the newly loaded retuning metadata and delegates into the existing retuning guard planner without overstating live handler parity.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/CmTuneRuntimePlanService.cs` with:
  - `CmTuneRuntimePlanStatus`
  - `CmTuneResolvedTuningAction`
  - `CmTuneRuntimePlan`
  - Java-ordered branch planning for:
    - missing target item
    - unidentified target -> identify branch
    - identified target without a tuning scroll -> audit branch
    - missing tuning scroll
    - tuning scroll without retuning metadata
    - guard-blocked retuning action
    - successful execution handoff intent
- Kept the unit planner-only on purpose:
  - no `GameClientPacketFactory` registration
  - no `GameServerConnection` dispatch wiring
  - no live scheduler, observer, cooldown, inventory, persistence, or packet-send side effects
- Added `dotnetConversion/tests/Aion.GameServer.Tests/CmTuneRuntimePlanServiceTests.cs` with focused regression coverage for all Java branch categories in `CM_TUNE.runImpl`.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmTuneRuntimePlanServiceTests|FullyQualifiedName~TuningActionGuardPlanServiceTests|FullyQualifiedName~TuningActionExecutionPlanServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused retuning planner validation passed with 20 tests.
- The first full solution run reported one transient unrelated failure in `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate`.
- The isolated rerun of that test passed with 1 test.
- The second full solution rerun passed cleanly with 4726 tests total.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE`
- `com.aionemu.gameserver.model.templates.item.actions.TuningAction`
- `com.aionemu.gameserver.model.templates.item.actions.ItemActions.getTuningAction()`
- Existing Java retuning sources already reviewed in Sessions 1774-1776 for guard, execution, and metadata parity

## Migration Parity Table - UOW-1777

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE.runImpl` branch ordering | `Aion.GameServer.Services.CmTuneRuntimePlanService` | Runtime Decision Planner | Partial | Unit Tested | Partial Parity | C# now models the Java branch order and silent/audit branches, but the packet is not yet wired into the live connection path. |
| Java `CM_TUNE` identified-target audit branch | `CmTuneRuntimePlanStatus.AuditAlreadyIdentifiedWithoutScroll` | Audit / No-Op Boundary | Complete | Unit Tested | Verified Parity | The planner preserves the Java audit string exactly. |
| Java `CM_TUNE` handoff into `TuningAction.canAct(...)` using loaded scroll metadata | `CmTuneResolvedTuningAction` + `TuningActionGuardPlanService` delegation | Runtime Input Bridge | Complete | Unit Tested | Verified Parity | Loaded retuning metadata now feeds the guard planner in Java order. |
| Java `CM_TUNE` handoff into `TuningAction.act(...)` | `CmTuneRuntimePlanStatus.ExecuteTuning` + resolved action payload | Runtime Intent Boundary | Partial | Unit Tested | Partial Parity | Intent is modeled, but no live scheduler / packet dispatch / persistence integration exists yet. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_ReturnsNoTargetWhenLookupMisses` | Missing target item returns immediately. | Java `CM_TUNE.runImpl` source | Unit | No live inventory lookup |
| `CreatePlan_PrefersIdentifyBranchBeforeScrollHandling` | Unidentified targets take the identify branch before any scroll handling. | Java `CM_TUNE.runImpl` source | Unit | No live identify execution |
| `CreatePlan_AuditsIdentifiedTargetWithoutScroll` | Identified target with scroll id `0` takes the Java audit branch. | Java `CM_TUNE.runImpl` source | Unit | No live audit sink |
| `CreatePlan_ReturnsMissingScrollWhenObjectLookupFails` | Missing tuning scroll returns silently. | Java `CM_TUNE.runImpl` source | Unit | No live inventory lookup |
| `CreatePlan_ReturnsMissingActionWhenScrollTemplateHasNoTuningMetadata` | Scrolls without retuning metadata stop before guard evaluation. | Java `CM_TUNE.runImpl` + `ItemActions.getTuningAction()` | Unit | No live action binding |
| `CreatePlan_UsesGuardPlanWhenResolvedActionCannotAct` | Guard failures are delegated into the existing retuning guard planner. | Java `CM_TUNE.runImpl` + `TuningAction.canAct` | Unit | No live packet dispatch |
| `CreatePlan_ResolvesExecutableActionWithLoadedMetadata` | Successful plans carry resolved scroll/target/templates plus target type and `no_reduce`. | Java `CM_TUNE.runImpl` + `TuningAction` metadata | Unit | No live `action.act(...)` wiring |

## Risks / Gaps

- The planner is intentionally not live-wired yet, so client packets still do not traverse this path in production C#.
- The Java identify branch still lacks the adjacent C# non-live/live execution boundary.
- `CM_TUNE_RESULT` / `ItemActionService.applyTuneResult` remains unported, so the preview accept/cancel half of the retuning flow is still open.
- The first full-suite run again surfaced the same transient unrelated inventory-expansion flake; rerun evidence is documented explicitly.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 runtime planner service, 1 resolved-action DTO, 1 status surface, and 7 focused unit tests.
- Total artifacts with verified parity: 2 grouped rows.
- Total artifacts needing verification: 2 grouped rows.
- Total blocked artifacts: live `CM_TUNE` packet wiring, Java identify-item execution parity, and the `CM_TUNE_RESULT` / tune-apply runtime path.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the adjacent non-live `CM_TUNE_RESULT` / `ItemActionService.applyTuneResult` application boundary next so the current retuning planner chain can cover the full Java preview decision before any live packet wiring is attempted.
- Safe alternatives if a different isolated slice is preferred:
  - `ItemActionService.identifyItem` non-live delayed execution boundary
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmTuneRuntimePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmTuneRuntimePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1777-Completion.md`
- `docs/Phase-6-Session-1777-Handoff.md`
