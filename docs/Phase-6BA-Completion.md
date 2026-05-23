# Phase 6BA Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AZ and covers Sessions 377-384.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 775 tests.

---

## Recent Work Completed

- Added `WorldNpcDamageService` as the first actual staged NPC damage caller into `WorldNpcLifeStatsService.ReduceHpAsync`, including guards for missing/unspawned NPCs, missing attackers, and missing max HP.
- Added `SmAttackStatus` plus `SmAttackStatusType`/`SmAttackStatusLog`, and threaded packet creation/fanout through NPC HP reduction before death workflow side effects.
- Added `WorldNpcCombatStateService` for staged Java `AggroList.addDamage` and `Creature.incrementAttackedCount` order around HP reduction.
- Added `WorldNpcCombatEventService` for staged attacked-observer notifications and nearby `CREATURE_NEEDS_SUPPORT` support-AI request surfaces.
- Added `WorldNpcCastingInterruptService` for staged Java casting interruption before attacked observers, including item-skill cancel, guaranteed cancel, boss protection, and deterministic chance-roll formula coverage.
- Added `WorldNpcSkillDamageService` as the first staged skill/effect caller into the damage workflow, covering regular/provoked damage, periodic spell damage, spell drain, delayed spell attack, proc instant, and bleed periodic mappings.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 384, including migration parity tables, test lists, remaining risks, metrics, and next recommended work for every completed unit.

---

## Commits In This Handoff

- `d2260c056` - `Add NPC damage workflow caller`
- `2d33248a9` - `Add NPC attack status packet`
- `ce2c9131e` - `Track NPC combat state`
- `81cd66322` - `Add NPC combat event surface`
- `8553a2ca4` - `Add NPC casting interruption surface`
- `2ef9572e3` - `Add NPC skill damage caller`
- `3ff2b29be` - `Add NPC periodic skill damage mappings`
- `ce24a5ade` - `Add NPC delayed and bleed damage mappings`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.WorldNpcDamageService` | Partial | Unit Tested | Partial Parity | C# now stages the main attack order: casting interruption, attacked observers, aggro add, support-AI requests, HP reduction/packet, and attacked count. Real packet/skill/AI attack callers and many side effects remain incomplete. |
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats` | `Aion.GameServer.Services.WorldNpcLifeStatsService` | Partial | Unit Tested | Partial Parity | HP reduction, HP percentage, packet-before-death callback, and one-shot death workflow are staged. Heal/MP/FP paths, invulnerability, restore tasks, `killingBlow`, and observers remain pending. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmAttackStatus` | Partial | Unit Tested | Partial Parity | Packet layout and representative type/log selection are covered by deterministic tests. Runtime packet capture and all effect/log combinations remain pending. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `Aion.GameServer.Services.WorldNpcCombatStateService` | Partial | Unit Tested | Partial Parity | C# records per-attacker damage/hate and attacked count. Java hate modifiers, decay, target selection, team threat, and full notify semantics remain missing. |
| `com.aionemu.gameserver.controllers.ObserveController` | `Aion.GameServer.Services.WorldNpcCombatEventService`; `WorldNpcSkillAttackObserverNotification`; `WorldNpcSkillDotAttackedObserverNotification` | Partial | Unit Tested | Partial Parity | C# returns staged attacked/attack/DOT observer DTOs. Dynamic observer registration and callback execution are not ported. |
| `com.aionemu.gameserver.ai.event.AIEventType` / `AbstractAI` | `Aion.GameServer.Services.WorldNpcSupportAiRequest`; `WorldNpcAiEventType` | Partial | Unit Tested | Needs Verification | C# records support requests for nearby NPCs. Java `onCreatureEvent`, `canHandleEvent`, tribe/guard/geo checks, and delayed aggro notifier are missing. |
| `com.aionemu.gameserver.skillengine.model.Skill` / `SkillTemplate` | `Aion.GameServer.Services.WorldNpcCastingInterruptService`; `WorldNpcCastingSkill` | Partial | Unit Tested | Needs Verification | C# stages casting interruption decisions, including Java formula rounding. Full skill runtime, current-cast lifecycle, packets/tasks, and random source are not ported. |
| `com.aionemu.gameserver.skillengine.effect.DamageEffect` and selected subclasses | `Aion.GameServer.Services.WorldNpcSkillDamageService`; `WorldNpcSkillDamageKind` | Partial | Unit Tested | Partial Parity | Regular, provoked, periodic spell, spell drain, delayed spell, proc instant, and bleed caller mappings are staged. Full `Effect`, `EffectReserved`, `AttackUtil`, over-time scheduling, drains/heals, and observer callbacks remain pending. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil` | Not started; future C# skill result calculation service | Not Started | No Tests | Unknown | All current skill-damage tests supply final damage directly. Java damage calculation, dodge/resist/cannot-miss, movement modifiers, shield-ignore, elemental scaling, and critical/godstone remain open. |
| `com.aionemu.gameserver.controllers.VisibleObjectController` | Spawn/despawn cleanup delegates for life/combat/event/casting state | Partial | Unit Tested | Needs Verification | Staged runtime state clears on despawn. Java known-list, controller, observer, AI, skill, handler, and broader cleanup remain incomplete. |

Metrics from the current handoff window:

- Total Java artifacts discovered or re-confirmed: 25+
- Total artifacts ported or staged: 20+ partial C# artifacts/DTOs/services/enums
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 25+
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains about 55% complete as a conservative game-core estimate; full skill runtime, `AttackUtil`, real combat callers, dynamic observers, support AI handlers, full aggro behavior, creature modeling, AI handlers, team loot, dynamic handlers, instances, and quests are still broad open areas.

---

## Important Limits

- `WorldNpcSkillDamageService` is a staged caller surface; no real `SkillEngine`, client attack packet, or NPC AI attack path invokes it yet.
- Damage calculation is not ported. Current callers pass final damage directly rather than computing through Java `AttackUtil`.
- Observer surfaces return DTOs or record staged events; they do not execute Java `ActionObserver` callbacks.
- Support-AI fanout records request DTOs only; it does not dispatch `NpcAI.onCreatureEvent` or schedule `AggroNotifier`.
- Delayed spell attack records delay metadata only; it does not schedule delayed execution.
- Spell drain computes HP/MP drain amounts only; it does not mutate effector HP/MP or broadcast heal/MP packets.
- Casting interruption clears staged cast state only; it does not perform Java `cancelCurrentSkill` packet/task/cooldown/animation side effects.
- Aggro state is damage-equals-hate only and object-id keyed; Java hate rules, target selection, teams, and decay are not ported.

---

## Next Unit Of Work

Recommended next unit: begin a staged `AttackUtil`-style skill result calculation shell for the skill-damage caller.

Suggested shape:

1. Re-read Java `AttackUtil.calculateSkillResult(...)`, `SkillAttackInstantEffect`, and nearby dodge/resist/cannot-miss paths enough to define a narrow input/output DTO.
2. Add a small C# calculation service that can accept explicit final-damage inputs plus staged fields for `rnddmg`, `cannotmiss`, movement-modifier eligibility, and shield-ignore flags.
3. Keep real stat/resist/crit/godstone formulas marked partial if the supporting stat/effect systems are not present.
4. Thread the result DTO into `WorldNpcSkillDamageService` without changing existing final-damage behavior.
5. Update `PHASE-6-PROGRESS.md` with a new session, migration parity table, risks, metrics, tests, and next recommended unit.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "WorldNpcDamageServiceTests|WorldNpcCastingInterruptServiceTests|WorldNpcCombatEventServiceTests|WorldNpcCombatStateServiceTests|WorldNpcLifeStatsServiceTests|WorldNpcDeathDropWorkflowServiceTests|WorldNpcSpawnServiceTests|GamePacketTests|GameServerBootstrapTests"
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Session 384, `docs/Phase-6AZ-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
