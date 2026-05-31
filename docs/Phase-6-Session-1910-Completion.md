# Phase 6 Session 1910 Completion - CM_BUY_ITEM World Snapshot Resolver Factory

Date: 2026-05-31
Unit of Work: UOW-1910
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `KnownList.findVisibleObjects`, `getObject`, and Java `CM_BUY_ITEM.runImpl`.
- Inspected C# `World.GetPlayers`, `World.GetNpcs`, `CmBuyItemWorldKnownVisibleObjectSnapshotCollectorService`, `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`, and connection buy-item tests.
- Confirmed the next safe unit was an opt-in diagnostic resolver factory, not default `GameServerConnection` wiring or live authorization.

## What Changed

- Added `CmBuyItemWorldKnownVisibleObjectResolverFactoryService`.
- Added `CmBuyItemWorldKnownVisibleObjectResolverFactoryPlan`.
- The factory creates an opt-in `GameServerConnection`-compatible resolver from a supplied `World`, using the world snapshot collector and supplied-facts population resolver bridge.
- Added tests for factory plan metadata and connection-level use through the existing optional `buyItemKnownObjectResolver` hook.
- Kept this work non-live. No default connection wiring, Java map-region neighbor traversal, two-way known-list add/remove, visibility callback, pet visibility ordering, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerBootstrapTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 41 tests.
- Related buy-item, known-list, world, and enter-world slice passed with 155 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4898 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The factory is opt-in diagnostic wiring only; it is not default `GameServerConnection` behavior.
- The world snapshot collector remains a same-world flat-container approximation, not Java region-neighbor traversal.
- Java-equivalent `KnownList.findVisibleObjects`, two-way add/remove, `forgetObjectsOrUpdateVisibility`, visibility callback behavior, pet ordering, and live known-list mutation remain unwired.
- No live `CM_BUY_ITEM` path treats this factory as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1910 rows in `docs/PHASE-6-PROGRESS.md` for:
  - opt-in world-snapshot resolver factory for `CM_BUY_ITEM`
  - `GameServerConnection.ResolveBuyItemTargetKind` diagnostic resolver composition
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: shift from known-list scaffolding to a disabled private-store purchase persistence/send adapter plan for `CM_BUY_ITEM` action `0`, unless Java runtime/golden capture becomes available.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
