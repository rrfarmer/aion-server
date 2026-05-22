# Phase 6U Completion Handoff

**Created**: May 22, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6T and covers Sessions 230-233.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.  
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 477 tests.

---

## Recent Work Completed

- Loaded Java `gameserver.housing.visibility.distance` / `HousingConfig.VISIBILITY_DISTANCE`, preserving the 200m default for house render known-list checks.
- Added `WorldHouse` snapshots for Java `model/house/House` visibility state, deriving map coordinates from static `HouseAddress` data and carrying owner, legion, inactive, door, sign, building, and position fields used by house render/update packets.
- Added `HousingVisibilityService`, with per-player house-known address tracking and render/delete delta calculation for `SM_HOUSE_RENDER` and `SM_DELETE_HOUSE`.
- Extended the C# `World` container with a house snapshot store so known-list refreshes can scan spawned houses independently of player objects.
- Wired enter-world and movement to refresh house known-list deltas, and wired disconnect cleanup for the tracked house-known set.
- Extended `SM_HOUSE_RENDER` and `SM_HOUSE_UPDATE` to serialize persistent `WorldHouse` snapshots through the same Java `AbstractHouseInfoPacket.writeCommonInfo` bridge as player-owned loaded houses.
- Updated `CM_HOUSE_SETTINGS` appearance updates to refresh the world-house snapshot and broadcast `SM_HOUSE_UPDATE` from the house position when static coordinates are available.
- Added DB-backed persistent custom-house loading for Java `HousesDAO.loadHouses(..., false)`, including owner/legion joins, studio exclusion, inactive custom-house derivation by owner acquire order, and Java door-state fallback semantics.
- Added `HousingWorldService` as a bootstrap `GameEngine`, loading persistent custom houses into the C# world before players need known-list scans.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 233, including the updated next-step queue.

---

## Commits In This Handoff

- `2e0c1257f` - `Load housing visibility config`
- `6bd8ae2c8` - `Add house visibility snapshots`
- `a2e1d3310` - `Wire house render visibility updates`
- `2acdb65e3` - `Load persistent houses into world`

---

## Important Limits

- The new house world store loads persistent custom houses from DB rows, but it does not yet synthesize unowned custom houses for static addresses that are missing from the `houses` table.
- Studio addresses `2001`/`3001` are intentionally excluded from the global persistent custom-house load; Java spawns studios per personal instance and that instance flow is still pending.
- House object/decor registry packets are still placeholders; `SM_HOUSE_RENDER`/`SM_HOUSE_UPDATE` continue to emit empty decor lines until house-object models and persistence are ported.
- GeoService door state updates are still not ported, so door state changes update packets/DB but not collision/geo.
- Visitor kick behavior still lacks real house-zone membership, friend/legion filtering, teleport-out movement using address exit coordinates, and recipient messages.
- Deleted-owner house revocation from Java `HousingService.revokeOwnershipOfDeletedPlayers` is still pending.
- Region-bucketed KnownList parity remains broader future work; the current house bridge uses world-house snapshots plus distance checks.

---

## Suggested Next Units

1. Continue housing from the new world-house baseline: synthesize missing unowned custom houses from static `HouseAddress` data, preserving Java default building and ownerless closed-door behavior.
2. Add per-instance studio spawning once a C# personal-instance/home entry flow exists.
3. Add GeoService door update hooks for house door-state changes.
4. Continue visitor kick side effects with house-zone membership, friend/legion filtering, recipient messages, and teleport-out to the loaded exit coordinates.
5. Add populated decor/object registry support behind `SM_HOUSE_RENDER`/`SM_HOUSE_UPDATE`, then expand expirable lifecycle work to house objects.
6. Continue AP side effects only when a real C# home exists for legion contribution or siege callbacks.
7. Resume the stigma/effect slice with full `SkillEngine` effect apply/remove behavior and corresponding stat/effect packet fanout.
