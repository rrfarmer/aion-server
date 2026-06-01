# Phase 6 Session 1953 Handoff - Repurchase State Lifecycle Planner

Date: 2026-06-01
Unit of Work: UOW-1953
Status: Completed

## What Changed

- Added `RepurchaseStatePlanService`, a disabled non-live C# planner for Java `RepurchaseService` state lifecycle behavior.
- The planner records:
  - `addRepurchaseItems` map-entry replacement.
  - nonzero object-id dedupe from Java `new HashSet<>(items)`.
  - `getRepurchaseItems` found and missing-key `Collections.emptySet()` behavior.
  - `removeRepurchaseItems` present-key removal and absent-key no-op behavior.
  - `canRepurchase` object-id membership checks.
- Added focused C# tests for each disabled lifecycle boundary.
- This unit does not implement live singleton state, Java `HashSet` bucket iteration order, socket dispatch, persistence, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchaseStatePlanServiceTests|FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# repurchase state/packet/execution planner slice passed with 23 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 4999 tests.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.

## Known Gaps

- C# still has no live `RepurchaseService` singleton map equivalent.
- The disabled planner dedupes nonzero object IDs, but it does not emulate Java `HashSet` bucket iteration order.
- Java dummy object-id `0` equality/identity behavior is not fully modeled.
- Returned Java set mutability and concurrent map timing remain unverified.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchaseStatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchaseStatePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1953-Completion.md`
- `docs/Phase-6-Session-1953-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.model.trade.RepurchaseList`
- `com.aionemu.gameserver.model.gameobjects.AionObject`

## C# Artifacts Touched

- `Aion.GameServer.Services.RepurchaseStatePlanService`
- `Aion.GameServer.Services.RepurchaseStateSnapshot`
- `Aion.GameServer.Tests.RepurchaseStatePlanServiceTests`

## Parity Table Updates

- Added Session 1953 rows to `PHASE-6-PROGRESS.md` for:
  - `RepurchaseService.addRepurchaseItems`
  - `RepurchaseService.getRepurchaseItems`
  - `RepurchaseService.removeRepurchaseItems`
  - `RepurchaseService.canRepurchase`
  - `AionObject.hashCode/equals`

## Next Recommended Unit of Work

- Next sequential task: integrate `RepurchaseStatePlanService` into the existing disabled sell-to-shop diagnostic snapshot path so `TradeService.performSellToShop -> RepurchaseService.addRepurchaseItems` can carry an explicit replacement plan, while still avoiding live singleton mutation.

Safe alternative candidates:

- Add another narrow Java golden item-info vector only if the fixture remains simple, such as equipped-slot nonzero or one basic manastone socket.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase state work, inspect C# `TradeSellToShopPlanService.RepurchaseDiagnosticSnapshotPlanService` and decide whether to attach or replace it with the new explicit state replacement plan.
- Avoid claiming Java `HashSet` iteration parity; UOW-1953 only models lifecycle and nonzero object-id dedupe.
