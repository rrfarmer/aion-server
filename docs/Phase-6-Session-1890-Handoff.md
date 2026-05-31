# Phase 6 Session 1890 Handoff - CM_BUY_ITEM Repurchase Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1890
Status: Completed

## What Changed

- Added `CmBuyItemRepurchaseCompositionPlanService`, a non-live planner that chains parsed `CmBuyItem` values into `CmBuyItemRepurchaseReadPlanService` and then `CmBuyItemRepurchaseRunPlanService`.
- Added composition status, step, input, and plan records.
- Added focused tests for successful parser-to-dispatch intent, parser audit propagation, amount audit propagation, non-repurchase skip, interaction audit ordering, and optional `RepurchasePlan` carry-through.
- Kept this work non-live. No socket handler, actual known-list lookup, live `DialogService` call, live NPC state query, singleton repurchase mutation, repository writes, packet fanout, audit logging side effects, Java runtime capture, or real client validation was enabled.

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
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC `canBuy()`, and repurchase singleton access are represented by supplied facts and existing planners.
- Java action `1` sell-to-shop and action `13`-`16` buy-from-shop composition remain outside this unit.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemRepurchaseCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseReadPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseRunPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1890-Completion.md`
- `docs/Phase-6-Session-1890-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.readImpl`
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellToShop`
  - C# `CmBuyItem`
  - C# `CmBuyItemRepurchaseCompositionPlanService`
  - C# `TradeSellToShopPlanService`
  - C# `GameServerConnection` handler behavior for parsed packets without live handling
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
