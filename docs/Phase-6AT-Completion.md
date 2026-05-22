# Phase 6AT Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AS and covers Sessions 348-351.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 678 tests.

---

## Recent Work Completed

- Added `SM_LOOT_STATUS` opcode 205 and `SM_LOOT_ITEMLIST` opcode 206 with Java-shaped payloads for target object id, loot status/effect id, visible drop entries, optional sockets, and solo loot-confirmation suppression.
- Added C# player looting state parity: `PlayerCreatureState.Looting`, `Player.LootingNpcObjectId`, and `StartLooting` / `StopLooting` helpers matching Java `DropService.requestDropList` and `DropService.closeDropList`.
- Extended `WorldNpcDropItem` with Java `DropItem.getLootEffectId`, optional socket, and only-possible-looter checks, and extended `WorldNpcDropRegistration` with `DropNpc`-style current looting player state.
- Added `WorldNpcLootService` for Java `DropService.requestDropList` / `closeDropList`: open-list guards for missing drops, loot rights, and already-looted corpses; successful opens send the item list plus open status, mark the player looting, and emit start-loot emotion; closes clear the looting player and emit end-loot emotion.
- Wired `CM_START_LOOT` action 0/1 through `GameServerConnection` and game-server DI, then added the first `CM_LOOT_ITEM` collection path for Java `DropService.requestDropItem`, narrowed to direct solo item collection while group/alliance distribution remains deferred.
- Extended `WorldNpcDropRegistrationService` with current-drop lookup by item index and count updates after collection, removing a drop when its remaining count reaches zero.
- `WorldNpcLootService.RequestDropItem` now validates registration, loot rights, item templates, and solo-collection support; uses `InventoryAddService.CreateAddItemPlan`; applies added/updated inventory state to the player; and emits `SM_INVENTORY_ADD_ITEM` / `SM_INVENTORY_UPDATE_ITEM` collect packets.
- After a successful solo pickup, the loot service resends `SM_LOOT_ITEMLIST` while drops remain, or sends `SM_LOOT_STATUS.CLOSE_DROP_LIST`, clears looting, clears the registration looter, emits end-loot emotion, and deletes/unregisters the empty corpse when the final drop is collected.
- Added tracked world-NPC decay tasks in `WorldNpcSpawnService`, including `HasDecayTask`, `PendingDecayCount`, and `CancelDecay` returning the remaining delay, mirroring Java `TaskId.DECAY` cancellation at the loot boundary.
- `WorldNpcLootService.RequestDropList` now cancels a pending corpse decay task when a loot list opens and stores the remaining milliseconds on the `DropNpc`-style registration; `CloseDropList` resumes the stored decay delay when drops remain.
- Added the empty-drop corpse cleanup boundary for the current solo loot path: final collection deletes the visible world NPC through `WorldNpcSpawnService` and unregisters the current-drop/registration maps, with breadcrumbs for Java `DropService.resendDropList` and `NpcController.onDespawn`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 351 and refreshed the validation baseline.

---

## Commits In This Handoff

- `cbcb73bde` - `Handle NPC loot list requests`
- `5878833c6` - `Collect solo NPC loot items`
- `7de771608` - `Resume NPC decay after looting`
- `f2e8d663b` - `Delete empty NPC loot corpses`

---

## Important Limits

- Real Java drop generation is still pending: drop calculators, global drops, quest drops, and registration from the actual NPC death path.
- Future combat/life-stat NPC death callers still need to invoke the world NPC death/drop bridge so drops, corpse decay, despawn, and respawn are ordered against Java `NpcController.onDie` / `RespawnService`.
- Loot collection currently covers only the direct solo path. Group/alliance kinah splitting, item distribution, rolls, bids, winner messages, and team-member confirmation behavior remain pending.
- Limit-one and lore item checks, temporary trade predicates, pet auto-sell, quality announcements, free-for-all scheduling/broadcasts, and friendly-NPC race filters remain pending.
- Empty-corpse deletion is implemented for the current solo collection path only. Broader non-solo/drop-aware cleanup and persistence-side inventory save boundaries still need future parity passes.
- Ordinary NPC dialog remains a guard-only surface. Actual `AIEventType.DIALOG_START`, quest dialog start, and full `DialogService` / NPC AI selection remain pending.
- `WorldPosition.InstanceId` and rift fanout exist, but the world container and broadcast scoping are still mostly world-id based rather than full Java `WorldMapInstance` containers.

---

## Suggested Next Units

1. Build the Java drop-generation bridge from real NPC death flow: drop calculators, global drops, quest drops, and registration into the current `WorldNpcDropRegistrationService` maps.
2. Connect combat/life-stat NPC death callers into `WorldNpcSpawnService.TryScheduleWorldNpcDeath(objectId)` and verify drop registration, corpse decay, despawn, and respawn ordering against Java `NpcController.onDie` / `RespawnService`.
3. Deepen `CM_LOOT_ITEM` beyond direct solo collection: group/alliance kinah and item distribution, rolls/bids, winner messages, and Java team confirmation behavior.
4. Add the remaining Java loot guards and side effects: limit-one/lore checks, temporary trade predicates, pet auto-sell, quality announcements, free-for-all broadcast scheduling, friendly-NPC loot filters, and broader drop-aware corpse cleanup.
5. Deepen ordinary NPC dialog from the guard surface into Java AI/dialog start flow: `AIEventType.DIALOG_START`, quest dialog start, `DialogService`, and NPC controller selection.
6. Continue world-instance work beyond rift fanout: instance-aware player/NPC visibility, broadcasts, per-instance spawn state, and house/studio visibility side effects.
7. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
