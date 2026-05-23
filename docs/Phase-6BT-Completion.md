# Phase 6BT Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BS and covers Sessions 463-470.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1003 tests.

---

## Recent Work Completed

- Routed Java `ReviveType.KISK_REVIVE` (`4`) through the first C# `CM_REVIVE` kisk branch: dead gate, active bound-kisk lookup, kisk position lookup, resurrection charge decrement, `SM_KISK_UPDATE` direct/fanout, and delete-on-zero cleanup.
- Added kisk revive HP/MP restore parity from Java `PlayerReviveService.kiskRevive`: 30% restore by default, no-resurrect-penalty 100% restore branch as an injectable option, dead-state clear, resurrection emotion fanout, and stats/speed refresh.
- Added authoritative teleport-to-kisk-position state and Java `TeleportAnimation.NONE`-style packet refresh: channel info, player spawn, same-world player-info/stat/motion refresh, old-position delete, and new-position visible-player refresh.
- Added revive cleanup state: DP reset by default, pending player-resurrection flag clear, resurrection skill reset, and known-list target clearing for players targeting the revived player.
- Added Java-shaped kisk group/alliance use-mask resolver callbacks so future team services can plug into `Kisk.isUseAllowed` masks `4` and `5`; default runtime behavior remains conservative without a live team resolver.
- Added `SM_DIE` packet coverage and sent a zero-kisk-time resurrection-option refresh to dead online members when their runtime kisk is removed.
- Strengthened kisk update fanout validation by exercising `PlayerKiskUpdateFanoutService` with the real `NpcVisibilityService` known-list cache.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 470 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `9c4408fed` - `Route kisk revive charge use`
- `56e208912` - `Restore kisk revive resources`
- `af97b678b` - `Teleport kisk revives to bindstone`
- `e9110b5ad` - `Reset revive DP state`
- `fa1bf7ad9` - `Clear revive target state`
- `ad97f1614` - `Add kisk team authorization boundary`
- `3cf253802` - `Refresh revive options on kisk removal`
- `2f895df49` - `Validate kisk fanout known list`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `CM_REVIVE` kisk branch | `GameServerConnection.HandleReviveAsync` + `PlayerKiskReviveService` | Partial | Unit + Regression Tested | Partial Parity | Kisk revive id `4` is wired through charge use, restore, teleport, fanout, and cleanup slices. Other revive types remain deferred. |
| `PlayerReviveService.kiskRevive` | `PlayerKiskReviveService`, `PlayerReviveRestoreService`, `PlayerTeleportService` | Partial | Unit + Regression Tested | Partial Parity | Covers safe kisk branch through restore/teleport. Prison/event redirects, soul-sickness, full effect detection, and full team updates remain missing. |
| `PlayerReviveService.revive` cleanup | `PlayerReviveRestoreService` + `PlayerReviveTargetCleanupService` | Partial | Unit Tested | Partial Parity | DP reset, player-resurrection flag clear, resurrection skill reset, and known-list target clearing are modeled. Aggro/team/soul-sickness/flying-before-death remain pending. |
| `Kisk.canBind` / use masks `4` and `5` | `PlayerKiskAuthorizationService` resolver callbacks | Partial | Unit Tested | Partial Parity | Java group/alliance checks can be supplied by future C# team services. Runtime bind still does not have live team membership wiring. |
| `KiskService.removeKisk` dead-member refresh | `PlayerKiskRemovalCleanupService` + `SmDie` send | Partial | Unit + Regression Tested | Partial Parity | Dead online members get a post-removal resurrection-option refresh with zero remaining kisk time. Full `SM_DIE(Player)` option derivation remains broader. |
| `SM_DIE` | `SmDie` | Partial | Packet Tested | Partial Parity | Java field order is packet-tested. Live instance/effect/item/invasion option calculations are not ported yet. |
| `Kisk.broadcastKiskUpdate` | `PlayerKiskUpdateFanoutService` + `NpcVisibilityService` | Partial | Unit + Regression Tested | Partial Parity | Direct member fallback and same-race visible fanout are validated against the C# known-NPC cache. Socket ordering and concurrent known-list timing still need validation. |
| `TeleportService.teleportTo` kisk revive path | `PlayerTeleportService.TeleportToKiskPosition` + packet sends | Partial | Unit + Regression Tested | Partial Parity | Authoritative position and movement reset are modeled. Full world despawn/spawn ownership, protection tasks, instance callbacks, and exact ordering remain pending. |

Metrics from the current handoff window:

- Total focused sessions covered: 8
- Total commits covered: 8
- Current full validation baseline: 1003 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: live effect-controller no-resurrect-penalty detection, full team membership wiring, aggro/team cleanup, full `SM_DIE(Player)` option derivation, and dedicated kisk controller/death-state callbacks
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, dedicated kisk controller/death state, full NPC/dialog AI, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full socket-order harnesses, full zone lifecycle handlers, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Kisk runtime is still a lightweight `WorldNpc` plus `PlayerKiskRuntimeState`, not a dedicated Java `Kisk`/`SummonedObject` with controller, life stats, attackability, AI, or death callbacks.
- `CM_REVIVE` only supports the kisk revive branch. Bind/obelisk, item, skill, instance, rebirth, delayed base revive, prison/event redirects, and rejection side effects are not ported.
- No-resurrect-penalty behavior is available as a restore-service parameter, but live `Effect::isNoResurrectPenalty` detection is not wired.
- DP reset is state-level with the existing stats refresh; the dedicated packeted DP boundary is still not connected to `CM_REVIVE`.
- Target clearing uses the current online-player registry plus `WorldVisibility` as the known-list approximation. Persistent player known-list ownership is still absent.
- Aggro clearing, group/alliance movement updates, soul-sickness, flying-before-death restoration, and full socket ordering remain pending.
- Group/alliance kisk use-mask rules have resolver callbacks, but no live team service supplies them yet.
- `SM_DIE` is packet-tested but does not yet compute skill/item/instance/invasion choices from live instance handlers, effects, or inventory.
- Offline kisk binding remains in-memory, matching Java runtime behavior but not server-restart recovery.

---

## Next Unit Of Work

Recommended next unit: continue kisk parity with a dedicated kisk controller/death-state bridge.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/Kisk.java`
   - `game-server/src/com/aionemu/gameserver/services/KiskService.java`
   - `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`
   - `game-server/src/com/aionemu/gameserver/controllers/VisibleObjectController.java`
2. Re-read C#:
   - `PlayerKiskLifetimeService`
   - `PlayerKiskRemovalCleanupService`
   - `WorldNpcLifeStatsService`
   - `WorldNpcDeathDropWorkflowService`
   - `WorldNpcSpawnService`
   - `GameServerConnection.RemoveRuntimeKiskAsync`
3. Keep the first unit narrow:
   - detect when a spawned kisk NPC dies or is deleted through an NPC death path
   - remove the matching runtime kisk registry/world state
   - reuse existing creator/member cleanup, `SM_KISK_UPDATE`, bind-point reset, dead-member `SM_DIE`, and NPC visibility refresh
   - explicitly defer full dedicated `KiskController`, attackability tuning, AI behavior, and exact socket-order validation if support is not present

Strong alternatives:

1. Wire live team membership into the new `PlayerKiskAuthorizationService` group/alliance resolver boundary when a faithful C# team model exists.
2. Add socket-loop coverage for kisk revive packet order: `CM_REVIVE` -> kisk update -> restore/emotion/stats -> teleport packets.
3. Add live no-resurrect-penalty effect detection only after the effect controller can faithfully expose `Effect::isNoResurrectPenalty`.
4. Continue the next revive cleanup only after a faithful aggro/team model exists.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerKiskReviveServiceTests|FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~PlayerTeleportServiceTests|FullyQualifiedName~PlayerReviveTargetCleanupServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerKiskRemovalCleanupServiceTests|FullyQualifiedName~PlayerKiskLifetimeServiceTests|FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests|FullyQualifiedName~PlayerKiskAuthorizationServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 463-470, `docs/Phase-6BS-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
