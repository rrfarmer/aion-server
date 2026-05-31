# Phase 6 Session 1904 Completion - CM_BUY_ITEM Known-List Target Fact Adapter

Date: 2026-05-31
Unit of Work: UOW-1904
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` target lookup and Java `KnownList.getObject` / `knows` source.
- Inspected C# `GameServerConnection.ResolveBuyItemTargetKind`, existing `CM_BUY_ITEM` diagnostic observer tests, and known-list membership services.
- Confirmed the next safe unit was an explicit known-list fact adapter for diagnostic target classification, not live trade/private-store/pet execution.

## What Changed

- Added `CmBuyItemKnownListTargetFactAdapterService`.
- The adapter records whether target classification came from an explicit per-player known-list fact or from the existing world-object-only approximation.
- Added terminal statuses for missing player, missing world object, not-known target, resolved known-list fact, and world-object-only approximation.
- Updated the `GameServerConnection` diagnostic `CM_BUY_ITEM` observer path with an optional `buyItemKnownObjectResolver`.
- When the resolver reports `false`, the diagnostic path now mirrors Java's `player.getKnownList().getObject(sellerObjId) == null` early return by classifying the target as unknown.
- Kept the existing non-live world-object approximation when no resolver is supplied, and marked it as non-Java-parity evidence in the adapter plan.
- Added `CmBuyItemKnownListTargetFactAdapterServiceTests`.
- Added a `GameServerConnectionBuyItemTests` case proving the optional resolver can reject an otherwise present world NPC target.
- Kept this work non-live. No trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, repurchase mutation, packet send, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-list/connection handler slice passed with 25 tests.
- Related buy-item/private-store/pet merchant slice passed with 79 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4877 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The adapter is fact-driven and non-live by design.
- Full Java-equivalent known-list population, refresh timing, region ownership, visibility authorization, live `CM_BUY_ITEM` handler execution, and real client behavior remain unwired.
- Existing callers without `buyItemKnownObjectResolver` still use the explicitly marked world-object-only approximation.

## Parity Table Updates

- Added Session 1904 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl -> player.getKnownList().getObject(sellerObjId)` target resolution
  - `KnownList.getObject` target-null gate
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: wire `buyItemKnownObjectResolver` to an existing C# player known-list membership snapshot in a disabled/non-live runtime-owned adapter, with tests for known, unknown, and missing snapshot behavior.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
