# Phase 6 Session 1890 Completion - CM_BUY_ITEM Repurchase Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1890
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM` action `2` read-to-run flow and current C# parser/read/run planners.
- Added a non-live composition planner that chains parsed packet output through repurchase read and run plans.
- Kept live socket handlers, known-list lookup, live interaction checks, NPC state queries, singleton repurchase state, repository writes, packet fanout, audit logging side effects, and Java runtime capture out of scope.

## What Changed

- Added `CmBuyItemRepurchaseCompositionPlanService`.
- Added composition status, step, input, and plan records.
- Added focused tests for successful parser-to-dispatch intent, parser audit propagation, amount audit propagation, non-repurchase skip, interaction audit ordering, and optional `RepurchasePlan` payload carry-through.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused composition slice passed with 6 tests.
- Related parser/read/run/repurchase slice passed with 53 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4789 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC `canBuy()`, and repurchase singleton access are represented by supplied facts and existing planners.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1890 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` action `2` read-to-run flow
  - the `RepurchaseService.repurchaseFromShop` dispatch intent composition
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `CM_BUY_ITEM` action `1` sell-to-shop read/run composition and add a non-live composition plan only if it can reuse existing parser and `TradeSellToShopPlanService` without live inventory/repository mutation.

Safe alternative candidates:

- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Inspect Java action `13`-`16` buy-from-shop run gating if staying non-live.
- Inspect live `GameServerConnection` handler gaps for `CmBuyItem` without enabling side effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
