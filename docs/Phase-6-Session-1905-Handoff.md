# Phase 6 Session 1905 Handoff - CM_BUY_ITEM Player Known-List Membership Resolver Adapter

Date: 2026-05-31
Unit of Work: UOW-1905
Status: Completed

## What Changed

- Added `CmBuyItemKnownListMembershipResolverAdapterService`.
- Added resolver adapter records/enums for projecting `PlayerKnownListMembershipService` snapshots into `CM_BUY_ITEM` target known-list facts.
- Changed `GameServerConnection`'s optional `buyItemKnownObjectResolver` shape to tri-state `Func<Player, int, object?, bool?>`.
- Player seller targets can now resolve `true` or `false` from the owner's player membership snapshot.
- NPC/pet/other seller targets return an unknown fact (`null`) because the current C# membership service is player-only and must not falsely reject Java-visible non-player objects.
- Added `CmBuyItemKnownListMembershipResolverAdapterServiceTests`.
- Added a `GameServerConnectionBuyItemTests` private-store player-target rejection case through the membership resolver adapter.
- Kept this strictly non-live. No known-list mutation, trade/private-store/pet execution, inventory mutation, Kinah/AP mutation, packet send, repository write, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemKnownListMembershipResolverAdapterServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerKnownListMembershipServiceTests|FullyQualifiedName~PlayerKnownListMembershipRefreshServiceTests|FullyQualifiedName~PlayerKnownListMembershipRegistryRefreshAdapterServiceTests|FullyQualifiedName~BindPointTeleportKnownListFanoutMembershipAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused membership resolver/known-list/buy-item slice passed with 39 tests.
- Related buy-item and known-list slice passed with 94 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4883 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The adapter is disabled/non-live by design.
- NPC, pet, and other `VisibleObject` seller membership remain unrepresented because the current membership snapshot is player-only.
- Region-backed Java known-list population, live resolver ownership, live `CM_BUY_ITEM` execution, and real client behavior remain unwired.
- World-object-only approximation remains for unresolved/null facts and must not be treated as verified known-list parity.

## Parity Table Updates

- Added Session 1905 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `KnownList.getObject` player-target lookup through `PlayerKnownListMembershipService`
  - `CM_BUY_ITEM.runImpl` private-store player target rejection when the target is not known
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a generic non-live known-visible-object membership adapter or snapshot that can represent NPC/pet/object membership for `CM_BUY_ITEM` without reusing the player-only membership service.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownListMembershipResolverAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownListTargetFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownListMembershipResolverAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1905-Completion.md`
- `docs/Phase-6-Session-1905-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing object known-list work, inspect:
  - Java `KnownList.getObject`, `knownObjects`, `findVisibleObjects`, and `forEachNpc`
  - Java `CM_BUY_ITEM.runImpl`
  - C# `CmBuyItemKnownListMembershipResolverAdapterService`
  - C# `CmBuyItemKnownListTargetFactAdapterService`
  - C# `GameServerConnection.ResolveBuyItemTargetKind`
  - C# `PlayerKnownListMembershipService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
