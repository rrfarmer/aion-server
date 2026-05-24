# Phase 6HZ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HY and covers Session 722.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "WorldNpcDamageServiceTests|EquipmentObserverBurnWorkflowServiceTests|IdianPolishServiceTests|ItemChargeServiceTests|IdianPolishBurnApplicationServiceTests|ItemChargeBurnApplicationServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 79 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1299 tests.

## Recent Work Completed

### Session 722 - Skill Damage Observer Burn Integration

- Extended `WorldNpcSkillDamageService` with optional item-template and persistence delegate dependencies for equipment observer burns.
- `ApplyDamageEffectAsync` invokes `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` when represented skill mappings emit effector attack or dot-attacked observer notifications.
- `WorldNpcSkillDamageResult` now carries the optional `EquipmentObserverBurnWorkflowResult`.
- The skill-damage service returns generated idian/charge packets for outer production callers to send in the correct context.
- Added `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_AppliesEquipmentObserverBurnsForOrdinaryAttack`.
- Added `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_AppliesEquipmentObserverBurnsForDotAttack`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.DamageEffect.applyEffect` | `Aion.GameServer.Services.WorldNpcSkillDamageService.ApplyDamageEffectAsync` | Skill Effect Caller | Partial | Regression Tested | Needs Verification | C# now invokes represented equipment observer burn workflow for ordinary attack-style damage effects when an effector attack observer notification is produced. Full Java `SkillEngine`, `Effect` lifecycle, `AttackUtil`, shield/result list integration, and live client packet ordering remain partial. |
| `com.aionemu.gameserver.skillengine.effect.AbstractOverTimeEffect.onPeriodicAction` / periodic damage effects | `WorldNpcSkillDamageService.ApplyDamageEffectAsync` through dot-attacked mappings | Skill Effect Caller / Dot Callback | Partial | Regression Tested | Needs Verification | C# now invokes the equipment observer burn workflow for represented periodic/dot damage mappings that produce dot-attacked observer notifications. Actual scheduled effect task lifecycle, live dot effect identity, and Java `Effect` ordering remain unverified. |
| `com.aionemu.gameserver.controllers.observer.ActionObserver.attack` | `WorldNpcSkillDamageResult.EquipmentObserverBurns` via `EquipmentObserverBurnWorkflowService` | Observer Callback Integration | Partial | Regression Tested | Needs Verification | Ordinary attack test validates idian and charge burns are planned/applied and persistence delegates are requested when skill id is zero. Real observe-controller dispatch, production socket send order, and attack-result sequencing remain unverified. |
| `com.aionemu.gameserver.controllers.observer.ActionObserver.dotattacked` | `WorldNpcSkillDamageResult.EquipmentObserverBurns` via `EquipmentObserverBurnWorkflowService` | Observer Callback Integration | Partial | Regression Tested | Needs Verification | Dot attack test validates nonzero skill id dot-attacked burns produce idian and charge packet results. Real Java periodic action dispatch and live effect ordering remain unverified. |
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `EquipmentObserverBurnWorkflowService` invoked from `WorldNpcSkillDamageService` | Model Helper Dependency | Partial | Regression Tested | Needs Verification | Integration now reaches the represented idian burn path from a represented skill damage caller. Java synchronization, exhausted-idian packet-before-clear ordering, `RandomBonusEffect` stat refresh, and DAO timing remain unresolved. |
| `com.aionemu.gameserver.model.items.ChargeInfo.updateChargePoints` | `EquipmentObserverBurnWorkflowService` invoked from `WorldNpcSkillDamageService` | Model Helper Dependency | Partial | Regression Tested | Needs Verification | Integration now reaches the represented charge burn path from a represented skill damage caller. Java `PersistentState` batching, synchronized mutation, and live persistence timing remain unresolved. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `WorldNpcSkillDamageResult.EquipmentObserverBurns.Packets` | Packet Fanout Dependency | Partial | Regression Tested with byte-level payload checks | Needs Verification | Tests verify skill-damage caller returns low-charge idian and compact charge update packets in represented order. The service returns packets but does not send them; production connection send ordering relative to `SM_ATTACK_STATUS` remains unverified. |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.storeIdianStones` | `WorldNpcSkillDamageService` optional idian persistence delegate | Persistence Dependency | Partial | Unit Tested with delegate capture | Needs Verification | Ordinary attack test verifies the skill-damage caller can request idian persistence through the workflow delegate. No live DAO, transaction/autocommit, rollback, or Java delete-order comparison was run. |
| Java item charge persistence through item `PersistentState.UPDATE_REQUIRED` | `WorldNpcSkillDamageService` optional charge persistence delegate | Persistence Dependency | Partial | Unit Tested with delegate capture | Needs Verification | Ordinary attack test verifies the skill-damage caller can request charge persistence through the workflow delegate. Java's later item flush versus direct delegate persistence remains a known difference needing production policy and DB validation. |

## Tests Added Or Updated

- `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_AppliesEquipmentObserverBurnsForOrdinaryAttack`
  - Validates represented ordinary attack damage with `skillId == 0` invokes equipment observer burns, mutates idian/charge state, returns idian-before-charge packets, and calls both persistence delegates.
- `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_AppliesEquipmentObserverBurnsForDotAttack`
  - Validates represented periodic spell/dot damage invokes dot-attacked equipment observer burns with a nonzero skill id and returns idian-before-charge packets.
- Existing world NPC damage, equipment observer workflow, idian, charge, application, and enter-world persistence tests matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, live `ObserveController` dispatch, Java-generated golden packets, packet ordering relative to `SM_ATTACK_STATUS`, threading behavior, live DAO behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 9
- Total artifacts ported or partially modeled in this handoff window: 1 represented skill-damage observer burn integration slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: production packet-send ordering, full SkillEngine/AttackUtil integration, live ObserveController dispatch, Java runtime/golden packet comparison, live DAO comparison, and synchronized/threading comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- `WorldNpcSkillDamageService` returns observer burn packets but does not yet send them through `GameServerConnection`; production packet ordering relative to `SM_ATTACK_STATUS` is still unresolved.
- The integration is limited to represented NPC skill damage/dot paths; full player-vs-player, auto-attack, and complete Java `AttackUtil`/`SkillEngine` paths remain partial.
- Java observer dispatch order is source-derived through the workflow but not proven against live `ObserveController`.
- Exhausted-idian serialization may still differ because Java sends the full item update before clearing `item.setIdianStone(null)`.
- Delegate persistence is covered by unit capture only; no live DAO, transaction/autocommit, rollback, or Java flush cadence comparison was run.
- No Java runtime, live database, or live-client comparison was run.

## Next Recommended Unit of Work

Add the production connection/caller fanout for `WorldNpcSkillDamageResult.EquipmentObserverBurns.Packets` where a safe represented skill-damage call path exists, sending returned idian/charge packets in the documented order and supplying `PlayerEnterWorldService` persistence delegates. If that routing is still too broad, add a focused caller-level test seam that proves packet order relative to `SM_ATTACK_STATUS` before enabling live routing.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HY-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
