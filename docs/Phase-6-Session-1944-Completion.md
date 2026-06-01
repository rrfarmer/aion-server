# Phase 6 Session 1944 Completion - CM_BUY_ITEM Action 2 Repurchase Read Capture

Date: 2026-06-01
Unit of Work: UOW-1944
Status: Completed

## Work Discovery

- Re-read required migration, orchestration, parity, progress, completion, and handoff documents before choosing work.
- Inspected Java `CM_BUY_ITEM.readImpl`, Java `RepurchaseList`, Java `RepurchaseService.canRepurchase`, Java `AionObject` object-id behavior, Java `Item`, the Java read-capture test, and C# `CmBuyItemRepurchaseReadPlanService` plus tests.
- Determined action `2` could be captured safely at the read boundary by seeding `RepurchaseService` with shell `Item` object ids and using a shell `Player` object id, while avoiding `runImpl` and live repurchase mutation.

## What Changed

- Extended Java `CM_BUY_ITEM_ReadGuardGoldenTest` with `readImpl_repurchaseActionFiltersItemsThroughRepurchaseServiceInFirstSeenOrder`.
- The Java test seeds `RepurchaseService` for player `5001` with repurchasable item object ids `101` and `102`, reads action `2` payload ids `[101, 999, 102, 101]`, and asserts the resulting `RepurchaseList` contains `[101, 102]`.
- The Java test restores the original singleton map in a `finally` block.
- No C# code change was needed because `CmBuyItemRepurchaseReadPlanServiceTests.CreatePlan_FiltersRepurchasableItemsInFirstSeenOrderAndDeduplicates` already covers the same filtered-order scenario with supplied repurchasable ids.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 3 test methods.
- Focused C# repurchase read planner tests passed with 9 tests.
- Wider C# repurchase/buy-item slice passed with 53 tests after a serial rerun.
- Java/Maven reactor test run passed with 1 commons test and 16 game-server tests.
- Broad C# game-server suite passed with 4977 tests.

## Known Gaps

- The Java test uses test-only `Unsafe.allocateInstance`, final-field object-id writes, and reflection against `RepurchaseService`. This is parser/read-filter evidence only.
- Java audit branches remain uncaptured in Java runtime tests because isolated audit logging reaches staff/static-data services.
- `CM_BUY_ITEM.runImpl`, live NPC target validation, `RepurchaseService.repurchaseFromShop`, inventory/Kinah mutation, item add behavior, encrypted frame decoding, and real-client validation remain pending.
- C# still represents repurchasable object ids as supplied facts; live player-bound singleton state remains unwired.

## Parity Table Updates

- Added Session 1944 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` action `2` repurchase-list read/filter behavior
  - Java `RepurchaseList.addRepurchaseItem` / `RepurchaseService.canRepurchase` read-time filtering evidence
- Full `CM_BUY_ITEM` remains Partial Parity. The action `2` read-filter boundary now has Java runtime/source-capture evidence aligned with existing C# planner tests.
