# Phase 6 Session 1904 Handoff - CM_BUY_ITEM Known-List Target Fact Adapter

Date: 2026-05-31
Unit of Work: UOW-1904
Status: Completed

## What Changed

- Added `CmBuyItemKnownListTargetFactAdapterService`.
- Added known-list target fact adapter records/enums for `CM_BUY_ITEM` diagnostic target classification.
- The adapter distinguishes explicit Java-shape known-list facts from the existing world-object-only approximation.
- Updated `GameServerConnection` with an optional `buyItemKnownObjectResolver` for the non-live `CM_BUY_ITEM` observer path.
- A resolver result of `false` now classifies the target as unknown, matching Java's `player.getKnownList().getObject(sellerObjId) == null` early return shape.
- Existing callers without a resolver still use world-object classification, but this remains explicitly marked as approximation-only evidence.
- Added `CmBuyItemKnownListTargetFactAdapterServiceTests`.
- Added a `GameServerConnectionBuyItemTests` case covering known-list rejection of an otherwise present world object.
- Kept this strictly non-live. No trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, repurchase mutation, packet send, repository write, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused known-list/connection handler slice passed with 25 tests.
- Related buy-item/private-store/pet merchant slice passed with 79 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4877 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The fact adapter is disabled/non-live by design.
- Full Java-equivalent known-list population, refresh timing, region ownership, live resolver wiring, live `CM_BUY_ITEM` execution, and real client behavior remain unwired.
- World-object-only classification remains for callers that do not provide `buyItemKnownObjectResolver`; it must not be treated as verified known-list parity.

## Parity Table Updates

- Added Session 1904 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl -> player.getKnownList().getObject(sellerObjId)` target resolution
  - `KnownList.getObject` target-null gate
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: wire `buyItemKnownObjectResolver` to an existing C# player known-list membership snapshot in a disabled/non-live runtime-owned adapter, with tests for known, unknown, and missing snapshot behavior.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownListTargetFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownListTargetFactAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1904-Completion.md`
- `docs/Phase-6-Session-1904-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing known-list runtime resolver work, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `KnownList.getObject`
  - C# `GameServerConnection.ResolveBuyItemTargetKind`
  - C# `CmBuyItemKnownListTargetFactAdapterService`
  - C# `PlayerKnownListMembershipService`
  - C# `PlayerKnownListMembershipRefreshService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
