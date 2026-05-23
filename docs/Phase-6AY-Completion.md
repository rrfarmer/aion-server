# Phase 6AY Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AX and covers Sessions 366-372.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 736 tests.

---

## Recent Work Completed

- Added default global-drop item generation to `WorldNpcGlobalDropService`, preserving Java `DropRegistrationService.addGlobalDrops` / `collectDrops` / `getItemCount` behavior for default rules: chance rolls, max-drop weighted selection, count rolls, Kinah scaling, member-limit distributed drops, and the default-vs-NPC-specific rule gate.
- Threaded generated global drops into `WorldNpcDropRegistrationWorkflowService` after quest drops, so global-only deaths now register current-drop maps and run the existing initial loot-enable plus scheduled free-for-all fanout.
- Added `EventDropTable`, `EventTemplateSummary`, and `WorldNpcEventDropRuleService` for Java timed event drop rules, including active-window filtering, disabled-event filtering, wildcard disables, and workflow integration after default globals.
- Bound Java `gameserver.event.service.disabled_events` into `GameServerOptions.Custom.DisabledEventNames` and wired the event drop rule service to consume that runtime config.
- Extended NPC template loading with Java `group_drop` and `abyss_type`, then applied NPC group restrictions, abyss-type exclusions, and the default global-drop abyss guard.
- Added Java `GlobalDropData.processRules` parity so `gd_npc_names` expand into concrete NPC ids at static-data load time and only concrete NPC-id rules bypass default-global NPC exclusions.
- Extended world-map loading with Java `drop_type`, applied `gd_world` restrictions, skipped known `WorldDropType.NONE` maps for default globals, and added the staged `InsideZones` modifier surface for future Java `npc.isInsideZone(...)` parity.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 372 and refreshed the validation baseline.

---

## Commits In This Handoff

- `422382ad3` - `Generate global NPC drops`
- `25cf59263` - `Add event drop rule surface`
- `5c481b8d0` - `Bind disabled event drop config`
- `9f0df7a02` - `Apply NPC template drop restrictions`
- `443ad53b8` - `Expand global drop NPC name rules`
- `f536a3dee` - `Apply world drop type restrictions`
- `1f98f4a1d` - `Stage global drop zone restrictions`

---

## Important Limits

- Real combat/life-stat NPC death callers still need to invoke `WorldNpcDeathDropWorkflowService`; the death/drop bridge is staged and covered, but not yet connected to every live death path.
- Zone restrictions have a predicate surface through `WorldNpcDropModifiers.InsideZones`, but the live `CM_SUBZONE_CHANGE` / `MapRegion` revalidation model is still missing.
- Siege/base spawn restrictions in Java `isAllowedDefaultGlobalDropNpc` are still pending because the relevant spawn-template/handler models are not yet ported into the death/drop surface.
- Runtime event scheduler/start/stop side effects are still pending, including event config-property application, quests, buffs, spawns, themes, surveys, inventory drops, and live config reload behavior.
- Handler-side quest drops, full mentor-nearby checks, and complete group/alliance quest loot rules remain pending.
- Live drop-rate boost inputs are still staged: NPC/player `BOOST_DROP_RATE`, player `DR_BOOST`, repose/salvation, house palace, and related rate configuration need their real stat/model sources.
- Optional sockets, temporary trade predicates, pet auto-loot/auto-sell, quality announcements, persistence save boundaries, and instance/AI `onDropRegistered` callbacks remain pending.
- Loot collection still covers only the direct solo path. Group/alliance kinah splitting, item distribution, rolls, bids, winner messages, and team-member confirmation behavior remain pending.

---

## Suggested Next Units

1. Wire real combat/life-stat NPC death callers into `WorldNpcDeathDropWorkflowService`, preserving Java `NpcController.onDie` order and avoiding duplicate legacy decay/drop paths.
2. Add the missing siege/base spawn model pieces needed by Java `isAllowedDefaultGlobalDropNpc`, then apply the siege/base global-drop exclusion guards.
3. Port enough live zone membership/revalidation for NPCs to feed `WorldNpcDropModifiers.InsideZones` from real `CM_SUBZONE_CHANGE` / `MapRegion` state.
4. Build the runtime event lifecycle: scheduler, start/stop, config-property application, event quests/buffs/spawns/themes/surveys/inventory drops, and reload behavior.
5. Add handler-side quest drops and full mentor-nearby checks to the quest-drop bridge.
6. Deepen `CM_LOOT_ITEM` beyond direct solo collection: group/alliance kinah, item distribution, rolls/bids, winner messages, and Java team confirmation behavior.
7. Add remaining Java loot side effects: optional socket selection, temporary trade predicates, pet auto-sell/auto-loot boundaries, quality announcements, persistence save boundaries, and broader drop-aware corpse cleanup.
8. Continue the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially world-house/NPC visibility, studio spawn callbacks, temporary spawn depth, walker/random-walk geo correction, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
