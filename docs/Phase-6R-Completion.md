# Phase 6R Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6Q and covers Sessions 208-216.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 461 tests.

---

## Recent Work Completed

- Tightened Java `ItemService.addStackableItem` parity for power shards: equipped shard stacks are filled before cube stacks, while normal stackables still ignore equipped rows.
- Broadened the C# `ExpireTimerTask` bridge to loaded cube/equipment items, regular warehouse items, account warehouse items, new non-stackable decompose rewards, Java cash-item warning thresholds, inventory/warehouse delete packets, storage refreshes, and `item_stones` cleanup.
- Loaded Java `assembly_items.xml` plus item-template `<assemble item="..."/>` metadata, then ported Java `AssemblyItemAction` runtime: all-part validation, 1s scheduled use, part consumption by item ID, success message, reward add, and Java's consumed-parts/full-inventory reward failure behavior.
- Parsed Java `<expextract item_id percent cost/>` metadata and ported `ExpExtractAction` runtime: 5s use/cancel, inventory/EXP guards, fixed/percent cost, source consumption by item ID, EXP persistence, `SM_STATUPDATE_EXP`, reward add, dice-inventory failure, and success messaging.
- Tightened reusable item-add partial behavior so capacity-limited plans preserve stack/new-row mutations, retain remainder counts, and expose inventory-full status for Java dice messaging.
- Parsed and routed opcode `208` `CM_COMPOSITE_STONES` with Java `CompositionAction` parity: combination tool/enchantment-stone validation, 5s self-only animation/cancel, sequential item-id consumption, Java reward-id calculation, reward add, and multi-consume/reward persistence.
- Parsed remaining small extraction metadata: `<extract/>` markers and `<apextract rate target/>` values now live on `ItemTemplateSummary`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 216, with validation and next-step queue updated.

---

## Important Limits

- Real-client validation remains deferred until the end-of-port readiness pass.
- `ExtractAction` and `ApExtractAction` runtime are metadata-only for now. Runtime needs `EnchantService.breakItem`, target equipment deletion side effects, AP rank/common-data persistence, AP stat packets, and exact failure messaging.
- XP extraction still lacks Java `PlayerCommonData.setExp` level-change side effects and quest item-removed observer callbacks.
- Assembly/composition still lack Java quest item-removed observer callbacks and real-client ordering validation of scheduled packet sequences.
- Expirable pet and house-object rows remain pending until their models and persistence surfaces exist.
- Casting/protection interruption, full item-use observer behavior, stance observers, quest/summon observers, power-shard side effects, charge/idian burn triggers, full SkillEngine effect apply/remove fanout, persistent known-list membership, and full NPC/dialog validation remain pending.

---

## Suggested Next Units

1. Scope `ExtractAction`/`ApExtractAction` runtime only after the missing support pieces are explicit: `EnchantService.breakItem`, target equipment deletion side effects, AP rank/common-data persistence, AP stat packets, and exact failure messaging.
2. Broaden the expirable lifecycle bridge to Java's remaining registered expirable types, pets and house objects, once the missing pet/house-object models and persistence surfaces exist.
3. Resume the stigma/effect slice with full `SkillEngine` effect apply/remove behavior and the corresponding stat/effect packet fanout.
4. Continue `CM_EMOTION` only after introducing one missing support model such as fly-zone/cooldown/FP timers, stance observers, sit observers, quest/summon observers, or reusable stat-speed calculation.
5. Wire charge, power-shard, and idian burn triggers into the future skill/combat observer paths.
6. Continue housing auction settlement, maintenance, sign, and appearance flows when the next slice should stay out of the stat engine.
7. Add persistent known-list membership and full NPC/dialog function validation, then revisit dialog-owned item services such as remodel preview, NPC-paid expansion, and vendor-like flows.
