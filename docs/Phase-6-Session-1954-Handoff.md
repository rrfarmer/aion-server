# Phase 6 Session 1954 Handoff - Sell-To-Shop Repurchase State Payload

Date: 2026-06-01
Unit of Work: UOW-1954
Status: Completed

## What Changed

- Integrated the disabled `RepurchaseStatePlanService` replacement payload into `RepurchaseDiagnosticSnapshotPlan`.
- Successful `TradeSellToShopPlan` diagnostics now carry an optional `RepurchaseStateReplacePlan` that models `TradeService.performSellToShop -> RepurchaseService.addRepurchaseItems(player, items)`.
- Existing `CreateDisabledPlan(sellToShopPlan)` callers remain valid; the service infers the player id from sell-plan facts when one is not provided.
- New tests cover explicit current-snapshot replacement, empty successful sell lists, and blocked sell-plan no-payload behavior.
- This unit does not implement live singleton state, Java `HashSet` bucket iteration order, socket dispatch, persistence, transaction behavior, or real-client validation.

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

- `StateReplacementPlan` is disabled and informational only.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java `HashSet` bucket iteration order is not emulated.
- Player object-id inference should be replaced by explicit player id when live caller integration exists.
- Returned Java set mutability, concurrent map timing, logout removal integration, live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellToShopPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1954-Completion.md`
- `docs/Phase-6-Session-1954-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.TradeService`
- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.model.gameobjects.AionObject`

## C# Artifacts Touched

- `Aion.GameServer.Services.RepurchaseDiagnosticSnapshotPlanService`
- `Aion.GameServer.Services.RepurchaseDiagnosticSnapshotPlan`
- `Aion.GameServer.Services.RepurchaseStatePlanService`
- `Aion.GameServer.Tests.TradeSellToShopPlanServiceTests`

## Parity Table Updates

- Added Session 1954 rows to `PHASE-6-PROGRESS.md` for:
  - `TradeService.performSellToShop` repurchase snapshot boundary
  - `RepurchaseService.addRepurchaseItems` via diagnostic replacement payload
  - `AionObject.hashCode/equals` repurchase dedupe dependency

## Next Recommended Unit of Work

- Next sequential task: add an equivalent disabled state-removal payload to `RepurchaseOutcomePlanService` so successful `RepurchaseService.repurchaseFromShop -> items.remove(repurchaseItem)` outcomes can carry the post-repurchase snapshot plan without mutating live state.

Safe alternative candidates:

- Add another narrow Java golden item-info vector only if the fixture remains simple, such as equipped-slot nonzero or one basic manastone socket.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase state work, inspect `RepurchasePlanService.CreatePlan` and `RepurchaseOutcomePlanService.CreateDisabledPlan`; the likely safe slice is to carry a disabled post-repurchase state plan for removed item object IDs.
- Avoid claiming Java `HashSet` iteration parity; UOW-1954 only threads the disabled replacement payload through sell diagnostics.
