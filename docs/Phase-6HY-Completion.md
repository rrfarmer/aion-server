# Phase 6HY Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HX and covers Session 721.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "EquipmentObserverBurnWorkflowServiceTests|IdianPolishServiceTests|IdianPolishBurnApplicationServiceTests|ItemChargeServiceTests|ItemChargeBurnApplicationServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 47 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1297 tests.

## Recent Work Completed

### Session 721 - Equipment Observer Burn Workflow Seam

- Added `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync`.
- Added `EquipmentObserverBurnEvent`.
- Added `EquipmentObserverBurnWorkflowResult`.
- The workflow maps one represented observer event into both idian and charge observer burns.
- It applies in-memory inventory updates and gathers generated `SmInventoryUpdateItem` packets.
- It optionally invokes persistence delegates for idian burn deletes and charge burn updates.
- It applies idian burns before charge burns, matching Java `ItemEquipmentListener.onItemEquipment` observer registration order.
- Added `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_AppliesIdianBeforeChargeAndRequestsBothPersistenceBoundaries`.
- Added `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_SkipsSkillAttackButAllowsDotAttacked`.
- Added `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_ReportsPersistenceFailureAfterApplyingPackets`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.stats.listeners.ItemEquipmentListener.onItemEquipment` | `Aion.GameServer.Services.EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` | Observer Workflow / Service | Partial | Regression Tested | Needs Verification | C# now has a reusable seam that sequences idian observer burns before conditioning charge burns, matching Java observer registration order in `onItemEquipment`. Actual `ObserveController` registration, live observer dispatch, stat recalculation, summon stat updates, item-set recalculation, enchant/tempering behavior, threading, and production combat/effect caller invocation remain unported or outside this unit. |
| `com.aionemu.gameserver.model.items.IdianStone.onEquip` | `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` via `IdianPolishService.BurnEquippedWeaponPolishChargeForObserverEvent` | Observer Dependency | Partial | Regression Tested | Needs Verification | Workflow invokes the represented idian observer burn path first and gathers `POLISH_CHARGE` or exhausted full update packets. Java `RandomBonusEffect.applyEffect/endEffect`, exact exhausted packet-before-clear serialization, DAO ordering, and live stat/effect fanout remain unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo.attack` | `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` via `ItemChargeService.BurnEquippedChargePoints` | Observer Callback Dependency | Partial | Regression Tested | Needs Verification | C# workflow preserves the outgoing attack `skillId == 0` guard through the charge service and can request charge persistence. Real attack observer dispatch and packet ordering relative to combat status packets remain unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo.attacked` | `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` via `ItemChargeService.BurnEquippedChargePoints` | Observer Callback Dependency | Partial | Regression Tested | Needs Verification | C# workflow maps incoming attacked events to defend burn data and can request charge persistence. Real incoming damage caller and threading behavior remain unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo.dotattacked` | `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` via `ItemChargeService.BurnEquippedChargePoints` | Observer Callback Dependency | Partial | Regression Tested | Needs Verification | C# workflow maps dot-attacked events to defend burn data while allowing nonzero skill ids, matching the represented charge observer helper. Dot effect scheduling, lifecycle ordering, and live effect dispatch remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `EquipmentObserverBurnWorkflowResult.Packets` containing `SmInventoryUpdateItem` | Packet Fanout Dependency | Partial | Regression Tested with byte-level payload checks | Needs Verification | Tests verify idian compact polish packets are ordered before charge compact packets for the represented event. No Java golden observer-dispatch capture, encrypted-frame comparison, socket ordering comparison, or live-client validation was run. |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.storeIdianStones` | Optional `saveIdianPolishBurnAsync` delegate supplied to `EquipmentObserverBurnWorkflowService` | Persistence Dependency | Partial | Unit Tested with delegate capture | Needs Verification | Workflow can request exhausted-idian persistence through a delegate, but no live DAO, transaction/autocommit, rollback, or Java delete-order comparison was run. |
| Java item charge persistence through item `PersistentState.UPDATE_REQUIRED` | Optional `saveItemChargeBurnAsync` delegate supplied to `EquipmentObserverBurnWorkflowService` | Persistence Dependency | Partial | Unit Tested with delegate capture | Needs Verification | Workflow can request charge persistence through a delegate. Java's persistent-state batching may differ from the direct C# persistence boundary; live DB comparison and production caller policy remain unresolved. |

## Tests Added Or Updated

- `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_AppliesIdianBeforeChargeAndRequestsBothPersistenceBoundaries`
  - Validates idian burn is applied before charge burn, compact packet ordering, in-memory inventory updates, and delegate-based persistence requests for both plans.
- `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_SkipsSkillAttackButAllowsDotAttacked`
  - Validates the shared workflow preserves Java's outgoing attack `skillId == 0` guard while allowing dot-attacked burns and producing both idian and charge packets when both thresholds are crossed.
- `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_ReportsPersistenceFailureAfterApplyingPackets`
  - Validates the workflow reports delegate persistence failure without hiding generated packets.
- Existing idian, charge, application, and enter-world persistence tests matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, live `ObserveController` dispatch, Java-generated golden packets, packet ordering relative to combat status packets, threading behavior, live DAO behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 8
- Total artifacts ported or partially modeled in this handoff window: 1 shared equipment observer burn workflow slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: production combat/effect caller invocation, live ObserveController dispatch, combat packet ordering comparison, Java runtime/golden packet comparison, live DAO comparison, and synchronized/threading comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- The workflow seam is not yet invoked from a production combat/effect caller.
- Java observer registration/removal and dispatch ordering are represented from source, but not proven against live `ObserveController` behavior.
- Packet ordering relative to `SM_ATTACK_STATUS`, damage/effect packets, stat updates, persistence, and observer callbacks remains unverified.
- Exhausted-idian serialization may still differ because Java sends the full item update before clearing `item.setIdianStone(null)`.
- Delegate persistence is a caller seam, not live DAO proof; transaction/autocommit/rollback behavior remains unverified.
- No Java runtime, live database, or live-client comparison was run.

## Next Recommended Unit of Work

Invoke `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` from the first stable represented combat/effect caller that can safely send returned packets and supply `PlayerEnterWorldService.SaveIdianPolishBurnMutationAsync` / `SaveItemChargeBurnMutationAsync` delegates. If that caller remains premature, add focused coverage for idian/charge packet ordering around an existing represented damage or dot-effect workflow without changing production routing.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HX-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
