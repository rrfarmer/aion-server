# Phase 6 Session 1897 Completion - CM_BUY_ITEM Buy Transaction Payload

Date: 2026-05-31
Unit of Work: UOW-1897
Status: Completed

## Scope

- Performed Work Discovery around C# `CmBuyItemBuyFromShopCompositionPlanService`, `TradeBuyTransactionPlanService`, and the Java `CM_BUY_ITEM` actions `13`-`16` buy-from-shop dispatch path.
- Connected the existing buy transaction planner as an optional non-live payload in the buy-from-shop composition descriptor.
- Kept live socket handlers, live `TradeService`, inventory mutations, Kinah/AP/item cost subtraction, item creation, limited-item mutation, packet fanout, audit logging side effects, repository writes, Java runtime capture, and real client validation out of scope.

## What Changed

- Added `BuyTransactionPlan` to `CmBuyItemBuyFromShopDispatchDescriptor`.
- Added `BuyTransactionPlan` to `CmBuyItemBuyFromShopCompositionInput`.
- Added `AttachBuyTransactionPlan` composition step.
- Attached the optional transaction payload only after the trade NPC type is recognized and the buy-from-shop dispatch can be composed.
- Updated tests to verify default dispatch has no payload, successful dispatch carries the optional non-live payload, and unknown trade NPC types still stop without dispatch or payload attachment.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~TradeApFormulaServiceTests|FullyQualifiedName~SmTradeListPacketPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-from-shop composition slice passed with 19 tests.
- Related trade/AP/`CM_BUY_ITEM` slice passed with 57 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4852 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The buy-from-shop composition planner remains non-live and is not invoked by `GameServerConnection`.
- The buy transaction planner remains non-live and is not executed by live `TradeService`.
- Java `PlayerRestrictions`, inventory free-slot state, trade-list item templates, limited-item state, Kinah/AP/item balances, transaction boundaries, item creation, packet sends, and audit logging remain represented by supplied facts or nested planners.

## Parity Table Updates

- Added Session 1897 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` actions `13`-`16` buy-from-shop dispatch payload
  - `TradeService.performBuyTransaction` planned transaction payload attached to buy-from-shop dispatch
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `PrivateStoreService.sellStoreItem` action `0` as a gap-scoped non-live planner.

Safe alternative candidates:

- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
