# Phase 6 Session 1908 Handoff - CM_BUY_ITEM Population Resolver Bridge

Date: 2026-05-31
Unit of Work: UOW-1908
Status: Completed

## What Changed

- Added `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`.
- Added `CmBuyItemKnownVisibleObjectPopulationResolverAdapterPlan`.
- The bridge refreshes supplied player/NPC facts with `CmBuyItemKnownVisibleObjectPopulationAdapterService` before delegating seller resolution to `CmBuyItemKnownVisibleObjectResolverAdapterService`.
- Added pure plan tests for known and stale NPC resolution.
- Added connection-path tests proving the diagnostic resolver accepts a supplied visible NPC target and rejects a stale NPC target before non-live buy-from-shop planner selection.
- Kept this strictly non-live. No Java region traversal, two-way known-list mutation, visibility callback, live resolver ownership, packet send, repository write, trade/private-store/pet mutation, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~NpcVisibilityServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-object/buy-item slice passed with 37 tests.
- Related buy-item and known-list slice passed with 110 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4894 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The bridge is a supplied-facts diagnostic boundary only.
- Java region-neighbor traversal, `forgetObjectsOrUpdateVisibility`, two-way add/remove, visibility updates, pet ordering, live known-list mutation, and live `CM_BUY_ITEM` resolver ownership remain unwired.
- Existing non-live `CM_BUY_ITEM` diagnostic paths still must not be treated as verified Java known-list authorization.

## Parity Table Updates

- Added Session 1908 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.update` / `findVisibleObjects` before `KnownList.getObject` resolver composition
  - `CM_BUY_ITEM.runImpl` target-known gate through the diagnostic population resolver bridge
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a disabled world-snapshot collector around `World`/`IWorldNpcObject` that can feed the population resolver bridge without enabling Java region traversal or live known-list mutation.

Safe alternative candidates:

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
- `docs/Phase-6-Session-1908-Completion.md`
- `docs/Phase-6-Session-1908-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing known-object population work, inspect:
  - Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `add`, `remove`, `isAwareOf`, and `getObject`
  - Java `CM_BUY_ITEM.runImpl`
  - C# `CmBuyItemKnownVisibleObjectPopulationResolverAdapterService`
  - C# `CmBuyItemKnownVisibleObjectPopulationAdapterService`
  - C# `World`, `WorldVisibility`, `IWorldNpcObject`, and existing visibility tests
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
