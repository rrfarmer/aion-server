# Phase 6 Session 1942 Completion - CM_BUY_ITEM Action 13 Trade-List Read Capture

Date: 2026-06-01
Unit of Work: UOW-1942
Status: Completed

## Work Discovery

- Re-read the required migration, orchestration, parity, progress, completion, and handoff documents before choosing work.
- Inspected Java `CM_BUY_ITEM.readImpl`, Java `TradeList`/`TradeItem`, the existing Java `CM_BUY_ITEM_ReadGuardGoldenTest`, C# `CmBuyItem`, C# `CmBuyItemTests`, and related buy-item composition tests.
- Selected a safe parser-only read boundary: Java action `13` buy-from-shop creates a `TradeList` and records item/count pairs in packet order without touching audit logging or live trade mutation.

## What Changed

- Extended Java `CM_BUY_ITEM_ReadGuardGoldenTest` with `readImpl_buyFromShopActionStoresTradeListItemsInReadOrder`.
- The Java test invokes `readImpl` with seller `7001`, action `13`, amount `2`, items `(100000001, 1)` and `(100000002, 5)`, then asserts `isAudit=false` and matching ordered `TradeList` entries.
- Updated C# `CmBuyItemTests.ReadFrom_ReadsSellerActionAmountAndItemsLikeJava` to use action `13` and the same item/count values.
- Kept the unit parser-only. No `runImpl`, target lookup, trade-template lookup, buy transaction mutation, packet send, socket dispatch, or real-client validation was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 2 game-server tests.
- Focused C# `CmBuyItemTests` passed with 9 tests.
- Wider C# buy-item slice passed with 67 tests.
- Java/Maven reactor test run passed with 1 commons test and 15 game-server tests.
- Broad C# game-server suite passed with 4972 tests.

## Known Gaps

- Java audit branches remain uncaptured in Java runtime tests because isolated audit logging reaches staff/static-data services.
- Java action `2` repurchase-list filtering remains outside this unit.
- The Java test uses test-only `Unsafe.allocateInstance` and reflection for an `AionConnection` shell and private field inspection. This is parser evidence only, not connection lifecycle parity.
- `CM_BUY_ITEM.runImpl`, live NPC target validation, `TradeService.performBuyFromShop`, inventory/Kinah/AP mutation, limited-item state, encrypted frame decoding, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1942 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` action `13` buy-from-shop trade-list read formation
  - Java `TradeList.addItem` / `TradeItem` action `13` payload storage evidence
- Full `CM_BUY_ITEM` remains Partial Parity. The specific action `13` read-list boundary now has Java runtime/source-capture evidence aligned with C# tests.
