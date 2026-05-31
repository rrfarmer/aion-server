# Phase 6 Session 1820 Handoff - Craft Start Side-Effect Boundary Plan

Date: 2026-05-31
Unit of Work: UOW-1820
Status: Completed

## What Changed

- Added a non-live `CraftStartSideEffectBoundaryPlan`.
- Added `CraftStartSideEffectBoundaryStatus` and `CraftStartSideEffectBoundaryStep`.
- Extended `CmCraftStartCompositionPlan` with `SideEffectBoundaryPlan`.
- Planned Java's successful craft-start order without executing side effects:
  - `checkCraft` inventory mutation
  - `checkCraft` inventory packet intent
  - optional DP spend
  - `CraftingTask` creation
  - `CraftingTask.start`
- Added CM_CRAFT composition tests for ready, no-DP, validation-failed, and runtime-blocked boundary behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 313 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4565 tests.

## Known Gaps

- The boundary is non-live and does not mutate inventory, send packets, spend DP, persist item state, create `CraftingTask`, or start scheduler work.
- Java storage delete quest callbacks/logging are not modeled.
- The C# recipe summary cannot distinguish Java nullable DP from a zero DP value.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add persistence-state planning for craft-consumed item updates/deletes so ordered mutation operations can map to future Java-compatible persistence without writing live state by default.

Safe alternative candidates:

- Add a live-safe craft finish cooldown application mutation plan.
- Begin wiring the boundary into a disabled live executor facade with tests proving no side effects dispatch by default.
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
- Re-inspect Java `Storage.decreaseItemCount`, Java `ItemPacketService`, Java item DAO/update/delete persistence paths, C# ordered inventory operation planning, and current inventory/item repository support.
- Keep the next unit non-live unless it explicitly scopes and verifies packet sending, persistence writes, or inventory mutation side effects.
