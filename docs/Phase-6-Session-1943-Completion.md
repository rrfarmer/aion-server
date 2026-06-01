# Phase 6 Session 1943 Completion - CM_BUY_ITEM Trade-List Action Set Read Capture

Date: 2026-06-01
Unit of Work: UOW-1943
Status: Completed

## Work Discovery

- Re-read the latest UOW-1942 handoff and completion notes before choosing work.
- Re-inspected Java `CM_BUY_ITEM.readImpl`, Java `TradeList`/`TradeItem`, the Java read-capture test, C# `CmBuyItem`, C# `CmBuyItemTests`, and related buy-item composition tests.
- Determined Java action `2` repurchase-list capture still risks player-bound singleton state and selected the safe documented alternative: broaden non-audit trade-list action read coverage.

## What Changed

- Updated Java `CM_BUY_ITEM_ReadGuardGoldenTest`.
- The Java trade-list read test now verifies actions `1`, `13`, `14`, `15`, `16`, and `17` all store `(100000001, 1)` and `(100000002, 5)` in a `TradeList` in read order without setting `isAudit`.
- Converted the C# `CmBuyItemTests` trade-list parser assertion into a theory over the same action IDs and values.
- Kept the unit parser-only. No action `2` repurchase filtering, audit logging, live handler execution, trade/private-store/pet mutation, socket dispatch, or real-client validation was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 2 test methods.
- Focused C# `CmBuyItemTests` passed with 14 tests.
- Wider C# buy-item slice passed with 83 tests.
- Java/Maven reactor test run passed with 1 commons test and 15 game-server tests.
- Broad C# game-server suite passed with 4977 tests.

## Known Gaps

- Java action `2` `RepurchaseList.addRepurchaseItem` filtering remains uncaptured in Java runtime tests.
- Java audit branches remain uncaptured in Java runtime tests because isolated audit logging reaches staff/static-data services.
- The Java test uses test-only `Unsafe.allocateInstance` and reflection for an `AionConnection` shell and private field inspection. This is parser evidence only, not connection lifecycle parity.
- `CM_BUY_ITEM.runImpl`, live private-store/NPC/pet target validation, trade-template lookup, buy/sell transaction persistence, limited-item state, encrypted frame decoding, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1943 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` trade-list action set (`1`, `13`, `14`, `15`, `16`, `17`)
  - Java `TradeList.addItem` / `TradeItem` trade-list action payload storage evidence
- Full `CM_BUY_ITEM` remains Partial Parity. The non-repurchase trade-list read action set now has Java runtime/source-capture evidence aligned with C# tests.
