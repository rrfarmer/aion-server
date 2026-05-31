# Phase 6 Session 1887 Completion - CM_BUY_ITEM Repurchase Read Planner

Date: 2026-05-31
Unit of Work: UOW-1887
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM` action `2` read validation and `RepurchaseList` request formation.
- Added a non-live C# planner for the read-side repurchase list behavior.
- Kept live packet registration, socket handlers, NPC target gating, repurchase singleton state, repository writes, packet fanout, audit logging, and Java runtime capture out of scope.

## What Changed

- Added `CmBuyItemRepurchaseReadPlanService`.
- Added `CmBuyItemReadItem` and `CmBuyItemRepurchaseReadPlan` records.
- Added focused tests for amount guard, item/count exploit guards, amount-limited processing, action `2` filtering, first-seen ordering, duplicate removal, non-repurchase behavior, and Java action `0` item-index guard behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~SmRepurchaseTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused read planner slice passed with 9 tests.
- Related repurchase/dialog/sell-to-shop slice passed with 52 tests.
- First broad attempt timed out at 244 seconds before producing a result.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed on rerun with 4762 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The planner is non-live and is not registered as opcode `51` / `CM_BUY_ITEM`.
- Packet byte parsing is represented by supplied values; no live `PacketBuffer` parser was added.
- Java `RepurchaseService.canRepurchase(player, itemObjectId)` is represented by caller-supplied repurchasable ids.
- Java `CM_BUY_ITEM.runImpl` target lookup, NPC interaction gate, `npc.canBuy()`, and live `RepurchaseService.repurchaseFromShop` dispatch remain outside this unit.

## Parity Table Updates

- Added Session 1887 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` action `2`
  - `RepurchaseList.addRepurchaseItem`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect and model Java `CM_BUY_ITEM.runImpl` action `2` target/NPC gating as a non-live run plan, including `isAudit || player == null`, known-list target lookup, `DialogService.isInteractionAllowed`, and `npc.canBuy()` before dispatching to the existing repurchase planner.

Safe alternative candidates:

- Add an isolated `CmBuyItem` packet parser only if opcode registration and no-handler behavior can be kept safe and covered by tests.
- Wire `TradeSellToShopPlanService` only after inventory/repository/packet mutation ordering and rollback behavior are scoped.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
