# Phase 6 Session 1887 Handoff - CM_BUY_ITEM Repurchase Read Planner

Date: 2026-05-31
Unit of Work: UOW-1887
Status: Completed

## What Changed

- Added `CmBuyItemRepurchaseReadPlanService`, a non-live planner for Java `CM_BUY_ITEM.readImpl` action `2`.
- Added request item and plan records for source-reviewed read/list behavior.
- Added focused tests for Java amount and exploit guards, amount-limited processing, `RepurchaseList` filtering, `LinkedHashSet` order/de-duplication, non-repurchase action handling, and action `0` non-positive item-index allowance.
- Kept this work isolated. No live `CM_BUY_ITEM` opcode registration, socket handler, target lookup, NPC gating, singleton repurchase state, repository writes, packet fanout, audit logging, Java runtime capture, or real client validation was enabled.

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
- The planner is non-live and is not wired into opcode `51` / `CM_BUY_ITEM`.
- Packet byte parsing is represented by supplied values, not a live C# `PacketBuffer` client packet.
- Java `RepurchaseService.canRepurchase(player, itemObjectId)` is represented by caller-supplied repurchasable ids.
- Java `CM_BUY_ITEM.runImpl` action `2` target lookup, interaction gate, NPC `canBuy()`, and live `RepurchaseService.repurchaseFromShop` dispatch remain unmodeled.

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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseReadPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemRepurchaseReadPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellToShopPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1887-Completion.md`
- `docs/Phase-6-Session-1887-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM` action `2`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `DialogService.isInteractionAllowed`
  - Java NPC `canBuy()` semantics
  - C# `NpcDialogInteractionAllowedPlanService`
  - C# known-list/target lookup planners, if any
  - C# `RepurchasePlanService`
  - C# client packet parser infrastructure
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
