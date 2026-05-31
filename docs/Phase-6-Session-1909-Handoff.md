# Phase 6 Session 1909 Handoff - CM_BUY_ITEM World Snapshot Collector

Date: 2026-05-31
Unit of Work: UOW-1909
Status: Completed

## What Changed

- Added read-only `World.GetPlayers()` and `World.GetPlayers(int worldId)` helpers.
- Added `CmBuyItemWorldKnownVisibleObjectSnapshotCollectorService`.
- Added `CmBuyItemWorldKnownVisibleObjectSnapshot`.
- The collector snapshots same-world players and NPCs from the C# `World` container so the existing population resolver bridge can consume world-backed supplied facts.
- Added tests for collector filtering and connection-path use through the diagnostic population resolver bridge.
- Kept this strictly non-live. No Java region traversal, two-way known-list mutation, visibility callback, live resolver ownership, packet send, repository write, trade/private-store/pet mutation, Java runtime output, or real client validation was enabled.

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

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- This collector is a same-world C# world-container snapshot, not Java region-neighbor traversal.
- Java `forgetObjectsOrUpdateVisibility`, two-way add/remove, visibility updates, pet ordering, live known-list mutation, and live `CM_BUY_ITEM` resolver ownership remain unwired.
- Existing non-live `CM_BUY_ITEM` diagnostic paths still must not be treated as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1909 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `WorldMapInstance` visible object source for known-list scans through C# world player/NPC snapshots
  - `KnownList.findVisibleObjects` same-world supplied-facts collection for `CM_BUY_ITEM`
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: use the world snapshot collector in an opt-in diagnostic connection setup or composition helper without making it default live `CM_BUY_ITEM` authorization.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/World/World.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownVisibleObjectMembershipService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownVisibleObjectMembershipServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1909-Completion.md`
- `docs/Phase-6-Session-1909-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing known-object population work, inspect:
  - Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `add`, `remove`, `isAwareOf`, and `getObject`
  - Java `CM_BUY_ITEM.runImpl`
  - C# `World.GetPlayers`, `World.GetNpcs`
  - C# `CmBuyItemWorldKnownVisibleObjectSnapshotCollectorService`
  - C# `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
