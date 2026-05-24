# Phase 6IF Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IE and covers Session 728.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "WorldNpcResourceStatsServiceTests|EquipmentObserverBurnWorkflowServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 61 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1307 tests.

## Recent Work Completed

### Session 728 - Incoming Damage Observer Burn Persistence Delegate Coverage

- Added `WorldNpcResourceStatsServiceTests.ApplyIncomingHpDamageAndObserverBurnsAsync_RequestsObserverBurnPersistenceAfterDamage`.
- Verified `PlayerIncomingDamageObserverFanoutService.ApplyIncomingHpDamageAndObserverBurnsAsync` forwards idian and charge persistence delegates after the represented player damage/status packet path.
- Preserved packet order assertions: attack status, HP stat update, idian inventory update, charge inventory update.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.stats.container.PlayerLifeStats` / inherited `CreatureLifeStats.reduceHp` side effects | `Aion.GameServer.Services.PlayerIncomingDamageObserverFanoutService.ApplyIncomingHpDamageAndObserverBurnsAsync` | Player Damage Caller Seam | Partial | Regression Tested | Needs Verification | Existing represented damage-first ordering is now also covered while observer persistence delegates are supplied. Full Java player damage controller, death workflow, PvP logic, attack-result lists, and real route invocation remain missing. |
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `PlayerIncomingDamageObserverFanoutService` forwarding idian persistence delegate to `EquipmentObserverBurnFanoutService` / workflow | Model Helper / Persistence Boundary | Partial | Regression Tested with delegate capture | Needs Verification | Test verifies idian burn persistence delegate receives the mutated item object id after represented player damage. Java synchronized mutation, exhausted-idian full update before clear, `ItemStoneListDAO.storeIdianStones`, and stat/effect refresh remain unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo.updateChargePoints` / `ChargeInfo.attacked` | `PlayerIncomingDamageObserverFanoutService` forwarding charge persistence delegate to `EquipmentObserverBurnFanoutService` / workflow | Model Helper / Persistence Boundary | Partial | Regression Tested with delegate capture | Needs Verification | Test verifies charge burn persistence delegate receives the mutated item object id after represented player damage. Java persistent-state batching, equipment persistent state, synchronized mutation, and live DAO timing remain unverified. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` persistence-side item mutation helpers | Delegate-compatible C# calls accepted by `PlayerIncomingDamageObserverFanoutService` | Persistence Dependency | Partial | Regression Tested with delegate capture | Needs Verification | The represented seam can now carry the same delegate shape as `PlayerEnterWorldService.SaveIdianPolishBurnMutationAsync` and `SaveItemChargeBurnMutationAsync`. Live DI caller wiring, live MySQL, transaction/autocommit/rollback, and Java flush cadence remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS` | `SmAttackStatus` from `WorldNpcResourceStatsService` ordered before persistence-backed observer packets | Packet | Partial | Regression Tested | Needs Verification | Test keeps attack-status packet first in captured order. No Java golden bytes, live socket ordering, encrypted-frame comparison, or live-client validation was run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_HP` | `SmStatUpdateHp` from `WorldNpcResourceStatsService` ordered before persistence-backed observer packets | Packet | Partial | Regression Tested | Needs Verification | HP stat update is asserted before idian/charge inventory packets. Java runtime order remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `SmInventoryUpdateItem` packets emitted after persistence delegate capture | Packet | Partial | Regression Tested with byte-level payload checks | Needs Verification | Tests assert idian-before-charge payloads while persistence delegates are supplied. Exhausted-idian serialization, Java golden packet comparison, and live client behavior remain unverified. |

## Tests Added Or Updated

- `WorldNpcResourceStatsServiceTests.ApplyIncomingHpDamageAndObserverBurnsAsync_RequestsObserverBurnPersistenceAfterDamage`
  - Validates represented player damage packet ordering remains damage/status first, then idian/charge inventory packets, while both observer-burn persistence delegates receive item object `10`.
- Existing world NPC resource stats, equipment observer workflow, and enter-world persistence tests matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden packets, live `ObserveController`, live `GameServerConnection`, encrypted frame order, live DAO behavior, reflection behavior, threading behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 persistence delegate coverage slice for represented player incoming damage observer burns.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live player combat route invocation, full SkillEngine/AttackUtil integration, real ObserveController lifecycle, Java runtime/golden packet comparison, live DAO comparison, and synchronized/threading comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- Persistence is delegate-captured only; no live DAO, transaction/autocommit/rollback, or Java flush cadence comparison was run.
- The represented incoming damage seam is still not invoked from a real player combat or skill route.
- Full Java player damage controller behavior, death workflow, PvP/duel rules, `AttackUtil`, result-list handling, and auto-attack/skill caller coverage remain missing.
- Java observer dispatch order, stat/effect refresh lifecycle, synchronized mutation, exhausted-idian serialization, and live socket ordering remain unresolved.

## Next Recommended Unit of Work

Inspect Java combat client packets and current C# `GameClientPacketFactory` for the smallest missing skill/attack packet parser that can safely feed represented combat services. Prefer a parser/DTO plus source-derived tests if the runtime caller is still too broad; otherwise wire the real caller into `WorldNpcSkillDamageFanoutService` or `PlayerIncomingDamageObserverFanoutService`.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IE-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
