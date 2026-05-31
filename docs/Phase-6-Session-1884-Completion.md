# Phase 6 Session 1884 Completion - Repurchase Packet Shape

Date: 2026-05-31
Unit of Work: UOW-1884
Status: Completed

## Scope

- Performed Work Discovery around Java `SM_REPURCHASE` and repurchase packet/state surfaces.
- Added the C# server packet shape for repurchase-list display.
- Reused the existing item-info blob writer to keep packet item encoding aligned with inventory packets.
- Kept live dialog wiring, `CM_BUY_ITEM` handling, repository writes, packet send ordering, and Java runtime capture out of scope.

## What Changed

- Added `SmRepurchase`.
- Added `RepurchasePacketItem`.
- Added `SmRepurchaseTests`.
- The packet models Java `SM_REPURCHASE.writeImpl` field order:
  - target NPC object id
  - constant `1`
  - item count
  - per-item object id
  - per-item template id
  - per-item l10n string
  - per-item full item-info blob
  - per-item repurchase price

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
