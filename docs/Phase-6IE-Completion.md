# Phase 6IE Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6ID and covers Session 727.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "WorldNpcResourceStatsServiceTests|EquipmentObserverBurnWorkflowServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 60 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1306 tests.

## Recent Work Completed

### Session 727 - Dot-Attacked Player Incoming Damage Observer Ordering Seam

- Added `PlayerIncomingDamageObserverFanoutService.ApplyIncomingDotHpDamageAndObserverBurnsAsync`.
- Refactored the existing direct attacked incoming HP damage seam so direct attacked and dot-attacked variants share the damage-first ordering path.
- Preserved distinct Java observer event mapping: direct incoming damage maps to `EquipmentObserverBurnEvent.Attacked`; periodic/dot incoming damage maps to `EquipmentObserverBurnEvent.DotAttacked`.
- Added a test proving nonzero skill-id dot-attacked damage still burns both idian polish and charge after damage/status packets.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.AbstractOverTimeEffect.onPeriodicAction` / periodic player damage callbacks | `Aion.GameServer.Services.PlayerIncomingDamageObserverFanoutService.ApplyIncomingDotHpDamageAndObserverBurnsAsync` | Periodic Damage Caller Seam | Partial | Regression Tested | Needs Verification | C# now has a represented dot-attacked player HP damage seam that orders attack-status/HP stat update before equipment observer packets. Full Java scheduled effect lifecycle, `Effect` identity, abnormal state ticking, and real combat route invocation remain missing. |
| `com.aionemu.gameserver.model.stats.container.PlayerLifeStats` / inherited `CreatureLifeStats.reduceHp` side effects | `PlayerIncomingDamageObserverFanoutService` shared incoming damage path via `WorldNpcResourceStatsService.IncreasePlayerHpAsync` negative damage path | Player Damage Dependency | Partial | Regression Tested | Needs Verification | Direct attacked and dot-attacked seams now share the same damage-first ordering path. Java death workflow, PvP logic, attack-result lists, and full damage controller behavior remain partial. |
| `com.aionemu.gameserver.controllers.observer.ActionObserver.dotattacked` | `PlayerIncomingDamageObserverFanoutService.ApplyIncomingDotHpDamageAndObserverBurnsAsync` invoking `EquipmentObserverBurnFanoutService` with `EquipmentObserverBurnEvent.DotAttacked` | Observer Callback Integration | Partial | Regression Tested | Needs Verification | Test validates dot-attacked observer burns run after represented player damage and allow nonzero skill ids. Real `ObserveController` dot dispatch, effect object propagation, and observer lifecycle remain unverified. |
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `EquipmentObserverBurnFanoutService` invoked through dot-attacked player incoming damage | Model Helper Dependency | Partial | Regression Tested | Needs Verification | Test validates defend polish burn and packet fanout for dot-attacked nonzero skill damage. Java synchronized mutation, exhausted-idian serialization, DAO timing, and `RandomBonusEffect` refresh remain unresolved. |
| `com.aionemu.gameserver.model.items.ChargeInfo.dotattacked` | `EquipmentObserverBurnFanoutService` invoked through dot-attacked player incoming damage | Model Helper Dependency | Partial | Regression Tested | Needs Verification | Test validates dot-attacked charge burn occurs with nonzero skill id, unlike `ChargeInfo.attacked`. Java synchronized mutation, `PersistentState.UPDATE_REQUIRED`, and live player lookup remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS` | `SmAttackStatus` from `WorldNpcResourceStatsService` ordered before dot-attacked observer packets | Packet | Partial | Regression Tested | Needs Verification | Packet object order is asserted locally. No Java golden bytes, live socket ordering, encrypted-frame comparison, or live-client validation was run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_HP` | `SmStatUpdateHp` from `WorldNpcResourceStatsService` ordered before dot-attacked observer packets | Packet | Partial | Regression Tested | Needs Verification | HP stat update order is asserted locally before idian/charge packets. Java runtime order relative to dot observer callbacks remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `SmInventoryUpdateItem` packets sent after dot-attacked player incoming damage | Packet | Partial | Regression Tested with byte-level payload checks | Needs Verification | Tests assert idian-before-charge inventory update payloads for dot-attacked damage. Exhausted-idian full update, Java golden packet comparison, and live client behavior remain unverified. |

## Tests Added Or Updated

- `WorldNpcResourceStatsServiceTests.ApplyIncomingDotHpDamageAndObserverBurnsAsync_AllowsNonzeroSkillChargeAndIdianBurnsAfterDamage`
  - Validates represented dot-attacked player damage with a nonzero skill id mutates HP, broadcasts attack status, sends HP stat update, then sends idian and charge defender packets in that order.
- Existing world NPC resource stats, equipment observer workflow, and enter-world persistence tests matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden packets, live `Effect` object dispatch, live `ObserveController`, live `GameServerConnection`, encrypted frame order, live DAO behavior, reflection behavior, threading behavior, date/time scheduling behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 8
- Total artifacts ported or partially modeled in this handoff window: 1 represented dot-attacked player incoming damage plus equipment observer ordering seam.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: live periodic effect route invocation, full SkillEngine/AttackUtil integration, real ObserveController/Effect lifecycle, Java runtime/golden packet comparison, live DAO comparison, and synchronized/threading comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- The represented dot-attacked seam is production-registered through `PlayerIncomingDamageObserverFanoutService`, but not invoked from a real periodic effect or skill route.
- Full Java scheduled effect lifecycle, `Effect` identity propagation, abnormal state handling, PvP/duel logic, death workflow, and `AttackUtil` remain missing.
- Java observer dispatch order and stat/effect refresh lifecycle remain source-derived and not live verified.
- Exhausted-idian serialization, DAO flush cadence, transaction behavior, synchronized mutation, and live socket ordering remain unresolved.
- Date/time scheduling is named but not implemented in this unit; no scheduler/runtime comparison was run.

## Next Recommended Unit of Work

Inspect Java combat client packets and current C# `GameClientPacketFactory` for the smallest missing skill/attack packet parser that can safely feed represented combat services. If still too broad, continue player incoming observer parity by adding persistence delegate wiring/tests for `PlayerIncomingDamageObserverFanoutService` so live DAO boundaries are ready when a real caller arrives.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6ID-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
