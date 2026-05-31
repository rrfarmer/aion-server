# Phase 6 Session 1822 Handoff - Disabled Craft Start Executor Facade

Date: 2026-05-31
Unit of Work: UOW-1822
Status: Completed

## What Changed

- Added a disabled live craft-start executor facade.
- Added `CraftStartLiveExecutorFacadePlan`.
- Added `CraftStartLiveExecutorOperation`.
- Added `CraftStartLiveExecutorFacadeStatus`.
- Added `CraftStartLiveExecutorOperationKind`.
- Added `CraftStartLiveExecutorOperationStatus`.
- Added `CraftStartLiveExecutorFacadePlanService.CreateDisabledPlan(...)`.
- Recorded Java craft-start success boundaries without dispatching:
  - inventory mutation
  - inventory persistence state/write boundary
  - inventory packets
  - optional DP spend
  - `CraftingTask` creation
  - `CraftingTask.start`
- Added tests proving the disabled facade does not dispatch success side effects by default.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 318 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4570 tests.

## Known Gaps

- The executor facade is disabled and does not mutate inventory, send packets, write persistence, spend DP, create `CraftingTask`, or start scheduler work.
- Java storage delete quest callbacks/logging are still not executed.
- Java transaction behavior and object-id release after successful delete remain pending.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add exact Java SQL descriptor planning for craft inventory update/delete rows, including `InventoryDAO.DELETE_QUERY`, `InventoryDAO.UPDATE_QUERY`, delete-before-update grouping, and the known gap around object-id release after successful delete.

Safe alternative candidates:

- Add a live-safe craft finish cooldown application mutation plan.
- Add a disabled inventory packet send adapter for craft-start packet intent.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `InventoryDAO.UPDATE_QUERY`, `InventoryDAO.DELETE_QUERY`, `InventoryDAO.store`, C# `CraftStartInventoryPersistencePlan`, and current SQL descriptor patterns such as system mail persistence planning.
- Keep the next unit non-live unless it explicitly scopes and verifies database execution and rollback behavior.
