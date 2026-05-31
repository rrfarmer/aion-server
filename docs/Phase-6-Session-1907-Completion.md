# Phase 6 Session 1907 Completion - CM_BUY_ITEM Known-Object Population Adapter

Date: 2026-05-31
Unit of Work: UOW-1907
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `getObject`, and Java `CM_BUY_ITEM.runImpl`.
- Inspected C# `CmBuyItemKnownVisibleObjectMembershipService`, `PlayerKnownListMembershipRefreshService`, `WorldVisibility`, `NpcVisibilityService`, `World`, and generic known-object membership tests.
- Confirmed the next safe unit was a disabled supplied-facts population adapter for `CM_BUY_ITEM` known-object facts, not live region traversal or live handler mutation.

## What Changed

- Added `CmBuyItemKnownVisibleObjectPopulationAdapterService`.
- Added `CmBuyItemKnownVisibleObjectPopulationResult` with explicit non-live and approximation flags.
- The adapter refreshes an owner player's generic known-object snapshot from supplied online-player and NPC facts using the existing `WorldVisibility` distance/world approximation.
- The adapter excludes the owner player, upserts visible player/NPC candidates as `KnownListRefresh`, deduplicates by object ID, and removes stale snapshot entries that are no longer visible in the supplied facts.
- Added tests for player/NPC population, world/distance filtering, owner exclusion through the normal add path, stale fact removal, and approximation metadata.
- Kept this work non-live. No Java region-neighbor scan, two-way known-list add/remove, visibility callback, pet visibility ordering, live socket resolver ownership, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 33 tests.
- Related buy-item and known-list slice passed with 106 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4890 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The population adapter is supplied-facts-only and non-live by design.
- Java-equivalent `KnownList.findVisibleObjects`, map-region neighbor traversal, two-way add/remove, `forgetObjectsOrUpdateVisibility`, visibility callback behavior, pet ordering, and live known-list mutation remain unwired.
- The adapter can populate facts for the generic resolver, but no live `CM_BUY_ITEM` path owns or refreshes those facts.

## Parity Table Updates

- Added Session 1907 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.findVisibleObjects` / `forgetObjectsOrUpdateVisibility` supplied-facts population approximation
  - `KnownList.isAwareOf` / owner-exclusion behavior through known-object snapshot refresh
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: wire the population adapter output into a connection-level diagnostic fixture or service boundary with `CmBuyItemKnownVisibleObjectResolverAdapterService`, still without enabling live dispatch or live known-list ownership.

Safe alternative candidates:

- Add a disabled world-snapshot collector around `World`/`IWorldNpcObject` that feeds the population adapter without region traversal.
- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
