# Phase 6 Session 1889 Completion - CM_BUY_ITEM Packet Parser

Date: 2026-05-31
Unit of Work: UOW-1889
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM.readImpl`, Java opcode `51` registration, C# packet factory behavior, and existing client packet tests.
- Added an isolated C# parser for the raw `CM_BUY_ITEM` payload and registered opcode `51`.
- Kept live socket handlers, run dispatch, known-list lookup, live trade mutation, repurchase filtering, repository writes, packet fanout, audit logging side effects, and Java runtime capture out of scope.

## What Changed

- Added `CmBuyItem`.
- Added `CmBuyItemEntry`.
- Registered opcode `51` as InGame-only in `GameClientPacketFactory`.
- Added focused parser tests for field order, state registration, amount guard, item/count exploit guards, action `0` item-index allowance, and break behavior on invalid items.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused parser slice passed with 9 tests.
- Related parser/read/run/repurchase slice passed with 47 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4783 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- `CmBuyItem` is parser-only and does not execute Java `runImpl` behavior.
- Java `RepurchaseList.addRepurchaseItem` filtering remains represented by the separate non-live read planner.
- Java `TradeList.addItem` behavior for non-repurchase actions is represented only as raw item tuple collection here.
- Live socket handling, known-list lookup, packet fanout, repository persistence, transaction behavior, audit logging side effects, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1889 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` raw parser
  - `AionClientPacketFactory` opcode `51`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a non-live composition test/service that chains `CmBuyItem` parser output into `CmBuyItemRepurchaseReadPlanService` and then `CmBuyItemRepurchaseRunPlanService`, proving the full parser-to-dispatch intent remains non-live and does not mutate trade/repurchase state.

Safe alternative candidates:

- Wire `TradeSellToShopPlanService` only after inventory/repository/packet mutation ordering and rollback behavior are scoped.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Inspect Java `CM_BUY_ITEM` action `1` sell-to-shop parser/run composition if staying non-live.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
