# Phase 6AZ Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AY and covers Sessions 373-376.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 749 tests.

---

## Recent Work Completed

- Added `WorldNpcDeathDropOptions` and generalized `WorldNpcDeathDropWorkflowService.HandleDeathAsync` so the staged death/drop bridge can model Java `NpcController.onDie` AI decisions for `ALLOW_RESPAWN`, `REWARD_LOOT`, and `ALLOW_DECAY`.
- Added the `LootRewardDisabled` skip status and immediate-delete/no-decay result surface for Java death paths that disallow loot reward or corpse decay.
- Added `WorldNpcAiStateService.MarkDied`, threaded it into the death workflow, and cleared stale AI state on spawn/despawn so object-id reuse does not inherit dead/walking state.
- Added `WorldNpcLifeStatsService` / `WorldNpcLifeStats` as the first narrow NPC HP/MP runtime surface, with lethal HP reduction calling the staged death workflow exactly once.
- Wired world-NPC spawn/despawn to initialize/clear staged NPC life stats from `NpcTemplateSummary.MaxHp` through lazy delegates, avoiding a DI construction cycle.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 376, including migration parity tables, test lists, remaining risks, metrics, and next recommended work for each completed unit.

---

## Commits In This Handoff

- `7ee3ef8e0` - `Model NPC death AI decisions`
- `f0a1fa9bd` - `Track NPC death AI state`
- `e331e5d10` - `Add NPC life stats death trigger`
- `052ac4d39` - `Initialize NPC life stats on spawn`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.NpcController` | `Aion.GameServer.Services.WorldNpcDeathDropWorkflowService` | Partial | Unit Tested | Partial Parity | Death option ordering, AI died marker, loot-reward skip, decay/no-decay, respawn scheduling, and staged drop workflow are covered. Real combat/life-stat caller, instance callback, `super.onDie`, AP/XP/DP reward, pet loot, and team reward flow remain pending. |
| `com.aionemu.gameserver.ai.poll.AIQuestion` / `com.aionemu.gameserver.ai.NpcAI` | `Aion.GameServer.Services.WorldNpcDeathDropOptions`; `WorldNpcAiStateService` | Partial | Unit Tested | Needs Verification | Only death-path `ALLOW_RESPAWN`, `REWARD_LOOT`, `ALLOW_DECAY`, and DIED state are modeled. Live AI polling, `SiegeService.isRespawnAllowed`, and Java AI handlers/scripts are not wired. |
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats` | `Aion.GameServer.Services.WorldNpcLifeStatsService.ReduceHpAsync` | Partial | Unit Tested | Partial Parity | Narrow HP reduction and one-shot death trigger are staged. Attack-status packets, observers, invulnerability, heal/MP paths, `killingBlow`, effect metadata, max-stat sync, and Java monitor semantics remain incomplete. |
| `com.aionemu.gameserver.model.stats.container.NpcLifeStats` | `Aion.GameServer.Services.WorldNpcLifeStats`; `WorldNpcLifeStatsService` | Partial | Unit Tested | Needs Verification | Spawn initializes current HP from template max HP and despawn clears stats. Max MP, restore scheduling, and full game-stat-derived max values are missing. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner` / `VisibleObjectController` | `Aion.GameServer.Services.WorldNpcSpawnService` | Partial | Unit Tested | Needs Verification | Spawn/despawn now clear staged AI/life runtime state. Java controller, known-list, aggro, handler, and drop cleanup remain broader than the C# staged surface. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS` | Not started for NPC damage | Not Started | No Tests | Unknown | Damage serialization and sighted-player fanout are not implemented in the NPC life-stat reducer yet. |

Metrics from the current handoff window:

- Total Java artifacts discovered or re-confirmed: 10
- Total artifacts ported or staged: 7 partial C# artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains about 55% complete as a conservative game-core estimate; live combat, packets, AI handlers, creature death lifecycle, team loot, dynamic handlers, instances, and quests are still broad open areas.

---

## Important Limits

- No live client packet, skill, or AI attack path invokes `WorldNpcLifeStatsService.ReduceHpAsync` yet.
- `WorldNpcLifeStatsService` does not emit Java `SM_ATTACK_STATUS`, notify HP/death observers, schedule HP restore, evaluate invulnerability, or carry skill/effect/log metadata.
- C# NPC life stats currently use template max HP only; max MP and full Java game-stat-derived max values remain missing.
- `WorldNpcDeathDropWorkflowService` still does not perform Java `CreatureController.onDie` side effects: movement abort, casting/effect cleanup, creature dead state, death emotion broadcast, observer notification, and hate cleanup.
- AI DIED state is a lightweight runtime-state marker, not Java's full AI event machine or dynamic AI handler/script execution.
- Group/alliance loot distribution, rolls/bids, winner messages, pet auto-loot/auto-sell, optional sockets, quality announcements, and instance/AI `onDropRegistered` callbacks remain pending.

---

## Next Unit Of Work

Recommended next unit: add the first actual NPC damage/combat caller into `WorldNpcLifeStatsService.ReduceHpAsync`.

Suggested shape:

1. Re-read Java `CreatureController.onAttack(...)`, `CreatureLifeStats.reduceHp(...)`, and `SM_ATTACK_STATUS` enough to preserve call order.
2. Add a narrow C# damage entry point, likely a small service or method that accepts world NPC, attacker player, damage, and optional death options, then calls `ReduceHpAsync`.
3. Keep packet fanout and observer callbacks explicitly staged if the support surfaces are not ready, but document them in the parity table.
4. Cover nonlethal damage, lethal damage, duplicate lethal damage, missing NPC, and no-direct-drop/decay scheduling through the existing death workflow.
5. Keep `PHASE-6-PROGRESS.md` updated with a new session, migration parity table, risks, metrics, tests, and next recommended unit.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "WorldNpcLifeStatsServiceTests|WorldNpcDeathDropWorkflowServiceTests|WorldNpcSpawnServiceTests|GameServerBootstrapTests"
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Session 376, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
