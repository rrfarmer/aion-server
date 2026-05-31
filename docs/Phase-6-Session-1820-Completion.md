# Phase 6 Session 1820 Completion - Add Craft Start Side-Effect Boundary Plan

Date: 2026-05-31
Unit of Work: UOW-1820
Status: Complete

## Scope

Add a non-live craft-start side-effect boundary plan around CM_CRAFT start composition. The boundary records Java's successful `startCrafting` side-effect order without spending DP, mutating inventory, sending packets, creating `CraftingTask`, starting scheduler work, persisting item state, or executing live craft completion.

## Completed Work

- Added `CraftStartSideEffectBoundaryPlan`.
- Added `CraftStartSideEffectBoundaryStatus`.
- Added `CraftStartSideEffectBoundaryStep`.
- Extended `CmCraftStartCompositionPlan` with `SideEffectBoundaryPlan`.
- Planned the Java successful start order:
  - apply `checkCraft` inventory mutation
  - send `checkCraft` inventory packets
  - optionally spend recipe DP
  - create `CraftingTask`
  - start `CraftingTask`
- Marked validation-failure and runtime-blocked paths as non-live, non-dispatching, and not planned for success side effects.
- Added focused tests proving:
  - ready CM_CRAFT composition carries the side-effect boundary
  - boundary steps preserve Java order with DP spend
  - recipes with no DP cost omit the DP step
  - validation failures and runtime-blocked plans do not expose success side-effect steps

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 313 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4565 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`

## Migration Parity Table - UOW-1820

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` success side-effect order | `CmCraftStartCompositionPlan.SideEffectBoundaryPlan` | Orchestration Planner | Partial | Unit Tested | Partial Parity | C# records non-live success ordering for checkCraft inventory mutation/packets, optional DP spend, task creation, and task start; no live mutation/send/task execution occurs. |
| `CraftService.startCrafting` DP branch | `CraftStartSideEffectBoundaryStep.SpendRecipeDp` | Orchestration Planner | Partial | Unit Tested | Partial Parity | C# includes the DP step only when the current model has a positive DP cost; the C# recipe summary does not distinguish Java nullable zero-cost DP from absent DP. |
| `CraftService.startCrafting` validation-failure boundary | `CraftStartSideEffectBoundaryStatus.ValidationFailed` | Orchestration Planner | Partial | Unit Tested | Partial Parity | C# records that success side effects are not planned after `checkCraft` failure; live cancel packet dispatch remains non-live intent only. |

## Risks / Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No item persistence state changes are written.
- Java storage delete quest callbacks/logging are not modeled.
- No live DP spend occurs through this boundary.
- No live `CraftingTask` is created or started.
- The C# recipe summary uses an integer DP value and cannot distinguish Java `null` DP from zero DP.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add a persistence-state planning slice for craft-consumed item updates/deletes so the ordered mutation operations can map to future Java-compatible item update/delete persistence without performing writes by default.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - begin wiring the boundary into a disabled live executor facade with tests proving no side effects dispatch by default
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1820-Completion.md`
- `docs/Phase-6-Session-1820-Handoff.md`
