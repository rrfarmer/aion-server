# Phase 6Z Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6Y and covers Sessions 259-262.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 490 tests.

---

## Recent Work Completed

- Loaded Java NPC `TalkInfo` metadata into C# static data. `NpcTemplateSummary` now carries `talk_info.distance`, whitespace-parsed `func_dialogs`, and `SupportsDialogAction`, matching Java `NpcTemplate.getTalkDistance` / `supportsAction`.
- Added real static-data coverage for broker NPC `799211`, confirming `DialogAction.OPEN_VENDOR` (`33`) function metadata is available for future broker/dialog targeting checks.
- Loaded item-template `weapon_boost` into `ItemTemplateSummary`, preserving the Java `ItemTemplate.getWeaponBoost` value needed by `PlayerGameStats.getPowerShardDamage`.
- Added real static-data coverage for power shard template `169000005`, confirming its `WeaponBoost` value is `20`.
- Added `EquipmentService.UsePowerShard`, a Java `Equipment.usePowerShard` / `decreaseEquippedItemCount` planner that decrements equipped shard stacks, deletes exhausted stacks, equips the next same cube stack into the same shard slot, or reports burn-out/deactivation.
- Added Java `STR_MSG_WEAPON_BOOST_MODE_BURN_OUT` system-message coverage for the future runtime caller.
- Added `IdianPolishService.BurnEquippedWeaponPolishCharge`, a Java `PolishChargeCondition.validate` planner that burns charged idian stones on equipped weapons while skipping `MAIN_OFF_HAND` / `SUB_OFF_HAND` equipment-set slots.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 262 and updated the Phase 6 next-step queue.

---

## Commits In This Handoff

- `7aefac84d` - `Load NPC dialog function metadata`
- `0bb2287eb` - `Load power shard weapon boost metadata`
- `bd9ae1f2c` - `Plan power shard stack burn`
- `a3e438eaf` - `Plan idian polish condition burn`

---

## Important Limits

- NPC function ids and talk distances are now loaded, but runtime broker/dialog validation is still object-id-only because C# still lacks ordinary spawned NPC visible objects with template references and true region/known-list membership.
- Power-shard metadata and stack burn planning are ready, but `UsePowerShard` is not invoked until a C# combat/stat path exists for Java `PlayerGameStats.getPowerShardDamage`.
- Idian polish charge planning now covers Java `PolishChargeCondition`, but it still needs a real `SkillEngine` condition/effect invocation point plus runtime packet/persistence handling.
- Java `IdianStone.onEquip` attack/defend observers are still not modeled. The current C# stat packet can read charged main-hand idian effects, but observer-driven burn and stat refresh fanout remain future work.
- AP rank changes still lack legion contribution fanout and `SiegeService.onAbyssPointsAdded`, pending C# homes for those systems.
- Housing still has the Phase 6Y limits: no studio spawning lifecycle, incomplete visitor kick side effects, first-pass house/NPC known-list fidelity, and door-state tracking without full GeoService collision behavior.

---

## Suggested Next Units

1. Add a real spawned-NPC visible-object/template path, then replace broker/dialog object-id-only checks with Java `Player.isTargetingNpcWithFunction` parity using known-list/range and `NpcTemplate.supportsAction`.
2. Build the C# combat/stat entry point for Java `PlayerGameStats.getPowerShardDamage`, using `ItemTemplateSummary.WeaponBoost` and `EquipmentService.UsePowerShard`, then add persistence and owner packets (`SM_INVENTORY_UPDATE_ITEM`, `SM_DELETE_ITEM`, burn-out message, state unset).
3. Wire `IdianPolishService.BurnEquippedWeaponPolishCharge` into the future `SkillEngine` condition path, including low-charge/exhausted update packets and `ItemStoneListDAO.storeIdianStones`-equivalent persistence at the correct Java points.
4. Continue `IdianStone.onEquip` observer parity for attack/defend burns and stat refresh fanout once the observer/effect lifecycle has a C# home.
5. Continue housing with studio spawning, visitor kick side effects, and fuller house-object/NPC known-list delivery when the instance/teleport/known-list surfaces are ready.
6. Resume AP/abyss integration when C# has homes for legion contribution and siege callbacks.
7. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
8. Real-client validate decompose, assembly, XP extraction, composition, extraction, AP extraction, and house-object delayed use ordering once the readiness pass begins.
