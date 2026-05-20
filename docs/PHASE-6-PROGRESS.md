# Phase 6: Port Game Core - Detailed Plan & Progress

**Status**: IN PROGRESS (started May 20, 2026)  
**Target**: Port gameplay systems in Java dependency order while keeping database and packet behavior compatible.  
**Validation Approach**: Parity/unit/integration tests first; real-client validation remains an end-of-port readiness step.  
**Code Trace Convention**: GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior they mirror.

---

## Resume Snapshot

Last updated: May 20, 2026

- Phase 5 is complete for automated infrastructure parity. Real-client validation is intentionally deferred to later readiness validation.
- Current active work is Phase 6c enter-world. The C# path handles `CM_ENTER_WORLD`, validates missing/online/reentry/duplicate-world cases, loads the player common row, player-owned inventory rows, player skills, active skill cooldowns, active item cooldowns, working quests, motions, client settings, and obelisk bind point, marks the character online, stores it in the world container, transitions the connection to `InGame`, sends `SM_ENTER_WORLD_CHECK`, then sends the implemented post-enter packets `SM_SKILL_LIST`, `SM_SKILL_COOLDOWN`, `SM_ITEM_COOLDOWN`, `SM_QUEST_LIST`, current-title `SM_TITLE_INFO`, `SM_MOTION`, `SM_AFTER_TIME_CHECK_4_7_5`, optional `SM_UI_SETTINGS` blobs, Java-split `SM_INVENTORY_INFO`, `SM_CHANNEL_INFO`, obelisk `SM_BIND_POINT_INFO`, baseline `SM_PLAYER_SPAWN`, and `SM_GAME_TIME`.
- Character creation is DB-backed and writes `players`, `player_appearance`, `player_skills`, and starter `inventory` rows. It uses Java-style starter items, equipment-slot selection, level-1 autolearn skills, old-name reservation checks, and membership character limits.
- Startup now preloads `IDFactory` from Java-equivalent used-ID tables before gameplay allocation.
- Next implementation slice should expand enter-world object loading with item stones, recipes, title lists/completed quest list, life stats, warehouse/account data, then continue the Java retail packet sequence with warehouse/full-title/emotion/prices/recipe/friend/block/stats packets.
- Latest validation: `dotnet test dotnetConversion\AionServer.slnx` passed with 258 tests.

---

## Phase 6 Scope

From `csharp-port.md`, dependency order:

1. Account and character list flow
2. Character create/delete/restore
3. Player enter-world flow
4. Inventory and equipment
5. Movement and known-list updates
6. NPC and spawn engine basics
7. Skills and effects
8. Combat and damage
9. Loot and item use
10. Quest state persistence
11. Player logout/save flow
12. Periodic saves and recovery behavior

---

## Current Checklist

### Phase 6a: Account And Character Flow
- [x] DB-backed character list from `players`, `player_appearance`, and visible equipped `inventory`
- [x] Login bridge character-count response for server-list fanout
- [x] Delete/restore shell using Java `deletion_date` semantics
- [ ] Account object/session model beyond infrastructure auth fields

### Phase 6b: Character Creation
- [x] Parse Java-shaped `CM_CREATE_CHARACTER`
- [x] Java response codes in `SM_CREATE_CHARACTER`
- [x] Typed `player_initial_data` holder with race spawn points and starter items by class
- [x] Item equipment-slot mapping from Java `ItemGroup`
- [x] Java-like basic validation: normalized names, used name, valid/forbidden name, starting class, same-race creation mode
- [x] Build Java-shaped select-screen entry for newly created character
- [x] MySQL creation repository writes `players`, `player_appearance`, and starter `inventory` rows transactionally
- [x] Initial skill learning from `skill_tree`
- [x] Old-name reservation lookup through `old_names`
- [x] Membership-specific character limit configuration from `membership.properties`
- [x] Startup `IDFactory` preload from Java DAO-equivalent used-ID tables

### Phase 6c: Enter World
- [ ] Load full player object graph (partial: common player row, inventory/equipment item rows, player skills, active skill/item cooldowns, working quests, motions, client settings, and obelisk bind point)
- [x] Java-shaped `CM_ENTER_WORLD` gate checks for missing character, online/reentry state, duplicate world presence
- [x] Mark player online, update `last_online`, transition connection to in-game, and send `SM_ENTER_WORLD_CHECK`
- [x] Load `player_skills` and send Java-shaped `SM_SKILL_LIST`
- [x] Load future `player_cooldowns` rows and send Java-shaped `SM_SKILL_COOLDOWN`
- [x] Load future `item_cooldowns` rows and send Java-shaped `SM_ITEM_COOLDOWN`
- [x] Load `player_quests` working states and send Java-shaped `SM_QUEST_LIST`
- [x] Load `player_motions` and send Java-shaped login `SM_MOTION`
- [x] Load `player_settings` client blobs and send Java-shaped `SM_UI_SETTINGS`
- [x] Send current-title `SM_TITLE_INFO` and `SM_AFTER_TIME_CHECK_4_7_5`
- [ ] Inventory/equipment load and stat application (partial: typed inventory rows loaded and `SM_INVENTORY_INFO` sent; item stones/stat application pending)
- [x] Send Java-shaped `SM_INVENTORY_INFO` with kinah-first ordering, 10-item splits, final empty packet, and current item-info blobs
- [x] Load `player_bind_point` and send obelisk `SM_BIND_POINT_INFO`
- [x] Send `SM_CHANNEL_INFO`, baseline `SM_PLAYER_SPAWN`, and `SM_GAME_TIME`
- [x] Position/world placement baseline from `players.world_id`, `x`, `y`, `z`, and `heading`

---

## Session Log

### Session 1 (May 20, 2026)
- Started Phase 6 after completing Phase 5 automated infrastructure parity.
- Added `PlayerInitialDataTable` and static-data parsing for Java `player_initial_data.xml`.
- Added equipment-slot metadata to typed item summaries using Java `ItemGroup` slot masks.
- Added `CharacterCreationService` for Java-like character creation validation and select-screen record construction.
- Added typed `skill_tree` loading for level-1 autolearn skills.
- Added `ICharacterCreationRepository` with an empty implementation and a MySQL implementation that stores `players`, `player_appearance`, starter `inventory`, and initial `player_skills` rows in one transaction.
- Added old-name reservation checks against `old_names`, matching Java's `OldNamesDAO.isNameReserved` query.
- Wired character creation into `GameServerConnection` and DI.
- Added focused tests for open-window response, successful creation record construction, name-used response, same-race validation, and forbidden class validation.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 43 tests.

### Session 2 (May 20, 2026)
- Added `IUsedIdRepository` and MySQL startup preload for the same used-ID sources Java reserves in `IDFactory`: `players`, `inventory`, `player_registered_items`, `legions`, `mail`, `guides`, `houses`, and `player_pets`.
- Added runtime ID reservation to the C# `IDFactory` and wired preload into `GameServerBootstrapService`.
- Loaded membership character-limit settings from `membership.properties` and applied Java's additional-character threshold/count rule during creation validation.
- Added `PlayerEnterWorldService`, `IPlayerEnterWorldRepository`, and `SM_ENTER_WORLD_CHECK` for the first enter-world gate.
- Added typed in-memory `Player` and `InventoryItem` models; the enter-world repository now loads the player common row and player-owned inventory/equipment rows before marking the character online.
- Wired `CM_ENTER_WORLD` handling into `GameServerConnection`, including successful transition to `InGame`.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.

### Session 3 (May 20, 2026)
- Added Java-source breadcrumb comments across the current GameServer parity surface so later debugging can compare C# behavior directly against Java files/methods.
- Covered Phase 5/6 GameServer areas already touched: config/bootstrap, static-data load/merge, ID factory, game packet frame/crypto/parsers/writers, login/chat bridge packets, character selection, character creation, and first enter-world gate.
- Clarified this resume snapshot and the ongoing comment convention for future GameServer work.

### Session 4 (May 20, 2026)
- Added `PlayerSkill` and enter-world repository loading from `player_skills`, matching Java `PlayerSkillListDAO.loadSkillList`.
- Added Java-shaped `SM_SKILL_LIST` and `SkillEntryWriter` payload mapping for loaded skills.
- Wired successful `CM_ENTER_WORLD` handling to send `SM_SKILL_LIST` immediately after `SM_ENTER_WORLD_CHECK`, matching the next implemented step in `PlayerEnterWorldService.enterWorld`.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.

### Session 5 (May 20, 2026)
- Added `cooldownId` to typed skill template summaries so Java `SkillTemplate.getCooldownId()` lookups can map DB cooldown rows back to learned skills.
- Added enter-world loading for future `player_cooldowns` rows, matching `PlayerCooldownsDAO.loadPlayerCooldowns` filtering against current time.
- Added Java-shaped `SM_SKILL_COOLDOWN` packet serialization, including learned-skill lookup by cooldown ID, login `notify=false`, remaining seconds, duration milliseconds, and Java's duration sort.
- Wired successful enter-world handling to send `SM_SKILL_COOLDOWN` after `SM_SKILL_LIST` when loaded cooldowns map to learned skills.
- Added enter-world loading for future `item_cooldowns` rows, matching `ItemCooldownsDAO.loadItemCooldowns`, and Java-shaped `SM_ITEM_COOLDOWN` packet serialization.
- Wired successful enter-world handling to send `SM_ITEM_COOLDOWN` after skill cooldowns when item cooldowns exist.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.

### Session 6 (May 20, 2026)
- Added typed player quest, motion, and client-settings models with Java parity comments for their source DAO/model behavior.
- Extended enter-world loading with `player_quests`, `player_motions`, and `player_settings` rows, plus `players.title_id`.
- Added Java-shaped packet writers for `SM_QUEST_LIST`, current-title `SM_TITLE_INFO`, login-list `SM_MOTION`, `SM_AFTER_TIME_CHECK_4_7_5`, and padded `SM_UI_SETTINGS`.
- Wired the successful enter-world sequence after cooldowns to send working quests, current title, motions, after-time check, and optional UI/shortcut/house-buddy setting blobs in Java order.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 7 (May 20, 2026)
- Added item template metadata needed by item packets: client description ID/L10n string, item mask, equipment categorization, two-hand detection, cloth/equipment flags, and polish/stigma helpers.
- Extended enter-world player common loading with `npc_expands`, `quest_expands`, and `item_expands`.
- Added Java-shaped `SM_INVENTORY_INFO` packet creation for login: kinah is always first, cube equipment precedes unequipped cube items, packets split at 10 entries, and a final empty packet is emitted.
- Added a current item-info blob writer mirroring Java `ItemInfoBlob.getFullBlob` for loaded fields: composite item, equipped slot, weapon/armor/shield/accessory/wing/plume slot blobs, enchant info, conditioning, polish, premium option, stigma shard, general info, and wrap count. Item stone/godstone/idiyan detail remains a follow-up because those tables are not loaded yet.
- Wired inventory info after UI settings in the successful enter-world sequence and threaded `IDFactory` into the client connection path so missing zero-kinah objects can be allocated like Java `Storage.increaseKinah(0)`.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 8 (May 20, 2026)
- Added typed obelisk bind point loading from `player_bind_point`, matching `PlayerBindPointDAO.loadBindPoint`.
- Added Java-shaped `SM_CHANNEL_INFO`, obelisk `SM_BIND_POINT_INFO`, baseline non-personal `SM_PLAYER_SPAWN`, and `SM_GAME_TIME` packet writers.
- Wired the successful enter-world sequence after inventory info to send channel info, obelisk bind point info (falling back to `player_initial_data` spawn location), player spawn, and game time.
- Current gaps in this cluster: kisk bind point/object state, beginner-channel metadata, personal-map sign handling, and richer world instance IDs.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

---

## Next Steps

1. Expand enter-world object loading with item stones/godstones/idiyans, recipes, title lists/completed quest list, life stats, warehouse/account data.
2. Continue the Java retail enter-world packet sequence with warehouse info, full title list, emotion list, prices, recipe cooldown/list, friend/block lists, instance/abyss/stats info, and later macro/mail/housing/broker packets.
3. Add focused live-DB opt-in coverage for creation and enter-world once local schema fixtures are ready.
