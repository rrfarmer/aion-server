# Phase 6 Session 1955 Completion - Repurchase Outcome State Removal Payload

Date: 2026-06-01
Unit of Work: UOW-1955
Status: Completed

## Scope

- Added a disabled post-repurchase state-removal payload for Java `RepurchaseService.repurchaseFromShop -> items.remove(repurchaseItem)`.
- Kept the integration supplied-snapshot only and non-live, without mutating singleton or player repurchase state.

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, Session 1954 completion, and Session 1954 handoff.
- Inspected Java `RepurchaseService.repurchaseFromShop`.
- Inspected C# `RepurchasePlanService`, `RepurchaseOutcomePlanService`, `RepurchaseStatePlanService`, and existing repurchase/CM_BUY_ITEM outcome tests.

## Changes

- Added `RepurchaseStateItemRemovalPlan`.
- Added `RepurchaseStatePlanService.CreateRemoveItemsDisabledPlan`, which removes supplied item object IDs from a supplied player snapshot and records missing IDs.
- `RepurchaseOutcomePlanService.CreateDisabledPlan` now accepts optional player object id and current snapshots.
- Successful repurchase outcomes with supplied snapshot context now carry `StateItemRemovalPlan`.
- Existing no-context callers remain source-compatible and leave `StateItemRemovalPlan` null.
- Added/extended tests for:
  - removing item IDs from a supplied snapshot without removing the map entry,
  - missing snapshot behavior,
  - repurchase outcome carrying a post-removal state payload,
  - repurchase outcome remaining no-context diagnostic-only when snapshot context is omitted.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchaseStatePlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# repurchase state/outcome slice first failed on a test-only JavaSource assertion, then passed with 37 tests after the breadcrumb was corrected.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5004 tests.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.

## Known Gaps

- The state-removal payload is disabled and does not update live `Player.RepurchaseItems` or a singleton map.
- Existing `CmBuyItemSideEffectOutcomePlanService` does not pass snapshot context yet.
- Java `HashSet` bucket iteration order is not emulated.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchaseStatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchaseStatePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1955-Completion.md`
- `docs/Phase-6-Session-1955-Handoff.md`

## Parity Position

- Partial Parity for disabled post-repurchase item-removal state planning.
- The implementation is source-reviewed and C# unit-tested, but live Java/C# mutation timing, concurrency, set iteration, transaction behavior, and socket behavior remain unverified.
