# Phase 6Y Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6X and covers Sessions 251-258.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 485 tests.

---

## Recent Work Completed

- Implemented `CM_USE_HOUSE_OBJECT` / `CM_RELEASE_OBJECT` runtime branches for storage and postbox house objects, including Java talk-range checks, owner-only storage denial, postbox mailbox state/dialog behavior, occupant tracking, use updates, and release/cancel cleanup.
- Implemented the main Java `UseableItemObject` reward path: owner/visitor guards, daily/cooldown checks, required equipped/cube item checks, delayed `SM_USE_OBJECT` gauge completion, reward item grants, remove-count consumption, use-count persistence, cooldown mutation, and no-final-reward deletion.
- Finished final-reward handoff parity for expired house use-items: owner recovery, visitor denial, `expire_time` preservation, final reward claim/delete, and cooking duplicate-reward denial.
- Extended `ExpirableTaskService` to loaded active-house registry objects and wired expired registered objects through Java `HouseObject.onExpire`-style deletion, packet fanout, cooldown cleanup, registry refresh, and ID release.
- Added runtime `canExpireNow` deferral for use-item/storage/postbox occupants and the available C# `NpcObject.canExpireNow` target representation.
- Added Java `HousesDAO.loadHouses`-style validation at the world-house load boundary so bad/duplicate persistent DB snapshots do not enter `World`, while skipped valid template addresses still synthesize ownerless custom houses.
- Added a C# `IHouseDoorStateService` bridge for Java `GeoService.setHouseDoorState` and wired startup/live world-house updates to keep door state available to future collision/entry work.
- Extracted shared Java `AbyssPointsService.onRankChanged` side effects for C# AP rank changes and reused them for AP extraction plus AP-paid item charge/charge-all completion.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 258 and pruned completed housing/AP next-step items.

---

## Commits In This Handoff

- `80aecb436` - `Handle storage and postbox house objects`
- `db39971e5` - `Handle useable house object rewards`
- `f4f2828c0` - `Handle house object final rewards`
- `4a136c601` - `Expire registered house objects`
- `c8aeeee2b` - `Defer busy house object expiration`
- `3b6cd51f0` - `Validate loaded world houses`
- `12340d2f1` - `Track house door states`
- `0c1b2cc2b` - `Apply AP charge rank side effects`

---

## Important Limits

- Studio addresses `2001`/`3001` are still not modeled as Java per-personal-instance spawned houses. C# currently has world-house snapshots and owner login state, but no full personal-instance/studio spawn lifecycle.
- Visitor kick side effects remain incomplete. C# sends the owner door-setting messages, but full Java house-zone membership, friend filtering, recipient messages, and teleport-out behavior still need the supporting teleport/instance model.
- House-object/NPC known-list fidelity is still partial. C# uses visible world-house snapshots and distance checks, not full Java region-bucketed `KnownList` membership or complete NPC function/dialog validation.
- The `IHouseDoorStateService` bridge tracks door state, but full collision geometry behavior is still pending until the C# GeoService equivalent exists.
- AP rank changes now cover visible rank packets, rank-limited equipment cleanup, and abyss transform skill refreshes. Legion contribution fanout and `SiegeService.onAbyssPointsAdded` remain pending because those supporting systems do not yet have C# homes.
- Full passive `SkillEngine` effect apply/remove fanout after temporary skill mutations remains pending.
- Real-client validation is still needed for scheduled item-use ordering and newly ported house-object use flows.

---

## Suggested Next Units

1. Continue housing with studio spawning only after choosing the C# instance model: Java `HousingService.spawnHouses(instance, registeredId)` calls `spawnStudio` for personal instances and excludes studios from global custom-house maps.
2. Continue visitor kick side effects once teleport/instance support is ready: house-zone membership, friend exemptions, `STR_MSG_HOUSING_REQUEST_OUT` / owner-change messages, and teleport to exit coordinates or near-door coordinates.
3. Tighten NPC/dialog known-list validation: replace object-id-only guards with visible NPC template/function checks for broker, storage, house NPCs, and dialog actions.
4. Continue world/known-list fidelity for house objects: broadcast placement/move/despawn/use side effects to already sighted non-owner players and eventually replace the distance scan with a fuller region/known-list model.
5. Add the real GeoService collision side of house doors behind the new `IHouseDoorStateService` bridge.
6. Resume AP/abyss integration when C# has homes for legion contribution and siege callbacks.
7. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
8. Real-client validate decompose, assembly, XP extraction, composition, extraction, AP extraction, and house-object delayed use ordering once the readiness pass begins.
