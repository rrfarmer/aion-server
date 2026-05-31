# Phase 6 Session 1906 Handoff - CM_BUY_ITEM Generic Known-Object Membership Facts

Date: 2026-05-31
Unit of Work: UOW-1906
Status: Completed

## What Changed

- Added `CmBuyItemKnownVisibleObjectMembershipService`.
- Added generic known-visible-object membership records/enums for player, NPC, pet, and other seller target facts.
- Added `CmBuyItemKnownVisibleObjectResolverAdapterService`.
- The resolver adapter projects generic known-object snapshots into `GameServerConnection`'s tri-state `buyItemKnownObjectResolver`.
- Added `CmBuyItemKnownVisibleObjectMembershipServiceTests`.
- Added connection-level proof that a known NPC fact allows the existing non-live buy-from-shop planner selection and an unknown NPC fact rejects before planner selection.
- Exposed narrow internal helpers on `GameServerConnectionBuyItemTests` for adapter-focused tests.
- Kept this strictly non-live. No Java region scan, known-list mutation, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused generic known-object/buy-item slice passed with 31 tests.
- Related buy-item and known-list slice passed with 102 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4888 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The generic known-object membership snapshot is fact-driven and non-live by design.
- Java-equivalent region-backed known-list population, two-way relation enforcement, visibility updates, pet visibility ordering, live resolver ownership, live `CM_BUY_ITEM` execution, and real client behavior remain unwired.
- World-object-only approximation remains for callers that do not provide resolver-backed facts.

## Parity Table Updates

- Added Session 1906 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.knownObjects` / `KnownList.getObject` generic object membership facts
  - `CM_BUY_ITEM.runImpl` NPC target known-list gate through generic resolver facts
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a disabled known-object population input adapter that can build `CmBuyItemKnownVisibleObjectMembershipService` snapshots from supplied world/player/NPC facts without enabling live region traversal.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownVisibleObjectMembershipService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownVisibleObjectMembershipServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownListTargetFactAdapterService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1906-Completion.md`
- `docs/Phase-6-Session-1906-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing known-object population work, inspect:
  - Java `KnownList.findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `add`, and `getObject`
  - Java `CM_BUY_ITEM.runImpl`
  - C# `World`, `WorldVisibility`, `IWorldNpcObject`, and `CmBuyItemKnownVisibleObjectMembershipService`
  - C# `PlayerKnownListMembershipRefreshService` for the existing player-only approximation pattern
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
