# Phase 6 Session 1900 Completion - CM_BUY_ITEM Pet Merchant Payload

Date: 2026-05-31
Unit of Work: UOW-1900
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM` action `17`, Java `PetFunction.getRatePrice`, Java `PetService.sell`, C# `CmBuyItemHandlerCompositionPlanService`, and C# `TradeSellToShopPlanService`.
- Connected the pet merchant action `17` branch to a non-live handler payload that carries the pet merchant sell modifier and an optional sell-to-shop plan.
- Kept live socket handlers, live pet sale execution, inventory mutation, Kinah mutation, repurchase state mutation, packet sends, audit/log side effects, repository writes, Java runtime capture, and real client validation out of scope.

## What Changed

- Replaced the former unsupported pet sell status with `SelectedPetSellToShopPlanner`.
- Added `InvokePetSellToShopPlanner` handler composition step.
- Added `PetSellModifier` and optional `PetSellToShopPlan` to `CmBuyItemHandlerCompositionInput`.
- Added `PetSellModifier` and `PetSellToShopPlan` outputs to `CmBuyItemHandlerCompositionPlan`.
- Updated pet-target action `17` handling to select the non-live pet sell-to-shop planner payload only when the pet has a merchant function.
- Updated focused handler composition tests for merchant action `17` payload carry-through and skip-branch isolation.

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
