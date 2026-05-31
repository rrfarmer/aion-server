# Phase 6 Session 1900 Handoff - CM_BUY_ITEM Pet Merchant Payload

Date: 2026-05-31
Unit of Work: UOW-1900
Status: Completed

## What Changed

- Updated `CmBuyItemHandlerCompositionPlanService` to select a non-live pet sell-to-shop planner payload for pet-target action `17`.
- Replaced the former unsupported pet sell status with `SelectedPetSellToShopPlanner`.
- Added `InvokePetSellToShopPlanner` step.
- Added `PetSellModifier` and optional `PetSellToShopPlan` to handler composition input.
- Added `PetSellModifier` and `PetSellToShopPlan` outputs to handler composition plans.
- The pet-target action `17` branch now carries Java `pf.getRatePrice()` equivalent input only when the pet has a merchant function.
- Updated handler composition tests for selected merchant action `17`, missing merchant function, and non-action-17 pet branch isolation.
- Kept this work non-live. No socket handler, live pet sale execution, inventory mutation, Kinah mutation, repurchase state mutation, packet send, audit/log side effect, repository write, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused handler/pet sell-to-shop slice passed with 24 tests.
- Related pet/trade/private-store slice passed with 64 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4859 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- `CmBuyItemHandlerCompositionPlanService` remains non-live and is not invoked by live `GameServerConnection`.
- `TradeSellToShopPlanService` remains non-live and is not executed by live pet merchant handling.
- Java pet object-template lookup, merchant function metadata loading, inventory state, Kinah mutation, repurchase state mutation, packet sends, audit/log side effects, and real client behavior remain represented by supplied facts or separate non-live planners.

## Parity Table Updates

- Added Session 1900 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` pet-target action `17` dispatch composition
  - `PetFunction.getRatePrice` sell modifier payload to `TradeService.performSellToShop`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect whether `CmBuyItemHandlerCompositionPlanService` can be wired into a no-op diagnostic path from live `GameServerConnection` without executing any trade/private-store/pet side effects.

Safe alternative candidates:

- Add a disabled private-store live-executor facade plan that consumes the selector and purchase plans without mutating state.
- Add a disabled pet merchant live-executor facade plan that consumes the pet sell payload without mutating inventory or Kinah.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemHandlerCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1900-Completion.md`
- `docs/Phase-6-Session-1900-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If considering live/no-op handler wiring, inspect:
  - C# `GameServerConnection` packet dispatch surface
  - C# `CmBuyItem`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - Existing non-live diagnostic or disabled executor patterns
  - Java `CM_BUY_ITEM.runImpl`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
