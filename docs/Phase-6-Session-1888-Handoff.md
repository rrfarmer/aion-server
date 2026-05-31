# Phase 6 Session 1888 Handoff - CM_BUY_ITEM Repurchase Run Planner

Date: 2026-05-31
Unit of Work: UOW-1888
Status: Completed

## What Changed

- Added `CmBuyItemRepurchaseRunPlanService`, a non-live planner for Java `CM_BUY_ITEM.runImpl` action `2` repurchase dispatch gates.
- Added target-kind, input, run-plan, and dispatch descriptor records.
- Added focused tests for Java early exits, target branch behavior, interaction audit, NPC `canBuy()` gating, would-dispatch output, and optional `RepurchasePlan` payload carry-through.
- Kept this work isolated. No live opcode registration, socket handler, actual known-list lookup, live `DialogService` call, live `npc.canBuy()` query, singleton repurchase mutation, repository writes, packet fanout, audit logging side effects, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~NpcDialogInteractionAllowedPlanServiceTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused run planner slice passed with 12 tests.
- Related repurchase/read/dialog slice passed with 78 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4774 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The planner is non-live and is not wired into opcode `51` / `CM_BUY_ITEM`.
- Java known-list lookup is represented by caller-supplied target kind.
- Java `DialogService.isInteractionAllowed(player, npc)` is represented by caller-supplied interaction facts or the separate interaction planner.
- Java `npc.canBuy()` is represented by caller-supplied facts.
- Java `RepurchaseService.repurchaseFromShop` live mutation remains represented by the separate non-live `RepurchasePlanService`.

## Parity Table Updates

- Added Session 1888 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl` action `2`
  - the `RepurchaseService.repurchaseFromShop` call-site descriptor
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect whether an isolated `CmBuyItem` client packet parser can be added safely without live handler execution, covering opcode `51` registration risk, `PacketBuffer` field reads, and no-handler behavior before any live trade mutation is enabled.

Safe alternative candidates:

- Add a composition test that chains read-plan output through run-plan dispatch into `RepurchasePlanService`, staying non-live.
- Wire `TradeSellToShopPlanService` only after inventory/repository/packet mutation ordering and rollback behavior are scoped.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseRunPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemRepurchaseRunPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseReadPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemRepurchaseReadPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1888-Completion.md`
- `docs/Phase-6-Session-1888-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.readImpl`
  - Java `CM_BUY_ITEM.runImpl`
  - Java `AionClientPacketFactory` opcode `51`
  - C# `GameClientPacketFactory`
  - C# `PacketBuffer` strict/non-strict reads
  - C# `GameServerConnection` handler behavior for parsed packets without live handling
  - C# `CmBuyItemRepurchaseReadPlanService`
  - C# `CmBuyItemRepurchaseRunPlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
