# Phase 6 Session 1908 Completion - CM_BUY_ITEM Population Resolver Bridge

Date: 2026-05-31
Unit of Work: UOW-1908
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `getObject`, and Java `CM_BUY_ITEM.runImpl`.
- Inspected C# `CmBuyItemKnownVisibleObjectMembershipService`, `CmBuyItemKnownVisibleObjectPopulationAdapterService`, `CmBuyItemKnownVisibleObjectResolverAdapterService`, `GameServerConnection.ResolveBuyItemTargetKind`, and connection buy-item tests.
- Confirmed the next safe unit was a diagnostic population-to-resolver bridge, not live known-list ownership or live `CM_BUY_ITEM` dispatch.

## What Changed

- Added `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`.
- Added `CmBuyItemKnownVisibleObjectPopulationResolverAdapterPlan`.
- The bridge refreshes supplied player/NPC facts through `CmBuyItemKnownVisibleObjectPopulationAdapterService` immediately before resolving the seller with `CmBuyItemKnownVisibleObjectResolverAdapterService`.
- Added tests proving the bridge accepts a supplied visible NPC seller, removes stale facts before resolving, and can drive the existing `GameServerConnection` diagnostic `buyItemKnownObjectResolver`.
- Kept this work non-live. No Java region-neighbor scan, two-way known-list add/remove, visibility callback, pet visibility ordering, live resolver ownership, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 37 tests.
- Related buy-item and known-list slice passed with 110 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4894 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The bridge is supplied-provider driven and non-live by design.
- Java-equivalent `KnownList.findVisibleObjects`, map-region neighbor traversal, two-way add/remove, `forgetObjectsOrUpdateVisibility`, visibility callback behavior, pet ordering, and live known-list mutation remain unwired.
- No live `CM_BUY_ITEM` path owns the supplied providers or treats this bridge as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1908 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.update` / `findVisibleObjects` before `KnownList.getObject` resolver composition
  - `CM_BUY_ITEM.runImpl` target-known gate through the diagnostic population resolver bridge
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a disabled world-snapshot collector around `World`/`IWorldNpcObject` that can feed the population resolver bridge without enabling Java region traversal or live known-list mutation.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
