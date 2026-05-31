# Phase 6 Session 1884 Handoff - Repurchase Packet Shape

Date: 2026-05-31
Unit of Work: UOW-1884
Status: Completed

## What Changed

- Added `SmRepurchase`.
- Added `RepurchasePacketItem`.
- Added `SmRepurchaseTests`.
- The packet is source-reviewed against Java `SM_REPURCHASE.writeImpl`.
- It writes:
  - target NPC object id
  - constant `1`
  - item count
  - per-item object id
  - per-item template id
  - per-item l10n string
  - per-item full item-info blob through `SmInventoryInfo.WriteItemInfoBlob`
  - per-item repurchase price
- Kept this work isolated. No live dialog wiring, `CM_BUY_ITEM` handling, repository writes, packet fanout, Java runtime capture, audit logging, or real repurchase state mutation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmRepurchaseTests|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused repurchase packet slice passed with 3 tests.
- Related repurchase packet/planner slice passed with 17 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4744 tests.

## Known Gaps

- No Java runtime/golden packet bytes were captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- `SmRepurchase` is not wired into `DialogService`/BUY_AGAIN or any live socket send path.
- C# has no live `RepurchaseService` state store equivalent for Java's singleton map.
- Java `CM_BUY_ITEM` action `2` read/run validation, target NPC checks, audit logging, and `npc.canBuy()` gating remain unwired.
- Item-info blob parity is inherited from existing shared packet helpers and remains unverified for Java `SM_REPURCHASE` specifically.

## Parity Table Updates

- Added Session 1884 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `SM_REPURCHASE`
  - `DialogService` BUY_AGAIN packet boundary
  - `CM_BUY_ITEM` repurchase action `2`
- All rows remain `Partial Parity` or `Needs Verification`; none are Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `RepurchaseService.addRepurchaseItems`, `TradeService.performSellToShop`, and C# sell-to-shop surfaces to decide whether a small non-live sell-to-shop repurchase-capture planner can be added without touching live packet/repository ordering.

Safe alternative candidates:

- Wire `SmRepurchase` into a non-live dialog composition plan only if existing `NpcDialogServiceSelectPlanService` boundaries make it safe.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Wire `PrivateStorePurchasePlanService` only after repository/packet mutation ordering is scoped and tests can cover no-partial-send behavior.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmRepurchase.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmRepurchaseTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryInfo.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1884-Completion.md`
- `docs/Phase-6-Session-1884-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing repurchase work, inspect:
  - Java `TradeService.performSellToShop`
  - Java `RepurchaseService.addRepurchaseItems`
  - Java `RepurchaseService.getRepurchaseItems`
  - Java `SM_REPURCHASE`
  - C# sell-to-shop surfaces, if any
  - C# `SmRepurchase`
  - C# `RepurchasePlanService`
- If JDK 25 and Maven become available, prioritize the condition preview Java capture draft before further source-only work.
