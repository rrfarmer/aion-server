# Phase 6 Session 1907 Handoff - CM_BUY_ITEM Known-Object Population Adapter

Date: 2026-05-31
Unit of Work: UOW-1907
Status: Completed

## What Changed

- Added `CmBuyItemKnownVisibleObjectPopulationAdapterService`.
- Added `CmBuyItemKnownVisibleObjectPopulationResult` with explicit flags showing the result is non-live, supplied-facts based, and not Java region known-list parity.
- The adapter refreshes `CmBuyItemKnownVisibleObjectMembershipService` snapshots from supplied online-player and NPC facts using `WorldVisibility`.
- It excludes the owner player, records visible player/NPC facts as `KnownListRefresh`, deduplicates by object ID, and removes stale facts absent from the supplied visible set.
- Added focused tests for visible player/NPC upsert, distance/world filtering, approximation metadata, and stale fact removal.
- Kept this strictly non-live. No Java region scan, two-way known-list relation enforcement, visibility callback, live resolver ownership, packet send, repository write, trade/private-store/pet mutation, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 33 tests.
- Related buy-item and known-list slice passed with 106 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4890 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- This adapter is a supplied-facts `WorldVisibility` approximation only.
- Java region-neighbor traversal, `forgetObjectsOrUpdateVisibility`, two-way add/remove, visibility updates, pet ordering, live known-list mutation, and live `CM_BUY_ITEM` resolver ownership remain unwired.
- Existing non-live `CM_BUY_ITEM` diagnostic paths still must not be treated as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1907 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.findVisibleObjects` / `forgetObjectsOrUpdateVisibility` supplied-facts population approximation
  - `KnownList.isAwareOf` / owner-exclusion behavior through known-object snapshot refresh
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownVisibleObjectMembershipService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownVisibleObjectMembershipServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1907-Completion.md`
- `docs/Phase-6-Session-1907-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing known-object population work, inspect:
  - Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `add`, `remove`, `isAwareOf`, and `getObject`
  - Java `CM_BUY_ITEM.runImpl`
  - C# `CmBuyItemKnownVisibleObjectMembershipService`, `CmBuyItemKnownVisibleObjectPopulationAdapterService`, and `CmBuyItemKnownVisibleObjectResolverAdapterService`
  - C# `GameServerConnection.ResolveBuyItemTargetKind`
  - C# `World`, `WorldVisibility`, `IWorldNpcObject`, and `PlayerKnownListMembershipRefreshService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
