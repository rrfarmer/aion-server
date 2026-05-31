# Phase 6 Session 1909 Completion - CM_BUY_ITEM World Snapshot Collector

Date: 2026-05-31
Unit of Work: UOW-1909
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `getObject`, and Java `CM_BUY_ITEM.runImpl`.
- Inspected C# `World`, `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`, `CmBuyItemKnownVisibleObjectPopulationAdapterService`, `WorldVisibility`, and existing connection buy-item tests.
- Confirmed the next safe unit was a disabled world-container snapshot collector for supplied player/NPC facts, not Java region traversal or live known-list ownership.

## What Changed

- Added read-only `World.GetPlayers()` and `World.GetPlayers(int worldId)` snapshot helpers.
- Added `CmBuyItemWorldKnownVisibleObjectSnapshotCollectorService`.
- Added `CmBuyItemWorldKnownVisibleObjectSnapshot` with explicit non-live and non-Java-region-parity metadata.
- The collector gathers same-world player and NPC candidates from the C# `World` container so they can feed the existing supplied-facts population resolver bridge.
- Added tests for same-world player/NPC snapshot collection and connection-level use through the population resolver bridge.
- Kept this work non-live. No Java map-region neighbor traversal, two-way known-list add/remove, visibility callback, pet visibility ordering, live resolver ownership, packet send, repository write, trade/private-store/pet mutation, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerBootstrapTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 39 tests.
- Related buy-item, known-list, world, and enter-world slice passed with 153 tests.
- First broad run hit the known transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure.
- The targeted transient rerun passed with 1 test.
- The broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` then passed with 4896 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The collector snapshots all same-world C# world-container players/NPCs, not Java map-region neighbors.
- Java-equivalent `KnownList.findVisibleObjects`, map-region neighbor traversal, two-way add/remove, `forgetObjectsOrUpdateVisibility`, visibility callback behavior, pet ordering, and live known-list mutation remain unwired.
- No live `CM_BUY_ITEM` path owns this collector as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1909 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `WorldMapInstance` visible object source for known-list scans through C# world player/NPC snapshots
  - `KnownList.findVisibleObjects` same-world supplied-facts collection for `CM_BUY_ITEM`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: use the world snapshot collector in an opt-in diagnostic connection setup or composition helper without making it default live `CM_BUY_ITEM` authorization.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
