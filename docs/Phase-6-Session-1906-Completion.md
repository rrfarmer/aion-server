# Phase 6 Session 1906 Completion - CM_BUY_ITEM Generic Known-Object Membership Facts

Date: 2026-05-31
Unit of Work: UOW-1906
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and handoff context before selecting work.
- Inspected Java `KnownList.knownObjects`, `KnownList.getObject`, `findVisibleObjects`, `forEachNpc`, and Java `CM_BUY_ITEM.runImpl`.
- Inspected C# `World`, `IWorldNpcObject`, `CmBuyItemKnownListTargetFactAdapterService`, `CmBuyItemKnownListMembershipResolverAdapterService`, and `GameServerConnection` buy-item diagnostic tests.
- Confirmed the next safe unit was a generic non-live known-visible-object snapshot and resolver for `CM_BUY_ITEM`, not live region population or live execution.

## What Changed

- Added `CmBuyItemKnownVisibleObjectMembershipService`.
- Added generic known-object membership candidates, entries, snapshots, update reasons, and object-kind metadata for player, NPC, pet, and other seller targets.
- Added `CmBuyItemKnownVisibleObjectResolverAdapterService`.
- The resolver projects generic known-object snapshots into the existing tri-state `buyItemKnownObjectResolver` shape.
- Added tests for storing NPC and player membership, owner exclusion, known NPC fact resolution, unknown NPC fact rejection, and connection-level NPC accept/reject behavior.
- Exposed narrow internal buy-item test helpers so the new adapter tests can reuse the existing connection fixture without duplicating socket setup.
- Kept this work non-live. No Java region scan, known-list mutation, NPC/pet spawn ownership, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownVisibleObjectMembershipServiceTests|FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused generic known-object/buy-item slice passed with 31 tests.
- Related buy-item and known-list slice passed with 102 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4888 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The generic membership service is fact-driven and non-live by design.
- Java-equivalent `KnownList.findVisibleObjects`, map-region neighbor traversal, two-way add/remove, visibility state updates, pet visibility ordering, and real known-list mutation remain unwired.
- The resolver can consume supplied generic object facts, but no live code populates those facts yet.

## Parity Table Updates

- Added Session 1906 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.knownObjects` / `KnownList.getObject` generic object membership facts
  - `CM_BUY_ITEM.runImpl` NPC target known-list gate through generic resolver facts
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a disabled known-object population input adapter that can build `CmBuyItemKnownVisibleObjectMembershipService` snapshots from supplied world/player/NPC facts without enabling live region traversal.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
