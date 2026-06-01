# Phase 6 Session 1931 Handoff - Pet Auto-Sell Notification Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1931
Status: Completed

## What Changed

- Added a disabled `PetAutoSellPlanService` for Java `PetService.sell`.
- The new planner records missing-pet, not-selling, missing-MERCHANT-function, and empty-trade-list return branches.
- Eligible auto-sell diagnostics now record the Java `TradeService.performSellToShop` invocation boundary and the `STR_MSG_MERCHANT_PET_GET_SELL_ITEM(pet.getName())` notification intent.
- Added `SmSystemMessage.MerchantPetGetSellItem` for Java message `1402570`.
- Added regression coverage for eligible auto-sell, blocked supplied sell plan plus notification intent, early-return guards, the system-message factory, and the existing `CM_BUY_ITEM` pet merchant path staying notification-free.
- Kept this strictly non-live. No live pet common-data mutation, auto-sell activation, inventory mutation, Kinah mutation, repurchase write, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet/trade outcome slice initially failed because a stale Windows `testhost` process held the test DLL. After stopping that stale process, the same focused filter passed with 38 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4963 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live pet common-data auto-sell state, pet object-template function lookup, pet item selection, and auto-sell activation/deactivation are not wired.
- Live `TradeService.performSellToShop` execution, seller inventory mutation, repurchase state, Kinah mutation, transaction/rollback behavior, repository writes, and packet dispatch remain disabled.
- The planner consumes supplied diagnostic facts and an optional disabled sell plan; it does not prove parity with a live Java pet, inventory, drop, or loot caller.
- `CM_BUY_ITEM` pet action `17` remains intentionally notification-free because Java emits `STR_MSG_MERCHANT_PET_GET_SELL_ITEM` from `PetService.sell`, not from the socket pet-merchant branch.
- Existing non-live pet/trade paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1931 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `PetService.sell`
  - `PetService.activateAutoSell`
  - `SM_SYSTEM_MESSAGE.STR_MSG_MERCHANT_PET_GET_SELL_ITEM`
  - `TradeService.performSellToShop` auto-sell caller boundary
- All rows remain `Partial Parity`, `Regression Tested` or source-reviewed, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetMerchantSellLiveExecutorFacadePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1931-Completion.md`
- `docs/Phase-6-Session-1931-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing the recommended private-store diagnostic slice, inspect:
  - Java `CM_BUY_ITEM.runImpl` player target action `0`
  - Java `PrivateStoreService.getBoughtItems`
  - Java `PrivateStoreService.sellStoreItem`
  - C# `PrivateStoreBoughtItemsPlanService`
  - C# `PrivateStorePurchasePlanService`
  - C# `CmBuyItemHandlerCompositionPlanService`
- If continuing pet work instead, inspect:
  - Java `PetService.activateAutoSell`
  - Java `SM_PET(PetSpecialFunction.AUTOSELL, activate)`
  - C# `CmPet` parser and `SmPet` special-function packet tests
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
