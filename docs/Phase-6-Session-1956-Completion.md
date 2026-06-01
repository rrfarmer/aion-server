# Phase 6 Session 1956 Completion - CM_BUY_ITEM Repurchase State Context

Date: 2026-06-01
Unit of Work: UOW-1956
Status: Completed

## Scope

- Threaded supplied repurchase snapshot context into the disabled `CM_BUY_ITEM` repurchase side-effect outcome path.
- Kept the integration diagnostic-only and non-live, without mutating `Player.RepurchaseItems` or a singleton map.

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, Session 1955 completion, and Session 1955 handoff.
- Inspected Java `CM_BUY_ITEM.runImpl` action 2.
- Inspected Java `RepurchaseService.repurchaseFromShop`.
- Inspected C# `CmBuyItemSideEffectOutcomePlanService`, `GameServerConnection.HandleBuyItem`, `CmBuyItemHandlerCompositionPlanService`, and existing CM_BUY_ITEM/repurchase tests.

## Changes

- `CmBuyItemSideEffectOutcomePlanService.CreateDisabledPlan` now accepts optional repurchase player object id and current repurchase snapshots.
- The selected repurchase outcome branch passes those facts into `RepurchaseOutcomePlanService.CreateDisabledPlan`, allowing the UOW-1955 `StateItemRemovalPlan` to appear in CM_BUY_ITEM action 2 side-effect outcomes.
- `GameServerConnection.HandleBuyItem` now supplies a one-player `RepurchaseStateSnapshot` from `Player.RepurchaseItems` for action 2 repurchase diagnostics.
- Existing callers remain source-compatible because the new parameters are optional.
- Added/extended tests for:
  - no-context repurchase outcomes still leaving `StateItemRemovalPlan` null,
  - supplied-snapshot CM_BUY_ITEM side-effect outcomes carrying state-removal payloads,
  - GameServerConnection repurchase diagnostics carrying the state-removal payload from live player facts.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# CM_BUY_ITEM/repurchase slice first failed at compile time due test-only `Assert.NotNull` return-value use, then passed with 53 tests after fixing the assertions.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite first timed out at 180 seconds without a test failure report, then passed with 5005 tests when rerun with a longer timeout.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.

## Known Gaps

- The CM_BUY_ITEM repurchase state-removal payload is disabled and does not update live `Player.RepurchaseItems` or a singleton map.
- Java `HashSet` bucket iteration order is not emulated.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1956-Completion.md`
- `docs/Phase-6-Session-1956-Handoff.md`

## Parity Position

- Partial Parity for disabled CM_BUY_ITEM repurchase state-context wiring.
- The implementation is source-reviewed and C# unit-tested, but live Java/C# mutation timing, concurrency, set iteration, transaction behavior, and socket behavior remain unverified.
