# Phase 6: Port Game Core - Detailed Plan & Progress

**Status**: IN PROGRESS (started May 20, 2026)  
**Target**: Port gameplay systems in Java dependency order while keeping database and packet behavior compatible.  
**Validation Approach**: Parity/unit/integration tests first; real-client validation remains an end-of-port readiness step.  
**Code Trace Convention**: GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior they mirror.

---

## Resume Snapshot

Last updated: May 20, 2026

- Phase 5 is complete for automated infrastructure parity. Real-client validation is intentionally deferred to later readiness validation.
- Current active work is Phase 6c enter-world. The C# path handles `CM_ENTER_WORLD`, validates missing/online/reentry/duplicate-world cases, loads the player common row plus `player_appearance`, cube inventory rows plus `item_stones` details, regular warehouse rows plus `item_stones` details, account warehouse rows plus `item_stones` details, player skills, active skill cooldowns, active item cooldowns, quests, titles, motions, emotions, recipes, macros, mailbox rows with attached mailbox item template IDs/full item state, broker settlement summary, owned house rows, active craft cooldowns, active portal cooldowns, life stats, friends, blocked users, abyss rank, client settings, and obelisk bind point, marks the character online, stores it in the world container, transitions the connection to `InGame`, sends `SM_ENTER_WORLD_CHECK`, then sends the implemented post-enter packets `SM_SKILL_LIST`, `SM_SKILL_COOLDOWN`, `SM_ITEM_COOLDOWN`, `SM_QUEST_COMPLETED_LIST`, `SM_QUEST_LIST`, current-title and bonus-title `SM_TITLE_INFO`, `SM_MOTION`, `SM_AFTER_TIME_CHECK_4_7_5`, optional `SM_UI_SETTINGS` blobs, Java-split `SM_INVENTORY_INFO`, `SM_CHANNEL_INFO`, obelisk `SM_BIND_POINT_INFO`, baseline `SM_PLAYER_SPAWN`, `SM_GAME_TIME`, Java-shaped regular/account `SM_WAREHOUSE_INFO`, empty auxiliary warehouse placeholders, full-title `SM_TITLE_INFO`, `SM_EMOTION_LIST`, baseline `SM_PRICES`, optional `SM_RECIPE_COOLDOWN`, `SM_FRIEND_LIST`, `SM_BLOCK_LIST`, `SM_INSTANCE_INFO`, `SM_ABYSS_RANK`, baseline `SM_STATS_INFO`, mailbox-state `SM_MAIL_SERVICE`, housing auction-result `SM_SYSTEM_MESSAGE` notifications plus refresh `SM_RECEIVE_BIDS`, Java-split `SM_MACRO_LIST`, `SM_RECIPE_LIST`, broker settled-icon `SM_BROKER_SERVICE`, and housing owner-state `SM_HOUSE_OWNER_INFO`. `CM_LEVEL_READY` now returns the implemented Java baseline map-ready packets: self `SM_PLAYER_INFO`, GM-aware `SM_ACCOUNT_PROPERTIES`, active-motion `SM_MOTION`, and `SM_CUBE_UPDATE` cube size. Friend/block list request surfaces now cover `CM_SHOW_FRIENDLIST`, `CM_SHOW_BLOCKLIST`, and `CM_MARK_FRIENDLIST` with Java-shaped `SM_FRIEND_LIST`, `SM_BLOCK_LIST`, and `SM_MARK_FRIENDLIST` responses. Keepalive/time-check surfaces now cover `CM_TIME_CHECK` -> `SM_AFTER_TIME_CHECK_4_7_5` + `SM_TIME_CHECK`, `CM_PING` -> `SM_PONG`, and `CM_PING_REQUEST` -> `SM_PING_RESPONSE`; Java's ping anti-cheat kick is still deferred. Public chat now parses `CM_CHAT_MESSAGE_PUBLIC` and broadcasts Java-shaped normal/shout `SM_MESSAGE` packets, including sender race filter, shout coordinates, self delivery, sight-range filtering, and loaded block-list checks. In-game mail packets now cover list/read/attachment/delete service responses: `CM_CHECK_MAIL_LIST` -> service `2`, `CM_READ_MAIL` -> service `3`, `CM_GET_MAIL_ATTACHMENT` -> service `5`, and `CM_DELETE_MAIL` -> service `6`, with read/attachment/delete final state persisted through `IMailRepository`. `CM_SEND_MAIL` now performs DB-backed recipient validation, Java `isTrading` and `AdminService.canOperate(..., "mail")` staff item restriction checks with senderless golden-yellow `SM_MESSAGE` denial text, Java `ItemFactory.newItem(itemId, count)` count clamping/default activation/expiration/tuning/amplification state for split-created attachments, and normal/kinah/tradeable-item/courier-pass item mail persistence with service `1` status responses plus Java-shaped `SM_SYSTEM_MESSAGE` failure packets for not-enough-money and early item validation, and refreshes an online recipient's mailbox state/list after sender success. `CM_READ_EXPRESS_MAIL` now spawns and dismisses a Java-shaped zephyr postman object for the requesting player via `SM_NPC_INFO`/`SM_DELETE`, with full sight-range known-list fanout still pending. Broker client opcodes `117`, `123`-`130` now parse with Java field layouts; sell-window price range, registered-items, settled-items, item-ID search (`CM_BROKER_SEARCH` mask `0`), numeric and class/recipe category-mask list/search, cancel-registered returns, settle-account collection, register-item persistence with Java `AdminService.canOperate(..., "broker")` staff item restriction checks and `ItemFactory.newItem(itemId, count)` count/default-state handling for split-created registrations, buy-item persistence with the same split-created handling, cube-full rejection, baseline `PlayerRestrictions.canTrade` online/trading/dead guards, and first-pass target selection/audit are backed by the `broker`/broker-storage `inventory` tables. Housing auction surfaces now include Java parsers for `CM_GET_HOUSE_BIDS`, `CM_REGISTER_HOUSE`, and `CM_PLACE_BID`, plus an empty Java-shaped `SM_HOUSE_BIDS` response for auction-list requests until house-bid persistence is ported. Broker paths still lack Java's full NPC `DialogAction.OPEN_VENDOR` function validation until NPC/known-list systems are ported. `ItemTemplateSummary` now parses Java item creation dependencies for activation count, expiration minutes, enchant type, tune eligibility, and conditioning level; inventory/warehouse/mail item blobs now emit zero-charge conditioning info for conditionable templates.
- Movement packet surface now covers Java movement masks/glide flags, `CM_MOVE` opcode `48`, `CM_MOVE_IN_AIR` opcode `49`, `SM_MOVE` opcode `55`, mutable player position, and PlayerMoveController-style target/vector/glide/vehicle state. A first known-list bridge now broadcasts `SM_MOVE`, baseline player enter `SM_PLAYER_INFO` plus companion `SM_MOTION` action `7`, player logout `SM_DELETE`, postman `SM_NPC_INFO`, and postman `SM_DELETE` to active players in the same world within Java's default 95m visible distance. Persistent cached KnownList membership, full player-info dependent state, anti-hack, protection/fall/glide side effects, and strict flying-state gates remain pending.
- Player close/logout now has Java `CM_QUIT`/`CM_MAY_QUIT` packet surfaces and a `PlayerLeaveWorldService` baseline: active players are removed from the world container, marked offline in memory, current position/world/heading and key common-data fields are persisted, current HP/MP/FP are saved to `player_life_stats`, active skill/item cooldown rows are refreshed, `last_online` is refreshed, and `online=false` is written after the save step. `CM_QUIT` sends Java-shaped `SM_QUIT_RESPONSE` and either returns to authed character-selection state or closes the socket after the response.
- Character creation is DB-backed and writes `players`, `player_appearance`, `player_skills`, and starter `inventory` rows. It uses Java-style starter items, equipment-slot selection, level-1 autolearn skills, old-name reservation checks, and membership character limits.
- Startup now preloads `IDFactory` from Java-equivalent used-ID tables before gameplay allocation.
- Next implementation slice should continue full known-list fanout/movement broadcast, housing auction/bid flows, broader chat packet surfaces, or equipment stat application once item templates/stat functions are in scope.
- Latest validation: `dotnet test dotnetConversion\AionServer.slnx` passed with 283 tests.

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
- [ ] Load full player object graph (partial: common player row, cube inventory/equipment item rows with mana/fusion/godstone/idian details, regular warehouse rows with item-stone details, account warehouse rows with item-stone details, player skills, active skill/item cooldowns, quests, titles, motions, emotions, recipes, macros, mailbox rows, broker settlement summary, owned house rows, active craft cooldowns, active portal cooldowns, life stats, friends, blocked users, abyss rank, client settings, and obelisk bind point)
- [x] Java-shaped `CM_ENTER_WORLD` gate checks for missing character, online/reentry state, duplicate world presence
- [x] Mark player online, update `last_online`, transition connection to in-game, and send `SM_ENTER_WORLD_CHECK`
- [x] Load `player_skills` and send Java-shaped `SM_SKILL_LIST`
- [x] Load future `player_cooldowns` rows and send Java-shaped `SM_SKILL_COOLDOWN`
- [x] Load future `item_cooldowns` rows and send Java-shaped `SM_ITEM_COOLDOWN`
- [x] Load `player_quests` states and send Java-shaped `SM_QUEST_COMPLETED_LIST` plus `SM_QUEST_LIST`
- [x] Load `player_titles` and send Java-shaped full-title `SM_TITLE_INFO`
- [x] Load `player_emotions` and send Java-shaped `SM_EMOTION_LIST`
- [x] Load `player_recipes` and send Java-shaped `SM_RECIPE_LIST`
- [x] Load `player_macrosses` and send Java-shaped `SM_MACRO_LIST`
- [x] Load `mail` rows with attached mailbox item template IDs and send Java-shaped mailbox-state `SM_MAIL_SERVICE`
- [x] Handle `CM_CHECK_MAIL_LIST` and send Java-shaped mailbox-list `SM_MAIL_SERVICE` service ID `2`
- [x] Handle `CM_READ_MAIL` and send Java-shaped read-letter `SM_MAIL_SERVICE` service ID `3`; persisted read flag through `IMailRepository`
- [x] Handle `CM_GET_MAIL_ATTACHMENT` and send Java-shaped attachment-state `SM_MAIL_SERVICE` service ID `5`; persisted final attached item/kinah state through `IMailRepository`
- [x] Handle `CM_DELETE_MAIL` and send Java-shaped delete `SM_MAIL_SERVICE` service ID `6`; persisted delete through `IMailRepository`
- [x] Handle `CM_SEND_MAIL` service ID `1` for DB-backed normal/kinah/tradeable-item/courier-pass item mail with recipient lookup, race/full/block validation, sender kinah deduction, item full-move/partial-split persistence, courier-pass consumption, mail insert, mailbox count update, sender inventory delete/update packets, and online recipient mailbox refresh
- [x] Handle `CM_READ_EXPRESS_MAIL` action parsing with duplicate/cooldown postman state, Java system-message responses, and owner-visible postman `SM_NPC_INFO`/`SM_DELETE`; full sight-range known-list fanout remains pending
- [x] Load broker settlement summary and send Java-shaped settled-icon `SM_BROKER_SERVICE`
- [x] Register and parse broker client opcodes `117`, `123`-`130`; DB-backed responses exist for sell-window price range, registered items, settled items, item-ID search, numeric/class/recipe category-mask list/search, cancel-registered item return, settle-account collection, register-item persistence, and buy-item persistence
- [x] Load owned `houses` rows and send Java-shaped owner-state `SM_HOUSE_OWNER_INFO`
- [x] Send Java-shaped `SM_RECEIVE_BIDS` auction refresh on login when new unread house-auction system mail exists
- [x] Send Java-shaped housing auction-result `SM_SYSTEM_MESSAGE` notifications before login `SM_RECEIVE_BIDS`
- [x] Load future `craft_cooldowns` rows and send Java-shaped `SM_RECIPE_COOLDOWN`
- [x] Send baseline Java-shaped `SM_PRICES`
- [x] Load `friends`/friend common rows and send Java-shaped `SM_FRIEND_LIST`
- [x] Load `blocks`/blocked-player names and send Java-shaped `SM_BLOCK_LIST`
- [x] Load future `portal_cooldowns` rows and send Java-shaped login `SM_INSTANCE_INFO`
- [x] Load `abyss_rank` and send Java-shaped `SM_ABYSS_RANK`
- [x] Load `player_life_stats` and send baseline Java-shaped `SM_STATS_INFO`
- [x] Load `player_motions` and send Java-shaped login `SM_MOTION`
- [x] Load `player_settings` client blobs and send Java-shaped `SM_UI_SETTINGS`
- [x] Send current-title `SM_TITLE_INFO` and `SM_AFTER_TIME_CHECK_4_7_5`
- [ ] Inventory/equipment load and stat application (partial: typed inventory rows and `item_stones` rows loaded; item-stone packet display is implemented, stat application pending)
- [x] Send Java-shaped `SM_INVENTORY_INFO` with kinah-first ordering, 10-item splits, final empty packet, and current item-info blobs including mana/fusion stones, godstone ID, idian polish number, and polish charge
- [x] Load regular/account warehouse rows and send Java-shaped login `SM_WAREHOUSE_INFO`
- [x] Load `player_bind_point` and send obelisk `SM_BIND_POINT_INFO`
- [x] Send `SM_CHANNEL_INFO`, baseline `SM_PLAYER_SPAWN`, and `SM_GAME_TIME`
- [x] Position/world placement baseline from `players.world_id`, `x`, `y`, `z`, and `heading`
- [x] Load `player_appearance` into the in-game `Player` model for future `SM_PLAYER_INFO`

### Phase 6d: Movement And Known List
- [x] Register and parse Java-shaped `CM_MOVE` opcode `48`
- [x] Register and parse Java-shaped `CM_MOVE_IN_AIR` opcode `49`
- [x] Add Java movement mask and glide flag constants
- [x] Track PlayerMoveController-style movement mask, target, vector, glide, geyser, vehicle, jumping, and flight-distance state
- [x] Add Java-shaped `SM_MOVE` opcode `55` for player movement payloads
- [x] Update active player position from movement packets
- [x] Broadcast `SM_MOVE` to same-world active players within Java's default 95m visible distance
- [x] Broadcast express postman spawn/delete packets through the same visible-player bridge
- [x] Broadcast player logout `SM_DELETE` through the same visible-player bridge
- [x] Add baseline Java-shaped `SM_PLAYER_INFO` opcode `32`
- [x] Broadcast player enter `SM_PLAYER_INFO` through the visible-player bridge
- [x] Broadcast player enter `SM_MOTION` action `7` active-motion payload after `SM_PLAYER_INFO`
- [ ] Port movement anti-hack, protection, fall/glide side effects, and exact flying-state gates
- [ ] Add persistent known-list object enter/leave/update fanout for players/NPCs
- [ ] Complete `SM_PLAYER_INFO` dependent state for legion, transforms, exact stats, store, ride/stance, display settings, target/team/house, and viewer-specific enemy race handling

### Phase 6k: Logout And Saves
- [x] Register and parse `CM_QUIT` opcode `3` and `CM_MAY_QUIT` opcode `4`
- [x] Send Java-shaped `SM_QUIT_RESPONSE` opcode `98`
- [x] Route `CM_QUIT` through leave-world cleanup, authed-state return, or response-before-close behavior
- [x] Remove active players from the world container on close/logout
- [x] Persist current position/world/heading, common expansion/title/DP/repose/mailbox count fields, refreshed `last_online`, and `online=false`
- [x] Persist current life stats (`player_life_stats.hp/mp/fp`) on logout
- [x] Refresh persisted skill cooldown rows with Java's 28s remaining-time threshold
- [x] Refresh persisted item cooldown rows with Java's 30s remaining-time threshold
- [ ] Persist effects, quests, inventory dirty state, and social/group/legion logout side effects
- [ ] Add periodic save task/recovery behavior

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

### Session 9 (May 20, 2026)
- Added typed player title and emotion models with Java expirable-style `secondsUntilExpiration` helpers.
- Extended enter-world loading with `player_titles`, `player_emotions`, and `players.bonus_title_id`, matching `PlayerTitleListDAO.loadTitleList`, `PlayerEmotionListDAO.loadEmotions`, and `PlayerDAO.loadPlayerCommonData`.
- Added Java-shaped `SM_QUEST_COMPLETED_LIST`, full-list and bonus-title variants of `SM_TITLE_INFO`, and `SM_EMOTION_LIST`.
- Wired successful enter-world handling to send completed quests before working quests, bonus-title info after current-title info, then full-title and emotion lists after `SM_GAME_TIME` in the retail sequence.
- Current gaps in this cluster: completed quest repeat flag still defaults to Java's non-repeat value until quest template repeat metadata is ported; membership-granted global emotions are not synthesized yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 10 (May 20, 2026)
- Added enter-world loading for `player_recipes` and future `craft_cooldowns`, matching `PlayerRecipesDAO.load` and `CraftCooldownsDAO.loadCraftCooldowns`.
- Added Java-shaped `SM_PRICES`, `SM_RECIPE_COOLDOWN`, and `SM_RECIPE_LIST` packet writers.
- Wired the implemented retail sequence after title/emotion to send baseline prices, optional recipe cooldowns, and the recipe list. `SM_RECIPE_LIST` is currently sent after skipped macro/mail/housing packets; it should move into the exact Java slot when those systems land.
- Current gaps in this cluster: `SM_PRICES` uses Java config defaults (`100/100/100`) until siege influence pricing is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 11 (May 20, 2026)
- Added typed friend and blocked-user models and enter-world loading from `friends`, `blocks`, and joined `players` rows, matching `FriendListDAO.load` and `BlockListDAO.load`.
- Added Java-shaped `SM_FRIEND_LIST` and `SM_BLOCK_LIST` packet writers.
- Wired the implemented retail sequence after recipe cooldowns to send friend and block lists before the deferred recipe-list tail packet.
- Current gaps in this cluster: friend online status uses persisted/common-row online state; exact Java `World.getPlayer(...).getFriendList().getStatus()` behavior should be tightened when richer world/player social state lands. Friend house address/door fields remain zero until housing is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 12 (May 20, 2026)
- Added typed abyss rank model and enter-world loading from `abyss_rank`, including Java's default grade-9 soldier fallback when no row exists.
- Added Java-shaped `SM_ABYSS_RANK` packet writer.
- Wired the implemented retail sequence to send abyss rank after friend/block lists. `SM_INSTANCE_INFO` is still skipped ahead of it until instance cooldown static data and portal cooldowns are ported.
- Current gaps in this cluster: daily/weekly abyss rank rollover logic and ranking-cache position calculation are not ported yet; the C# packet uses persisted `rank_pos` as the available ranking position.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 13 (May 20, 2026)
- Added typed `InstanceCooltimeTable` parsing from Java `instance_cooltimes.xml`, including the `id`, `worldId`, `race`, and `maxcount` fields consumed by `SM_INSTANCE_INFO`.
- Added typed `PlayerPortalCooldown` and enter-world loading from future `portal_cooldowns` rows, matching `PortalCooldownsDAO.loadPortalCooldowns`.
- Added Java-shaped login `SM_INSTANCE_INFO` packet writer with update type `2`, active-player instance list, remaining reuse seconds, max counts, negative entry counts, and race visibility byte.
- Wired the implemented retail sequence to send `SM_INSTANCE_INFO` after `SM_BLOCK_LIST` and before `SM_ABYSS_RANK`.
- Current gaps in this cluster: targeted/single-instance update packets and multi-player instance-info fanout are not ported yet; the current implementation covers the login all-instance path used by enter-world.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 51 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 258 tests.

### Session 14 (May 20, 2026)
- Added typed `PlayerLifeStats` and enter-world loading from `player_life_stats`, matching `PlayerLifeStatsDAO.loadPlayerLifeStat` for loaded HP/MP/FP values.
- Extended player common-row loading with `recoverexp`, `dp`, and `reposte_energy` so stats packets can include Java `PlayerCommonData` XP/DP/repose fields.
- Added baseline Java-shaped `SM_STATS_INFO` packet writer in opcode `1`, including class base stats, Java base HP/MP formulas, loaded current HP/MP/FP, XP fields, DP, inventory capacity/size, repose values, and the base-stat tail.
- Wired the implemented retail sequence to send `SM_STATS_INFO` after `SM_ABYSS_RANK`.
- Current gaps in this cluster: equipment/item/effect stat functions are not applied yet, so the packet uses Java no-equipment/no-effect baseline stats; missing `player_life_stats` rows are treated as full baseline HP/MP/FP but are not auto-inserted yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 52 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 259 tests.

### Session 15 (May 20, 2026)
- Split enter-world item loading into Java-like cube inventory, regular warehouse, and account warehouse loads, matching `InventoryDAO.loadStorage` owner semantics for `CUBE`, `REGULAR_WAREHOUSE`, and `ACCOUNT_WAREHOUSE`.
- Extended player common-row loading with `wh_npc_expands` and `wh_bonus_expands` so regular warehouse packets can carry Java's warehouse expansion level.
- Added Java-shaped `SM_WAREHOUSE_INFO` packet writer in opcode `168`, including regular warehouse split packets, final empty packets, account warehouse with kinah-inclusive item list, and empty login placeholders for absent pet/housing warehouse IDs.
- Wired the implemented retail sequence to send warehouse info after `SM_GAME_TIME` and before full-title/emotion info.
- Current gaps in this cluster: legion warehouse open/use flows are not ported; pet/housing storage contents are not loaded yet, so login currently sends the Java-style empty placeholders for those storage IDs.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 52 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 259 tests.

### Session 16 (May 20, 2026)
- Added DB-backed item-stone models to `InventoryItem` for Java `ManaStone`, `GodStone`, fusion `ManaStone`, and `IdianStone` state restored by `ItemStoneListDAO.load`.
- Extended cube, regular warehouse, and account warehouse item loading to hydrate `item_stones` rows for categories `MANASTONE`, `GODSTONE`, `FUSIONSTONE`, and `IDIANSTONE` after each Java-like storage load.
- Updated item blob serialization to mirror Java `CompositeItemBlobEntry`, `EnchantInfoBlobEntry`, and `PolishInfoBlobEntry` for fusion stones, mana stones, godstone ID, idian stone ID/polish number, and idian polish charge.
- Added focused packet coverage that parses a serialized `SM_INVENTORY_INFO` item blob and verifies the stone/godstone/idian fields at their Java-shaped offsets.
- Current gaps in this cluster: Java's invalid-stone cleanup checks against item templates/socket counts are not ported yet; equipment/item stat application from stones remains pending for the later stats/equipment slice.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 53 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 260 tests.

### Session 17 (May 20, 2026)
- Added typed `PlayerMacro` state matching Java `Macros.Macro`.
- Extended enter-world loading with `player_macrosses`, matching `PlayerMacrosDAO.loadMacros`.
- Added Java-shaped `SM_MACRO_LIST` in opcode `231`, including the clear-list flag, negative macro count, UTF-16 macro XML body, empty-list clear packet, and Java-style dynamic packet splitting.
- Wired the implemented retail sequence to send macro list packets after `SM_STATS_INFO` and before `SM_RECIPE_LIST`, matching `PlayerEnterWorldService.sendMacroList`'s relative slot after the still-deferred mail/housing/passport services.
- Current gaps in this cluster: macro create/update/delete client packets and persistence are not ported yet; this covers login restore/display parity only.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 53 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 260 tests.

### Session 18 (May 20, 2026)
- Added typed `PlayerMail` state matching the mailbox `Letter` rows restored by `MailDAO.loadPlayerMailbox`.
- Extended enter-world loading with `mail` rows for the active player, including unread state, attached item object ID, attached kinah, letter type, and received time.
- Added Java-shaped mailbox-state `SM_MAIL_SERVICE` in opcode `161` for service ID `0`, writing total, unread, unread express, and unread Black Cloud counts.
- Wired the implemented retail sequence to send mailbox state after `SM_STATS_INFO` and before macro/recipe restore, matching the login effect of `MailService.onPlayerLogin` while heavier mail services remain deferred.
- Current gaps in this cluster: `CM_CHECK_MAIL_LIST`, mail list packet service ID `2`, read mail service ID `3`, attachment retrieval service ID `5`, delete service ID `6`, and send-mail persistence are not ported yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 53 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 260 tests.

### Session 19 (May 20, 2026)
- Added typed `PlayerBrokerSettlementSummary` state matching the login subset of `BrokerService.getSettledItemsForPlayer` and `extractEarnedKinahForSoldItems`.
- Extended enter-world loading with a broker settlement summary query over `broker` rows for the player's Java broker race, counting settled rows and summing earned kinah for sold settled rows.
- Added Java-shaped settled-icon `SM_BROKER_SERVICE` in opcode `146`, matching `SM_BROKER_SERVICE(boolean showSettledIcon, long settledKinah)` and `writeShowSettledIcon`.
- Wired the implemented retail sequence to send the broker settled icon after `SM_RECIPE_LIST` when the player has any settled broker rows, matching `BrokerService.onPlayerLogin`.
- Current gaps in this cluster: full broker startup cache, search/list/register/cancel/buy/settle interaction packets, settled item page details, and broker item persistence tasks are not ported yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 53 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 260 tests.

### Session 20 (May 20, 2026)
- Added typed `PlayerHouse` state for the owner-info subset of Java `House`: address ID, building ID, acquire time, next payment time, and active/inactive flag.
- Extended enter-world loading with owned `houses` rows. Studio addresses `2001`/`3001` take precedence like `HousingService.findPlayerHouses`; otherwise custom houses are ordered by acquire time and the oldest is treated as active while later houses are marked inactive, matching Java startup inactive-state derivation.
- Added Java-shaped `SM_HOUSE_OWNER_INFO` in opcode `263`, including active house address/building, owner-state flags, town level placeholder, Java-like weeks-until-next-pay calculation, inactive house address/building, and inactive grace seconds.
- Wired the implemented retail sequence to send housing owner profile info after broker login notification, matching `HousingService.onPlayerLogin`'s final packet.
- Current gaps in this cluster: exact town level, exact auction-end-based inactive-house grace scheduling, housing payment overdue/sequestrate system messages, house static-data ownership integration, house registry/scripts/objects, and housing interaction packets are not ported yet. Login `SM_RECEIVE_BIDS` auction refresh was added later in Session 24.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 53 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 260 tests.

### Session 21 (May 20, 2026)
- Extended `PlayerMail` with the attached item template ID needed by Java `SM_MAIL_SERVICE.writeLettersList`.
- Updated mailbox loading to left-join attached mailbox inventory rows (`StorageType.MAILBOX`, ID `127`) so list packets can write both attached item object ID and template ID like Java `MailDAO.loadPlayerMailbox` plus `InventoryDAO.loadItems`.
- Added Java-shaped `SM_MAIL_SERVICE` service ID `2` mailbox-list packets, including newest-first ordering, express-only unread express/Black Cloud filtering, negative final packet counts, UTF-16 sender/title sizing, and dynamic splitting against the Java static body size.
- Added `CM_CHECK_MAIL_LIST` opcode `133` and wired in-game handling to answer from the active player, matching `CM_CHECK_MAIL_LIST.runImpl -> MailService.sendMailList(player, expressOnly, false)`.
- Current gaps in this cluster: mail read service ID `3`, attachment retrieval service ID `5`, delete service ID `6`, send-mail persistence, and mailbox refresh packets remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 261 tests.

### Session 22 (May 20, 2026)
- Extended loaded `PlayerMail` state to carry the attached mailbox `InventoryItem` when present, including `item_stones` hydration, so read-mail packets can reuse the Java-shaped item-info blob writer.
- Added Java-shaped `SM_MAIL_SERVICE` service ID `3` read-letter packets with Java mailbox count packing, sender/title/message strings, no-attachment zeros, attached item object/template/name/blob fields, attached kinah, timestamp seconds, and letter type.
- Added `CM_READ_MAIL` opcode `134` and wired in-game handling to send the read-letter packet before marking the in-memory letter unread flag false, matching `MailService.readMail`.
- Current gaps in this cluster: read state persistence is only in memory until logout/save persistence is ported; attachment retrieval service ID `5`, delete service ID `6`, send-mail persistence, and mailbox refresh packets remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 261 tests.

### Session 23 (May 20, 2026)
- Added Java-shaped `SM_MAIL_SERVICE` service ID `1`, `5`, and `6` writers for send-mail status, attachment retrieval state, and delete-mail state.
- Added `CM_SEND_MAIL`, `CM_GET_MAIL_ATTACHMENT`, and `CM_DELETE_MAIL` opcode parsing for Java opcodes `132`, `136`, and `137`.
- Wired attachment retrieval to the active player's in-memory mailbox: item attachments move from mailbox storage into the cube list and clear the letter attachment fields; kinah attachments increase or create the cube kinah item and clear attached kinah; both paths send service ID `5` after the state change like Java `MailService.getAttachments`.
- Wired delete-mail handling to remove requested letters from the active player's in-memory mailbox and send service ID `6` with post-removal counts, matching Java `MailService.deleteMail` packet shape.
- Wired `CM_SEND_MAIL` to the Java-shaped service ID `1` status response shell. It keeps Java's early no-response cases for unsupported/forbidden letter types and overlong recipients, but currently returns `NO_SUCH_CHARACTER_NAME` for normal attempts until recipient lookup, cross-player mailbox update, commission/inventory handling, and DB persistence are ported.
- Then-current gaps in this cluster were durable read/attachment/delete persistence, complete send-mail behavior, mailbox reserve upload/refresh packets, express-postman `CM_READ_EXPRESS_MAIL`, and related system-message failure paths. Sessions 25-28 cover the persistence, normal/kinah/tradeable-item send-mail, express-mail parser/state shell, and first mail system-message paths; mailbox refresh and courier-pass item mail remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 261 tests.

### Session 24 (May 20, 2026)
- Added Java-shaped `SM_RECEIVE_BIDS` in opcode `259`, matching `network/aion/serverpackets/SM_RECEIVE_BIDS`.
- Added login auction-result detection matching the refresh portion of `HousingBidService.onPlayerLogin`: unread `$$HS_AUCTION_MAIL` rows received since `LastOnline` with result IDs `FAILED_BID`, `FAILED_SALE`, `SUCCESS_SALE`, `WIN_BID`, `GRACE_START`, or `GRACE_SUCCESS` now trigger `SM_RECEIVE_BIDS(0)`.
- Wired the refresh packet immediately after mailbox-state `SM_MAIL_SERVICE`, preserving the Java ordering where `HousingBidService.onPlayerLogin` runs after `MailService.onPlayerLogin`.
- Current gaps in this cluster: full auction list/register/bid/cancel flows remain pending. Session 30 adds the login auction-result system-message notifications.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 261 tests.

### Session 25 (May 20, 2026)
- Added `IMailRepository`/`MySqlMailRepository` for final-state persistence of the mail actions already ported from Java.
- Wired `CM_READ_MAIL` handling to persist `unread=false` after sending the read-letter packet, matching the final state of `Letter.setReadLetter` plus `MailDAO.storeLetter`.
- Wired `CM_GET_MAIL_ATTACHMENT` item retrieval to persist the attached item moving from `StorageType.MAILBOX` (`127`) to cube storage and clear `mail.attached_item_id`; kinah retrieval now persists `attached_kinah_count=0`.
- Wired `CM_DELETE_MAIL` handling to delete selected `mail` rows through the repository after the active mailbox removes them, matching `MailDAO.deleteLetter` final state.
- Registered the repository in GameServer DI and threaded it into `GameServerConnection`.
- Current gaps in this cluster: complete send-mail persistence/recipient lookup/commission handling is still pending; exact item slot allocation on attachment retrieval uses the current C# first-available placeholder until full inventory add semantics are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 261 tests.

### Session 26 (May 20, 2026)
- Extended `IMailRepository` with recipient common-data lookup, recipient block-list check, and transactional sent-mail storage.
- Upgraded `CM_SEND_MAIL` handling from a status shell to DB-backed normal/kinah mail: Java early no-response cases are preserved for overlong recipients, Black Cloud sends, negative kinah, unsupported letter types, and item-attached sends.
- Added Java-like recipient validation for missing character, race mismatch, full mailbox, and recipient block-list membership, returning `SM_MAIL_SERVICE` service ID `1` statuses.
- Added normal/express mail fee calculation for non-item mail, sender cube kinah deduction, `mail` row insert, and recipient `players.mailbox_letters` increment in one transaction.
- Current gaps in this cluster: item-attached send mail remains pending until full inventory split/tradeability/disposition handling is ported; online-recipient mailbox push/refresh is not implemented yet. Session 27 added the generic `SM_SYSTEM_MESSAGE` packet and wired the not-enough-money mail path.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 261 tests.

### Session 27 (May 20, 2026)
- Added generic Java-shaped `SM_SYSTEM_MESSAGE` opcode `25` support for golden-yellow client message IDs and wired the mail slice to use it for `STR_NOT_ENOUGH_MONEY`, `STR_MAIL_SEND_USED_ITEM`, `STR_MAIL_SEND_CAN_NOT_SEND_EQUIPPED_ITEM`, `STR_POSTMAN_ALREADY_SUMMONED`, and `STR_POSTMAN_UNABLE_IN_COOLTIME`.
- Registered and parsed `CM_READ_EXPRESS_MAIL` opcode `162`, matching Java's action byte.
- Added express/Black Cloud postman state tracking on the loaded `Player` and handled close/icon-click actions with Java-like duplicate summon and express cooldown checks. Actual postman visible-object spawning, flight-state rejection, and known-list fanout remain pending until the spawn/movement engine slice.
- Tightened `CM_SEND_MAIL` failure behavior so insufficient sender kinah now returns Java's `SM_SYSTEM_MESSAGE.STR_NOT_ENOUGH_MONEY`; item-attached attempts now perform the Java invalid/missing/equipped-item checks before stopping at the still-pending mutation path.
- Then-current gaps in this cluster: item-attached send mail still needed item move/split, item-stone/DB handling, sender item delete/update packets, and DB transaction support. Session 28 covers the tradeable item move/split path; courier-pass disposition and express/Black Cloud courier spawning remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.

### Session 28 (May 20, 2026)
- Added Java-shaped `SM_DELETE_ITEM` opcode `28` and `SM_INVENTORY_UPDATE_ITEM` opcode `29` for the mail send paths that remove or reduce sender inventory items.
- Extended item template summaries with Java `ItemMask.TRADEABLE` parity so mail item eligibility can distinguish loaded tradeable items from normally untradeable/soul-bound items.
- Extended `IMailRepository` with a transactional item-attached send-mail path: full-count sends move the existing inventory row to mailbox storage `127`; partial-stack sends insert a new mailbox item row and reduce the sender's original stack; both paths update sender kinah, insert the `mail` row, and increment recipient `mailbox_letters`.
- Upgraded `CM_SEND_MAIL` item handling from validation-only to persisted tradeable item attachments. It now applies Java-like item quality commission, moves or splits the sender item, updates in-memory inventory/mailbox state, sends sender kinah/item update packets, and returns service ID `1` success.
- Then-current gaps in this cluster: courier-pass `Disposition` support for normally untradeable cash items was not parsed yet; `AdminService.canOperate` restrictions and online-recipient mailbox state/list refresh were still pending. Session 29 covers the courier-pass path.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.

### Session 29 (May 20, 2026)
- Extended item template static-data loading to preserve nested `<disposition id count>` data, matching Java `ItemTemplate.getDisposition`.
- Added a real Java static-data assertion for item `100000216`, which carries courier pass `188950002 x6`, so the parser is covered by fixture data.
- Upgraded `CM_SEND_MAIL` item handling for normally untradeable/soul-bound items with courier-pass disposition data: the send path now requires enough pass items, consumes them with Java-like `decreaseByItemId` semantics across cube stacks, persists pass stack updates/deletes in the same mail transaction, and sends the corresponding inventory update/delete packets before the mailed item/kinah updates.
- Current gaps in this cluster: `AdminService.canOperate` restrictions are still absent; exact `ItemFactory.newItem` defaults for partial stack mail are approximate. Session 31 covers online-recipient mailbox state/list refresh.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.

### Session 30 (May 20, 2026)
- Added Java-shaped housing auction-result `SM_SYSTEM_MESSAGE` helpers for `STR_MSG_HOUSING_BID_CANCEL`, `STR_MSG_HOUSING_BID_WIN`, `STR_MSG_HOUSING_AUCTION_SUCCESS`, and `STR_MSG_HOUSING_AUCTION_FAIL`.
- Refactored login house-auction mail detection so `SM_RECEIVE_BIDS` refresh and the new system-message notifications share the same unread `$$HS_AUCTION_MAIL` parsing, matching `HousingBidService.onPlayerLogin`.
- Wired enter-world to send those housing auction-result system messages immediately after mailbox-state `SM_MAIL_SERVICE` and before `SM_RECEIVE_BIDS`, preserving Java ordering.
- Current gaps in this cluster: full auction list/register/bid/cancel flows, housing payment overdue/sequestrate messages, and house registry/scripts/objects remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.

### Session 31 (May 20, 2026)
- Added a game-client connection registry so online players can be found by object ID, matching the `World.getPlayer` lookup used by `SystemMailService.updateRecipientMailbox`.
- Registered/unregistered active player connections on enter-world and connection close, and tracked regular/express mailbox state from `CM_CHECK_MAIL_LIST` using Java `PlayerMailboxState` values.
- Upgraded successful `CM_SEND_MAIL` to send the sender's success packet first, then append the new letter to an online recipient's mailbox and push Java-shaped mailbox-state/list refresh packets. Express mail now also sends `SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY` to the online recipient.
- Current gaps in this cluster: visible postman object spawning/known-list fanout remains pending; exact `AdminService.canOperate` and exact partial-stack `ItemFactory.newItem` defaults remain approximate.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 54 tests.

### Session 32 (May 20, 2026)
- Added Java-shaped broker client packet parsers and opcode registrations for `CM_BROKER_SELL_WINDOW` (`117`), `CM_BROKER_LIST` (`123`), `CM_BROKER_SEARCH` (`124`), `CM_BROKER_REGISTERED` (`125`), `CM_BUY_BROKER_ITEM` (`126`), `CM_REGISTER_BROKER_ITEM` (`127`), `CM_BROKER_CANCEL_REGISTERED` (`128`), `CM_BROKER_SETTLE_LIST` (`129`), and `CM_BROKER_SETTLE_ACCOUNT` (`130`).
- Expanded `SM_BROKER_SERVICE` beyond the login settled icon with Java-shaped empty searched-items, registered-items, settled-items, remove-settled-icon, and sell-window payload writers.
- Wired the read-only broker requests to safe empty response shells so the packet surfaces are present while the repository-backed broker cache, item filtering, register, buy, cancel, and settle mutations remain deferred.
- Current gaps in this cluster: full broker startup cache, search filtering/sorting, registered/settled item detail pages, sell price history, register/buy/cancel/settle persistence, and broker item inventory mutations are not ported yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 55 tests.

### Session 33 (May 20, 2026)
- Added `IBrokerRepository`/`MySqlBrokerRepository` and `PlayerBrokerItem` models for DB-backed broker read pages, matching the `BrokerDAO.loadBroker` row shape and broker-storage item hydration.
- Added broker item-stone hydration for broker inventory rows so registered/search/settled item packets can reuse the Java-shaped enchant/polish/wrap item info fields.
- Upgraded `SM_BROKER_SERVICE` searched, registered, and settled item writers to emit non-empty item rows matching Java `writeItemInfo`, `writeRegisteredItemInfo`, and `writeShowSettledItems`.
- Wired `CM_BROKER_SELL_WINDOW` to DB-backed low/high active price ranges, `CM_BROKER_REGISTERED` to the player's active registered items, `CM_BROKER_SETTLE_LIST` to settled items plus earned kinah, and `CM_BROKER_SEARCH` to item-ID search when the client mask is `0`.
- Current gaps in this cluster: category-mask list/search needs `BrokerItemMask`/item-template category matching; register/buy/cancel/settle account mutations and broker inventory movement remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 55 tests.

### Session 34 (May 20, 2026)
- Added `BrokerItemMaskMatcher` with the Java numeric `BrokerItemMask` families backed by `templateId / 100000` and `templateId / 10000`, covering weapon, armor, accessory, broad skill-related, home decor, furniture, craft material, consumable, and other categories.
- Wired `CM_BROKER_LIST` and nonzero-mask `CM_BROKER_SEARCH` to load active race broker items from the DB, filter by the numeric broker mask, sort by Java broker sort types, and send Java-shaped searched item pages.
- Added focused tests for the broker numeric mask matcher and kept item-ID search/list packet tests intact.
- Current gaps in this cluster at the end of Session 34: class-specific stigma/manual filters and recipe-specialization filters still need richer item action/class metadata; register/buy/cancel/settle account mutations and broker inventory movement remained pending. Session 35 covers cancel-registered item returns.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 56 tests.

### Session 35 (May 20, 2026)
- Added Java-shaped `SM_INVENTORY_ADD_ITEM` opcode `27` for broker returns, including `ItemPacketService.ItemAddType.BROKER_RETURN` (`0x2F`) and the same full item-info blob tail used by Java `SM_INVENTORY_ADD_ITEM.writeItemInfo`.
- Added Java-shaped `SM_CUBE_UPDATE` opcode `130` cube-size packets so broker returns follow Java `ItemPacketService.sendStorageUpdatePacket` by updating cube item count after the returned item add.
- Added `SM_BROKER_SERVICE` cancel-registered packet type `4` (`writeC(4), writeC(unk), writeD(itemId)`) and packet coverage for the broker cancel acknowledgement.
- Extended `IBrokerRepository` with DB-backed registered-item lookup and transactional cancel persistence: the broker-storage `inventory` row moves back to cube storage (`item_location=0`, first-available slot) and the matching `broker` row is deleted using Java `BrokerDAO.deleteBrokerItem` keys.
- Wired `CM_BROKER_CANCEL_REGISTERED` to load the seller-owned active broker item, return it to the in-memory cube, send `SM_INVENTORY_ADD_ITEM` broker return, send the broker cancel acknowledgement, then refresh registered items like `BrokerService.cancelRegisteredItem`.
- Current gaps in this cluster: Java's broker-NPC targeting audit and inventory-full rejection are not enforced yet because targeting and full cube-capacity services are still out of scope; broker register, buy, and settle account mutations remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 56 tests.

### Session 36 (May 20, 2026)
- Added a settlement model for `BrokerService.settleAccount` covering sold broker rows, unsold returned item rows, and the resulting kinah item update.
- Extended `IBrokerRepository` with full settled-account loading and transactional settlement persistence: unsold broker-storage items move back to cube storage, sold/returned broker rows are deleted with Java `BrokerDAO.deleteBrokerItem` keys, and collected kinah is upserted in `inventory`.
- Wired `CM_BROKER_SETTLE_ACCOUNT` to collect sold-item kinah with Java `INC_KINAH_COLLECT`, return unsold settled items with Java default `ITEM_COLLECT` add packets plus `SM_CUBE_UPDATE`, refresh settled page `0`, and send the remove-settled icon when no settled rows remain.
- Current gaps in this cluster: Java's broker-NPC targeting audit and inventory-full rejection are still absent, so unsold settled returns are not capacity-limited yet; broker register and buy mutations remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 56 tests.

### Session 37 (May 20, 2026)
- Added Java-shaped `SM_BROKER_SERVICE` register-item type `3` success/error packet writers, including the success item-position byte and the Java error-body padding used by broker messages.
- Added DB-backed `CM_REGISTER_BROKER_ITEM` handling for full-stack and partial-stack registrations: validates price/count/tradeability/max registered item count, applies Java registration commission tiers, updates sender kinah, moves full items to broker storage or creates a new partial broker-storage item, and inserts the `broker` row transactionally.
- Wired sender inventory packets for the registration mutation: full-stack registration sends `SM_DELETE_ITEM`, partial-stack registration sends `SM_INVENTORY_UPDATE_ITEM` with `DEC_ITEM_USE`, kinah commission sends `DEC_KINAH_BUY`, and successful registration sends the Java broker service registration packet.
- Current gaps in this cluster: dynamic `PricesService` influence/tax modifiers are still baseline `100/100/100`, `AdminService.canOperate`, trading-state checks, and broker-NPC targeting audit are still absent; broker buy mutation remains pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 56 tests.

### Session 38 (May 20, 2026)
- Added Java-like broker list/search cache fields to `Player` so `CM_BUY_BROKER_ITEM` can refresh the buyer's last broker page after mutation, matching `BrokerPlayerCache`.
- Extended broker persistence with active-item lookup and buy transactions: full purchases move the broker-storage item into the buyer cube and mark the broker row sold/settled; partial purchases reduce the active broker stack, create a buyer cube item, and insert a sold/settled broker row for seller settlement.
- Wired `CM_BUY_BROKER_ITEM` to validate self-purchase and kinah, send Java `STR_VENDOR_CAN_NOT_BUY_MY_REGISTER_ITEM` for self-purchase, deduct buyer kinah, add the bought item with `BROKER_BUY`, send `SM_CUBE_UPDATE`, notify an online seller with the settled-icon packet, and refresh the buyer's cached search/list page.
- Current gaps in this cluster: inventory-full rejection, NPC-targeting audit, and broader restriction checks are still absent until those support systems are ported; partial-purchase item defaults are approximate compared with Java `ItemFactory.newItem`.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 56 tests.

### Session 39 (May 20, 2026)
- Extended item template static-data summaries with Java `ItemTemplate.levelRestrictions` (`restrict`) and `ItemActions.getCraftLearnAction` (`<craftlearn recipeid>`) metadata so broker category filters can see the same data Java sees.
- Completed the missing Java `BrokerItemMask` specializations: class-specific stigma/manual masks now use `BrokerPlayerClassExtraFilter` parity, including advanced-class fallback to the starting class, and craft design masks now use `BrokerRecipeFilter` parity through `RecipeTemplate.skillId`.
- Wired broker list/search mask filtering to pass the loaded `RecipeTemplateTable`, preserving the existing numeric mask behavior while enabling recipe specialization masks `6040` through `6046`.
- Added focused broker matcher coverage plus real Java static-data assertions for RANGER skillbook restrictions and `152200001 -> 155000001` craft-learn recipe parsing.
- Current gaps in this cluster: inventory-full rejection, NPC-targeting audit, broader restriction checks, and exact `ItemFactory.newItem` defaults for split-created items are still deferred until those support systems are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 57 tests.

### Session 40 (May 20, 2026)
- Added typed postman NPC objects for `VisibleObjectSpawner.spawnPostman`, including Java postman NPC IDs `798100`/`798101`, owner forward-offset positioning, creator/master fields, and world registration for the temporary object.
- Added Java-shaped `SM_NPC_INFO` opcode `14` and `SM_DELETE` opcode `22` packet writers for the postman spawn/dismiss path.
- Extended NPC template static-data summaries with the nested/template fields consumed by `SM_NPC_INFO`: title ID, height, attack speed, max HP, run speed, and bound radius.
- Upgraded `CM_READ_EXPRESS_MAIL` action `1` to send the owner-visible postman NPC packet for unread Black Cloud or express mail, action `0` to send the delete packet, and connection close to remove the transient postman object without a client packet.
- Current gaps in this cluster: no full sight-range known-list fanout yet, no `GeoService.getClosestCollision` projection yet, and no flight-state rejection until movement/flight state is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 57 tests.

### Session 41 (May 20, 2026)
- Added an `InventoryCapacity` helper for Java `StorageType.CUBE`/`ItemStorage.isFull` parity using the 27-slot base plus 9 slots per NPC/quest/item expansion, while ignoring kinah and equipped rows in the C# flattened inventory model.
- Added Java-shaped full-inventory system-message helpers for `STR_MSG_FULL_INVENTORY`, `STR_EXCHANGE_FULL_INVENTORY`, and `STR_MAIL_TAKE_ALL_CANCEL`.
- Wired cube-capacity checks into broker buy, cancel-registered returns, settled unsold-item returns, and mail item attachment pickup so these paths stop before mutating DB/in-memory state when the cube is full.
- Added focused coverage for cube limit/free-slot calculation and the newly used system-message IDs.
- Current gaps in this cluster: stack merging/special cube categories are still approximate until richer item add semantics and `ItemTemplate.getExtraInventoryId` are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 58 tests.

### Session 42 (May 20, 2026)
- Added Java movement mask and glide flag constants from `controllers/movement/MovementMask` and `GlideFlag`.
- Added PlayerMoveController-style movement state on `Player`, including current mask, target destination, relative vector, glide/geyser fields, vehicle fields, jump flag, and flight-path distance.
- Registered and parsed `CM_MOVE` opcode `48` and `CM_MOVE_IN_AIR` opcode `49`, matching Java field layouts for absolute movement, relative vectors, glide geyser location IDs, and vehicle tails.
- Added Java-shaped `SM_MOVE` opcode `55`, including manual-position relative-vector vs absolute-target tails, glide/geyser bytes, and Java's vehicle tail shape.
- Wired active-player movement handling to update in-memory position and movement state from movement packets. Full sight-range broadcast, anti-hack, protection/fall/glide side effects, and strict flying-state gates remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 61 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 268 tests.

### Session 43 (May 20, 2026)
- Added a first visible-player broadcast bridge on the game-client connection registry, matching Java `PacketSendUtility.broadcastToSightedPlayers` at the currently available world/position level.
- Added `WorldVisibility` with Java `VisibleObject.getVisibleDistance` default range (`95m`) and same-world squared-distance checks, covered by focused tests.
- Routed `CM_MOVE` manual-position and immediate movement updates through the visible-player bridge with `SM_MOVE`, excluding the moving player like Java's default `toSelf=false` branch.
- Routed express postman spawn/delete packets through the visible-player bridge so nearby active players can receive `SM_NPC_INFO` and `SM_DELETE` instead of only the owner-local fallback.
- Current gaps in this cluster: this is not yet Java's persistent two-way `KnownList`; object enter/leave spawn packets, visibility-state caching, region changes, and NPC/player discovery remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 62 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 269 tests.

### Session 44 (May 20, 2026)
- Added a Java `PlayerLeaveWorldService.leaveWorld` baseline to `PlayerEnterWorldService`: close/logout now marks the player offline in memory, removes them from the world container, refreshes `last_online`, and calls the repository save path.
- Extended `IPlayerEnterWorldRepository` with `SavePlayerLogoutAsync` and implemented the MySQL path to persist current position/world/heading, exp/recoverable exp, expansion levels, title IDs, DP, mailbox letter count, repose energy, `last_online`, and `online=false`.
- Wired `GameServerConnection.CloseAsync` to dismiss transient postman state, unregister the active connection, save logout state, remove the world object, and clear the active player reference.
- Current gaps in this cluster: Java's full logout chain still needs effect/cooldown/life-stat persistence, quest logout hooks, inventory dirty saves, summon/pet/duel/group/legion side effects, and periodic save/recovery behavior.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 63 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 270 tests.

### Session 45 (May 20, 2026)
- Extended the logout repository save path with Java `PlayerLifeStatsDAO.updatePlayerLifeStat` parity for current HP/MP/FP.
- Added an insert fallback when a `player_life_stats` row is missing, matching Java's load-time `insertPlayerLifeStat` recovery behavior.
- Kept the close/logout service flow unchanged: life stats are saved as part of the same logout persistence call before `online=false`.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 63 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 270 tests.

### Session 46 (May 20, 2026)
- Extended logout persistence with Java `PlayerCooldownsDAO.storePlayerCooldowns` parity: existing `player_cooldowns` rows are deleted, then active skill cooldowns with more than 28 seconds remaining are reinserted.
- Extended logout persistence with Java `ItemCooldownsDAO.storeItemCooldowns` parity: existing `item_cooldowns` rows are deleted, then active item cooldowns with more than 30 seconds remaining are reinserted with use-delay values.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 63 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 270 tests.

### Session 47 (May 20, 2026)
- Registered and parsed Java `CM_QUIT` opcode `3` for `Authed`/`InGame`, including the stay-connected byte.
- Registered Java `CM_MAY_QUIT` opcode `4` as the no-op in-game ready-to-quit packet.
- Added Java-shaped `SM_QUIT_RESPONSE` opcode `98`, including normal vs edit-mode response code, unknown byte, and `-1` tail.
- Routed `CM_QUIT` through the leave-world cleanup path. Stay-connected quits now clear the active player and return the connection to `Authed`; full quits send `SM_QUIT_RESPONSE` before closing without double-saving.
- Current gaps in this cluster: plastic-surgery edit-mode validation and character-list account-data refresh are still pending because targeting/NPC dialog and account selection refresh are not ported yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 65 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 272 tests.

### Session 48 (May 20, 2026)
- Added player leave/delete fanout through the visible-player broadcast bridge: close/logout now sends `SM_DELETE` for the departing player to same-world active players within the Java default visible range before unregistering/removing the player.
- This mirrors the Java `PlayerController.notSee` delete-packet branch at the current non-cached visibility level.
- Current gaps in this cluster: player enter/spawn fanout still needs a real `SM_PLAYER_INFO` implementation and richer loaded player appearance/equipment/legion/transform state.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 65 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 272 tests.

### Session 49 (May 20, 2026)
- Added `CharacterAppearance` to the in-game `Player` model.
- Extended `LoadPlayerAsync` to left-join `player_appearance` and hydrate the same field set used by Java `PlayerAppearanceDAO.loadPlayerAppearance`.
- This prepares the missing data dependency for a later real `SM_PLAYER_INFO` known-list enter packet; current player-enter fanout is still blocked on the packet writer and visible equipment/legion/transform state.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 65 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 272 tests.

### Session 50 (May 20, 2026)
- Added baseline Java-shaped `SM_PLAYER_INFO` opcode `32` for known-list player enter visibility.
- The first pass writes Java field order using loaded position/common data, race/class/gender/template IDs, player name/title/DP, loaded appearance, equipped cube items for the visible equipment mask, movement vector/current position, level from the experience table when available, abyss rank, and safe defaults for systems not ported yet.
- Wired successful enter-world to broadcast `SM_PLAYER_INFO` to nearby same-world active players through the visible-player bridge after the entering player's own `SM_PLAYER_SPAWN`.
- Current gaps in this cluster: the packet still uses defaults for legion, transforms, exact calculated stats/speeds, private store, flight transport, player settings display/deny, target/team/house, CP info, ride/stance follow-up packets, and viewer-specific enemy race/icon handling.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 66 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 273 tests.

### Session 51 (May 20, 2026)
- Extended `SM_MOTION` with Java action `7` (`SM_MOTION(int playerId, activeMotions)`) for player-known-list visibility.
- Wired successful enter-world fanout to send `SM_MOTION` action `7` immediately after baseline `SM_PLAYER_INFO`, matching Java `PlayerController.sendPlayerInfoPackets`.
- Current approximation: the DB model only tracks active motion IDs, not Java's active-motion type slots, so the packet writes up to five active IDs in stable order until motion-template/type metadata is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 66 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 273 tests.

### Session 52 (May 20, 2026)
- Added Java-shaped `CM_TARGET_SELECT` opcode `31` for in-game target selection and a `Player.TargetObjectId` field mirroring Java `VisibleObject.target` at the currently available object-id level.
- Routed target-select packets through `GameServerConnection` so normal selection/clear requests update active-player target state; target-of-target assist remains deferred until full target references and KnownList visibility checks are ported.
- Added a first broker target audit layer for Java `Player.isTargetingNpcWithFunction(brokerObjId, DialogAction.OPEN_VENDOR)`: broker list/search/registered/settled/cancel/register/buy/collect paths now require the selected object id to match the broker object id before processing.
- Current gaps in this cluster: the C# audit still lacks Java's NPC template function check, KnownList visibility/team fallback, radar-audit behavior, assist-key system messages, trading-state guards, and `AdminService.canOperate` item restrictions.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 67 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 274 tests.

### Session 53 (May 20, 2026)
- Added a Java `AdminService` options surface to `GameServerOptions`: `gameserver.administration.unrestricted_itemtrade` is parsed from `admin.properties`, and operational item ids are loaded from `config/administration/item.restriction.txt`.
- Carried the login-server account access level into the active in-game `Player`, matching the Java `PlayerAccount.accessLevel` dependency used by `AdminService.hasAccess`.
- Added `AdminService.canOperate(player, null, item, type)` parity for staff item operations: normal players pass, unrestricted staff pass, restricted staff can use only allow-listed item ids, and denied operations are logged.
- Wired the staff item restriction check into Java's two current C# call sites: `MailService.sendMail` item attachments (`type="mail"`) and `BrokerService.registerItem` (`type="broker"`).
- Then-current approximation: Java sends a plain text message to restricted staff on denial; this pass logged the denial until Session 55 added the generic server-message packet surface.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 67 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 274 tests.

### Session 54 (May 20, 2026)
- Added `Player.IsTrading` as the C# surface for Java `Player.isTrading` guards used by mail and broker flows.
- Added a baseline `PlayerRestrictions.canTrade` helper for currently available state: online players pass unless they are trading or have loaded dead life stats. Missing C# life-stat rows are allowed because Java recovers missing life stats during load.
- Wired Java trade guards into `CM_BROKER_SELL_WINDOW`, `CM_REGISTER_BROKER_ITEM`, `BrokerService.registerItem`, `BrokerService.buyBrokerItem`, `BrokerService.cancelRegisteredItem`, and `MailService.sendMail`.
- Current gaps in this cluster: shutdown-progress denial, full exchange-session state, and Java's exact system-message feedback for trade denial are still deferred until those systems/packets are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 67 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 274 tests.

### Session 55 (May 20, 2026)
- Added Java-shaped `SM_MESSAGE` opcode `24` for the senderless `PacketSendUtility.sendMessage(player, msg)` path, using `ChatType.GOLDEN_YELLOW` and the manual `SM_MESSAGE(0, null, msg, chatType)` field layout.
- Wired restricted-staff `AdminService.canOperate` denial to send Java's plain text `"You cannot use {type} with this item."` message to the player instead of only logging.
- Added packet-level coverage for the golden-yellow system chat payload.
- Current gaps in this cluster: player/NPC-sender chat, shout coordinates, race-filtered public chat, and broader chat-channel routing are not ported yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 68 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 275 tests.

### Session 56 (May 20, 2026)
- Registered and parsed Java housing auction client packets: `CM_GET_HOUSE_BIDS` opcode `218`, `CM_REGISTER_HOUSE` opcode `219`, and `CM_PLACE_BID` opcode `221`.
- Added Java-shaped `SM_HOUSE_BIDS` opcode `256` with the first/last header, last-bid fields, registered-house fields, and bid-count tail.
- Wired `CM_GET_HOUSE_BIDS` to return an empty `SM_HOUSE_BIDS` packet for now, matching the Java list response shape while the C# port lacks `HousingBidService`/`HouseBidsDAO` persistence.
- Current gaps in this cluster: real auction registration, bid placement, auction list rows, bid refunds, auction end scheduling, and house static-data mapping remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 69 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 276 tests.

### Session 57 (May 20, 2026)
- Added a local Java `ItemFactory.newItem(itemId, count)` helper for split-created C# items, including Java's max-stack count clamp with kinah exempted.
- Routed broker partial registration, broker partial purchase, and partial item mail attachment creation through the helper so split-created items start with fresh default item state instead of copied per-item state.
- Current gaps in this cluster: `ItemTemplateSummary` still does not parse template activation count, expiration time, tune/amplification/enchant-type, or charge defaults, so those Java `Item` constructor defaults remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 69 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 276 tests.

### Session 58 (May 20, 2026)
- Extended `ItemTemplateSummary`/static-data loading with Java `ItemTemplate` creation dependencies: `activate_count`, `expire_time`, `enchant_type`, Java `canTune()` derivation from `rnd_count`/random bonus/enchant bonus/option-slot bonus, and `<improve level>` conditioning metadata.
- Applied those template defaults in the local `ItemFactory.newItem` helper so split-created broker/mail items now carry activation count, computed expiration epoch seconds, `tune_count=-1` for unidentified tunable items, and Java amplified state when `enchant_type == 1`.
- Updated `SM_INVENTORY_INFO` item blobs to include Java's conditioning-info blob for conditionable templates even when current charge points are zero.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 70 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 277 tests.

### Session 59 (May 20, 2026)
- Registered and parsed Java `CM_LEVEL_READY` opcode `9` for in-game map-ready notification.
- Routed `CM_LEVEL_READY` to the currently ported Java baseline packet sequence: self `SM_PLAYER_INFO`, `SM_ACCOUNT_PROPERTIES`, active-motion `SM_MOTION` action `7`, and `SM_CUBE_UPDATE.cubeSize`.
- Added `gameserver.administration.gm_panel` config parsing so `SM_ACCOUNT_PROPERTIES` enables the GM panel at the same access-level threshold as Java `AdminConfig.GM_PANEL`.
- Current gaps in this cluster: house-object list, instance-count info, windstream announcements, actual delayed world spawn semantics, protection/fly/siege/rift/weather/quest-effect/pet/town/event/team-brand side effects, and exact post-level-ready service fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 71 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 278 tests.

### Session 60 (May 20, 2026)
- Registered Java no-payload social list request packets: `CM_MARK_FRIENDLIST` opcode `110`, `CM_SHOW_BLOCKLIST` opcode `158`, and `CM_SHOW_FRIENDLIST` opcode `230`.
- Added Java-shaped `SM_MARK_FRIENDLIST` opcode `279`, and routed the social list requests to the loaded active-player friend/block state already used by login packets.
- Current gaps in this cluster: add/remove friend/block flows, friend status updates, note persistence/broadcast, offline buddy answer handling, and online-world friend status refresh remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 72 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 279 tests.

### Session 61 (May 20, 2026)
- Registered Java keepalive/time packets: `CM_TIME_CHECK` opcode `18`, `CM_PING` opcode `44`, and `CM_PING_REQUEST` opcode `103`.
- Added Java-shaped responses `SM_TIME_CHECK` opcode `39`, `SM_PONG` opcode `142`, and `SM_PING_RESPONSE` opcode `128`; `CM_TIME_CHECK` preserves Java's `SM_AFTER_TIME_CHECK_4_7_5` then time echo ordering.
- Current gaps in this cluster: Java's ping interval audit/kick behavior is not enforced yet; the C# pass tracks the last ping only enough for future tightening.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 73 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 280 tests.

### Session 62 (May 20, 2026)
- Registered and parsed Java `CM_CHAT_MESSAGE_PUBLIC` opcode `27` for in-game public chat type + UTF-16 message payloads.
- Extended `SM_MESSAGE` with the Java player-origin constructor shape: sender object id/name, race filter (`raceId + 1` for non-staff players), message hardcap, and shout coordinates.
- Routed normal and shout public chat through the existing visibility bridge with self delivery and loaded block-list filtering, matching Java's `broadcastToPlayers` branch at the systems currently ported.
- Current gaps in this cluster: chat commands, `PlayerRestrictions.canChat`, name filtering, group/alliance/league/legion/commander/channel chat, per-recipient staff race-filter suppression, and full chat logging are still deferred until those services/models exist.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 76 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 283 tests.

---

## Next Steps

1. Continue full known-list fanout, housing auction/bid flows, or remaining chat packet surfaces.
2. Port equipment/item stat application once item stat functions and template modifiers are in scope.
3. Add focused live-DB opt-in coverage for creation and enter-world once local schema fixtures are ready.
