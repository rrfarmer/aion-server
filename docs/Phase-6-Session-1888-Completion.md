# Phase 6 Session 1888 Completion - CM_BUY_ITEM Repurchase Run Planner

Date: 2026-05-31
Unit of Work: UOW-1888
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM.runImpl` action `2` target and NPC gating.
- Added a non-live C# planner for the run-side repurchase dispatch path.
- Kept live opcode registration, socket handlers, known-list lookup, live interaction checks, singleton repurchase state, repository writes, packet fanout, audit logging side effects, and Java runtime capture out of scope.

## What Changed

- Added `CmBuyItemRepurchaseRunPlanService`.
- Added target-kind, input, run-plan, and dispatch descriptor records.
- Added focused tests for audit/player early exits, non-repurchase action skip, target miss, non-NPC target skip, interaction audit ordering, NPC `canBuy()` skip, would-dispatch behavior, and optional `RepurchasePlan` payload carry-through.

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
- The planner is non-live and is not wired into opcode `51` / `CM_BUY_ITEM`.
- Java known-list lookup is represented by caller-supplied target kind.
- Java `DialogService.isInteractionAllowed(player, npc)` is represented by caller-supplied interaction facts or the separate interaction planner.
- Java `npc.canBuy()` is represented by caller-supplied facts.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, and real-client validation remain unimplemented.

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
