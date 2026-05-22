# Phase 6P Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6O and covers the item-use action expansion through Sessions 193-198.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 428 tests.

---

## Recent Work Completed

- Ported Java `EmotionLearnAction.act` for `CM_USE_ITEM`: emotion cards now parse card metadata, validate duplicate/missing IDs, persist `player_emotions`, consume the source item, broadcast instant item-use animation, and send Java-shaped `SM_EMOTION_LIST(action=1)`.
- Ported Java `TitleAddAction`: title cards now parse `titleid` plus optional duration, preserve Java race/duplicate guards, persist `player_titles`, consume the card, send title system messages, and refresh the owner with full-list `SM_TITLE_INFO`.
- Ported Java `SkillLearnAction`: skill books now parse skill/class/level metadata, validate class/race/level/known-skill guards, persist `player_skills`, emit Java-shaped skill-list packets, and consume the book.
- Ported Java `ExpandInventoryAction`: cube and warehouse tickets now parse expansion metadata, validate Java ticket limits, persist `players.item_expands` / `players.wh_bonus_expands`, consume the ticket, and send cube or warehouse refresh packets.
- Ported the item-target branch of Java `DyeAction`: dye items now parse color and optional duration, honor the Java dyeable item mask, persist item color fields, consume the dye item, send success/error messages, and refresh equipped appearance when needed.
- Ported Java `AnimationAddAction`: motion cards now parse motion action metadata, use the Java 1s item-use animation, persist `player_motions`, replace active motions by type, consume the card, send owner `SM_MOTION(action=2)` packets, and refresh visible active-motion state.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 198.

---

## Important Limits

- A shared C# `ExpireTimerTask` equivalent is still missing for temporary emotions, titles, and motions. Timeout removal packets/messages remain pending.
- `SkillLearnAction` is routed and persisted, but passive `SkillEngine` effect application, profession level-up action animations, recipe autolearn side effects, and nearby quest refreshes remain future slices.
- NPC-paid cube and warehouse expansion flows still depend on fuller NPC/dialog function validation; the completed unit only covers item-ticket expansion parity.
- Java `DyeAction.dyeHouseObject` is not ported yet. House-object painting should wait until house object edit/spawn state is broad enough to mirror that branch.
- Remodel is primarily the `ItemRemodelService` and NPC dialog flow rather than a simple item action, so it needs dialog validation before a faithful port.
- Cosmetic action is not started. It needs cosmetic static data, appearance persistence, packet fanout, and item-use routing.
- Decomposition is not started. It depends on decomposable static data, reward item creation, inventory capacity checks, and random reward behavior.
- Stigma/effect application, charge/power-shard/idian observer triggers, persistent known-list membership, full NPC/dialog validation, and broader housing lifecycle work remain pending.
- Real-client validation remains deferred until the end-of-port readiness pass.

---

## Suggested Next Units

1. Add a C# expirable-task bridge for temporary emotions, titles, and motions, including timeout removal packets/messages and persistence cleanup where Java does it.
2. Continue `CM_USE_ITEM` routing with a bounded Java action only after checking that its static data, persistence surface, and packet fanout are representable in C#.
3. Consider cosmetic action next if cosmetic static data and appearance persistence can be added in a contained slice; otherwise choose a smaller item action with less reward/random state.
4. Tackle decomposition only after adding decomposable data loading and a Java-shaped reward item creation path.
5. Resume the stigma/effect slice with full `SkillEngine` effect apply/remove behavior after temporary skill mutations.
6. Continue NPC/dialog function validation before remodel, NPC-paid expansion, or other dialog-owned item/equipment flows.
7. Wire charge, power-shard, and idian burn triggers into the future skill/combat observer paths once observer lifecycle support is ready.
