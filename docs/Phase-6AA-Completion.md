# Phase 6AA Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6Z and covers Sessions 263-265.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 499 tests.

---

## Recent Work Completed

- Added `PowerShardDamageService.GetPowerShardDamage`, the first C# calculation surface for Java `PlayerGameStats.getPowerShardDamage`. It checks `CreatureState.POWERSHARD`, reads Java right/left power-shard slots, applies parsed `ItemTemplateSummary.WeaponBoost`, handles two-handed main-hand damage, and delegates optional stack burn to `EquipmentService.UsePowerShard`.
- Corrected the power-shard fixture slot bits to Java `ItemSlot.POWER_SHARD_RIGHT` / `POWER_SHARD_LEFT` values (`1 << 13` and `1 << 14`).
- Added `PlayerEnterWorldService.SavePowerShardUseMutationAsync` plus `IPlayerEnterWorldRepository.SavePowerShardUseMutationAsync`, so returned `PowerShardUseResult` planners can persist count decrements, exhausted equipped-stack deletes, and replacement stack equip-state/slot updates in one transaction.
- Added `PlayerEnterWorldService.SaveIdianPolishBurnMutationAsync` plus `IPlayerEnterWorldRepository.SaveIdianPolishBurnMutationAsync`, preserving the Java `IdianStone.decreasePolishCharge` nuance that only zero-charge idian depletion immediately calls the `ItemStoneListDAO.storeIdianStones`-equivalent persistence path.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 265 and updated the Phase 6 next-step queue.

---

## Commits In This Handoff

- `678d567f9` - `Calculate power shard boost damage`
- `be543f0df` - `Persist power shard use mutations`
- `5040068a9` - `Persist exhausted idian burn deletes`

---

## Important Limits

- Power-shard calculation, stack-burn planning, and persistence boundaries now exist, but no combat damage caller invokes `PowerShardDamageService` yet. The runtime still needs owner packets (`SM_INVENTORY_UPDATE_ITEM`, `SM_DELETE_ITEM`, burn-out message) and boost-state cleanup when the final stack burns out.
- `PolishChargeCondition` planning and exhausted-idian persistence boundaries now exist, but C# still lacks the real `SkillEngine` condition/effect caller and Java packet fanout for low-charge/exhausted idian updates.
- Java `IdianStone.onEquip` attack/defend observers are still not modeled. The current C# stat packet can read charged main-hand idian effects, but observer-driven burn and stat refresh fanout remain future work.
- NPC function ids and talk distances are loaded, but runtime broker/dialog validation is still object-id-only because C# still lacks ordinary spawned NPC visible objects with template references and true region/known-list membership.
- AP rank changes still lack legion contribution fanout and `SiegeService.onAbyssPointsAdded`, pending C# homes for those systems.
- Housing still has the Phase 6Y/6Z limits: no studio spawning lifecycle, incomplete visitor kick side effects, first-pass house/NPC known-list fidelity, and door-state tracking without full GeoService collision behavior.

---

## Suggested Next Units

1. Add the C# combat/stat caller for Java `PlayerGameStats.getPowerShardDamage`, then apply `PowerShardDamageResult.InventoryItems`, persist `PowerShardUseResult` mutations, and send Java inventory/delete/burn-out/state packets.
2. Wire `IdianPolishService.BurnEquippedWeaponPolishCharge` into the future `SkillEngine` condition path, including low-charge and exhausted-idian owner packets plus in-memory item replacement.
3. Continue `IdianStone.onEquip` observer parity for attack/defend burns and stat refresh fanout once the observer/effect lifecycle has a C# home.
4. Add a real spawned-NPC visible-object/template path, then replace broker/dialog object-id-only checks with Java `Player.isTargetingNpcWithFunction` parity using known-list/range and `NpcTemplate.supportsAction`.
5. Continue housing with studio spawning, visitor kick side effects, and fuller house-object/NPC known-list delivery when the instance/teleport/known-list surfaces are ready.
6. Resume AP/abyss integration when C# has homes for legion contribution and siege callbacks.
7. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
8. Real-client validate decompose, assembly, XP extraction, composition, extraction, AP extraction, and house-object delayed use ordering once the readiness pass begins.
