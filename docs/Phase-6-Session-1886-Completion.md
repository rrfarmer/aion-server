# Phase 6 Session 1886 Completion - BUY_AGAIN Repurchase Dialog Composition

Date: 2026-05-31
Unit of Work: UOW-1886
Status: Completed

## Scope

- Performed Work Discovery around Java `DialogService` BUY_AGAIN and C# dialog descriptor planning.
- Added non-live composition support for carrying a `SmRepurchase` packet snapshot through the dialog planner.
- Kept live socket sends, repurchase singleton state, repository writes, packet fanout, and Java runtime capture out of scope.

## What Changed

- Extended `NpcDialogServiceSelectInput` with optional `SmRepurchase RepurchasePacket`.
- Extended `NpcDialogServiceDescriptor` with optional `SmRepurchase RepurchasePacket`.
- Added a dedicated non-live `CreateRepurchasePlan` for dialog action `70` / BUY_AGAIN.
- Added focused test coverage proving BUY_AGAIN produces a `RepurchasePacket` descriptor carrying the supplied packet snapshot.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~NpcDialogControllerDispatchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~SmRepurchaseTests|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused dialog planner slice passed with 18 tests.
- Related dialog/repurchase slice passed with 62 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4753 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The planner is non-live and does not send packets.
- The optional `SmRepurchase` snapshot must be assembled by a caller; no live repurchase singleton state exists in C#.
- Java `RepurchaseService.getRepurchaseItems(player.getObjectId())` and item collection ordering remain outside this unit.
- Live BUY_AGAIN socket handling, packet fanout, repository persistence, transaction behavior, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1886 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `DialogService` BUY_AGAIN branch
  - `SM_REPURCHASE` dialog descriptor use
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect and model Java `CM_BUY_ITEM` action `2` read validation as a non-live parser/plan, including amount guard, count guard, item id guard, and repurchase-list `canRepurchase` filtering.

Safe alternative candidates:

- Inspect Java `RepurchaseList` and add an isolated C# repurchase request parser/list planner before any live socket handler changes.
- Wire `TradeSellToShopPlanService` only after inventory/repository/packet mutation ordering and rollback behavior are scoped.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
