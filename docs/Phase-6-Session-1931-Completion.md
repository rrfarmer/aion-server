# Phase 6 Session 1931 Completion - Pet Auto-Sell Notification Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1931
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `PetService.sell`.
- Inspected Java `PetService.activateAutoSell`.
- Inspected Java `SM_SYSTEM_MESSAGE.STR_MSG_MERCHANT_PET_GET_SELL_ITEM`.
- Inspected C# `TradeSellToShopPlanService`, pet merchant disabled facade/outcome services, `SmSystemMessage`, and pet merchant tests.

## What Changed

- Added a disabled `PetAutoSellPlanService` for the separate Java `PetService.sell` auto-sell path.
- Recorded the Java guard order:
  - missing pet returns
  - pet not currently auto-selling returns
  - missing MERCHANT function returns
  - empty trade list returns before sell/notification
- Recorded the non-live Java side-effect boundaries for eligible auto-sell:
  - build a trade list from supplied item object IDs/counts
  - invoke `TradeService.performSellToShop(pet.getMaster(), tradeList, null, pf.getRatePrice())`
  - send `STR_MSG_MERCHANT_PET_GET_SELL_ITEM(pet.getName())`
- Added the C# `SmSystemMessage.MerchantPetGetSellItem` packet factory for message `1402570`.
- Added regression coverage proving:
  - eligible auto-sell records sell-to-shop and notification intents without dispatch
  - the notification intent is separate from disabled sell-plan success and still recorded after a blocked supplied sell plan
  - early-return guards do not record sell or notification intents
  - `CM_BUY_ITEM` pet merchant action `17` remains notification-free
- Kept this work non-live. No live pet common-data mutation, auto-sell activation, inventory mutation, Kinah mutation, repurchase write, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet/trade outcome slice initially failed because a stale Windows `testhost` process held the test DLL. After stopping that stale process, the same focused filter passed with 38 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4963 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live pet common-data auto-sell state, pet object-template function lookup, pet item selection, and auto-sell activation/deactivation are not wired.
- Live `TradeService.performSellToShop` execution, seller inventory mutation, repurchase state, Kinah mutation, transaction/rollback behavior, repository writes, and packet dispatch remain disabled.
- The planner consumes supplied diagnostic facts and an optional disabled sell plan; it does not prove parity with a live Java pet, inventory, drop, or loot caller.
- `CM_BUY_ITEM` pet action `17` remains intentionally notification-free because Java emits `STR_MSG_MERCHANT_PET_GET_SELL_ITEM` from `PetService.sell`, not from the socket pet-merchant branch.

## Parity Table Updates

- Added Session 1931 rows in `docs/PHASE-6-PROGRESS.md` for:
  - Java `PetService.sell`
  - Java `PetService.activateAutoSell`
  - Java `SM_SYSTEM_MESSAGE.STR_MSG_MERCHANT_PET_GET_SELL_ITEM`
  - Java `TradeService.performSellToShop` auto-sell caller boundary
- All rows remain `Partial Parity`, `Regression Tested` or source-reviewed, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.

Safe alternative candidates:

- Add Java-runtime golden capture for `CM_BUY_ITEM` or pet auto-sell once compatible Java and Maven are available.
- Continue buy-from-shop diagnostics around Java `PricesService.getBuyPrice(price, race)` global influence/tax facts without enabling live execution.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a separate disabled activation planner.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
