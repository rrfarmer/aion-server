# Phase 6 Session 1897 Handoff - CM_BUY_ITEM Buy Transaction Payload

Date: 2026-05-31
Unit of Work: UOW-1897
Status: Completed

## What Changed

- Updated `CmBuyItemBuyFromShopCompositionPlanService` to carry an optional `TradeBuyTransactionPlan`.
- Added `BuyTransactionPlan` to the buy-from-shop composition input and dispatch descriptor.
- Added `AttachBuyTransactionPlan` composition step.
- Limited payload attachment to the valid buy-from-shop dispatch path after Java trade NPC type classification succeeds.
- Updated focused tests for default no-payload dispatch, successful non-live payload attachment, and unknown trade NPC type payload exclusion.
- Kept this work non-live. No socket handler, live `TradeService`, inventory mutation, Kinah/AP/item subtraction, limited-item mutation, item creation, packet fanout, audit logging side effects, repository writes, Java runtime capture, or real client validation was enabled.

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
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemBuyFromShopCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemBuyFromShopCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeBuyTransactionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1897-Completion.md`
- `docs/Phase-6-Session-1897-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing trade work, inspect:
  - Java `CM_BUY_ITEM` action `0`
  - Java `PrivateStoreService.sellStoreItem`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# private-store or trade planner coverage, if present
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
