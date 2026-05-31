# Phase 6 Session 1910 Handoff - CM_BUY_ITEM World Snapshot Resolver Factory

Date: 2026-05-31
Unit of Work: UOW-1910
Status: Completed

## What Changed

- Added `CmBuyItemWorldKnownVisibleObjectResolverFactoryService`.
- Added `CmBuyItemWorldKnownVisibleObjectResolverFactoryPlan`.
- The factory creates an opt-in `GameServerConnection`-compatible resolver from a supplied `World`, using `CmBuyItemWorldKnownVisibleObjectSnapshotCollectorService` and `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`.
- Added tests for factory plan metadata and connection-path use through the optional diagnostic resolver hook.
- Kept this strictly non-live. No default connection wiring, Java region traversal, two-way known-list mutation, visibility callback, packet send, repository write, trade/private-store/pet mutation, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerBootstrapTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 41 tests.
- Related buy-item, known-list, world, and enter-world slice passed with 155 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4898 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- This is opt-in diagnostic resolver wiring only.
- Java region-neighbor traversal, `forgetObjectsOrUpdateVisibility`, two-way add/remove, visibility updates, pet ordering, live known-list mutation, and live `CM_BUY_ITEM` resolver ownership remain unwired.
- Existing non-live `CM_BUY_ITEM` diagnostic paths still must not be treated as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1910 rows in `docs/PHASE-6-PROGRESS.md` for:
  - opt-in world-snapshot resolver factory for `CM_BUY_ITEM`
  - `GameServerConnection.ResolveBuyItemTargetKind` diagnostic resolver composition
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: shift from known-list scaffolding to a disabled private-store purchase persistence/send adapter plan for `CM_BUY_ITEM` action `0`, unless Java runtime/golden capture becomes available.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownVisibleObjectMembershipService.cs`
- `dotnetConversion/src/Aion.GameServer/World/World.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownVisibleObjectMembershipServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1910-Completion.md`
- `docs/Phase-6-Session-1910-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl` action `0`
  - Java `PrivateStoreService.sellStoreItem`
  - C# `PrivateStorePurchasePlanService`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemWorldKnownVisibleObjectResolverFactoryService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
