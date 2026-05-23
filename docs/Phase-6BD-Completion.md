# Phase 6BD Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6BC and covers Sessions 395-396.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 858 tests.

---

## Recent Work Completed

- Added a staged Java `AttackUtil.calculateSkillResult` finalization surface to `WorldNpcSkillResultCalculationService`.
- Added `WorldNpcSkillFinalizationOptions` and `WorldNpcSkillFinalizationResult` to record effector-NPC owner damage, shared-target division, PvP/PvE modifier, Java's negative-damage clamp, affected-NPC AI damage, and final integer handoff.
- Kept the skill-result tail hook distinct from the older attack-list `modifyDamageByNpcAi` staged hook because Java runs them in different places.
- Added a staged Java `AttackUtil.calculateMagicalOverTimeSkillResult` surface.
- Added `WorldNpcSkillMagicalOverTimeRequest`, `WorldNpcSkillMagicalOverTimeOptions`, and `WorldNpcSkillMagicalOverTimeResult` to record trap bypass, magical skill damage, base magical observer multiplier, optional position-1 magical status recalculation, critical damage, PvP/PvE modifier, minimum-1 floor, affected-NPC AI damage, and final integer return.
- Added a float-preserving critical helper for magical over-time damage so exact Java-style checkpoints survive until final truncation.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 396, including migration parity tables, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `b7acf16c1` - `Add NPC skill result finalization shell`
- `8cceb371a` - `Add NPC magical over-time damage shell`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.attack.AttackUtil.calculateSkillResult` | `Aion.GameServer.Services.WorldNpcSkillResultCalculationService.Calculate`; `WorldNpcSkillFinalizationOptions`; `WorldNpcSkillFinalizationResult` | Partial | Unit Tested | Partial Parity | C# stages final tail ordering from explicit inputs. Live stat/effect/runtime integration is pending. |
| `com.aionemu.gameserver.skillengine.effect.EffectTemplate.isShared` | `WorldNpcSkillFinalizationOptions.TemplateIsShared` | Partial | Unit Tested | Needs Verification | C# stages the shared-template gate. Live template lookup is pending. |
| `com.aionemu.gameserver.skillengine.model.Skill.getEffectedList` | `WorldNpcSkillFinalizationOptions.HasSkill`; `EffectedListCount` | Partial | Unit Tested | Needs Verification | C# stages null-skill and target-count gates. Real skill state is pending. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.adjustDamageByPvpOrPveModifiers` | `WorldNpcSkillFinalizationOptions.PvpPveMultiplier`; `WorldNpcSkillMagicalOverTimeOptions.PvpPveMultiplier` | Partial | Unit Tested | Needs Verification | C# accepts resolved multipliers. Real PvP/PvE stat formulas and `Effect.getPvpDamage` are pending. |
| `com.aionemu.gameserver.ai.NpcAI.modifyOwnerDamage` | `WorldNpcSkillFinalizationOptions.EffectorNpcOwnerDamageMultiplier` | Partial | Unit Tested | Needs Verification | C# stages the skill-result effector NPC hook. Real AI dispatch is pending. |
| `com.aionemu.gameserver.ai.NpcAI.modifyDamage` | `WorldNpcSkillFinalizationOptions.EffectedNpcDamageMultiplier`; `WorldNpcSkillMagicalOverTimeOptions.EffectedNpcDamageMultiplier` | Partial | Unit Tested | Needs Verification | C# stages the affected-NPC hook after the Java clamp/floor point. Real AI dispatch is pending. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil.calculateMagicalOverTimeSkillResult` | `WorldNpcSkillResultCalculationService.CalculateMagicalOverTime`; `WorldNpcSkillMagicalOverTimeRequest`; `WorldNpcSkillMagicalOverTimeOptions`; `WorldNpcSkillMagicalOverTimeResult` | Partial | Unit Tested | Partial Parity | C# stages periodic magical damage order from explicit inputs. Live effect/stat/runtime integration is pending. |
| `com.aionemu.gameserver.model.gameobjects.Trap` | `WorldNpcSkillMagicalOverTimeOptions.EffectorIsTrap` | Partial | Unit Tested | Needs Verification | C# records Java's trap bypass branch. Real trap object modeling is pending. |
| `com.aionemu.gameserver.controllers.ObserveController.getBaseMagicalDamageMultiplier` | `WorldNpcSkillMagicalOverTimeOptions.BaseMagicalDamageMultiplier` | Partial | Unit Tested | Needs Verification | C# accepts a resolved observer multiplier. Live observer iteration is pending. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculateMagicalSkillDamage` | `WorldNpcSkillMagicalOverTimeOptions.MagicalSkillDamage` | Partial | Unit Tested | Needs Verification | C# accepts resolved magical skill damage. Real magic boost, elemental, target stat, and template formulas are pending. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil.calculateMagicalStatus` | `WorldNpcSkillAttackStatusCalculationOptions`; `WorldNpcSkillAttackStatusCalculationResult` | Partial | Unit Tested | Partial Parity | C# reuses the staged magical status calculator at Java's position-1 recalculation gate. Live probability/stat checks remain staged. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil.calculateWeaponCritical` | `WorldNpcSkillCriticalDamageResult`; `CalculateSkillCriticalDamageFromExact` | Partial | Unit Tested | Partial Parity | C# preserves float damage for over-time critical checkpoints. Live weapon/stat lookup is pending. |
| `com.aionemu.gameserver.skillengine.effect.BleedEffect` / `PoisonEffect` / `SpellAttackEffect` / `SpellAtkDrainEffect` | No complete C# over-time caller surface yet | Not Started | No Tests | Needs Verification | Java caller behavior was identified as the next useful unit: reserve/start periodic damage, DOT/drain packet metadata, observer notification, and drain healing. |

Metrics from the current handoff window:

- Total Java artifacts discovered or re-confirmed: 20+
- Total artifacts ported or staged: 10+ partial C# artifacts/DTOs/helpers plus reused status/critical surfaces
- Total artifacts with verified runtime parity: 0
- Total artifacts needing verification: 20+
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains about 55% complete as a conservative game-core estimate; full `AttackUtil`, live observer execution, effect runtime, stat/equipment lookup, real attack callers, dynamic observers, AI handlers, team loot, dynamic handlers, instances, and quests are still broad open areas.

---

## Important Limits

- `WorldNpcSkillResultCalculationService` is still a staged calculation shell, not full Java `AttackUtil` parity.
- Finalization inputs are explicit. C# does not yet read live `Effect`, `Skill`, `EffectTemplate`, `Creature`, `NpcAI`, or `StatFunctions` state.
- Java `calculateSkillResult` currently clamps finalization damage only when it is negative; it does not use a minimum-1 floor in that tail.
- Java `calculateMagicalOverTimeSkillResult` uses a minimum-1 floor before the affected-NPC AI hook and does not floor again after that hook. The staged C# result mirrors this ordering.
- Over-time caller behavior is not ported yet: `EffectReserved`, abnormal-state start/end, DOT observer notification, HP/MP drain healing, attack packet logs, and hop types remain pending.
- The old `WorldNpcSkillNpcAiDamageModifier` is still the staged attack-list hook for Java `modifyDamageByNpcAi`; it should not be confused with the skill-result tail or magical over-time affected-NPC hooks.
- Real `Effect` mutation is still absent; current result DTOs are not consumed by a full skill runtime.

---

## Next Unit Of Work

Recommended next unit: add a staged over-time effect caller surface for Java `BleedEffect`, `PoisonEffect`, `SpellAttackEffect`, and `SpellAtkDrainEffect`.

Suggested shape:

1. Re-read:
   - `game-server/src/com/aionemu/gameserver/skillengine/effect/BleedEffect.java`
   - `game-server/src/com/aionemu/gameserver/skillengine/effect/PoisonEffect.java`
   - `game-server/src/com/aionemu/gameserver/skillengine/effect/SpellAttackEffect.java`
   - `game-server/src/com/aionemu/gameserver/skillengine/effect/SpellAtkDrainEffect.java`
2. Add narrow C# DTOs or a small service surface that records caller behavior without pretending full `Effect` runtime parity exists:
   - `calculateBaseValue(effect)` input
   - call into the staged `CalculateMagicalOverTime`
   - reserved HP damage for bleed/poison/spell attack start effects
   - periodic attack packet metadata: `LOG.BLEED`, `LOG.POISON`, `LOG.SPELLATK`, `LOG.SPELLATKDRAIN`
   - DOT observer notification vs attack observer notification
   - `SpellAtkDrainEffect` HP/MP heal percentages from inflicted damage
3. Keep live abnormal-state mutation, real `EffectReserved` storage, real controller attack dispatch, observer collections, and life-stat healing unresolved until supporting systems exist.
4. Update `PHASE-6-PROGRESS.md` with Session 397, a migration parity table, tests, risks, metrics, and the next recommended unit.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "WorldNpcSkillResultCalculationServiceTests|WorldNpcCastingInterruptServiceTests|WorldNpcCombatEventServiceTests|WorldNpcCombatStateServiceTests|WorldNpcDamageServiceTests|WorldNpcLifeStatsServiceTests|WorldNpcDeathDropWorkflowServiceTests|WorldNpcSpawnServiceTests|GamePacketTests|GameServerBootstrapTests"
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 395-396, `docs/Phase-6BC-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
