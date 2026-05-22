# Phase 6AB Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AA and covers Sessions 266-267.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 506 tests.

---

## Recent Work Completed

- Added a generic `IWorldNpcObject` / `WorldNpc` visible-object bridge for future ordinary NPC spawns, while keeping the postman NPC compatible with the same template/position contract.
- Added `NpcDialogTargetingService.ValidateTargetingNpcWithFunction`, matching Java `Player.isTargetingNpcWithFunction` against the current C# world model: target object ID, spawned NPC object type, first-pass visibility, and `NpcTemplateSummary.SupportsDialogAction`.
- Updated broker packet guards to prefer spawned-NPC validation for Java `DialogAction.OPEN_VENDOR` (`33`), with the old object-id fallback retained only for the current no-ordinary-NPC-spawns state.
- Split studio loading from globally spawned custom-house loading. `IHousingRepository.LoadWorldStudiosAsync` now mirrors Java `HousesDAO.loadHouses(..., true)` for addresses `2001` / `3001`, while custom houses stay on the non-studio filter.
- Added an owner-keyed studio cache in `HousingWorldService` plus `TryGetPlayerStudio` and `TrySpawnStudio`, preserving Java `HousingService`'s separate `customHouses` / `studios` maps and the `spawnStudio(worldId, instanceId, registeredId)` shape as far as the current no-instance `WorldPosition` allows.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 267 and updated the Phase 6 next-step queue.

---

## Commits In This Handoff

- `89e3efc95` - `Add NPC dialog targeting bridge`
- `403ef5d4c` - `Cache housing studios for on-demand spawn`

---

## Important Limits

- Ordinary NPC spawn data still does not populate `WorldNpc` instances. Broker validation has a compatibility fallback until broker NPCs have real visible-object/template ownership in `World`.
- `NpcDialogTargetingService` uses first-pass `WorldVisibility` as the known-list proxy. Full region-bucketed `KnownList` and regular spawn engine ownership are still pending.
- Studio rows now load into an owner-keyed cache and can be spawned on demand, but the C# world model still lacks instance IDs. Multiple live studio instances sharing address `2001` or `3001` cannot yet be represented accurately through `World.GetHouses`.
- Future instance/teleport code still needs to call `HousingWorldService.TrySpawnStudio` at the Java `registeredId` spawn point; current enter-world house registration is not a full instance lifecycle substitute.
- Power-shard and idian burn work from 6AA still lacks the final combat/skill callers and packet fanout.

---

## Suggested Next Units

1. Populate ordinary `WorldNpc` instances from spawn data and remove the broker object-id fallback once broker NPC templates are genuinely present in `World`.
2. Wire studio spawn calls from the future instance/teleport `registeredId` path, then make house visibility instance-aware so multiple studio owners can share studio addresses safely.
3. Add visitor kick side effects for house ownership/settings/building changes, matching Java `HouseController.kickVisitors`.
4. Add the C# combat/stat caller for Java `PlayerGameStats.getPowerShardDamage`, then apply `PowerShardDamageResult.InventoryItems`, persist `PowerShardUseResult` mutations, and send Java inventory/delete/burn-out/state packets.
5. Wire `IdianPolishService.BurnEquippedWeaponPolishCharge` into the future `SkillEngine` condition path, including low-charge and exhausted-idian owner packets plus in-memory item replacement.
6. Continue `IdianStone.onEquip` observer parity for attack/defend burns and stat refresh fanout once the observer/effect lifecycle has a C# home.
7. Resume AP/abyss integration when C# has homes for legion contribution and siege callbacks.
8. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
9. Real-client validate decompose, assembly, XP extraction, composition, extraction, AP extraction, and house-object delayed use ordering once the readiness pass begins.
