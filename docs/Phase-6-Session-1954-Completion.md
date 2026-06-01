# Phase 6 Session 1954 Completion - Sell-To-Shop Repurchase State Payload

Date: 2026-06-01
Unit of Work: UOW-1954
Status: Completed

## Scope

- Integrated the disabled repurchase state replacement plan into the existing sell-to-shop diagnostic snapshot path.
- Kept the integration informational and non-live, without mutating singleton or player repurchase state.

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, Session 1953 completion, and Session 1953 handoff.
- Inspected Java `TradeService.performSellToShop` and `RepurchaseService.addRepurchaseItems`.
- Inspected C# `TradeSellToShopPlanService`, `RepurchaseDiagnosticSnapshotPlanService`, `RepurchaseStatePlanService`, and existing sell/repurchase tests.

## Changes

- Added `RepurchaseDiagnosticSnapshotPlan.StateReplacementPlan`.
- `RepurchaseDiagnosticSnapshotPlanService.CreateDisabledPlan` now creates a disabled `RepurchaseStateReplacePlan` for successful sell-to-shop plans.
- The integration can accept an explicit player object id and current snapshots, and still supports the existing `CreateDisabledPlan(sellToShopPlan)` call shape by inferring the player id from sell-plan facts.
- Extended sell-to-shop tests to assert:
  - successful sells carry a state replacement payload,
  - successful empty sell lists still replace the snapshot with an empty one,
  - supplied current snapshots are replaced for the seller while other players' snapshots remain,
  - blocked sell plans do not carry a replacement payload.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~RepurchaseStatePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# sell/repurchase diagnostic slice passed with 46 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5000 tests.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.

## Known Gaps

- The replacement payload is disabled and does not update live `Player.RepurchaseItems` or a singleton map.
- Java `HashSet` bucket iteration order is not emulated.
- Player object-id inference is a diagnostic fallback; future live integration should pass the player id directly.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellToShopPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1954-Completion.md`
- `docs/Phase-6-Session-1954-Handoff.md`

## Parity Position

- Partial Parity for disabled sell-to-shop repurchase state diagnostics.
- The integration is source-reviewed and C# unit-tested, but live Java/C# mutation timing, concurrency, set iteration, transaction behavior, and socket behavior remain unverified.
