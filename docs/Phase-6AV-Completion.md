# Phase 6AV Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AU and covers Sessions 356-358.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 693 tests.

---

## Recent Work Completed

- Added `WorldNpcLootBroadcastService` as the live visible-world broadcast caller for Java `DropService.scheduleFreeForAll`, scheduling the existing free-for-all transition and broadcasting the returned `SM_LOOT_STATUS.LOOT_ENABLE` through `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync`.
- Preserved Java's friendly Elyos/Asmodian NPC same-race exclusion by carrying `WorldNpcFreeForAllResult.CanBroadcastTo` into the visible-world broadcast.
- Added `WorldNpcLootService.CreateInitialLootEnableStatus` for Java `DropRegistrationService.registerDrop` fanout, returning allowed looter ids plus the Java-shaped loot-enable status after a drop registration exists.
- Added `WorldNpcLootBroadcastService.SendInitialLootEnableAsync`, which sends the initial loot-enable packet directly to each allowed looter through `SendPacketToPlayerAsync` and reports target/sent counts.
- Added `WorldNpcLootBroadcastService.StartRegisteredDropFanoutAsync`, preserving the Java side-effect order for already-registered drops: direct initial loot-enable sends first, then `DropService.scheduleFreeForAll`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 358 and refreshed the validation baseline.

---

## Commits In This Handoff

- `dd45a33b3` - `Broadcast loot free-for-all status`
- `d3ef222b9` - `Send initial loot enable fanout`
- `c5f53b20b` - `Order registered loot fanout`

---

## Important Limits

- Real Java drop generation is still pending: custom NPC drop calculators, quest drops, global/event drops, drop modifiers, and registration from the actual NPC death/reward path.
- Future combat/life-stat NPC death callers still need to invoke the world NPC death/drop bridge so Java ordering is preserved across respawn scheduling, drop registration, initial loot-enable fanout, free-for-all scheduling, pet auto-loot, corpse decay, despawn, and respawn.
- The C# corpse signal for seen-NPC loot status still comes from the registered-drop map until the real NPC death/state model is wired.
- Loot collection still covers only the direct solo path. Group/alliance kinah splitting, item distribution, rolls, bids, winner messages, and team-member confirmation behavior remain pending.
- Temporary trade predicates, pet auto-sell, quality announcements, broader non-solo/drop-aware corpse cleanup, and persistence-side inventory save boundaries still need future parity passes.
- Ordinary NPC dialog remains a guard-only surface. Actual `AIEventType.DIALOG_START`, quest dialog start, and full `DialogService` / NPC AI selection remain pending.
- `WorldPosition.InstanceId` and rift fanout exist, but the world container and broadcast scoping are still mostly world-id based rather than full Java `WorldMapInstance` containers.

---

## Suggested Next Units

1. Build the Java drop-generation bridge from the real NPC reward/death flow: custom NPC drop calculators, quest drops, global/event drop rules, drop modifiers, and registration into the current `WorldNpcDropRegistrationService` maps.
2. Connect combat/life-stat NPC death callers into the drop-registration plus `WorldNpcSpawnService` death path, preserving Java order: schedule respawn before on-die effects, register drops, invoke the registered-drop fanout helper, run pet auto-loot when modeled, then schedule drop-aware corpse decay.
3. Deepen `CM_LOOT_ITEM` beyond direct solo collection: group/alliance kinah and item distribution, rolls/bids, winner messages, and Java team confirmation behavior.
4. Add the remaining Java loot side effects: temporary trade predicates, pet auto-sell/auto-loot boundaries, quality announcements, persistence save boundaries, and broader drop-aware corpse cleanup.
5. Deepen ordinary NPC dialog from the guard surface into Java AI/dialog start flow: `AIEventType.DIALOG_START`, quest dialog start, `DialogService`, and NPC controller selection.
6. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially world-instance visibility, housing/studio side effects, walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
