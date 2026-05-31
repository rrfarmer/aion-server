# Phase 6 Session 1889 Handoff - CM_BUY_ITEM Packet Parser

Date: 2026-05-31
Unit of Work: UOW-1889
Status: Completed

## What Changed

- Added `CmBuyItem`, an isolated C# client packet parser for Java `CM_BUY_ITEM`.
- Added `CmBuyItemEntry`.
- Registered opcode `51` as InGame-only in `GameClientPacketFactory`, matching Java `AionClientPacketFactory`.
- Added focused tests for field order, registration state, Java amount guard, item/count guards, action `0` non-positive item-index allowance, and invalid-item break behavior.
- Kept this work parser-only. No live socket handler, run dispatch, known-list lookup, trade mutation, repurchase filtering, repository writes, packet fanout, audit logging side effects, Java runtime capture, or real client validation was enabled.

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
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- `CmBuyItem` is parser-only and does not execute Java `runImpl` behavior.
- Java `RepurchaseList.addRepurchaseItem` filtering remains represented by `CmBuyItemRepurchaseReadPlanService`, not by the raw packet parser.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseReadPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseRunPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1889-Completion.md`
- `docs/Phase-6-Session-1889-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.readImpl`
  - Java `CM_BUY_ITEM.runImpl`
  - C# `CmBuyItem`
  - C# `CmBuyItemRepurchaseReadPlanService`
  - C# `CmBuyItemRepurchaseRunPlanService`
  - C# `RepurchasePlanService`
  - C# `GameServerConnection` handler behavior for parsed packets without live handling
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
