# Phase 6BU Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BT and covers Sessions 471-474.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1006 tests.

---

## Recent Work Completed

- Added a kisk death-removal bridge: registered runtime kisks that reach the NPC death workflow now short-circuit ordinary loot/respawn/decay, remove registry/world/id state, run creator/member cleanup, send dead-member `SM_DIE`, and refresh NPC visibility.
- Lifted connection-local kisk removal side effects into reusable `PlayerKiskRemovalRuntimeCleanupService`, so lifetime expiry, revive-charge deletion, and NPC-death cleanup can share the same Java `KiskService.removeKisk` packet/state behavior.
- Added Java-shaped `SM_RESURRECT` packet coverage as `SmResurrect`: UTF-16 creature name, resurrection skill id, and trailing zero.
- Added kisk attackability rule coverage from Java `Kisk.isEnemyFrom(Player)` / `Kisk.getType(Player)`: opposite-race plus player/kisk PvP-zone state yields `ATTACKABLE(0)`, otherwise `SUPPORT(54)`.
- Added an optional creature-type id seam to `SmNpcInfo` while preserving the existing default friend/support output for all current NPC callers.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 474 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `d6665affd` - `Remove runtime kisk on npc death`
- `2b05b68fb` - `Add resurrect packet coverage`
- `9dc2b355f` - `Add kisk attackability rule`
- `e732cf8ec` - `Allow npc info creature type override`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `Kisk.isActive()` / `KiskController.delete()` | `PlayerKiskDeathCleanupService.TryRemoveDiedKisk` + `WorldNpcDeathDropWorkflowService` kisk callbacks | Partial | Unit + Regression Tested | Partial Parity | Registered kisk NPC death removes runtime state and skips generic reward/decay. Dedicated C# `KiskController` layering remains pending. |
| `KiskService.removeKisk(Kisk)` | `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync` | Partial | Regression Tested | Partial Parity | Shared cleanup covers creator update, online member clear, bind-point reset, pending request clear, dead-member revive refresh, and NPC visibility refresh. Socket order still needs real-client validation. |
| `SM_RESURRECT` | `SmResurrect` | Partial | Packet Tested | Partial Parity | Packet layout matches Java. Live `ResurrectEffect` / `ResurrectPositionalEffect` callers are not ported yet. |
| `Kisk.isEnemyFrom(Player)` | `PlayerKiskAttackabilityService.IsEnemyFrom` | Partial | Unit Tested | Partial Parity | Mirrors opposite-race plus both-inside-PvP-zone rule. Live PvP-zone state is not wired yet. |
| `Kisk.getType(Player)` / `CreatureType` ids | `PlayerKiskAttackabilityService.GetCreatureType` + `PlayerKiskCreatureType` | Partial | Unit Tested | Partial Parity | Returns Java `ATTACKABLE(0)` or `SUPPORT(54)` at the service boundary. |
| `SM_NPC_INFO(Npc, Player)` creature type write | `SmNpcInfo(..., creatureTypeId)` | Partial | Packet Tested | Partial Parity | Current callers keep default `FRIEND(38)` output; future kisk viewer-specific callers can pass attackable/support ids. |

Metrics from the current handoff window:

- Total focused sessions covered: 4
- Total commits covered: 4
- Current full validation baseline: 1006 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: live PvP-zone membership, viewer-specific NPC-info dispatch, dedicated kisk controller/AI layering, full resurrection skill/effect callers, real-client socket ordering
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, live viewer-specific kisk attackability wiring, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full socket-order harnesses, full zone lifecycle handlers, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Kisk runtime is still a lightweight `WorldNpc` plus `PlayerKiskRuntimeState`, not a dedicated Java `Kisk`/`SummonedObject` with full controller, effect controller, known-list ownership, AI, attack ingress, or lifecycle callback layering.
- `WorldNpcDeathDropWorkflowService` can remove registered runtime kisks when called, but player-spawned kisk NPCs still need full combat/life-stat ingress to guarantee every damage/death source reaches that workflow.
- `SmNpcInfo` can now write an explicit creature type, but no production caller passes kisk-specific attackable/support ids yet.
- C# still lacks live `isInsidePvPZone()` state for players and kisk NPCs.
- `SmResurrect` is packet-tested but not emitted by live skill effects because the resurrect effect runtime is not ported.
- Group/alliance kisk use-mask rules still have resolver callbacks only; no live team service supplies actual membership.
- Real encrypted client socket ordering remains unverified for kisk cleanup, kisk revive, and kisk visibility fanout.

---

## Next Unit Of Work

Recommended next unit: continue kisk parity with live viewer-specific kisk NPC info only when the missing zone/viewer inputs can be added safely.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/Kisk.java`
   - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_NPC_INFO.java`
   - `game-server/src/com/aionemu/gameserver/world/knownlist/PlayerAwareKnownList.java`
   - zone/PvP helpers behind `Creature.isInsidePvPZone()`
2. Re-read C#:
   - `PlayerKiskAttackabilityService`
   - `SmNpcInfo`
   - `NpcVisibilityService`
   - `GameClientSocketServer.RefreshNpcVisibilityAsync`
   - `PlayerZoneStateService`
3. Keep the first unit narrow:
   - add a faithful C# PvP-zone state boundary only if it can mirror Java inputs without overgeneralizing
   - pass viewer-specific creature type into `SmNpcInfo` for kisk NPCs only
   - preserve default NPC packet output for all non-kisk callers
   - defer full `KiskController`, combat ingress, and AI behavior if they would require broader systems

Strong alternatives:

1. Add socket-loop coverage for kisk cleanup/fanout ordering once a test harness can assert encrypted packet order without flakiness.
2. Wire live team membership into `PlayerKiskAuthorizationService` only after a faithful group/alliance model exists.
3. Wire `SmResurrect` from future resurrect effects once the skill/effect runtime can represent `ResurrectEffect` and `ResurrectPositionalEffect`.
4. Continue revive cleanup only after faithful aggro/team/soul-sickness models exist.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDeathDropWorkflowServiceTests|FullyQualifiedName~PlayerKiskLifetimeServiceTests|FullyQualifiedName~PlayerKiskRemovalCleanupServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerKiskAttackabilityServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 471-474, `docs/Phase-6BT-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
