# Phase 6 Session 1823 Handoff - Craft Inventory SQL Descriptor Planning

Date: 2026-05-31
Unit of Work: UOW-1823
Status: Completed

## What Changed

- Added non-live SQL descriptor planning to craft-start inventory persistence.
- Added exact Java SQL constants:
  - `CraftStartInventoryPersistencePlan.JavaInventoryDeleteSql`
  - `CraftStartInventoryPersistencePlan.JavaInventoryUpdateSql`
- Added `CraftStartInventoryPersistenceSqlDescriptor`.
- Added `CraftStartInventoryPersistenceSqlOperationKind`.
- Extended `CraftStartInventoryPersistencePlan` with:
  - `SqlDescriptors`
  - `ObjectIdsPendingRelease`
  - `WouldReleaseObjectIdsAfterSuccessfulDelete`
  - `DidReleaseObjectIds`
- Descriptor order follows Java `InventoryDAO.store`: delete rows before update rows.
- `NoAction` deleted stacks do not produce SQL descriptors or object-id release intent.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 318 tests.
- First broad run timed out before returning a result.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4570 tests on rerun with a longer timeout.

## Known Gaps

- SQL descriptors remain non-live; no database writes execute.
- No transaction, commit/rollback, or Java autocommit behavior is implemented for these descriptors.
- Object ids are not released; the plan records only the Java post-delete release boundary.
- No live inventory mutation is applied.
- No live inventory packets are sent.
- No DP spend, `CraftingTask` creation, or task start occurs.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add a disabled inventory packet send adapter for craft-start packet intent so `SmInventoryUpdateItem`, `SmDeleteItem`, and `SmCubeUpdate` intent can be consumed by an adapter that proves packet dispatch remains off by default.

Safe alternative candidates:

- Add a live-safe craft finish cooldown application mutation plan.
- Begin a live-disabled craft inventory persistence adapter around the new SQL descriptors.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- Re-inspect Java `Storage.decreaseItemCount`, Java inventory packet send behavior, C# `CraftStartInventoryPacketPlan`, and existing disabled adapter patterns.
- Keep the next unit non-live unless it explicitly scopes live packet dispatch and verifies failure/ordering behavior.
