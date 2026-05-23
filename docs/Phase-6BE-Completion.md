# Phase 6BE Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6BD and covers Sessions 397-399.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 873 tests.

---

## Recent Work Completed

- Added staged over-time effect caller support for Java `BleedEffect`, `PoisonEffect`, `SpellAttackEffect`, and `SpellAtkDrainEffect`.
- Added staged start/periodic DTOs for HP over-time damage, abnormal metadata, reserve metadata, DOT observer metadata, attack observer metadata, and HP/MP drain amounts.
- Added `WorldNpcSkillDamageKind.PoisonPeriodic` so poison can emit `SM_ATTACK_STATUS.LOG.POISON` independently from bleed/spell DOTs.
- Added staged resource over-time effect caller support for Java `MpAttackEffect`, `FpAttackEffect`, `HealOverTimeEffect`, `HealEffect`, `MPHealEffect`, `FPHealEffect`, and `DPHealEffect`.
- Added staged resource start/periodic DTOs for MP/FP damage, HP/MP/FP/DP heal reserves, resource caps, HP heal-skill-deboost input, player-only FP/DP gates, and packet type/log metadata.
- Added staged instant resource/drain effect caller support for Java `MpAttackInstantEffect`, `FpAttackInstantEffect`, `DelayedFpAtkInstantEffect`, `SkillAtkDrainInstantEffect`, and `SpellAtkDrainInstantEffect`.
- Added staged instant DTOs for MP/FP reserves, delayed FP enemy gate, 1 second delayed drain metadata, and Java-specific skill-vs-spell drain packet type/log differences.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 399, including migration parity tables, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `b886f6e21` - `Add NPC over-time effect caller shell`
- `e90d36ca1` - `Add NPC resource over-time effect shell`
- `8ce39279d` - `Add NPC instant resource effect shell`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.AbstractOverTimeEffect` | `WorldNpcSkillOverTimeEffectStartResult`; `WorldNpcSkillResourceOverTimeStartResult` | Partial | Unit Tested | Partial Parity | C# records `checktime`, `300 + checktime` initial delay, and scheduling intent. It does not schedule live periodic tasks. |
| `com.aionemu.gameserver.skillengine.effect.BleedEffect` / `PoisonEffect` / `SpellAttackEffect` | `WorldNpcSkillOverTimeEffectKind`; `WorldNpcSkillDamageKind` periodic variants | Partial | Unit Tested | Partial Parity | C# stages start reserves, periodic HP damage, packet logs, and DOT observer metadata. Live effect runtime and observer execution are pending. |
| `com.aionemu.gameserver.skillengine.effect.SpellAtkDrainEffect` | `WorldNpcSkillOverTimeEffectKind.SpellAttackDrain`; `WorldNpcSkillDrainResult` | Partial | Unit Tested | Partial Parity | C# stages per-tick damage recalculation and HP/MP drain amounts. Live effector healing is pending. |
| `com.aionemu.gameserver.skillengine.effect.MpAttackEffect` / `FpAttackEffect` | `WorldNpcSkillResourceOverTimeEffectKind`; `WorldNpcSkillResourceOverTimePeriodicActionResult` | Partial | Unit Tested | Partial Parity | C# stages MP/FP percent resource damage and packet metadata. Live resource mutation is pending. |
| `com.aionemu.gameserver.skillengine.effect.HealOverTimeEffect` plus `HealEffect` / `MPHealEffect` / `FPHealEffect` / `DPHealEffect` | `WorldNpcSkillResourceOverTimeStartResult`; `WorldNpcSkillResourceOverTimePeriodicActionResult` | Partial | Unit Tested | Partial Parity | C# stages non-damage reserves, resource caps, HP deboost input, player-only FP/DP gates, and packet metadata. Live heal mutation is pending. |
| `com.aionemu.gameserver.skillengine.effect.MpAttackInstantEffect` / `FpAttackInstantEffect` / `DelayedFpAtkInstantEffect` | `WorldNpcSkillInstantResourceEffectKind`; `WorldNpcSkillInstantResourceEffectResult` | Partial | Unit Tested | Partial Parity | C# stages instant MP/FP reserve metadata, delayed FP enemy gate, delay metadata, and packet metadata. Live resource mutation and scheduling are pending. |
| `com.aionemu.gameserver.skillengine.effect.SkillAtkDrainInstantEffect` / `SpellAtkDrainInstantEffect` | `WorldNpcSkillInstantDrainEffectKind`; `WorldNpcSkillInstantDrainEffectResult` | Partial | Unit Tested | Partial Parity | C# stages delayed HP/MP drain amounts and Java packet type/log differences. Live damage-before-drain ordering and effector healing are pending. |
| `com.aionemu.gameserver.skillengine.model.EffectReserved` | `WorldNpcSkillEffectReservedResult` | Partial | Unit Tested | Needs Verification | C# records reserve metadata for HP/MP/FP/DP paths. It does not mutate live effect reserve storage. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | Delay/initial-delay metadata in staged DTOs | Partial | Unit Tested | Needs Verification | C# records scheduling intent only. Real scheduled callbacks, cancellation, and threading parity remain pending. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS` | `SmAttackStatusType`; `SmAttackStatusLog` | Partial | Unit Tested | Partial Parity | C# records packet type/log metadata across staged resource and drain paths. More packet golden coverage is still needed. |
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats` / `PlayerLifeStats` / `PlayerCommonData` resource methods | Staged resource/drain result DTOs | Partial | Unit Tested | Needs Verification | C# computes deltas but does not mutate live HP/MP/FP/DP state for these paths yet. |

Metrics from the current handoff window:

- Total Java artifacts discovered or re-confirmed: 35+
- Total artifacts ported or staged: 20+ partial C# artifacts/DTOs/enums/helpers
- Total artifacts with verified runtime parity: 0
- Total artifacts needing verification: 35+
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; full effect runtime, live resource mutation, scheduled callbacks, live observers, real attack callers, AI handlers, team loot, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- These new effect caller surfaces are staged metadata, not a live `Effect` runtime.
- C# still does not mutate live `EffectReserved` maps, abnormal states, MP, FP, DP, or effector HP/MP for the new resource/drain paths.
- Java scheduled behavior is represented as delay metadata only; no real `ThreadPoolManager.schedule` / `scheduleAtFixedRate` equivalent is invoked by these methods.
- `WorldNpcDamageService` remains HP/NPC-focused; broader Java `CreatureController` and player resource mutation are still missing.
- Observer notifications for DOT/attack paths are metadata only; registered observers are not executed.
- Packet type/log values are unit-tested as metadata but not yet covered by full packet golden vectors for each new effect caller.

---

## Next Unit Of Work

Recommended next unit: begin converting staged resource/effect metadata into live service boundaries.

Suggested shape:

1. Re-read Java resource mutation methods:
   - `game-server/src/com/aionemu/gameserver/model/stats/container/CreatureLifeStats.java`
   - `game-server/src/com/aionemu/gameserver/model/stats/container/PlayerLifeStats.java`
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java` DP methods
2. Add a focused C# resource stats surface, likely `WorldNpcResourceStatsService` or similar, that can stage MP/FP/DP reduce/increase behavior without disrupting existing HP/death workflow.
3. Preserve Java packet metadata: MP damage/heal, FP damage/heal, absorbed MP/HP, and DP changes.
4. Keep live player model persistence and full packet fanout unresolved if too broad, but record exact gaps in the Session 400 parity table.
5. Update `PHASE-6-PROGRESS.md` with Session 400, a migration parity table, tests, risks, metrics, and the next recommended unit.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "WorldNpcDamageServiceTests|WorldNpcSkillResultCalculationServiceTests|WorldNpcCastingInterruptServiceTests|WorldNpcCombatEventServiceTests|WorldNpcCombatStateServiceTests|WorldNpcLifeStatsServiceTests|WorldNpcDeathDropWorkflowServiceTests|WorldNpcSpawnServiceTests|GamePacketTests|GameServerBootstrapTests"
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 397-399, `docs/Phase-6BD-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
