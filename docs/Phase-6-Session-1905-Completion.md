# Phase 6 Session 1905 Completion - CM_BUY_ITEM Player Known-List Membership Resolver Adapter

Date: 2026-05-31
Unit of Work: UOW-1905
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` and Java `KnownList.getObject` / `knows`.
- Inspected C# `CmBuyItemKnownListTargetFactAdapterService`, `GameServerConnection.ResolveBuyItemTargetKind`, `PlayerKnownListMembershipService`, `PlayerKnownListMembershipRefreshService`, and registry refresh adapter tests.
- Confirmed the safe next unit was a disabled player-target resolver adapter. The existing C# membership snapshot tracks player objects only, while Java `KnownList.getObject` covers all `VisibleObject` types.

## What Changed

- Added `CmBuyItemKnownListMembershipResolverAdapterService`.
- The adapter creates a tri-state `CM_BUY_ITEM` known-object resolver from `PlayerKnownListMembershipService`.
- Player seller targets return `true` or `false` from the owner player's membership snapshot.
- NPC, pet, and other seller target kinds return an unknown fact (`null`) instead of a false rejection, preserving the explicit gap versus Java's all-visible-object known list.
- Changed `GameServerConnection`'s optional buy-item known resolver from `Func<Player, int, bool>` to `Func<Player, int, object?, bool?>`.
- Added focused adapter tests for known player, unknown player, unsupported NPC target, missing membership service, and resolver delegate projection.
- Added a connection-level test proving the membership resolver can reject an unknown private-store player target without dispatching live side effects.
- Kept this work non-live. No player/NPC known-list mutation, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~BindPointTeleportKnownListFanoutMembershipAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused membership resolver/known-list/buy-item slice passed with 39 tests.
- Related buy-item and known-list slice passed with 94 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4883 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The resolver adapter is disabled and non-live by design.
- The adapter only resolves player seller targets because `PlayerKnownListMembershipService` only tracks known players.
- Java-equivalent NPC/pet/general `VisibleObject` known-list membership, region-backed known-list population, live resolver ownership, live `CM_BUY_ITEM` execution, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1905 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.getObject` player-target lookup through `PlayerKnownListMembershipService`
  - `CM_BUY_ITEM.runImpl` private-store player target rejection when the target is not known
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a generic non-live known-visible-object membership adapter or snapshot that can represent NPC/pet/object membership for `CM_BUY_ITEM` without reusing the player-only membership service.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
