# Phase 6 Session 1955 Handoff - Repurchase Outcome State Removal Payload

Date: 2026-06-01
Unit of Work: UOW-1955
Status: Completed

## What Changed

- Added `RepurchaseStateItemRemovalPlan`, a disabled supplied-snapshot plan for Java `RepurchaseService.repurchaseFromShop -> items.remove(repurchaseItem)`.
- Added `RepurchaseStatePlanService.CreateRemoveItemsDisabledPlan`.
- `RepurchaseOutcomePlanService.CreateDisabledPlan` can now carry `StateItemRemovalPlan` when callers provide player object id plus current repurchase snapshots.
- Existing no-context callers remain valid and leave `StateItemRemovalPlan` null.
- This unit does not implement live singleton state, Java `HashSet` bucket iteration order, socket dispatch, persistence, transaction behavior, or real-client validation.

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

- `StateItemRemovalPlan` is disabled and informational only.
- `CmBuyItemSideEffectOutcomePlanService` does not pass snapshot context yet.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java `HashSet` bucket iteration order is not emulated.
- Returned Java set mutability, concurrent map/set timing, logout removal integration, live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchaseStatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchaseStatePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1955-Completion.md`
- `docs/Phase-6-Session-1955-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.model.trade.RepurchaseList`
- `com.aionemu.gameserver.model.gameobjects.AionObject`

## C# Artifacts Touched

- `Aion.GameServer.Services.RepurchaseStatePlanService`
- `Aion.GameServer.Services.RepurchaseStateItemRemovalPlan`
- `Aion.GameServer.Services.RepurchaseOutcomePlanService`
- `Aion.GameServer.Services.RepurchaseOutcomePlan`
- `Aion.GameServer.Tests.RepurchaseStatePlanServiceTests`
- `Aion.GameServer.Tests.RepurchasePlanServiceTests`

## Parity Table Updates

- Added Session 1955 rows to `PHASE-6-PROGRESS.md` for:
  - `RepurchaseService.repurchaseFromShop` item removal branch
  - `RepurchaseService.repurchaseFromShop` final outcome
  - `AionObject.hashCode/equals` repurchase set dependency

## Next Recommended Unit of Work

- Next sequential task: inspect how to thread supplied repurchase snapshot context into the disabled `CM_BUY_ITEM` repurchase side-effect outcome path, so `CmBuyItemSideEffectOutcomePlanService` can carry the new `StateItemRemovalPlan` without enabling live mutation.

Safe alternative candidates:

- Add another narrow Java golden item-info vector only if the fixture remains simple, such as equipped-slot nonzero or one basic manastone socket.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase state work, inspect `CmBuyItemSideEffectOutcomePlanService.CreateRepurchaseOutcomePlan` and `CmBuyItemHandlerCompositionPlan` payloads. The likely safe slice is to add optional repurchase snapshot context to the side-effect outcome input without touching live handler dispatch.
- Avoid claiming Java `HashSet` iteration parity; UOW-1955 only models supplied-snapshot item removal.
