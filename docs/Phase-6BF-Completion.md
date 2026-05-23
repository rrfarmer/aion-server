# Phase 6BF Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6BE and covers Sessions 400-402.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 885 tests.

---

## Recent Work Completed

- Added `WorldNpcResourceStatsService` as the first live resource mutation boundary for staged effect metadata.
- Added `WorldNpcLifeStatsService.ApplyMpChange` so existing NPC life-stat snapshots can mutate MP with Java `CreatureLifeStats.reduceMp` / `increaseMp` clamp and dead-guard behavior.
- Added player MP and FP resource mutation methods that operate on `Player.LifeStats`, including Java's FP packet quirk where `SM_ATTACK_STATUS` uses HP percentage while `SM_FLY_TIME` remains a separate send intent.
- Made `Player.Dp` mutable and added `AddPlayerDp` parity behavior for Java `PlayerCommonData.addDp/setDp`: starting-class skip, online max-DP cap, and DP packet/stat side-effect intents.
- Added staged resource effect adapters for MP/FP over-time and instant resource outputs, preserving skill id and packet metadata into live resource mutation.
- Wired staged `DPHealEffect` output into `AddPlayerDp`, while keeping real DP packet classes and stat/speed recalculation as pending work.
- Kept HP heal explicitly unsupported by the resource adapter until Java `CreatureLifeStats.increaseHp` has its own parity boundary.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 402, including migration parity tables, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `a214e0a92` - `Add NPC resource stats boundary`
- `e18d01382` - `Wire NPC resource effect adapters`
- `0648eedb0` - `Wire NPC DP resource adapter`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats.reduceMp` | `WorldNpcLifeStatsService.ApplyMpChange`; `WorldNpcResourceStatsService.ReduceNpcMpAsync`; `ReducePlayerMpAsync` | Partial | Unit Tested | Partial Parity | C# mutates NPC snapshots and player MP with Java dead guard, clamp, MP percentage, and attack-status metadata. Restore tasks and player stat packet fanout are pending. |
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats.increaseMp` | `WorldNpcLifeStatsService.ApplyMpChange`; `WorldNpcResourceStatsService.IncreaseNpcMpAsync`; `IncreasePlayerMpAsync` | Partial | Unit Tested | Partial Parity | C# caps to max MP and preserves Java's no-delta skill packet branch. Full player `SM_STATUPDATE_MP` fanout is pending. |
| `com.aionemu.gameserver.model.stats.container.PlayerLifeStats.reduceFp` / `increaseFp` | `WorldNpcResourceStatsService.ReducePlayerFpAsync`; `IncreasePlayerFpAsync`; `SendFlyTimeUpdate` intent | Partial | Unit Tested | Partial Parity | C# clamps/caps FP and records fly-time update intent. Real `SM_FLY_TIME`, FP restore/reduce tasks, and group stat update behavior are pending. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addDp` / `setDp` | `WorldNpcResourceStatsService.AddPlayerDp`; mutable `Player.Dp`; DP side-effect intents | Partial | Unit Tested | Partial Parity | C# mutates DP, skips starting classes, caps online players with supplied max DP, and records DP info/stat update/stat visual update intents. Real DP packets are pending. |
| `com.aionemu.gameserver.skillengine.effect.MpAttackEffect` / `MPHealEffect` | `WorldNpcResourceStatsService.ApplyResourceOverTimePeriodicResultAsync` | Partial | Unit Tested/Indirect | Partial Parity | Staged MP resource outputs can now route to live MP mutation. Full effect runtime, scheduler, and template lookup are pending. |
| `com.aionemu.gameserver.skillengine.effect.FpAttackEffect` / `FPHealEffect` | `WorldNpcResourceStatsService.ApplyResourceOverTimePeriodicResultAsync` | Partial | Unit Tested/Indirect | Partial Parity | Staged FP resource outputs can now route to live player FP mutation. Real `SM_FLY_TIME` and task side effects are pending. |
| `com.aionemu.gameserver.skillengine.effect.MpAttackInstantEffect` | `WorldNpcResourceStatsService.ApplyInstantResourceResultAsync` | Partial | Unit Tested | Partial Parity | Staged instant MP attack output can reduce player MP and emit packet metadata. Real `EffectReserved` consumption is pending. |
| `com.aionemu.gameserver.skillengine.effect.FpAttackInstantEffect` / `DelayedFpAtkInstantEffect` | `WorldNpcResourceStatsService.ApplyInstantResourceResultAsync` | Partial | Unit Tested | Partial Parity | C# enforces player target for staged instant FP damage. Delayed scheduler execution and live enemy checks remain staged. |
| `com.aionemu.gameserver.skillengine.effect.DPHealEffect` | `WorldNpcResourceStatsService.ApplyResourceOverTimePeriodicResultAsync`; `AddPlayerDp` | Partial | Unit Tested | Partial Parity | Staged DP heal output can mutate DP and record packet/stat intents. Real DP packet classes and stats/speed visual recalculation are pending. |
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats.increaseHp` | HP unsupported adapter marker | Not Started | Unit Tested as Unsupported | Needs Verification | HP heal is intentionally blocked until disease guard, negative-heal-to-damage, killing-blow reset, HP observers, death handling, and packet behavior are ported. |

Metrics from the current handoff window:

- Total Java artifacts discovered or re-confirmed: 20+
- Total artifacts ported or staged: 10+ partial C# runtime/service/adapter artifacts plus packet intent markers
- Total artifacts with verified runtime parity: 0
- Total artifacts needing verification: 20+
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; full effect runtime, HP heal mutation, real resource packet classes, scheduled callbacks, live observers, real attack callers, AI handlers, team loot, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- `WorldNpcResourceStatsService` is a resource boundary and adapter surface, not full Java `Effect` runtime parity.
- C# now mutates live/staged MP, FP, and DP state through focused services, but it still does not mutate real `EffectReserved` maps or execute scheduled effect callbacks.
- HP heal remains intentionally unsupported until a dedicated Java `CreatureLifeStats.increaseHp` boundary is added.
- Real `SM_FLY_TIME`, `SM_DP_INFO`, and `SM_STATUPDATE_DP` packet classes are not ported yet; current DP/FP side effects are recorded as intents.
- Player resource mutation relies on explicit max HP/MP/FP/DP inputs. Live stat calculation and equipment/effect modifier integration remain pending.
- Restore task triggers, FP reduce/restore timers, player `SM_STATUPDATE_HP/MP`, group stat updates, HP/MP observers, and team fanout are not wired.
- The adapter methods consume staged DTOs. They do not resolve real `Effect`, `EffectTemplate`, `Creature`, `Skill`, `EffectReserved`, target selection, enemy checks, or Java scheduler state.

---

## Next Unit Of Work

Recommended next unit: add a dedicated HP heal boundary for Java `CreatureLifeStats.increaseHp`.

Suggested shape:

1. Re-read:
   - `game-server/src/com/aionemu/gameserver/model/stats/container/CreatureLifeStats.java`
   - `game-server/src/com/aionemu/gameserver/model/stats/container/PlayerLifeStats.java`
   - Existing C# `WorldNpcLifeStatsService.ReduceHpAsync` and `WorldNpcResourceStatsService`
2. Add a focused HP increase surface that does not disrupt existing HP/death workflow:
   - `value < 0` redirects to HP damage path, matching Java's negative-heal behavior.
   - disease abnormal guard blocks positive healing.
   - dead targets return `0` / no mutation.
   - heal caps at max HP.
   - killing-blow reset intent is recorded where C# does not yet model the field.
   - `SM_ATTACK_STATUS` metadata uses HP percentage and Java packet type/log choices.
3. Keep live HP observers, player `SM_STATUPDATE_HP`, team stat update, restore task triggers, disease source modeling, and full effect runtime unresolved if too broad, but record exact gaps in Session 403.
4. Update `PHASE-6-PROGRESS.md` with Session 403, a migration parity table, tests, risks, metrics, and the next recommended unit.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "WorldNpcResourceStatsServiceTests|WorldNpcDamageServiceTests|WorldNpcSkillResultCalculationServiceTests|WorldNpcCastingInterruptServiceTests|WorldNpcCombatEventServiceTests|WorldNpcCombatStateServiceTests|WorldNpcLifeStatsServiceTests|WorldNpcDeathDropWorkflowServiceTests|WorldNpcSpawnServiceTests|GamePacketTests|GameServerBootstrapTests"
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 400-402, `docs/Phase-6BE-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
