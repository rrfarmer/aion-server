# Phase 6: Port Game Core - Detailed Plan & Progress

**Status**: IN PROGRESS (started May 20, 2026)  
**Target**: Port gameplay systems in Java dependency order while keeping database and packet behavior compatible.  
**Validation Approach**: Parity/unit/integration tests first; real-client validation remains an end-of-port readiness step.  
**Workflow Cadence**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Source Of Truth**: Java remains authoritative for packet layouts, guard order, persistence behavior, side effects, naming, and deferred-system boundaries.
**Code Trace Convention**: GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior they mirror.

---

## Resume Snapshot

Last updated: May 22, 2026

- Phase 5 is complete for automated infrastructure parity. Real-client validation is intentionally deferred to later readiness validation.
- Housing known-list work now has a first-pass C# world-house store: Java `gameserver.housing.visibility.distance` loads as 200m by default, persistent custom `houses` rows are loaded into `World` at `HouseAddress` map coordinates during bootstrap, owner-entered houses refresh their world snapshots, and `SM_HOUSE_RENDER`/`SM_DELETE_HOUSE` deltas are sent on enter/movement using housing distance instead of the generic 95m creature distance. `CM_HOUSE_SETTINGS` appearance updates now broadcast from the house snapshot/position when available.
- Current active work is Phase 6E game-core parity continuation. The C# path handles `CM_ENTER_WORLD`, validates missing/online/reentry/duplicate-world cases, loads the player common row plus `player_appearance`, cube inventory rows plus `item_stones` details, regular warehouse rows plus `item_stones` details, account warehouse rows plus `item_stones` details, player skills, active skill cooldowns, active item cooldowns, quests, titles, motions, emotions, recipes, macros, mailbox rows with attached mailbox item template IDs/full item state, broker settlement summary, owned house rows, active craft cooldowns, active portal cooldowns, life stats, friends, blocked users, abyss rank, client settings, and obelisk bind point, marks the character online, stores it in the world container, transitions the connection to `InGame`, sends `SM_ENTER_WORLD_CHECK`, then sends the implemented post-enter packets `SM_SKILL_LIST`, `SM_SKILL_COOLDOWN`, `SM_ITEM_COOLDOWN`, `SM_QUEST_COMPLETED_LIST`, `SM_QUEST_LIST`, current-title and bonus-title `SM_TITLE_INFO`, `SM_MOTION`, `SM_AFTER_TIME_CHECK_4_7_5`, optional `SM_UI_SETTINGS` blobs, Java-split `SM_INVENTORY_INFO`, `SM_CHANNEL_INFO`, obelisk `SM_BIND_POINT_INFO`, baseline `SM_PLAYER_SPAWN`, `SM_GAME_TIME`, Java-shaped regular/account `SM_WAREHOUSE_INFO`, empty auxiliary warehouse placeholders, full-title `SM_TITLE_INFO`, `SM_EMOTION_LIST`, baseline `SM_PRICES`, optional `SM_RECIPE_COOLDOWN`, `SM_FRIEND_LIST`, `SM_BLOCK_LIST`, `SM_INSTANCE_INFO`, `SM_ABYSS_RANK`, baseline `SM_STATS_INFO`, mailbox-state `SM_MAIL_SERVICE`, housing auction-result `SM_SYSTEM_MESSAGE` notifications plus refresh `SM_RECEIVE_BIDS`, Java-split `SM_MACRO_LIST`, `SM_RECIPE_LIST`, broker settled-icon `SM_BROKER_SERVICE`, and housing owner-state `SM_HOUSE_OWNER_INFO`. `CM_LEVEL_READY` now returns the implemented Java baseline map-ready packets: self `SM_PLAYER_INFO`, GM-aware `SM_ACCOUNT_PROPERTIES`, active-motion `SM_MOTION`, and `SM_CUBE_UPDATE` cube size. Title surfaces now cover `CM_TITLE_SET` and `CM_BONUS_TITLE`, validating against loaded `player_titles`, sending Java-shaped self/broadcast `SM_TITLE_INFO`, and updating the loaded player fields that logout persistence already saves. Motion surfaces now cover `CM_MOTION`, Java motion-type active-slot replacement/removal, `player_motions.active` persistence, self `SM_MOTION(action=5)`, and visible-player `SM_MOTION(action=7)` active-slot broadcast. Friend/block surfaces now cover `CM_SHOW_FRIENDLIST`, `CM_SHOW_BLOCKLIST`, `CM_MARK_FRIENDLIST`, `CM_FRIEND_STATUS`, `CM_FRIEND_ADD`, `CM_FRIEND_DEL`, `CM_BLOCK_ADD`, `CM_BLOCK_DEL`, `CM_FRIEND_SET_MEMO`, `CM_BLOCK_SET_REASON`, and buddy `CM_QUESTION_RESPONSE` with Java-shaped `SM_FRIEND_LIST`, `SM_BLOCK_LIST`, `SM_MARK_FRIENDLIST`, `SM_FRIEND_STATUS`, `SM_FRIEND_RESPONSE`, `SM_BLOCK_RESPONSE`, `SM_FRIEND_NOTIFY`, `SM_FRIEND_UPDATE`, and `SM_QUESTION_WINDOW` responses, plus DB-backed reciprocal `friends` insert/delete, `blocks` insert/delete, and memo/reason updates; `CM_FRIEND_STATUS` now fans out online friend status updates and login/logout notifications through the connection registry. Keepalive/time-check surfaces now cover `CM_TIME_CHECK` -> `SM_AFTER_TIME_CHECK_4_7_5` + `SM_TIME_CHECK`, `CM_PING` -> `SM_PONG`, and `CM_PING_REQUEST` -> `SM_PING_RESPONSE`; Java's ping anti-cheat kick is still deferred. Public chat now parses `CM_CHAT_MESSAGE_PUBLIC` and broadcasts Java-shaped normal/shout `SM_MESSAGE` packets, including sender race filter, shout coordinates, self delivery, sight-range filtering, and loaded block-list checks. Whisper chat now parses `CM_CHAT_MESSAGE_WHISPER`, resolves online recipients by normalized Java-style character name, applies offline/block/level/cross-faction denials with Java `SM_SYSTEM_MESSAGE` IDs, and delivers Java-shaped whisper `SM_MESSAGE` packets through the online connection registry. Chat info requests now parse `CM_CHAT_PLAYER_INFO` and `CM_CHAT_GROUP_INFO`, resolve online targets by Java-style name, and return baseline `SM_CHAT_WINDOW` personal/no-group payloads. Chat auth now parses `CM_CHAT_AUTH`, sends Java-shaped `SM_CS_PLAYER_AUTH` over the GameServer-to-ChatServer bridge, processes `CM_CS_PLAYER_AUTH_RESPONSE`, and forwards `SM_CHAT_INIT` tokens to the client; player leave now also sends Java-shaped `SM_CS_PLAYER_LOGOUT` to ChatServer. In-game mail packets now cover list/read/attachment/delete service responses: `CM_CHECK_MAIL_LIST` -> service `2`, `CM_READ_MAIL` -> service `3`, `CM_GET_MAIL_ATTACHMENT` -> service `5`, and `CM_DELETE_MAIL` -> service `6`, with read/attachment/delete final state persisted through `IMailRepository`. `CM_SEND_MAIL` now performs DB-backed recipient validation, Java `isTrading` and `AdminService.canOperate(..., "mail")` staff item restriction checks with senderless golden-yellow `SM_MESSAGE` denial text, Java `ItemFactory.newItem(itemId, count)` count clamping/default activation/expiration/tuning/amplification state for split-created attachments, and normal/kinah/tradeable-item/courier-pass item mail persistence with service `1` status responses plus Java-shaped `SM_SYSTEM_MESSAGE` failure packets for not-enough-money and early item validation, and refreshes an online recipient's mailbox state/list after sender success. `CM_READ_EXPRESS_MAIL` now spawns and dismisses a Java-shaped zephyr postman object for the requesting player via `SM_NPC_INFO`/`SM_DELETE`, with full sight-range known-list fanout still pending. Broker client opcodes `117`, `123`-`130` now parse with Java field layouts; sell-window price range, registered-items, settled-items, item-ID search (`CM_BROKER_SEARCH` mask `0`), numeric and class/recipe category-mask list/search, cancel-registered returns, settle-account collection, register-item persistence with Java `AdminService.canOperate(..., "broker")` staff item restriction checks and `ItemFactory.newItem(itemId, count)` count/default-state handling for split-created registrations, buy-item persistence with the same split-created handling, cube-full rejection, baseline `PlayerRestrictions.canTrade` online/trading/dead guards, and first-pass target selection/audit are backed by the `broker`/broker-storage `inventory` tables. Housing auction/settings surfaces now include Java parsers for `CM_HOUSE_SETTINGS`, `CM_GET_HOUSE_BIDS`, `CM_REGISTER_HOUSE`, `CM_PLACE_BID`, and `CM_HOUSE_PAY_RENT`, parsed Java housing auction/pay/register/fee/commission/bid-step/min-bid-level/maintenance config, DB-backed Java-shaped `SM_HOUSE_BIDS` list responses from `house_bids`/`houses` with static housing land/building/sale-level/maintenance-fee metadata, race filtering, last-bid and registered-house headers, packet splitting, and default auction countdowns, plus DB-backed `CM_REGISTER_HOUSE` initial-auction registration with fee deduction and Java system responses, DB-backed `CM_PLACE_BID` validation/mutation with quest gate, bidding window, self/grace/overdue/level/highest-bidder/Kinah/bid-step checks, Kinah deduction, previous-bid refund mail, online previous-bidder refreshes, success price-change messages, and `SM_RECEIVE_BIDS` refreshes, DB-backed `CM_HOUSE_PAY_RENT` maintenance-fee payment with Kinah deduction, `houses.next_pay` persistence, and Java-shaped `SM_HOUSE_PAY_RENT`, and DB-backed `CM_HOUSE_SETTINGS` owner-name/sign/door state with `SM_HOUSE_ACQUIRE` and Java door-order system messages. Broker paths still lack Java's full NPC `DialogAction.OPEN_VENDOR` function validation until NPC/known-list systems are ported. `ItemTemplateSummary` now parses Java item creation dependencies for activation count, expiration minutes, enchant type, tune eligibility, and conditioning level; inventory/warehouse/mail item blobs now emit zero-charge conditioning info for conditionable templates.
- ItemTemplateSummary now also parses Java `attack_type`, `<weapon_stats>`, `m_slots`, `s_slots`, `max_enchant`, `can_exceed_enchant`, stat bonus set IDs, enchant/tempering template names, idian polish set IDs, godstone proc metadata, and direct item stat modifiers; `SkillTemplateSummary` preserves Java armor/weapon/shield mastery and weapon-dual passive metadata; `SM_STATS_INFO` applies first-pass equipped item template weapon stats/modifiers, socketed mana/fusion stone template modifiers, fusioned weapon template modifiers/10% weapon stat bonuses, selected random bonus modifiers, Java item-set part/full bonuses, Java enchant-template stat effects, Java tempering/plume stat effects, Java idian POLISH random-bonus stat effects, learned Java armor/weapon/shield mastery skill effects, and Java bonus-title stat modifiers to current stats on login while full Java stat-container parity remains pending.
- Java charge-conditioned item modifiers under `<conditions><charge value="..."/>` now load into `ItemStatModifier.ChargeCondition`; the `SM_STATS_INFO` equipment bridge validates them with Java `ItemChargeCondition`/`ChargeInfo` thresholds so conditioning-gated template stats only apply at the required equipped-item charge level.
- Conditioning service flow now covers Java `CM_CHARGE_ITEM` opcode `78` for selected cube items and Java `CM_DIALOG_SELECT` charge-all actions for equipped items: static `<improve>` and `recommend_rank` metadata, Java `ItemChargeService` price/rank math, kinah/AP payment mutation, `SM_QUESTION_WINDOW` charge-all confirmation, `inventory.charge` persistence, AP rank persistence for AP payments, charge-only `SM_INVENTORY_UPDATE_ITEM` blobs, success/all-complete charge system messages, and post-charge `SM_STATS_INFO` refresh. Item-use charge consumables now parse Java `<actions><charge capacity="..."/>`, consume the source item, and condition matching equipped items without an extra payment step.
- Idian polish flow now covers Java `CM_USE_ITEM` opcode `37` type `2` for polish idians, static `<idian>` burn metadata, Java weighted POLISH random-bonus selection, source idian consumption, target idian replacement in `item_stones`, full target inventory update, polish system messages, Java-shaped item-use animation completion, `POLISH_CHARGE` inventory blobs, and a reusable `IdianPolishService.decreasePolishCharge` bridge for future skill/combat observers. Exact 5s item-use scheduling/abort observers, identify/attack-mode guards, and combat/skill burn trigger integration remain pending.
- `CM_EQUIP_ITEM` now parses Java `restrict`, `restrict_max`, `<uselimits gender/rank>`, and `<stigma>` metadata, rejects invalid class, low-level, high-level, race, gender, AP-rank, missing required equip-skill, and unidentified item attempts, starts Java-shaped soul-bind confirmation for unbound soul-bound equipment, applies a first-pass `StigmaService` equip/unequip bridge for normal stigma skills, Kinah cost, linked unlocks, and Java `MembershipConfig.STIGMA_SLOT_QUEST` membership slot overrides, and keeps the first-pass equip/unequip/switch persistence, stats refresh, and appearance fanout.
- `CM_MANASTONE` opcode `74` now covers Java's stigma charge-stone branch, action `1` enchant-stone handling, action `2` manastone socketing with supplement chance/count consumption, manastone removal branch, a first godstone socketing foundation, action `8` amplification foundation, and the Java `ItemSocketService.addManaStone` slot allocator foundation: matching charge stones consume with Java `DEC_STIGMA_USE`, success increments `inventory.enchant`, equipped stigmas refresh temporary stigma skills, failure destroys the stigma and removes its skills, action `1` uses Java `EnchantItemAction.canAct`, `EnchantmentStone.getByItemId`, `EnchantService.enchantItem`, base/amplified chance configs, supplement count/chance rules, +1/+2/+3 crit rolls, success cap, failure downgrade/amplification reset/destruction behavior, source/supplement consumption, target enchant/tune/amplified/buff-skill persistence, Java-shaped enchant and exceed-skill messages, +20 `exceed_enchant_skill` buff-skill selection/removal, equipped temporary skill add/remove packets, Java 4s delayed `TaskId.ITEM_USE` scheduling, movement-cancel animation/message handling, and +15/+20 race-filtered announce fanout, action `2` uses Java `EnchantService.socketManastone` chance math from `gameserver.rates.manastone_chances`, supplement `<actions><enchant>` metadata, source/supplement consume/delete, success/failure messages, DB `item_stones` insertion, target item-info update, equipped stats refresh, Java 2s delayed `TaskId.ITEM_USE` scheduling, and movement-cancel animation/message handling, action `3` validates the current NPC target object, removes normal/fusion `item_stones` slots, charges Java's 650 Kinah fee, persists the stone delete/Kinah mutation, and emits Java-shaped remove-option system/inventory packets, action `4` validates cube-only/non-equipped target weapons, Java `CAN_PROC_ENCHANT` mask, godstone source/template metadata, source consume/delete, target `item_stones` godstone persistence, and Java-shaped proc system/item packets, action `8` validates target/material/tool lookup, Java `canExceedEnchant`, max-enchant requirement, universal material IDs, source consume/delete, target `is_amplified`, and Java-shaped exceed system/item packets, and the manastone allocator now selects normal/special/fusion slots from Java `m_slots`/`s_slots` with the six-slot cap. Richer NPC known-list/range validation and full SkillEngine stat/effect fanout remain pending.
- Enter-world now applies Java `StigmaService.onPlayerLogin` membership `STIGMA_AUTOLEARN` before `SM_ENTER_WORLD_CHECK`/`SM_SKILL_LIST`, learning temporary stigma skills from level 20 through the player's current level with normal/linked stigma skill types.
- Stigma removal now mirrors Java `StigmaService.removeLinkedStigmaSkills` hidden-skill deletion notices: linked stigma skills are removed by stack and emit `STR_MSG_STIGMA_DELETE_HIDDEN_SKILL` (`1402895`) through equip/unequip and stigma-charge mutation flows.
- Non-autolearn stigma login now mirrors Java `StigmaService.onPlayerLogin`: equipped stigma stones are validated for slot permission, class, and same-slot conflicts, invalid stones are persisted as unequipped, and valid equipped stigma stones rebuild normal/linked temporary skills before the first enter-world skill list.
- Movement packet surface now covers Java movement masks/glide flags, `CM_MOVE` opcode `48`, `CM_MOVE_IN_AIR` opcode `49`, `SM_MOVE` opcode `55`, mutable player position, and PlayerMoveController-style target/vector/glide/vehicle state. A first known-list bridge now broadcasts `SM_MOVE`, baseline player enter `SM_PLAYER_INFO` plus companion `SM_MOTION` action `7`, player logout `SM_DELETE`, postman `SM_NPC_INFO`, and postman `SM_DELETE` to active players in the same world within Java's default 95m visible distance. Persistent cached KnownList membership, full player-info dependent state, anti-hack, protection/fall/glide side effects, and strict flying-state gates remain pending.
- Housing auction timing now includes Java `AuctionEndTask.tryProlongAuction` parity for the default Sunday-noon auction end: late bids can prolong individual house auctions by five-minute windows up to thirty minutes, and `SM_HOUSE_BIDS` countdowns use that per-house state.
- Housing auction and maintenance timing now parse the Java weekly cron strings from `housing.properties`, so auction countdown/prolongation math and rent due-date advancement are no longer limited to the default Sunday-noon and Monday-midnight schedules.
- `SM_HOUSE_OWNER_INFO` inactive-house grace seconds now mirror Java `House.findGraceEndTime`, using the configured auction-end schedule and the last auction end before the two-week inactive-house cap.
- `SM_HOUSE_OWNER_INFO` active-house town level now mirrors Java `House.getTownLevel`, backed by `HouseAddress.townId` from static housing data and `towns.level` from the game DB.
- Housing login now mirrors Java `HousingService.onPlayerLogin` overdue/sequestration notices before `SM_HOUSE_OWNER_INFO`.
- Housing auction timing now includes Java `AuctionEndTask.shouldRunOnStart` startup recovery for missed auction ends and the 30-minute prolongation window.
- Friend-list serialization now fills Java `HousingService.findActiveHouse` address and door-state fields in `SM_FRIEND_LIST`, using loaded DB house settings and online friend snapshot refreshes.
- Player notes now load/save through `players.note`; `CM_SET_NOTE` updates the loaded common data, refreshes online friends with `SM_FRIEND_LIST`, broadcasts Java-shaped `SM_UPDATE_NOTE`, and note strings are present in `SM_CHAT_WINDOW` and `SM_PLAYER_INFO`.
- `SM_PLAYER_INFO` now fills Java's selected-target object ID and active-house address fields from loaded player state; team and mentor fields are still explicit defaults pending team/mentor model parity.
- Login auth membership is now attached to active `Player` state and written to Java's `SM_CHAT_WINDOW` VIP byte plus `SM_PLAYER_INFO` membership marker.
- Player enter-world now loads legion membership/name from `legion_members`/`legions`, and personal `SM_CHAT_WINDOW` writes the Java legion-name field.
- `CM_HEADING_UPDATE` opcode `147` now parses Java's spin/heading byte and intentionally keeps Java's no-op `runImpl` behavior.
- `SM_PLAYER_INFO` now writes Java's legion member block with loaded legion ID/name and emblem type/color fields.
- `CM_REJECT_REVIVE` opcode `146` now mirrors Java's empty-payload, no-op packet.
- Macro create/delete now covers Java `CM_MACRO_CREATE`, `CM_MACRO_DELETE`, `SM_MACRO_RESULT`, loaded macro mutation, and `player_macrosses` persistence.
- Deferred gameplay parser coverage now includes Java `CM_REVIVE`, `CM_QUESTIONNAIRE`, `CM_START_LOOT`, `CM_LOOT_ITEM`, `CM_SUBZONE_CHANGE`, and `CM_CHANGE_CHANNEL`, with explicit handlers left as no-ops until revive/reward/drop/zone/channel systems are ported.
- `GameTimeService` now loads and stores Java `server_variables.time` through a C# `ServerVariablesDAO` equivalent, including periodic saves and shutdown save.
- Game-time periodic updates now broadcast Java-shaped `SM_GAME_TIME` to all online players through a world-wide packet fanout helper before saving time.
- `PeriodicSaveService` now mirrors Java `ServerRunTimeSaveTask`, storing `server_variables.serverLastRun` periodically and on shutdown.
- `CM_INSTANCE_INFO` opcode `192` now handles Java's current-player/no-team branch by returning `SM_INSTANCE_INFO(updateType, player)` from the loaded instance cooldown table.
- Housing maintenance timing now includes Java `MaintenanceTask.calculateImpoundDate` and overdue mail stage selection from `MailFormatter.sendHouseMaintenanceMail`.
- Player close/logout now has Java `CM_QUIT`/`CM_MAY_QUIT` packet surfaces and a `PlayerLeaveWorldService` baseline: active players are removed from the world container, marked offline in memory, current position/world/heading and key common-data fields are persisted, current HP/MP/FP are saved to `player_life_stats`, active skill/item cooldown rows are refreshed, `last_online` is refreshed, and `online=false` is written after the save step. `CM_QUIT` sends Java-shaped `SM_QUIT_RESPONSE` and either returns to authed character-selection state or closes the socket after the response.
- Character creation is DB-backed and writes `players`, `player_appearance`, `player_skills`, and starter `inventory` rows. It uses Java-style starter items, equipment-slot selection, level-1 autolearn skills, old-name reservation checks, and membership character limits.
- Startup now preloads `IDFactory` from Java-equivalent used-ID tables before gameplay allocation.
- `CM_EMOTION` now covers Java abnormal movement guards, stance-denial messages, ride sprint start/end, fly-teleport landing, first-pass fly/land FP task side effects, and stop-glide movement side effects. Full fly-zone/cooldown/stat-speed/observer behavior is still pending.
- `CM_USE_ITEM` now routes Java ride item actions, including static ride data, delayed mount animation, mount/dismount state, ride emotion broadcasts, ride-on-emotion cancellation exception, and sit-triggered dismount parity. It also routes Java craft-learn recipe items with recipe validation, `SM_LEARN_RECIPE`, DB `player_recipes` insertion, and source item consumption; emotion cards with `player_emotions` persistence and `SM_EMOTION_LIST(action=1)`; title cards with `player_titles` persistence, cash-title messages, and full `SM_TITLE_INFO` refresh; skill books with Java skill-tree/message selection and `player_skills` persistence; cube/warehouse expansion tickets with `players.item_expands` / `players.wh_bonus_expands` persistence plus cube/warehouse update packets; item-target dyes with `inventory.item_color/color_expires` persistence, dye system messages, target item update, and equipped appearance refresh; and motion cards with delayed item-use animation, `player_motions` persistence, `SM_MOTION(action=2)` owner updates, and visible active-motion refresh.
- Temporary emotion/title/motion lifecycle now has a C# `ExpireTimerTask` bridge: enter-world registers loaded temporary rows, new emotion/title/motion item actions register fresh expirable entries, logout unregisters the player, the periodic tick honors Java's `remainingSeconds < 0` expiry threshold, and timeout removal deletes the DB row while sending Java-shaped `SM_EMOTION_LIST`, `SM_TITLE_INFO`, `SM_MOTION(action=6)`, and cash-timeout system messages. The bridge now also registers loaded cube/equipment, regular-warehouse, and account-warehouse expirable items, sends Java minute-threshold cash-item warnings, deletes expired inventory rows plus `item_stones`, emits cube/warehouse delete packets and storage-size refreshes, and registers newly created non-stackable decompose reward items.
- `CM_APPEARANCE` opcode `197` now covers Java type `2` cosmetic item actions: static `cosmetic_items.xml` templates and item `<actions><cosmetic name="..."/>` metadata load into C# holders, race/gender/ride guards send Java system messages, supported appearance mutations and presets update `player_appearance`, the cosmetic item is deleted from inventory, and visible players receive a Java `onChangedPlayerAttributes`-style `SM_PLAYER_INFO` refresh. Character/legion rename coupon branches remain deferred until rename/world-cache services are ported.
- Decomposable item static data now loads Java `decomposable_items.xml` into a typed C# holder, including normal reward groups, selectable first-group rewards, chance/level gates, fixed rewards, random rewards, race/class restrictions, Java default counts, and item-template `<actions><decompose/>` markers.
- `CM_USE_ITEM` now routes Java `<decompose/>` item actions and opcode `236` `CM_SELECT_DECOMPOSABLE`: selectable decomposables send Java `SM_FIRST_SHOW_DECOMPOSABLE` and selection consumes the source, sends success/secondary packets, persists rewards through a reusable Java `ItemService.addItem`-style stack/new-row planner, sends `SM_INVENTORY_UPDATE_ITEM(INC_ITEM_COLLECT)` for merged stacks, and uses Java `DECOMPOSABLE` inventory-add type for new reward rows; normal decomposables run the Java 3s item-use animation, movement cancel message/cooldown removal, level/chance reward group selection, fixed/random reward resolution, source consumption, success/failure messages, and DB-backed reward stack/new-row persistence. Decompose canAct now separates normal cube fullness from Java special-cube fullness using item-template `<inventory id="...">` metadata, and the reusable item-add planner can use that metadata for non-overflow normal vs special slot checks. The item-add planner also mirrors Java's `POWER_SHARDS` branch by topping off equipped shard stacks before cube stacks, preserves partial stack/new-row mutations when a capacity-limited remainder exists, and reports inventory-full remainders so callers can send Java dice-inventory messaging.
- Assembly item static data now loads Java `assembly_items.xml` into a typed C# holder, and `CM_USE_ITEM` routes item-template `<actions><assemble item="..."/>` metadata through Java `AssemblyItemAction`: all required parts are validated, a 1s item-use animation is scheduled with Java item-cancel behavior, parts are consumed by item ID, the assembly success message is sent, and the reward is added through the reusable Java `ItemService.addItem`-style planner. Java's full-inventory behavior is preserved: consumed parts stay consumed and the dice inventory error is sent if the reward cannot fit.
- XP extraction item-template metadata now parses Java `<actions><expextract item_id percent cost/>` into `ItemTemplateSummary`, and `CM_USE_ITEM` routes Java `ExpExtractAction` runtime: 5s item-use animation/cancel, inventory-full and not-enough-exp guards, fixed/percent EXP cost calculation, source consumption by item ID, EXP persistence, `SM_STATUPDATE_EXP`, reward add, dice-inventory failure, and success messaging.
- Enchantment-stone composition now parses Java `<composition/>` metadata and opcode `208` `CM_COMPOSITE_STONES`, validating the combination tool plus two enchantment stones, scheduling Java's 5s self-only item-use animation/cancel, consuming tool/stone item IDs, calculating the Java reward enchantment-stone ID, and adding the reward through the reusable item-add planner.
- Extraction tool metadata now parses Java `<extract/>` markers and `<apextract rate target/>` metadata into item templates.
- Java `EnchantService.breakItem` now has a C# planner for `ExtractAction`: weapon/armor validation, effective-level stone selection, random count ranges, target deletion, source consume/decrement, and reward planning through the reusable Java `ItemService.addItem` bridge.
- `CM_USE_ITEM` now routes Java `<extract/>` runtime with 5s self item-use animation/cancel, canAct failure messages, target/source/reward persistence, target/source inventory packets, reward add/update packets, dice-inventory failure behavior, and newly-created expirable reward registration.
- AP extraction metadata now includes Java `<acquisition ap="...">` required AP values and Java `ItemMask.CAN_AP_EXTRACT`, enabling AP extraction targets to compute returned AP from real item metadata.
- `CM_USE_ITEM` now routes Java `<apextract/>` runtime with source/target validation, tool level/quality/target-type checks, target deletion, tool consume/decrement, AP-rank persistence, AP gain system message, `SM_ABYSS_RANK`, and visible-player `SM_ABYSS_RANK_UPDATE` fanout on rank changes.
- Java remodel now covers item-template `<remodel type/minutes>` metadata, opcode `90` `CM_ITEM_REMODEL` parsing, first-pass `ItemRemodelService.remodelItem` runtime validation, Kinah payment, extract item consumption, target `item_skin`/color mutation, inventory update/delete packets, and remodel system messages. NPC range/function validation remains pending with the broader NPC/dialog known-list work.
- Next implementation slice should start from the remaining equipment/gameplay queue: finish AP rank-change side effects beyond current packets (`Equipment.checkRankLimitItems`, `AbyssSkillService.updateSkills`, legion contribution/siege callbacks), broaden expirable lifecycle coverage to pets and house-object rows once their models exist, full SkillEngine effect application after temporary skill mutations, stance observers, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, charge/idian burn trigger integration, broader skill/effect stat strategy beyond mastery/title modifiers, housing auction settlement/maintenance/sign/appearance flows, persistent known-list membership, or full NPC/dialog known-list/function validation.
- Latest validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passed with 482 tests.

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
- [x] Handle `CM_TITLE_SET` and `CM_BONUS_TITLE` with loaded-title validation, Java-shaped `SM_TITLE_INFO` responses, visible-player display-title broadcast, and logout-persisted player title fields
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
- [x] Handle `CM_HOUSE_SETTINGS` with DB-backed door/show-owner/sign state, Java-shaped `SM_HOUSE_ACQUIRE`, and Java door-order `SM_SYSTEM_MESSAGE` responses; house appearance fanout and visitor kick side effects remain pending
- [x] Load future `craft_cooldowns` rows and send Java-shaped `SM_RECIPE_COOLDOWN`
- [x] Send baseline Java-shaped `SM_PRICES`
- [x] Load `friends`/friend common rows with active-house address/door state and send Java-shaped `SM_FRIEND_LIST`
- [x] Load `blocks`/blocked-player names and send Java-shaped `SM_BLOCK_LIST`
- [x] Handle `CM_FRIEND_ADD` with Java target-side denied-friend setting, question-window request/response flow, and DB-backed reciprocal friend insert
- [x] Handle `CM_BLOCK_ADD` with DB-backed target lookup/insert, Java denial order, `SM_BLOCK_LIST`, and `SM_BLOCK_RESPONSE`
- [x] Handle `CM_FRIEND_DEL` with DB-backed bidirectional friend delete, active-player list refresh, online friend list refresh, `SM_FRIEND_NOTIFY`, and `SM_FRIEND_RESPONSE`
- [x] Handle `CM_BLOCK_DEL` with DB-backed block delete, `SM_BLOCK_LIST`, and `SM_BLOCK_RESPONSE`
- [x] Handle `CM_FRIEND_SET_MEMO` with DB-backed friend memo update and `SM_FRIEND_LIST`
- [x] Handle `CM_BLOCK_SET_REASON` with DB-backed block reason update, `SM_BLOCK_LIST`, and `SM_BLOCK_RESPONSE`
- [x] Fan out `CM_FRIEND_STATUS` changes to online friends with Java-shaped `SM_FRIEND_UPDATE` and `SM_FRIEND_NOTIFY`
- [x] Load future `portal_cooldowns` rows and send Java-shaped login `SM_INSTANCE_INFO`
- [x] Load `abyss_rank` and send Java-shaped `SM_ABYSS_RANK`
- [x] Load `player_life_stats` and send baseline Java-shaped `SM_STATS_INFO`
- [x] Load `player_motions` and send Java-shaped login `SM_MOTION`
- [x] Handle `CM_MOTION` with Java motion-type active-slot replacement/removal, `player_motions.active` persistence, self `SM_MOTION(action=5)`, and visible-player active-motion `SM_MOTION(action=7)` broadcast
- [x] Load `player_settings` client/display/deny settings, send Java-shaped `SM_UI_SETTINGS`, handle `CM_UI_SETTINGS`/`CM_CUSTOM_SETTINGS`, broadcast `SM_CUSTOM_SETTINGS`, and persist settings on logout
- [x] Send current-title `SM_TITLE_INFO` and `SM_AFTER_TIME_CHECK_4_7_5`
- [ ] Inventory/equipment load and stat application (partial: typed inventory rows and `item_stones` rows loaded; item-stone packet display is implemented; first-pass equipped item template weapon stats/modifiers, socketed mana/fusion stone modifiers, fusioned weapon modifiers/10% bonuses, selected random bonus modifiers, item sets, enchantment, tempering, conditioning, idian POLISH stats, learned armor/weapon/shield mastery stats, and bonus-title stats now apply in `SM_STATS_INFO`; selected cube-item conditioning service/payment/update flow, equipped item charge-all confirmation/payment flow, charge consumable flow, idian polish mutation, unidentified item blob hiding, idian identify denial, `CM_EQUIP_ITEM` unidentified guard, soul-bind confirmation/persistence foundation, and first-pass stigma equip/unequip skill/Kinah/linked-unlock bridge are implemented; godstone effects, stigma enchant, full skill/effect stat-container parity, equip/unequip recompute, exact soul-bind timing/cancel observers, and charge/idian burn observers remain pending)
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
- [ ] Complete `SM_PLAYER_INFO` dependent state for transforms, exact stats, store, ride/stance, team/mentor, CP, and viewer-specific enemy race handling

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
- Current gaps in this cluster: friend online status uses persisted/common-row online state; exact Java `World.getPlayer(...).getFriendList().getStatus()` behavior should be tightened when richer world/player social state lands.
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
- Then-current gaps in this cluster were macro create/update/delete client packets and persistence; Session 98 covers those packet and persistence paths.
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
- Current gaps in this cluster: the packet still uses defaults for transforms, exact calculated stats/speeds, private store, flight transport, team/mentor, CP info, ride/stance follow-up packets, and viewer-specific enemy race/icon handling.
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

### Session 63 (May 20, 2026)
- Registered and parsed Java `CM_CHAT_MESSAGE_WHISPER` opcode `28` for recipient-name + message payloads.
- Added online-player-by-name lookup and direct player packet send helpers to the in-game connection registry, mirroring the Java `World.getPlayer(name)` and `PacketSendUtility.sendPacket(player, packet)` dependencies used by whispers.
- Routed whisper chat through Java denial order for currently available state: no such user (`1300627`), minimum whisper level (`1310004`), receiver block list (`1300628`), and cross-faction denial (`1401174`), then delivered `SM_MESSAGE` whisper chat type `4` to the recipient.
- Current gaps in this cluster: no-whispers custom state, `PlayerRestrictions.canChat`, `NameRestrictionService.filterMessage`, GM/staff chat logging, exact `ChatUtil` name-tag parsing, and per-recipient staff race-filter suppression remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 77 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 284 tests.

### Session 64 (May 20, 2026)
- Registered Java chat info request packets: `CM_CHAT_PLAYER_INFO` opcode `39` and `CM_CHAT_GROUP_INFO` opcode `61`.
- Added Java-shaped `SM_CHAT_WINDOW` opcode `99` with the currently available baseline branches: personal player info (`isGroup=false`) and no-group group info (`isGroup=true` when target lacks C# team state).
- Routed chat info requests through online name lookup with `STR_NO_SUCH_USER` fallback; player info uses visibility as the current KnownList approximation before sending the personal chat window.
- Current gaps in this cluster: real group/alliance chat-window branches, persistent KnownList membership, and exact name-tag parsing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 79 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 286 tests.

### Session 65 (May 20, 2026)
- Registered and parsed Java `CM_CHAT_AUTH` opcode `174` for client chat-server sign-in requests.
- Added Java-shaped `SM_CHAT_INIT` opcode `230` for forwarding ChatServer auth tokens to the game client.
- Added the GameServer-to-ChatServer `SM_CS_PLAYER_AUTH` bridge packet and taught the C# ChatServer bridge to dispatch `CM_CS_PLAYER_AUTH_RESPONSE` token replies after bridge auth.
- Routed `CM_CHAT_AUTH` through the authenticated ChatServer bridge with a pending callback back to the player's game connection.
- Current gaps in this cluster: chat-ban/gag follow-up, bridge reconnect replay for pending player auths, and real-client validation of the ChatServer endpoint advertised by `SM_VERSION_CHECK` remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 81 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 288 tests.

### Session 66 (May 20, 2026)
- Added the GameServer-to-ChatServer `SM_CS_PLAYER_LOGOUT` bridge packet and send helper.
- Wired active-player leave/close/logout through `ChatServer.sendPlayerLogout` parity before world removal, also clearing any pending chat-auth callback for that player.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 82 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 289 tests.

### Session 67 (May 20, 2026)
- Registered and parsed Java `CM_FRIEND_STATUS` opcode `170`.
- Added Java-shaped `SM_FRIEND_STATUS` opcode `227`, stored the active player's current friend-list status byte, and echoed the status response like Java `FriendList.setStatus(...)`.
- Current gaps in this cluster: friend add/remove, block add/remove/reason edit, friend memo persistence, friend notifications, question-window requests, and DAO-backed social writes remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 82 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 289 tests.

### Session 68 (May 20, 2026)
- Registered and parsed Java social edit/delete opcodes: `CM_FRIEND_DEL` (`112`), `CM_BLOCK_DEL` (`167`), `CM_BLOCK_SET_REASON` (`179`), and `CM_FRIEND_SET_MEMO` (`239`).
- Added Java-shaped `SM_FRIEND_RESPONSE` opcode `222`, `SM_BLOCK_RESPONSE` opcode `223`, and `SM_FRIEND_NOTIFY` opcode `225`, plus the Java `STR_BUDDYLIST_NOT_IN_LIST` and `STR_BLOCKLIST_NOT_IN_LIST` system-message IDs.
- Added `ISocialRepository`/`MySqlSocialRepository` with Java DAO-equivalent writes for `FriendListDAO.delFriends`, `FriendListDAO.setFriendMemo`, `BlockListDAO.delBlockedUser`, and `BlockListDAO.setReason`.
- Wired friend delete through bidirectional DB delete, active-player list refresh, online-friend list refresh and delete notification, and `SM_FRIEND_RESPONSE.TARGET_REMOVED`.
- Wired block delete, friend memo edit, and block reason edit through DB-backed writes and the Java list/response refresh packets.
- Current gaps in this cluster: friend add request/question-window flow, block add flow, online friend-status update fanout, and offline social request handling remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 82 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 289 tests.

### Session 69 (May 20, 2026)
- Registered and parsed Java `CM_BLOCK_ADD` opcode `166`.
- Extended `ISocialRepository`/`MySqlSocialRepository` with Java `PlayerService.getOrLoadPlayerCommonData(String)`-style target lookup and `BlockListDAO.addBlockedUser` insert.
- Routed block add through Java's denial order for self, full block list, missing target, target already on friend list, and already-blocked target; success adds the in-memory block entry and sends `SM_BLOCK_LIST` plus `SM_BLOCK_RESPONSE.BLOCK_SUCCESSFUL`.
- Added Java `STR_BLOCKLIST_NO_BUDDY` and `STR_BLOCKLIST_ALREADY_BLOCKED` system-message helpers used by the block-add denial path.
- Current gaps in this cluster: friend add request/question-window flow, online friend-status update fanout, and offline social request handling remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 82 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 289 tests.

### Session 70 (May 20, 2026)
- Added Java-shaped `SM_FRIEND_UPDATE` opcode `240` for single-friend status data refreshes.
- Upgraded `CM_FRIEND_STATUS` handling from local echo only to Java `FriendList.setStatus` baseline fanout: invalid status values normalize to online for server state, online friends receive updated reciprocal friend snapshots, and login/logout notifications are sent when the previous/effective status crosses Java's offline rule.
- Current gaps in this cluster: friend add request/question-window flow, offline social request handling, and exact friend note/common-data refresh fields beyond the currently loaded `Player` model remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 82 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 289 tests.

### Session 71 (May 20, 2026)
- Registered and parsed Java `CM_FRIEND_ADD` opcode `111` and buddy-list `CM_QUESTION_RESPONSE` opcode `50`.
- Added Java-shaped `SM_QUESTION_WINDOW` opcode `52` for friend-request prompts (`STR_BUDDYLIST_ADD_BUDDY_REQUEST`).
- Extended `Player` with a pending friend request marker mirroring Java `ResponseRequester` for the currently ported buddy request window.
- Extended `ISocialRepository`/`MySqlSocialRepository` with `FriendListDAO.addFriends`-style reciprocal `friends` inserts.
- Routed friend add through Java's available denial order: offline target, self/busy, GM restriction, already friend, cross-race hidden target, requester-blocked target, target-blocked requester, requester full, target full, and busy target prompt.
- Routed buddy question accept/deny responses: denial sends `SM_FRIEND_RESPONSE.TARGET_DENIED` to the requester; acceptance persists both rows, refreshes both in-memory friend lists, and sends `SM_FRIEND_LIST` plus `SM_FRIEND_RESPONSE.TARGET_ADDED` to both players.
- Then-current gaps in this cluster: target-side denied-friend setting, offline social request handling, and generic `ResponseRequester` support beyond buddy requests. Session 88 covers target-side denied-friend settings.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 82 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 289 tests.

### Session 72 (May 20, 2026)
- Added a typed housing static-data table for Java `HousingLand`, `HouseAddress`, and `Building.size` fields needed by housing auction packets.
- Extended `SM_HOUSE_BIDS` from empty-list only to Java row serialization: first/last flags, last-bid header, registered-house header, bid rows, static `100000` client value, bid counts, and split packets using the Java static/dynamic body sizes.
- Added `IHouseAuctionRepository`/`MySqlHouseAuctionRepository` to reconstruct Java `HouseBidsDAO.loadBids` state from `house_bids` joined to `houses`, preserving first-seen list indexes, initial offers, higher-bid acceptance, player latest-bid headers, owned-house registered auction headers, and race filtering through manager NPC tribe.
- Routed `CM_GET_HOUSE_BIDS` through the repository and static housing data so auction-list requests return live DB-backed `SM_HOUSE_BIDS` data instead of the prior empty placeholder.
- Current gaps in this cluster: `CM_REGISTER_HOUSE` and `CM_PLACE_BID` still log only; true mutations still need `HousingBidService.auction/bid`, kinah fee/payment updates, ownership/rent checks, refund/result mail, sign/appearance updates, and auction prolongation state.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 83 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 290 tests.

### Session 73 (May 20, 2026)
- Routed `CM_REGISTER_HOUSE` through the Java registration guard order for modeled state: registration-day window, active non-studio house, fee-paid state, available Kinah, already-registered house, and persistence failure fallback.
- Extended `IHouseAuctionRepository` with a transaction-backed registration write: verifies the active house owner, denies existing `house_bids` rows, deducts the registration fee from the cube Kinah item, and inserts the initial `player_id=0` bid row using the Java `HouseBidsDAO.INSERT_QUERY` shape.
- Added Java housing registration system-message helpers for not-enough-Kinah fee, registration timeout, overdue maintenance, successful own-house auction, and already-registered denial.
- On successful registration, the active player now receives the Kinah inventory update, `STR_MSG_HOUSING_AUCTION_MY_HOUSE`, and `SM_RECEIVE_BIDS(0)` refresh notification.
- Current gaps in this cluster: registration uses Java default config values for the Monday-Friday window and 30% fee until housing config parsing is broadened; sign/appearance updates are still unported; `CM_PLACE_BID` remains the next housing mutation and still needs bid validation, Kinah deduction, previous-bid refund mail, online refreshes, and auction prolongation.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 83 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 290 tests.

### Session 74 (May 20, 2026)
- Added typed GameServer housing options loaded from Java `config/main/housing.properties`: auction enable, pay enable, auction end cron text, registration day range, registration fee percentage, sales commission percentage, and bid-step limit.
- Updated `CM_REGISTER_HOUSE` to use the loaded housing auction enable flag, pay flag, registration-day range, and fee percentage instead of local hard-coded defaults.
- This prepares `CM_PLACE_BID` for Java-configured bid-step validation and future auction-end/prolongation work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 83 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 290 tests.

### Session 75 (May 20, 2026)
- Routed `CM_PLACE_BID` through the modeled Java `HousingBidService.bid` denial order: `HousingService.canOwnHouse` quest gate, missing bid, bidding-time cutoff, own-house bid, inactive grace house, overdue maintenance, minimum bid level, already-highest bid, highest bid on another house, insufficient Kinah, and bid-step overflow.
- Extended housing static-data loading with Java `Sale.level` so bid minimum-level checks fall back to the land sale option when type-specific housing config values are zero.
- Extended `IHouseAuctionRepository` with transaction-backed bid placement: locks current bid rows, rejects stale/lower bids, deducts the bidder's Kinah item, inserts the player bid, creates Java-shaped failed-bid refund system mail for the previous highest bidder, and increments that recipient's mailbox count.
- On successful bids, the active bidder now receives the Kinah inventory update, `STR_MSG_HOUSING_BID_SUCCESS`, `STR_MSG_HOUSING_PRICE_CHANGE`, and `SM_RECEIVE_BIDS(0)`; online previous bidders receive `STR_MSG_HOUSING_BID_CANCEL`, `SM_RECEIVE_BIDS(0)`, and mailbox refresh.
- Current gaps in this cluster: `AuctionEndTask.tryProlongAuction`, auction-end settlement, seller/buyer result mail beyond failed-bid refunds, house sign/appearance updates, auto-fill, and live-client packet timing are still pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 83 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 290 tests.

### Session 76 (May 20, 2026)
- Registered and parsed Java `CM_HOUSE_PAY_RENT` opcode `223` and added Java-shaped `SM_HOUSE_PAY_RENT` opcode `262`.
- Extended housing static-data loading with Java `HousingLand.maintenanceFee` and loaded `gameserver.housing.maintain.time` from `housing.properties`.
- Routed rent payment through the modeled Java branch: active-house lookup, pay-disabled/free-fee response, insufficient Kinah denial, Monday-maintenance due-date extension, four-week client cap, Kinah deduction, `houses.next_pay` persistence, local house-state refresh, and `SM_HOUSE_PAY_RENT`.
- Current gaps in this cluster: the maintenance cron helper currently models Java's default Monday-midnight schedule only; full `MaintenanceTask` impound/auction behavior and custom cron parsing are still pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 83 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 290 tests.

### Session 77 (May 21, 2026)
- Registered and parsed Java `CM_HOUSE_SETTINGS` opcode `73` for door state, owner-name visibility, and sign notice updates.
- Added Java-shaped `SM_HOUSE_ACQUIRE` opcode `275` and Java door-order `SM_SYSTEM_MESSAGE` helpers for open, friends-only, and closed settings changes.
- Extended loaded `PlayerHouse` state with Java `House.getPermissionsForDB`/`setPermissionsFromDB` settings and `sign_notice` fields, then routed settings changes through DB-backed `houses.settings`/`sign_notice` persistence and local active-house refresh. This persists immediately until Java's dirty `House.save` periodic-save path is ported.
- Current gaps in this cluster: house appearance known-list fanout, GeoService door state updates, and visitor kick side effects remain pending until full house/known-list systems are available.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 83 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 290 tests.

### Session 78 (May 21, 2026)
- Registered and parsed Java title-change packets: `CM_TITLE_SET` opcode `139` and `CM_BONUS_TITLE` opcode `233`.
- Routed display-title changes through Java `TitleList.setDisplayTitle` baseline behavior: validates against loaded `player_titles` unless the client clears to `0xFFFF`, sends self `SM_TITLE_INFO(action=1)`, broadcasts Java-shaped `SM_TITLE_INFO(action=3)` to visible players, and updates the in-memory title ID used by logout persistence.
- Routed bonus-title changes through Java `TitleList.setBonusTitle` baseline behavior: validates against loaded titles, sends `SM_TITLE_INFO(action=6)`, and updates the in-memory bonus-title ID used by logout persistence.
- Current gaps in this cluster: bonus-title stat modifier application and nearby quest refresh side effects remain pending until stats/effects and quest update services are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 84 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 291 tests.

### Session 79 (May 21, 2026)
- Registered and parsed Java `CM_MOTION` opcode `71` for custom animation active-slot changes.
- Added Java `Motion.motionType` mapping to C# motion state and corrected `SM_MOTION(action=7)` active-motion serialization to write type slots 1-5 instead of sorting active motion IDs.
- Added Java-shaped `SM_MOTION(action=5)` self responses and routed `CM_MOTION` through `MotionList.setActive` baseline behavior: active-slot replacement/removal, local loaded-motion refresh, DB-backed `player_motions.active` updates through `MotionDAO.updateMotion`-equivalent writes, and visible-player `SM_MOTION(action=7)` broadcast.
- Current gaps in this cluster: motion acquisition/removal item actions and expiration timers remain pending until item-use and expirable task systems are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 85 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 292 tests.

### Session 80 (May 21, 2026)
- Added `HouseAuctionTimingService` for Java `AuctionEndTask.tryProlongAuction` parity around the default Sunday-noon auction end, including five-minute per-house prolongations, the thirty-minute maximum window, expiry cleanup, and `onAuctionEnd` state clearing.
- Routed `CM_PLACE_BID` through the timing service after the Java bidding guards and before `HouseBidsDAO.addBid`-equivalent persistence, while keeping Sunday-noon timeout behavior for non-prolonged houses.
- Updated `SM_HOUSE_BIDS` auction countdowns to use per-house prolonged end times instead of only the regular default end time.
- Current gaps in this cluster: scheduled auction-end settlement, startup recovery around prolonged auctions, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 89 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 296 tests.

### Session 81 (May 21, 2026)
- Added a compact Java weekly cron parser for `HousingConfig`-style Quartz expressions such as `0 0 12 ? * SUN`, including named and numeric day-of-week support plus fallback to Java defaults for unsupported expressions.
- Wired `HouseAuctionTimingService` to `gameserver.housing.auction.end_time` so auction countdown and prolongation math follow the configured Java cron instead of only Sunday noon.
- Wired `CM_HOUSE_PAY_RENT` maintenance due-date extension to `gameserver.housing.maintain.time`, preserving Java `AbstractCronTask.getNextRunAfter` strict-after behavior.
- Current gaps in this cluster: full `MaintenanceTask` impound/auction behavior, scheduled auction-end settlement, startup recovery around prolonged auctions, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 94 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 301 tests.

### Session 82 (May 21, 2026)
- Updated `PlayerHouse.GetGraceSeconds` to mirror Java `House.secondsUntilGraceEnd`/`findGraceEndTime`: inactive grace now ends at the last configured auction end before acquire time plus fourteen days, instead of using a flat fourteen-day delta.
- Wired `SM_HOUSE_OWNER_INFO` through the configured auction-end schedule supplied by `HouseAuctionTimingService`, preserving Java behavior for custom housing auction cron values.
- Updated packet and model tests for the auction-end-based inactive-house grace value.
- Current gaps in this cluster: exact town level, full `MaintenanceTask` impound/auction behavior, scheduled auction-end settlement, startup recovery around prolonged auctions, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 96 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 303 tests.

### Session 83 (May 21, 2026)
- Added housing address town IDs to the typed static-data cache from Java `HouseAddress.townId`.
- Enriched loaded player houses with Java `House.getTownLevel` data by resolving each address town through the DB-backed `towns.level` table, with Java `TownService` level-1 startup seeding as the fallback for known towns.
- Updated `SM_HOUSE_OWNER_INFO` to write the active house town level instead of the previous zero placeholder, and added a packet test for the town-level byte.
- Current gaps in this cluster: full `MaintenanceTask` impound/auction behavior, scheduled auction-end settlement, startup recovery around prolonged auctions, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 97 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 304 tests.

### Session 84 (May 21, 2026)
- Added Java `STR_MSG_HOUSING_OVERDUE` and `STR_MSG_HOUSING_SEQUESTRATE` system-message helpers.
- Wired the enter-world housing tail through Java `HousingService.onPlayerLogin` maintenance notice behavior: overdue active houses receive the maintenance-due message, and players without an active house receive the seized-house message when unread final/third overdue system mail exists.
- Added packet tests for the new system-message IDs and the housing login notice selection rules, including pay-disabled suppression for active-house overdue notices.
- Current gaps in this cluster: full `MaintenanceTask` overdue mail generation, impound/auction behavior, scheduled auction-end settlement, startup recovery around prolonged auctions, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 98 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 305 tests.

### Session 85 (May 21, 2026)
- Added strict previous-run support to the weekly Java cron helper so C# can model `AbstractCronTask.findLastPlannedRun` for housing schedules.
- Added `HouseAuctionTimingService.ShouldRunAuctionEndOnStartup`, mirroring Java `AuctionEndTask.shouldRunOnStart`: run when the server was down across the planned auction end, or when it stopped within thirty minutes after the last planned auction end.
- Added timing tests for the previous-run helper and the startup recovery/prolongation window.
- Current gaps in this cluster: scheduled `AuctionEndTask.endAuction` execution, full `MaintenanceTask` overdue mail generation and impound/auction behavior, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 100 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 307 tests.

### Session 86 (May 21, 2026)
- Added `HouseMaintenanceTimingService` for Java `MaintenanceTask` timing parity, including configured weekly maintenance cron handling and `calculateImpoundDate` behavior.
- Added Java `MailFormatter.sendHouseMaintenanceMail` overdue stage selection for `$$HS_OVERDUE_1ST`, `$$HS_OVERDUE_2ND`, and `$$HS_OVERDUE_3RD` senders.
- Routed `CM_HOUSE_PAY_RENT` due-date advancement and four-week paid cap checks through the shared maintenance timing service.
- Current gaps in this cluster: full `MaintenanceTask` overdue mail persistence/delivery, impound/auction behavior, scheduled `AuctionEndTask.endAuction` execution, seller/buyer result mail beyond failed-bid refunds, house sign/appearance fanout, auto-fill, and live-client packet timing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 103 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 310 tests.

### Session 87 (May 21, 2026)
- Filled the previously reserved `SM_FRIEND_LIST` house address and door-state fields using Java `HousingService.findActiveHouse` parity: studio rows win first, otherwise the oldest loaded custom house is active.
- Extended loaded `PlayerFriend` snapshots with active-house state from DB `houses.settings`, and refresh online reciprocal friend snapshots when friend status changes or a friend request is accepted.
- Updated packet coverage so friend-list serialization asserts the nonzero house address and friends-only door byte.
- Current gaps in this cluster: offline social request handling and generic `ResponseRequester` support beyond buddy requests remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 103 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 310 tests.

### Session 88 (May 21, 2026)
- Extended `PlayerSettings` with Java `DeniedStatus` deny/display integer fields and loaded `player_settings` rows `-1` and `-2` alongside the existing client setting blobs.
- Routed `CM_FRIEND_ADD` through Java's target-side `DeniedStatus.FRIEND` guard after list-full checks and before question-window creation, sending `STR_MSG_REJECTED_FRIEND`.
- Added focused mask coverage for `PlayerSettings.DeniesFriendRequests`.
- Current gaps in this cluster: offline social request handling and generic `ResponseRequester` support beyond buddy requests remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 104 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 311 tests.

### Session 89 (May 21, 2026)
- Registered and parsed Java `CM_UI_SETTINGS` opcode `10` with the Java type/unused-size/data layout.
- Routed settings updates into the loaded `PlayerSettings` blobs for UI settings, shortcuts, and house buddies.
- Added logout persistence for Java `PlayerSettingsDAO.saveSettings`, including setting blobs plus display and deny integer rows.
- Added parser coverage for `CM_UI_SETTINGS`.
- Current gaps in this cluster: exact periodic save timing remains pending with the broader periodic save task.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 105 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 312 tests.

### Session 90 (May 21, 2026)
- Registered and parsed Java `CM_CUSTOM_SETTINGS` opcode `12` for display and deny bitmasks.
- Added Java-shaped `SM_CUSTOM_SETTINGS` opcode `184` and routed custom setting changes through self/visible-player fanout.
- Reused the logout settings persistence path so updated display/deny rows are saved with the rest of `PlayerSettingsDAO.saveSettings` parity.
- Added packet coverage for both the client parser and server response payload.
- Current gaps in this cluster: exact persistent known-list fanout remains pending with the broader known-list work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 91 (May 21, 2026)
- Loaded and persisted Java `PlayerCommonData.note` via the `players.note` column during enter-world/logout saves.
- Registered and parsed Java `CM_SET_NOTE` opcode `58`, updating the active player's note only when it changes.
- Added Java-shaped `SM_UPDATE_NOTE` opcode `104`, plus friend-list refreshes for online friends and visible-player fanout matching `PacketSendUtility.broadcastPacketAndReceive`.
- Filled note fields in `SM_CHAT_WINDOW` personal info and `SM_PLAYER_INFO`, alongside the recently ported display/deny settings fields.
- Current gaps in this cluster: exact persistent known-list fanout remains pending with the broader known-list work; chat group/alliance chat-window branches and exact name-tag parsing are still pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 92 (May 21, 2026)
- Filled Java `SM_PLAYER_INFO` target and active-house tail fields using loaded `Player.TargetObjectId` and the first non-inactive loaded `PlayerHouse`.
- Extended packet coverage to read through the full `SM_PLAYER_INFO` tail, asserting abyss rank, target, team/mentor defaults, active-house address, membership baseline, and CP placeholders.
- Current gaps in this cluster: real team ID, mentor flag/state, CP info, transforms, exact calculated stats, and viewer-specific enemy race handling remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 93 (May 21, 2026)
- Added `Player.AccountMembership` and set it from the authenticated LoginServer membership byte when a player enters world.
- Updated Java `SM_CHAT_WINDOW` personal info to write the target membership/VIP byte instead of the previous zero placeholder.
- Updated Java `SM_PLAYER_INFO` membership marker to write `1` for normal accounts and `3 + membership` for membership accounts.
- Current gaps in this cluster: real group/alliance chat-window branches, CP info, team/mentor state, and persistent KnownList membership remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 94 (May 21, 2026)
- Added loaded `Player.LegionId` and `Player.LegionName` from `legion_members` joined to `legions` during enter-world common-data loading.
- Updated Java `SM_CHAT_WINDOW` personal info to write the target legion name instead of the previous empty placeholder.
- Current gaps in this cluster: `SM_PLAYER_INFO` legion emblem/title payload, real group/alliance chat-window branches, persistent KnownList membership, and exact name-tag parsing remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 95 (May 21, 2026)
- Registered Java `CM_HEADING_UPDATE` opcode `147` and parsed the single heading byte sent by the spin packet.
- Routed the packet through `GameServerConnection` as an intentional no-op, matching Java `CM_HEADING_UPDATE.runImpl`.
- Current gaps in this cluster: movement anti-hack, protection/fall/glide side effects, exact flying-state gates, and full known-list movement state remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 96 (May 21, 2026)
- Loaded `legion_emblems` visible fields with enter-world legion data, including Java `LegionEmblemType` values (`DEFAULT=0`, `CUSTOM=0x80`).
- Updated `SM_PLAYER_INFO` to write Java's legion member block when the player has loaded legion data, falling back to the existing 12-byte empty block otherwise.
- Extended packet coverage to assert legion ID, emblem ID/type/colors, and legion name before the HP/DP/equipment section.
- Current gaps in this cluster: custom emblem data transfer packets, transforms, exact calculated stats, private store, team/mentor, CP info, and viewer-specific enemy race handling remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 97 (May 21, 2026)
- Registered Java `CM_REJECT_REVIVE` opcode `146`.
- Added the empty-payload parser and routed it through `GameServerConnection` as an intentional no-op, matching Java `CM_REJECT_REVIVE.readImpl/runImpl`.
- Current gaps in this cluster: full resurrection/revive services and related effect-state cleanup remain pending with combat/effect systems.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 106 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 313 tests.

### Session 98 (May 21, 2026)
- Registered and parsed Java `CM_MACRO_CREATE` opcode `175` and `CM_MACRO_DELETE` opcode `176`.
- Added Java-shaped `SM_MACRO_RESULT` opcode `232` for created/deleted responses.
- Added macro mutation helpers mirroring `PlayerService.addMacro/removeMacro`, updating loaded `Player.Macros` and persisting `player_macrosses` with Java `PlayerMacrosDAO` add/update/delete parity.
- Current gaps in this cluster: exact Java invalid macro ID exception behavior is guarded as a safe no-op; broader macro UI refresh is still login/list-packet based like the current C# surface.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 108 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 315 tests.

### Session 99 (May 21, 2026)
- Registered and parsed Java `CM_REVIVE` opcode `5`, `CM_QUESTIONNAIRE` opcode `145`, `CM_START_LOOT` opcode `154`, `CM_LOOT_ITEM` opcode `155`, `CM_SUBZONE_CHANGE` opcode `163`, and `CM_CHANGE_CHANNEL` opcode `172`.
- Routed the new packet surfaces through `GameServerConnection` with explicit Java parity breadcrumbs and deferred no-op behavior until `PlayerReviveService`, `HTMLService`, `DropService`, zone revalidation, and channel teleport systems are ported.
- Added packet factory coverage for each Java field layout and invalid-state rejection.
- Current gaps in this cluster: actual revive actions, questionnaire reward grants, loot list/item mutation, zone admin/debug messaging, and channel teleport side effects remain pending with their owning gameplay systems.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 109 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 316 tests.

### Session 100 (May 21, 2026)
- Added a C# `ServerVariablesDAO` parity repository for Java `server_variables` load/store access, including `loadInt`, `loadLong`, and `REPLACE INTO server_variables`.
- Updated `GameTimeService` to load Java `server_variables.time` on bootstrap, periodically save the current game-time minutes, and save again during shutdown.
- Registered the MySQL-backed server-variable repository in GameServer DI and added focused bootstrap coverage for time recovery plus periodic store.
- Then-current gaps in this cluster: Java's periodic world broadcast of `SM_GAME_TIME` to all players was still pending until Session 101; broader player/inventory dirty-save periodic tasks remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 110 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 317 tests.

### Session 101 (May 21, 2026)
- Added a world-wide packet fanout helper to `GameClientSocketServer`, matching Java `PacketSendUtility.broadcastToWorld`.
- Wired `GameTimeService`'s periodic update to broadcast Java-shaped `SM_GAME_TIME` to all online players before saving `server_variables.time`.
- Extended the game-time recovery/save test to assert the periodic `SM_GAME_TIME` broadcast path.
- Current gaps in this cluster: broader player/inventory dirty-save periodic tasks remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 110 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 317 tests.

### Session 102 (May 21, 2026)
- Added `PeriodicSaveService` as a GameEngine bootstrap service, matching Java `PeriodicSaveService` startup scheduling.
- Ported Java `ServerRunTimeSaveTask` behavior by periodically storing `server_variables.serverLastRun` with the current Unix milliseconds and storing it again on shutdown.
- Added focused coverage for periodic store plus shutdown store using the C# `ServerVariablesDAO` parity repository interface.
- Current gaps in this cluster: Java legion warehouse periodic saves and player general/item dirty-save tasks remain pending until those storage and dirty-state models are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 111 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 318 tests.

### Session 103 (May 21, 2026)
- Registered and parsed Java `CM_INSTANCE_INFO` opcode `192`, including the ignored `readD()` field and update-type byte.
- Routed the no-team/current-player branch through Java-shaped `SM_INSTANCE_INFO(updateType, player)` using the loaded static instance cooldown table.
- Added packet factory coverage for the request layout and invalid-state rejection.
- Current gaps in this cluster: Java's team-leader/member split responses for `updateType=1` remain pending until team membership models are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 112 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 319 tests.

### Session 104 (May 21, 2026)
- Registered Java `CM_SHOW_RESTRICTIONS` opcode `194`, the bodyless `/restriction` packet.
- Routed it through `GameServerConnection` to send `SM_SYSTEM_MESSAGE.STR_MSG_ACCUSE_INFO_NORMAL`, preserving Java's current normal-state response and documenting the future accusation-level variants.
- Added system-message ID coverage for `1400076` and packet factory coverage for in-game parsing plus invalid-state rejection.
- Current gaps in this cluster: actual accusation/restriction-level tracking and non-normal `STR_MSG_ACCUSE_INFO_*_LEVEL` responses remain pending until report/restriction state is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 113 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 320 tests.

### Session 105 (May 21, 2026)
- Registered Java `CM_CHECK_NICKNAME` opcode `177` for authenticated character-selection sessions and added Java-shaped `SM_NICKNAME_CHECK_RESPONSE` opcode `233`.
- Reused the C# character creation validation path to mirror Java `CM_CHECK_NICKNAME.runImpl`: `Util.convertName`, `PlayerService.isNameUsedOrReserved`, `NameRestrictionService.isValidName`, `NameRestrictionService.isForbidden`, then the one-byte creation response code.
- Added packet coverage for nickname request parsing/state rejection, response serialization, and service coverage for used, reserved, invalid, forbidden, and OK nickname responses.
- Current gaps in this cluster: this path still relies on the existing C# approximation of Java name reservation lookup; live-DB opt-in coverage remains pending with the broader creation/enter-world fixtures.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 114 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 321 tests.

### Session 106 (May 21, 2026)
- Registered and parsed Java `CM_REPORT_PLAYER` opcode `191`, including the unsigned report-type byte and reported-player name.
- Added Java system-message helpers for `STR_MSG_DO_NOT_ACCUSE`, `STR_INVALID_TARGET`, `STR_MSG_ACCUSE_SUBMIT`, and `STR_MSG_ACCUSE_COUNT_INFO`.
- Routed report type `0` through the current Java guard order for online cross-race/self checks, then returns the Java submit response with the current unlimited-report marker; report type `1` returns the Java count-info response.
- Current gaps in this cluster: real accusation counters, restrictions, audit persistence, offline target lookup nuances, and report abuse policy remain pending until the report/restriction subsystem is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 115 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 322 tests.

### Session 107 (May 21, 2026)
- Registered and parsed Java utility packet layouts for `CM_TELEPORT_ANIMATION_DONE` opcode `15`, `CM_CHECK_PAK` opcode `62`, `CM_PLAY_MOVIE_END` opcode `81`, `CM_SHOW_MAP` opcode `196`, and `CM_CHECK_MAIL_UNK` opcode `213`.
- Routed each through `GameServerConnection` with explicit Java parity breadcrumbs and deferred behavior for teleport task execution, pak audit logging, quest/instance movie-end hooks, Conqueror/Protector map scans, and Java's TODO mail-shop packet.
- Added combined packet factory coverage for field layouts and invalid-state rejection.
- Current gaps in this cluster: actual teleport task execution, audit policy persistence, cutscene quest/instance callbacks, Conqueror/Protector intruder scans, and any future Java behavior for `CM_CHECK_MAIL_UNK` remain pending with their owning systems.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 116 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 323 tests.

### Session 108 (May 21, 2026)
- Registered Java `CM_TIME_CHECK_QUIT` opcode `209` as a C# subclass of `CmTimeCheck`, matching Java's `CM_TIME_CHECK_QUIT extends CM_TIME_CHECK` inheritance.
- Reused the existing `CM_TIME_CHECK` parser and `GameServerConnection` response sequence (`SM_AFTER_TIME_CHECK_4_7_5`, then `SM_TIME_CHECK(nanoTime)`).
- Extended packet factory coverage for the opcode `209` in-game state and authed-state rejection.
- Current gaps in this cluster: none specific to this packet; it intentionally shares the already ported time-check behavior.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 116 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 323 tests.

### Session 109 (May 21, 2026)
- Registered Java `CM_SECURITY_TOKEN` opcode `92` across connected, authenticated, and in-game states.
- Added Java-shaped `SM_SECURITY_TOKEN` opcode `152`, writing region byte `0`, the token bytes, and the zero-filled mirror block.
- Added per-connection token generation matching Java `SecurityTokenService.generateToken` shape: 16 random bytes encoded as Base64, returned once an account is authenticated.
- Current gaps in this cluster: Java stores the token on the account object; the C# path keeps it per connection until a fuller account/session object is ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 116 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 323 tests.

### Session 110 (May 21, 2026)
- Registered and parsed Java `CM_RECIPE_DELETE` opcode `89`.
- Added Java-shaped `SM_RECIPE_DELETE` opcode `242`.
- Added `PlayerEnterWorldService.DeleteRecipeAsync` plus `PlayerRecipesDAO.delRecipe` parity repository deletion from `player_recipes`, updating the loaded `Player.Recipes` collection only after persistence succeeds.
- Routed the packet through Java `RecipeList.deleteRecipe` behavior: ignore missing recipes, otherwise delete and send the remove-recipe packet.
- Current gaps in this cluster: recipe acquisition/removal side effects from crafting, quests, and relinquish-craft flows remain pending beyond this direct client deletion path.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 118 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 325 tests.

### Session 111 (May 21, 2026)
- Registered and parsed Java `CM_OBJECT_SEARCH` opcode `11`, `CM_POSITION_SELF` opcode `17`, `CM_PLAYER_LISTENER` opcode `40`, and `CM_DELETE_QUEST` opcode `80`.
- Routed each through `GameServerConnection` with Java parity breadcrumbs and deferred behavior for spawn search, position-self acknowledgement, web rewards, and quest abandonment.
- Extended combined packet factory coverage for each field layout and invalid-state rejection.
- Current gaps in this cluster: `SPAWNS_DATA` nearest-NPC search plus `SM_SHOW_NPC_ON_MAP`, web reward delivery, timed quest cancellation, `SM_QUEST_ACTION`, and `QuestService.abandonQuest` remain pending with their owning systems.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 118 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 325 tests.

### Session 112 (May 21, 2026)
- Registered and parsed Java `CM_CLIENT_COMMAND_ROLL` opcode `107`.
- Added Java `SM_SYSTEM_MESSAGE` helpers for `STR_MSG_DICE_CUSTOM_ME` and `STR_MSG_DICE_CUSTOM_OTHER`.
- Routed `/roll` through Java behavior: non-positive max rolls become `100`, the sender receives the self roll message, and visible players receive the other-player roll message.
- Current gaps in this cluster: exact Java RNG stream is not shared; the C# path preserves the same inclusive range and packet side effects.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passes with 119 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx` passes with 326 tests.

### Session 113 (May 21, 2026)
- Extended `ItemTemplateSummary` and static-data loading with Java `attack_type`, `<weapon_stats>`, and direct template stat modifiers.
- Threaded static item templates into the enter-world stats path and updated `SM_STATS_INFO` to apply first-pass equipped item template weapon stats/modifiers to current stats on login.
- Added focused coverage for real static item stat parsing and packet-level equipped weapon/armor stat application.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, mana stones, fusion/random bonuses, item sets, enchantment, tempering, conditioning, armor mastery, title/skill/effect stats, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 120 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 114 (May 21, 2026)
- Extended the `SM_STATS_INFO` equipment bridge with Java `ItemEquipmentListener.addStonesStats` parity for socketed mana stones and fusion stones, using each stone item template's direct stat modifiers.
- Extended packet coverage so socketed stones affect current max HP, physical accuracy, and magical boost, and static-data coverage now proves real manastone template modifiers load from Java XML.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, fusion weapon item stats, random bonuses, item sets, enchantment, tempering, conditioning, idian/godstone effects, armor mastery, title/skill/effect stats, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 115 (May 21, 2026)
- Extended the `SM_STATS_INFO` equipment bridge with Java `ItemEquipmentListener.addWeaponStats` fusioned-weapon parity: fusion template modifiers apply except Java-excluded attack speed, PvP attack ratio, and casting-speed modifiers.
- Added the Java 10% fusioned weapon stat bonuses for attack and magical boost, preserving the main weapon attack-type decision for physical versus magical attack.
- Extended packet coverage so a fusioned staff contributes physical attack through the main physical weapon, contributes 10% magical boost, and does not leak excluded fusion attack-speed modifiers into current attack speed.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, random bonuses, item sets, enchantment, tempering, conditioning, idian/godstone effects, armor mastery, title/skill/effect stats, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter SmStatsInfo_AppliesEquippedItemTemplateStats` passes with 1 test.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 116 (May 21, 2026)
- Added a C# `ItemRandomBonusTable` parity holder for Java `ItemRandomBonusData`, loading `random_bonus` sets and their 1-based modifier groups from `item_random_bonuses.xml`.
- Exposed item template stat bonus set IDs from Java `rnd_bonus` attributes and threaded `StaticData.ItemRandomBonuses` into the enter-world `SM_STATS_INFO` path.
- Extended the equipment bridge with Java `RandomBonusEffect` parity for selected inventory and fusion random bonus modifiers from loaded `rnd_bonus`/`fusion_rnd_bonus` item rows.
- Extended static-data coverage for real random bonus modifiers and packet coverage for base-item and fusion-item random bonus effects on current stats.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, item sets, enchantment, tempering, conditioning, idian/godstone effects, armor mastery, title/skill/effect stats, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 117 (May 21, 2026)
- Added a C# `ItemSetTable` parity holder for Java `ItemSetData`, including set lookup by set ID and by item ID from `item_sets.xml`.
- Loaded Java item-set `itempart`, `partbonus`, and `fullbonus` stat modifiers into `StaticData.ItemSets`, with full-bonus counts matching Java `ItemSetTemplate.afterUnmarshal`.
- Extended the `SM_STATS_INFO` equipment bridge with Java `ItemEquipmentListener.recalculateItemSet` / `Equipment.itemSetPartsEquipped` parity: equipped set parts are counted once per item ID, alternate weapon-set slots are skipped, qualifying part bonuses apply, and full bonuses apply only when all set parts are equipped.
- Added static-data coverage for real Java set ID `2` and packet coverage proving item-set part/full bonuses affect current HP and physical defense.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, enchantment, tempering, conditioning, idian/godstone effects, armor mastery, title/skill/effect stats, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 118 (May 21, 2026)
- Added a C# `EnchantTable` parity holder for Java `EnchantData`, resolving enchant templates by item `enchant_name` first and item group otherwise.
- Extended static-data loading for Java `enchant_templates.xml`, including `enchant_list`, `enchant_data`, and `enchant_stat` rows, and exposed `ItemTemplateSummary.EnchantName`.
- Extended the `SM_STATS_INFO` equipment bridge with Java `EnchantService.applyEnchantEffect` / `EnchantEffect` parity for equipped items with `enchant > 0`, including Java's level-21 limitless bonus behavior and main-hand-only magical-boost enchant rule.
- Added static-data coverage proving real Java sword enchant modifiers load and packet coverage proving named enchant-template stats affect current physical attack and physical accuracy.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, exact slot-owned stat routing for off-hand attack enchant effects, tempering, conditioning, idian/godstone effects, armor mastery, title/skill/effect stats, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 119 (May 21, 2026)
- Added a C# `TemperingTable` parity holder for Java `TemperingData`, resolving regular accessory templates by item `tempering_name` first and item group otherwise.
- Extended static-data loading for Java `tempering_templates.xml`, including `tempering_list`, `tempering_data`, and `tempering_stat` rows, and exposed `ItemTemplateSummary.TemperingName`.
- Extended the `SM_STATS_INFO` equipment bridge with Java `TemperingEffect.apply` parity for equipped items with `tempering > 0`.
- Added Java plume special-case parity from `TemperingEffect.addPlumeStatFunctions` and `PlumStatEnum`: physical plumes add physical attack plus HP, magical plumes add magical boost plus HP, and random plume bonus is included in the primary stat.
- Added static-data coverage for real Java tempering templates and physical plume formulas, plus packet coverage proving tempering stats affect current HP, physical defense, and magical resist.
- Current gaps in this cluster: full Java `CreatureGameStats`/`Stat2` parity, conditioning, idian/godstone effects, armor mastery, title/skill/effect stats, exact slot-owned stat routing for off-hand attack enchant effects, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 120 (May 21, 2026)
- Parsed Java `<conditions><charge value="..."/>` under item-template stat modifiers into `ItemStatModifier.ChargeCondition`.
- Updated the `SM_STATS_INFO` equipment bridge to mirror Java `StatFunction.validate` / `ItemChargeCondition`: charge-gated item modifiers apply only when the equipped item's charge points reach Java charge level 1 or 2, using the `ChargeInfo.LEVEL1 = 500000` threshold.
- Added static-data coverage proving real Java conditioned dagger `100201371` keeps its charge condition, and packet coverage proving level-1 charge applies level-1 modifiers while level-2 modifiers stay inactive at exactly `500000` charge points.
- Current gaps in this cluster: charge burn observers, item charge service/payment/update packet flow, idian/godstone effects, armor mastery, title/skill/effect stats, full Java `CreatureGameStats`/`Stat2`, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 121 (May 21, 2026)
- Parsed Java idian item action `<polish set_id="..."/>` into `ItemTemplateSummary.PolishSetId`.
- Extended the `SM_STATS_INFO` equipment bridge with Java `IdianStone.onEquip` / `RandomBonusEffect(StatBonusType.POLISH)` parity: charged idians attached to main-hand weapons apply the selected POLISH random-bonus modifier group to current stats.
- Added static-data coverage proving real Java idian `166050001` maps to polish set `3` and that POLISH random-bonus set `3` loads its HP modifier.
- Added packet coverage proving a charged main-hand idian contributes HP/MP current-stat modifiers through the same Java random-bonus table path.
- Current gaps in this cluster: idian charge burn observers, polish item-use action, item_stones mutation/persistence after burn-out, inventory update packets, godstone combat procs, armor mastery, full Java `CreatureGameStats`/`Stat2`, and equip/unequip recompute fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "SmStatsInfo_AppliesEquippedItemTemplateStats|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 122 (May 21, 2026)
- Parsed Java `<godstone>` item-template metadata into `ItemGodstoneInfo`, mirroring `model/templates/item/GodstoneInfo` fields for skill ID, skill level, main/off-hand probabilities, break probability, and non-break count.
- Added static-data coverage proving real Java godstone item `168000001` loads its proc skill `8255`, skill level `1`, and probability `1000`.
- Current gaps in this cluster: `GodStone.tryActivate`, `CreatureController.applyGodStoneEffect`, proc cooldown/rate/reduce-stat handling, illusion godstone break persistence, and SkillEngine execution remain deferred until combat/skill systems are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 1 test.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 327 tests.

### Session 123 (May 21, 2026)
- Created `docs/Phase-6E-Completion.md` as the next handoff, carrying forward the Java-source-of-truth rule, 6D completed equipment-stat work, validation baseline, and the next unit queue.
- Next best focused units: `CM_CHARGE_ITEM` / `ItemChargeService`, idian polish burn/update persistence, armor mastery after choosing a skill/effect strategy, equip/unequip recompute fanout, or housing/known-list work.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 327 tests from Session 122.

### Session 124 (May 21, 2026)
- Registered and parsed Java `CM_CHARGE_ITEM` opcode `78`.
- Parsed Java item-template `<improve>` metadata plus `uselimits.recommend_rank` into `ItemTemplateSummary`, preserving the Java `Improvement` fields used by conditioning.
- Added `ItemChargeService` parity for Java conditioning price math, rank-limited available charge level, fusion-template improvement lookup, and target charge point calculation.
- Routed selected cube-item conditioning through Java guard order: current target check, inventory item lookup, kinah/AP payment, `inventory.charge` persistence, AP rank mutation persistence when charge way `2` is used, Java charge-bar-step inventory updates, success messages, all-complete messages, and `SM_STATS_INFO` refresh.
- Added Java `ItemUpdateType.CHARGE` behavior to `SM_INVENTORY_UPDATE_ITEM`: conditioning-only blob with no trailing update type.
- Current gaps in this cluster: equipped-item "charge all" dialog/action flow, Java charge burn observers, item-use charge consumables, idian polish mutation/burn persistence, rank-limit equipment recheck on AP-rank change, and full equipment stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 124 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 331 tests.

### Session 125 (May 21, 2026)
- Registered and parsed Java `CM_USE_ITEM` opcode `37` for the target-item branch used by idian polish actions.
- Parsed Java item-template `<idian burn_attack="..." burn_defend="...">` into `ItemTemplateSummary.IdianInfo`, and extended `ItemRandomBonusTable` with Java `selectRandomBonusNumber` weighted modifier-group selection.
- Added `IdianPolishService` for Java `PolishAction` and `IdianStone.decreasePolishCharge` parity decisions: polish target validation, source idian consumption, new `PlayerIdianStone` creation at `1000000` charge, low-charge threshold detection, and zero-charge idian removal.
- Routed the implemented polish branch through inventory state mutation and persistence: source count update/delete, idian row delete/insert in `item_stones` category `3`, Java-shaped `SM_DELETE_ITEM`/`SM_INVENTORY_UPDATE_ITEM`, polish success/failure system messages, item-use animation completion, and equipped-target `SM_STATS_INFO` refresh.
- Added Java `ItemUpdateType.POLISH_CHARGE` behavior to `SM_INVENTORY_UPDATE_ITEM`: polish-only blob with no trailing update type.
- Current gaps in this cluster: exact Java 5s item-use task/cancel observer/cooldown behavior, identify and attack-mode guards once those item/player flags exist, `PolishChargeCondition` skill hook-in, `IdianStone.onEquip` attack/defend observer burn triggers, and combat-driven persistence fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 128 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 335 tests.

### Session 126 (May 21, 2026)
- Parsed Java `<actions><charge capacity="..."/>` into `ItemTemplateSummary.ChargeActionMaxLevel` for conditioning consumables.
- Extended `CM_USE_ITEM` routing with the Java `ChargeAction` branch: source charge item lookup, equipped-item filtering by matching `Improvement.ChargeWay`, rank-limited target level via `ItemChargeService`, source item count/delete mutation, charge persistence for all updated equipped items, Java charge-only update blobs, charge success/all-complete messages, item-use animation completion, and `SM_STATS_INFO` refresh.
- Added repository/service persistence for the compound charge-action mutation so source consumption and equipped-item `inventory.charge` updates commit together.
- Current gaps in this cluster: exact Java 3s item-use task/cancel observer behavior, cooldown observer plumbing, NPC/dialog `ItemChargeService.startChargingEquippedItems` confirmation/payment path, Java charge burn observers, and broader equip/unequip stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 128 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 335 tests.

### Session 127 (May 21, 2026)
- Parsed Java skill-template `armormastery` effects and nested `change` rows into `SkillTemplateSummary`, with breadcrumbs to `ArmorMasteryEffect` and Java change entries.
- Extended the `SM_STATS_INFO` equipment bridge with a limited Java `ArmorMasteryEffect` / `StatArmorMasteryFunction` parity path: learned player skills now apply matching equipped armor subtype and Java slot factors (`30/25/15`) to armor mastery rate/fixed-bonus stat functions.
- Threaded loaded skill templates into login stats packets and added static-data plus packet coverage for real cloth armor mastery metadata and physical-defense output from an equipped cloth torso.
- Current gaps in this cluster: a full SkillEngine/effect-controller/stat-container port, non-armor passive skill effects, exact Java float/stat ordering beyond this bridge, equip/unequip recompute fanout, and dynamic skill learn/unlearn refreshes remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 128 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 335 tests.

### Session 128 (May 21, 2026)
- Registered and parsed Java `CM_DIALOG_SELECT` opcode `54`, including the Java field layout used by NPC dialog actions.
- Routed the narrow Java `DialogService` charge-all actions `CHARGE_ITEM_MULTI` and `CHARGE_ITEM_MULTI2` into `ItemChargeService.startChargingEquippedItems` parity: equipped matching items are filtered, one total kinah/AP payment is calculated, and Java charge-all `SM_QUESTION_WINDOW` confirmation requests are stored on the player.
- Added charge-all question response handling that consumes the pending request, performs the single payment, persists all equipped-item `inventory.charge` updates with kinah/AP mutation, sends charge update blobs/success messages, refreshes `SM_STATS_INFO`, and emits the Java all-complete message.
- Current gaps in this cluster: full NPC known-list lookup, NPC template `supportsAction`/function-dialog validation, dialog distance/protection/audit handling, exact response-requester generalization beyond friend/charge requests, and charge burn observers remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 130 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 337 tests.

### Session 129 (May 21, 2026)
- Parsed Java `player_titles.xml` into a new `TitleTemplateTable`, preserving title ID, race, name/description metadata, and Java modifier rows from `TitleTemplate.getModifiers`.
- Threaded title templates into `SM_STATS_INFO` and applied Java `TitleChangeListener.onBonusTitleChange` parity for the loaded `Player.BonusTitleId`, including the no-equipment case where title modifiers still need the stat pipeline.
- Added static-data coverage for real title `1` and packet coverage proving a bonus title increases current HP and physical defense.
- Current gaps in this cluster: title learn/remove expiration side effects, dynamic bonus-title stat recompute fanout beyond the existing title packet mutation, movement-speed title stat serialization once speed fields are modeled, and full Java `CreatureGameStats`/`Stat2` ordering remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 131 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 338 tests.

### Session 130 (May 21, 2026)
- Parsed Java skill-template `wpnmastery` and `shieldmastery` effects into `SkillTemplateSummary`, preserving weapon group and Java `change` rows beside the existing armor mastery metadata.
- Extended the `SM_STATS_INFO` current-stat bridge with limited Java `WeaponMasteryEffect` / `StatWeaponMasteryFunction` and `ShieldMasteryEffect` / `StatShieldMasteryFunction` parity: learned one-hand weapon mastery now routes to main/off-hand power only when matching weapons are equipped, two-hand mastery remains main-weapon-gated, and shield mastery applies only with an equipped shield.
- Added static-data coverage for real Java sword/shield mastery templates plus packet coverage for dual sword mastery and shield-gated block rate output.
- Current gaps in this cluster: dual-wield skill calculation randomness for Java `CalculationType.SKILL|DUAL_WIELD`, full passive SkillEngine/effect lifecycle, dynamic skill learn/unlearn stat refreshes, and full Java `CreatureGameStats`/`Stat2` ordering remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 133 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 340 tests.

### Session 131 (May 21, 2026)
- Parsed Java skill-template `wpndual` effects into `SkillTemplateSummary`, preserving the `WeaponDualEffect` fields used by Java's dual-wield gate and attack-stat tuning: min-damage ratio value/delta, skill efficiency, max-damage chance, and max-damage delta.
- Added static-data coverage for real Java skill `55` (`Advanced Dual-Wielding I`) so the future `CM_EQUIP_ITEM` gate can ask the skill table instead of re-reading XML.
- Current gaps in this cluster: `WeaponDualEffect.hasDualWieldEffect` integration in equip validation, `PlayerGameStats` skill-efficiency/min-damage-ratio fields, and attack-time dual-wield damage randomization remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 1 test.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 340 tests.

### Session 132 (May 21, 2026)
- Created `docs/Phase-6F-Completion.md` as the next handoff, carrying forward the Java-source-of-truth rule, the 6E charge/idian/dialog/stat work, the latest 340-test validation baseline, and the next unit queue.
- Next best focused units: `CM_EQUIP_ITEM` with the parsed weapon-dual gate, charge/idian burn observers, broader passive skill/effect stat strategy, housing/known-list work, or full NPC/dialog validation.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 340 tests from Session 131.

### Session 133 (May 21, 2026)
- Registered and parsed Java `CM_EQUIP_ITEM` opcode `38`, preserving the Java payload order `action`, `slotRead`, and `itemObjId`.
- Added a first-pass `EquipmentService` for Java `Equipment.equipItem`, `unEquipItem`, and `switchHands` routing: one-hand weapons are forced to main hand unless learned `wpndual` metadata proves `WeaponDualEffect.hasDualWieldEffect`, two-hand weapons occupy main/sub hand, colliding slots are unequipped, inventory-full rejection is modeled for unequip and two-hand swaps, and switch-weapons toggles active/off weapon slots.
- Added equipment mutation persistence for `inventory.is_equipped` and `inventory.slot`, Java `ItemUpdateType.EQUIP_UNEQUIP` inventory update packets, post-change `SM_STATS_INFO` refresh, and Java-shaped `SM_UPDATE_PLAYER_APPEARANCE` opcode `36` visible-equipment fanout.
- Added focused coverage for parser registration, one-hand/dual-wield gates, colliding-slot unequip, full-inventory rejection, and two-hand appearance mask serialization.
- Current gaps in this cluster: full Java equip guards for class/level/max-level/race/gender/AP-rank, item required equip skills, StigmaService, soul-bind confirmation, identified-item checks, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|ClientPacketFactory_ParsesEquipItem|SmMailService_WritesJavaShapedAttachmentAndInventoryPackets"` passes with 6 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 346 tests.

### Session 134 (May 21, 2026)
- Extended static item-template parsing with Java `restrict` required-level arrays, `restrict_max` max-level arrays, `<uselimits gender>`, and rank min/max metadata while preserving the existing class-restriction helper behavior.
- Added Java `Equipment.equipItem` validation for class, required level, max level, race, and gender before slot mutation, returning structured equip failure reasons that `GameServerConnection` maps to Java-shaped `SM_SYSTEM_MESSAGE` IDs.
- Added packet helpers for `STR_CANNOT_USE_ITEM_INVALID_CLASS`, `STR_CANNOT_USE_ITEM_TOO_LOW_LEVEL_MUST_BE_THIS_LEVEL`, `STR_CANNOT_USE_ITEM_INVALID_RACE`, `STR_CANNOT_USE_ITEM_INVALID_GENDER`, and `STR_CANNOT_USE_ITEM_TOO_HIGH_LEVEL`.
- Added focused coverage for equip validation failures, static-data level/gender/rank parsing, and the new system-message packet IDs/parameter order.
- Current gaps in this cluster: AP-rank guard/l10n, item required equip skills, StigmaService, soul-bind confirmation, identified-item checks, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|StaticDataLoadingTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 13 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 351 tests.

### Session 135 (May 21, 2026)
- Added Java `ItemUseLimits.verifyRank` parity to `ItemTemplateSummary` and wired `Equipment.equipItem` AP-rank rejection after the gender guard.
- Added a Java-shaped `AbyssRankEnum.getRankL10n` / `ChatUtil.l10n` helper to `PlayerAbyssRank`, so the invalid-rank system message carries the same localized client token as Java.
- Added `STR_CANNOT_USE_ITEM_INVALID_RANK` packet helper and mapped structured equip invalid-rank failures through `GameServerConnection`.
- Added focused coverage for rank-limited equip rejection and the invalid-rank system message parameter.
- Current gaps in this cluster: item required equip skills, StigmaService, soul-bind confirmation, identified-item checks, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 11 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 352 tests.

### Session 136 (May 21, 2026)
- Added Java `ItemGroup.getRequiredSkills` parity as an item-group skill map on `ItemTemplateSummary`, covering the Java weapon and armor group skill IDs used by `Equipment.checkAvailableEquipSkills`.
- Wired the required equip-skill guard into `EquipmentService` after the inventory-slot availability check and before slot mutation, preserving Java's silent no-message failure behavior.
- Added focused coverage for missing required equip skills and static-data coverage proving real Java sword templates expose `[37, 44]`.
- Current gaps in this cluster: StigmaService, soul-bind confirmation, identified-item checks, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|DataManager_LoadsRealJavaStaticDataManifestCounts"` passes with 13 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 353 tests.

### Session 137 (May 21, 2026)
- Created `docs/Phase-6G-Completion.md` as the next handoff, carrying forward the Java-source-of-truth rule, the 6F `CM_EQUIP_ITEM` mutation/guard work, the latest 353-test validation baseline, and the next unit queue.
- Next best focused units: identified item-state modeling plus equip identify guard, soul-bind confirmation foundation, StigmaService equip/unequip slices, charge/idian burn observers, broader passive skill/effect stat strategy, housing/known-list work, or full NPC/dialog validation.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 353 tests from Session 136.

### Session 138 (May 21, 2026)
- Added an `InventoryItem.IsIdentified` parity surface backed by Java `Item.isIdentified()` / `tune_count != -1`.
- Wired Java's silent unidentified-item rejection into `CM_EQUIP_ITEM` before equip mutation, leaving the no-message behavior intact.
- Applied identified-state hiding in item-info blobs for optional sockets, enchant bonus, random bonus, and tune count, matching Java `EnchantInfoBlobEntry` and `PremiumOptionInfoBlobEntry`.
- Extended idian polish validation with Java `PolishAction.canAct` identified-target denial and the corresponding `STR_MSG_POLISH_NEED_IDENTIFY` system message.
- Current gaps in this cluster: StigmaService, soul-bind confirmation, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|IdianPolishServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 18 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 356 tests.

### Session 139 (May 21, 2026)
- Added Java `ItemTemplate.isSoulBound` mask parity and a `PendingSoulBindRequest` response-request model for unbound soul-bound equipment.
- Routed `CM_EQUIP_ITEM` soul-bound equipment through Java-shaped `SM_QUESTION_WINDOW` code `95006`, duplicate request retry messaging, accept/cancel response handling, soul-bind start/finish `SM_ITEM_USAGE_ANIMATION`, success/cancel system messages, `inventory.is_soul_bound` persistence, and final equip mutation.
- Broadened equipment persistence to store `is_soul_bound` beside `is_equipped`/`slot`, matching Java `InventoryDAO.store` behavior for the modeled fields.
- Current gaps in this cluster: exact Java 5s delayed task timing, movement cancel observer, stance/state denials, StigmaService, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 15 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 358 tests.

### Session 140 (May 21, 2026)
- Parsed Java item-template `<stigma>` metadata into `ItemStigmaInfo` and preserved Java skill-template `stigma` attributes plus skill-tree by-skill lookup helpers used by `StigmaService`.
- Added a first-pass C# `StigmaService` bridge for Java `StigmaService.notifyEquipAction` / `removeStigmaSkills`: regular/advanced slot-count gates from level/quest state, equip Kinah cost, same-slot same-name replacement handling, normal temporary stigma skill add/remove, `SM_SKILL_LIST` stigma learn messages, `SM_SKILL_REMOVE`, stigma-not-enough-Kinah and removed-skill system messages, and Kinah count persistence alongside equipment mutation.
- Wired `CM_EQUIP_ITEM` equip/unequip through the stigma bridge while leaving Java linked-stigma unlock selection, stigma enchant, membership slot overrides, exact hidden-skill messaging, and full SkillEngine effect application as follow-up slices.
- Current gaps in this cluster: linked stigma unlocks, stigma enchant/charge-stone flow, exact Java 5s soul-bind timing, movement cancel observer, stance/state denials, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|StaticDataLoadingTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 21 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 361 tests.

### Session 141 (May 21, 2026)
- Added Java `StigmaService.addLinkedStigmaSkills` / `getLinkedStigmaLearnSkill` parity for the modeled stigma equip path, including the Java class/race/item-ID linked-stigma selection table and minimum-enchant-level linked skill level calculation.
- `CM_EQUIP_ITEM` now adds linked stigma temporary skills when the player has six chargeable equipped stigmas after the equip mutation, using Java `skill_tree` stigma type `4` to emit linked stigma skill type `3` through `SM_SKILL_LIST`.
- Added focused coverage for equipping the sixth chargeable stigma and receiving both the normal stigma skill and Elyos Gladiator linked stigma skill.
- Current gaps in this cluster: stigma enchant/charge-stone flow, membership slot overrides, exact hidden-skill message behavior, full SkillEngine effect application after temporary skill mutations, exact Java 5s soul-bind timing, movement cancel observer, stance/state denials, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|StaticDataLoadingTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 22 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 362 tests.

### Session 142 (May 21, 2026)
- Created `docs/Phase-6H-Completion.md` as the next handoff, carrying forward the Java-source-of-truth rule, the identified-item/soul-bind/stigma work from 6G, the latest 362-test validation baseline, and the next unit queue.
- Next best focused units: stigma enchant/charge-stone flow, membership stigma-slot overrides, exact soul-bind timing/cancel/stance behavior, charge/idian burn observers, broader passive skill/effect stat strategy, housing/known-list work, or full NPC/dialog validation.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 362 tests from Session 141.

### Session 143 (May 21, 2026)
- Registered and parsed Java `CM_MANASTONE` opcode `74`, including the Java action branches for add/enchant/godstone/amplification payloads and the remove-manastone NPC payload shape.
- Added a first-pass `StigmaService.chargeStigma` parity path for matching charge stones: Java success chance math, mismatched item-ID/enchant/max-level/chargeable guards, charge-stone consume, success `inventory.enchant` mutation, failure target deletion, equipped-stigma temporary skill refresh/removal, and Java-shaped `STR_MSG_STIGMA_ENCHANT_SUCCESS` / `STR_MSG_STIGMA_ENCHANT_FAIL` messages.
- Wired the stigma charge branch through `GameServerConnection` with Java-shaped `SM_ITEM_USAGE_ANIMATION`, `SM_INVENTORY_UPDATE_ITEM` `DEC_STIGMA_USE`/default item-use updates, `SM_DELETE_ITEM`, `SM_SKILL_LIST`, `SM_SKILL_REMOVE`, stats refresh for equipped stigmas, and a DB-backed `SaveStigmaChargeMutationAsync` persistence unit.
- Current gaps in this cluster: exact Java 5s delayed task scheduling, movement/cancel observers, item cooldown removal on abort, full non-stigma enchant/manastone/godstone/amplification handling in `CM_MANASTONE`, exact Java hidden-stigma message behavior, and full SkillEngine effect application remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 159 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 366 tests.

### Session 144 (May 21, 2026)
- Parsed Java `MembershipConfig` stigma permission keys `gameserver.quest.stigma.slot` and `gameserver.autolearn.stigma` into `GameServerMembershipOptions` beside the existing character-limit membership keys.
- Wired Java `Player.hasPermission(MembershipConfig.STIGMA_SLOT_QUEST)` parity into `StigmaService.getPossibleStigmaCount` and `getPossibleAdvancedStigmaCount` via loaded `Player.AccountMembership`, allowing membership-qualified players to use all three regular and all three advanced stigma slots without quest gates.
- Threaded the configured stigma-slot membership threshold through `GameServerConnection` into `EquipmentService.ChangeEquipment` for both direct equip and soul-bind-confirmed equip paths.
- Current gaps in this cluster: `STIGMA_AUTOLEARN` login-time temporary skill learning, exact hidden-stigma message behavior, full SkillEngine effect application after temporary skill mutations, and broader account permission surfaces remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 160 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 367 tests.

### Session 145 (May 21, 2026)
- Added Java `StigmaService.onPlayerLogin` membership-autolearn parity for `MembershipConfig.STIGMA_AUTOLEARN`: qualifying players now learn temporary stigma skills from level 20 through their current level using `SkillTreeTable.getTemplatesFor`, preserving normal stigma skill type `1` and linked stigma skill type `3`.
- Wired the login-time autolearn mutation into successful `CM_ENTER_WORLD` handling after account membership is attached and before `SM_ENTER_WORLD_CHECK` / the initial `SM_SKILL_LIST`, so the first skill-list payload reflects Java login state.
- Added focused stigma service coverage for membership-qualified autolearn and the non-member no-op path.
- Current gaps in this cluster: exact hidden-stigma message behavior, full SkillEngine effect application after temporary skill mutations, Java's non-autolearn login validation/unequip pass, and broader account permission surfaces remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 162 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 369 tests.

### Session 146 (May 21, 2026)
- Added Java `StigmaService.removeLinkedStigmaSkills` parity for hidden linked-stigma deletion: linked stigma skills are removed in stack groups, preserving the first/second skill l10n params and removed skill level for `STR_MSG_STIGMA_DELETE_HIDDEN_SKILL`.
- Threaded hidden-stigma delete messages through `StigmaService`, `EquipmentService`, and the `CM_MANASTONE` stigma charge flow, so unequip/replacement/charge-failure paths now emit Java-shaped `SM_SYSTEM_MESSAGE` id `1402895` after skill removals.
- Extended `SM_SYSTEM_MESSAGE` parameter handling to allow Java-style null string params and added system-message, equipment, and stigma-service tests for the hidden linked-stigma notice.
- Current gaps in this cluster: full SkillEngine effect application after temporary skill mutations, Java's non-autolearn login validation/unequip pass, and broader account permission surfaces remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 163 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 370 tests.

### Session 147 (May 21, 2026)
- Added the non-autolearn branch of Java `StigmaService.onPlayerLogin`: normal accounts now validate equipped stigma stones for allowed regular/advanced slots, class specificity, and duplicate same-slot conflicts before packets are sent.
- Invalid equipped login stigmas are copied to cube slot `0`, persisted through `SaveEquipmentMutationAsync`, and reflected in the loaded player inventory before `SM_ENTER_WORLD_CHECK`; valid equipped stigmas rebuild their temporary normal and linked stigma skills for the initial `SM_SKILL_LIST`.
- Preserved Java's `STIGMA_AUTOLEARN` early return: membership-qualified players still autolearn stigma skills and skip the equipped-stigma cleanup branch.
- Current gaps in this cluster: full SkillEngine effect application after temporary skill mutations, exact 5s item-use scheduling/abort observers, and broader account permission surfaces remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 166 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 373 tests.

### Session 148 (May 21, 2026)
- Created `docs/Phase-6I-Completion.md` as the next handoff after the 6H continuation, summarizing Sessions 143-147, the five committed stigma charge/membership/login/delete-message units, the current 373-test validation baseline, and the next focused unit queue.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 373 tests from Session 147.

### Session 149 (May 21, 2026)
- Added Java `CM_MANASTONE` action `3` manastone removal parity: a new C# `ItemSocketService.CreateRemoveManastonePlan` mirrors Java `ItemSocketService.removeManastone` for cube-only targets, normal/fusion `item_stones` slot deletion, Java 650 Kinah removal fee, and remove-option failure/success outcomes.
- Wired the removal branch through `GameServerConnection` after the current target-object guard, added Java-shaped `STR_REMOVE_ITEM_OPTION_*` system messages, sent Kinah and target item-info updates, and added DB-backed `SaveManastoneRemovalMutationAsync` persistence for `item_stones` deletion plus Kinah count mutation.
- Added focused service coverage for normal removal, fusion removal, and Java-shaped failure reasons.
- Current gaps in this cluster: full NPC object/range/template-function validation is still first-pass target-object-only, and non-stigma `CM_MANASTONE` add/enchant/godstone/amplification branches remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 169 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 376 tests.

### Session 150 (May 21, 2026)
- Added Java `CM_MANASTONE` action `4` godstone socketing foundation through `ItemSocketService.CreateSocketGodstonePlan`: cube-only target lookup, equipped-target denial, Java `Item.canSocketGodstone` mask parity, source godstone/template validation, source count/delete mutation, and target `PlayerGodstone` state.
- Wired godstone socketing through `GameServerConnection` with Java-shaped `SM_ITEM_USAGE_ANIMATION` start/finish packets, source consume/delete inventory packets, `STR_GIVE_ITEM_PROC_*` failure/success system messages, and a DB-backed `SaveGodstoneSocketMutationAsync` that replaces category `1` `item_stones` rows and persists source consumption.
- Added focused service coverage for godstone success, single-source deletion, and Java-shaped failure reasons.
- Current gaps in this cluster: exact Java 2s delayed task scheduling, movement cancel observer, item cooldown cleanup on abort, audit logging, and future godstone combat proc activation remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "ItemSocketServiceTests|ClientPacketFactory_ParsesManastone|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 379 tests.

### Session 151 (May 21, 2026)
- Added Java `ItemTemplate.max_enchant` and `ItemTemplate.canExceedEnchant` parsing to `ItemTemplateSummary`, with static-data coverage proving real exceed-capable templates load their max-enchant/amplification flags.
- Added Java `CM_MANASTONE` action `8` amplification foundation through a new C# `EnchantService.CreateAmplificationPlan`: target/material/tool lookup, already-amplified denial, Java `canExceedEnchant` guard, max-enchant-plus-bonus requirement, matching-item or universal-material validation, source count/delete mutation, and target `is_amplified` mutation.
- Wired amplification through `GameServerConnection` with Java-shaped `STR_MSG_EXCEED_*` system messages, source consume/delete inventory packets, target item-info update, and DB-backed `SaveItemAmplificationMutationAsync` persistence for `inventory.is_amplified` plus material/tool consumption.
- Current gaps in this cluster: Java non-stigma manastone socketing, enchant-stone success/failure, supplement use/count math, exact delayed item-use scheduling/cancel observers, and broader enchant buff/stat-effect fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EnchantServiceTests|StaticDataLoadingTests|ClientPacketFactory_ParsesManastone"` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 382 tests.

### Session 152 (May 21, 2026)
- Added Java `ItemTemplate.m_slots` and `ItemTemplate.s_slots` parsing to `ItemTemplateSummary`, with static-data coverage proving real special-slot templates load their normal/special manastone slot counts.
- Added a Java `ItemSocketService.addManaStone` allocator foundation in C#: target/fusion socket-count resolution with the Java six-basic-stone cap, normal-vs-special category counting, next-free-slot selection, normal/special slot ranges, and fusion-stone category tagging.
- Added focused `ItemSocketService` coverage for normal slot insertion after special slots, special-slot full failure, fusion-template slot insertion, and normal-category full failure.
- Current gaps in this cluster: wiring non-stigma `CM_MANASTONE` action `2` through delayed use animations, chance math, source/supplement consumption, DB `item_stones` insertion, equipped stat refresh, and action `1` enchant-stone behavior remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "ItemSocketServiceTests|StaticDataLoadingTests"` passes with 12 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 385 tests.

### Session 153 (May 21, 2026)
- Created `docs/Phase-6J-Completion.md` as the next handoff after the 6I continuation, summarizing Sessions 149-152, the four committed `CM_MANASTONE`/item socket units, the current 385-test validation baseline, and the next focused unit queue.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 385 tests from Session 152.

### Session 154 (May 21, 2026)
- Added Java non-stigma `CM_MANASTONE` action `2` no-supplement socketing through `EnchantService.CreateSocketManastonePlan`: Java `EnchantItemAction.canAct` family gate, `EnchantService.socketManastone` level/socket/rate/quality chance math, membership-indexed `gameserver.rates.manastone_chances`, source count/delete mutation, success/failure outcomes, and target item-info state.
- Wired action `2` through `GameServerConnection` with Java-shaped `SM_ITEM_USAGE_ANIMATION` start/finish packets, `STR_GIVE_ITEM_OPTION_*` system messages, source consume/delete inventory packets, target `SM_INVENTORY_UPDATE_ITEM` stats-change updates, equipped-item stats refresh, and DB-backed `SaveManastoneSocketMutationAsync` persistence for new `item_stones` rows plus source consumption.
- Current gaps in this cluster: supplement validation/count/chance math remains intentionally pending, exact Java delayed-task cancellation observers are still first-pass immediate, Java enchant-stone action `1` is still pending, and manastone socketing does not yet remove remaining tune counts.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EnchantServiceTests|ItemSocketServiceTests|GameServerOptionsTests|ClientPacketFactory_ParsesManastone"` passes with 21 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 389 tests.

### Session 155 (May 21, 2026)
- Added Java item `<actions><enchant>` parsing to `ItemTemplateSummary`, including manastone `count`, supplement chance, min/max level, and `manastone_only` flags used by `EnchantItemAction`.
- Extended non-stigma `CM_MANASTONE` action `2` with Java supplement parity: 1661-family validation, wrong-level `STR_ITEM_ENCHANT_ASSISTANT_NO_RIGHT_ITEM`, supplement success-chance bonuses, manastone-only single-count use, existing-socket count multiplication, insufficient-supplement no-consume failure, source consumption, supplement update/delete packets, and DB-backed supplement persistence in the socket mutation.
- Current gaps in this cluster: exact Java delayed-task cancellation observers are still first-pass immediate, Java enchant-stone action `1` is still pending, and manastone socketing does not yet remove remaining tune counts.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EnchantServiceTests|StaticDataLoadingTests|ClientPacketFactory_ParsesManastone|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 15 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 393 tests.

### Session 156 (May 21, 2026)
- Surfaced Java `ItemTemplate.getMaxTuneCount()` as `ItemTemplateSummary.MaxTuneCount`, preserving the Java `afterUnmarshal` behavior that normalizes non-equipment and non-tunable templates to zero while leaving tunable/randomizable equipment intact.
- Added Java `Item.removeRemainingTuningCountIfPossible()` parity to manastone socketing: successful `ItemSocketService.addManaStone` updates and failed `EnchantService.socketManastoneAct` outcomes now consume remaining tuning attempts when the target is identified and has unused tune count.
- Persisted the target `inventory.tune_count` inside the DB-backed manastone socket mutation so immediate C# mutations do not depend on a future dirty-inventory save loop.
- Current gaps in this cluster: exact Java delayed-task cancellation observers are still first-pass immediate, and Java enchant-stone action `1` is still pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EnchantServiceTests|ItemSocketServiceTests|StaticDataLoadingTests"` passes with 24 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 394 tests.

### Session 157 (May 21, 2026)
- Created `docs/Phase-6K-Completion.md` as the next handoff after the 6J continuation, summarizing Sessions 154-156, the three committed manastone socket/supplement/tune-count units, the current 394-test validation baseline, and the next focused unit queue.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 394 tests from Session 156.

### Session 158 (May 21, 2026)
- Added Java `CM_MANASTONE` action `1` enchant-stone handling through `EnchantService.CreateEnchantItemPlan`: Java `EnchantItemAction.canAct` target guards, `EnchantmentStone.getByItemId` Alpha/Beta/Gamma/Delta/Epsilon/Omega mapping, `gameserver.rates.enchantment_stone.base_chances` / `amplified_chances`, level/quality/enchant-level chance modifiers, supplement chance/count consumption, +1/+2/+3 crit rolls, success cap, max-enchant cap, amplified-only Omega guard, failure downgrade/reset behavior, enchant-type target destruction, and tune-count cleanup.
- Wired action `1` through `GameServerConnection` with Java-shaped 4s `SM_ITEM_USAGE_ANIMATION` start/finish packets, `STR_ENCHANT_ITEM_*`, `STR_GIVE_ITEM_OPTION_*`, `STR_MSG_EXCEED_CANNOT_02`, `STR_MSG_ENCHANT_ITEM_SUCCEED_NEW`, `STR_MSG_ENCHANT_TYPE1_ENCHANT_FAIL`, +15/+20 race-filtered announce fanout, source/supplement item-use updates, target item-info updates/deletes, equipped stats refresh, and DB-backed `SaveEnchantItemMutationAsync` persistence for `inventory.enchant`, `is_amplified`, `tune_count`, `buff_skill`, counts, and deletes.
- Current gaps in this cluster: Java's delayed movement/cancel observer and cooldown abort cleanup are still first-pass immediate, enchant +20 buff-skill selection/application remains pending because `exceed_enchant_skill` is not parsed yet, and full Java `SkillEngine` effect lifecycle/stat fanout is still future work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EnchantServiceTests|GameServerOptionsTests"` passes with 21 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 400 tests.

### Session 159 (May 21, 2026)
- Parsed Java `ItemTemplate.exceed_enchant_skill` into `ItemTemplateSummary` and validated the real Java static-data value for an exceed-capable sword template.
- Extended `EnchantService.CreateEnchantItemPlan` with Java `EnchantService.setEnchantLevel` / `getEquipBuff` parity for +20 exceed enchant: the C# plan now selects from the Java buff-skill table, stores/clears `inventory.buff_skill`, removes old equipped temporary skills when dropping below +20, learns the new equipped temporary skill when crossing into +20, and carries the Java exceed-skill system-message payload.
- Wired enchant buff-skill skill-list side effects through `GameServerConnection` with `SM_SKILL_REMOVE`, `SM_SKILL_LIST` message `1300050`, and `STR_MSG_EXCEED_SKILL_ENCHANT`, while leaving full SkillEngine stat/effect application for the broader effect-system slice.
- Current gaps in this cluster: Java's delayed movement/cancel observer and cooldown abort cleanup are still first-pass immediate, richer NPC known-list/range validation remains first-pass, and full Java `SkillEngine` effect lifecycle/stat fanout is still future work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EnchantServiceTests|StaticDataLoadingTests|GamePacketTests"` passes with 84 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 402 tests.

### Session 160 (May 21, 2026)
- Added a Java `ThreadPoolManager.schedule` foundation to the C# scheduler beside the existing fixed-rate scheduler, returning a cancellable one-shot `ScheduledTask` handle for delayed gameplay work.
- Added scheduler coverage for executing a delayed task and cancelling a delayed task before execution, which gives the upcoming Java `TaskId.ITEM_USE` item-use timing/cancel paths a tested primitive.
- Current gaps in this cluster: `CM_MANASTONE`, stigma charge, charge actions, idian polish, soul-bind, and other item-use paths still need to be moved from immediate execution onto this one-shot scheduler with Java movement/cancel observer cleanup.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GameServerBootstrapTests"` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 161 (May 21, 2026)
- Wired Java `TaskId.ITEM_USE` delayed execution into `CM_MANASTONE` action `1` enchant-stone handling: the start animation is sent immediately, then the validated enchant plan persists and emits source/supplement/target/skill/stat/final-animation packets after the Java 4s use delay.
- Added a connection-level pending item-use slot backed by the one-shot scheduler, mirroring Java `CreatureController.addTask(TaskId.ITEM_USE)` replacement semantics for this branch and cancelling the pending enchant on movement with Java `SM_ITEM_USAGE_ANIMATION` end state `3` plus `STR_ENCHANT_ITEM_CANCELED`.
- Added Java cancel system-message helpers for enchant-stone and manastone socket cancellation. Action `2` still uses the older immediate mutation path and should be the next delayed-use consumer.
- Current gaps in this cluster: action `2` delayed item-use scheduling/cancel, stigma charge/charge/idian polish/soul-bind delayed task migration, item cooldown abort cleanup, and full Java `SkillEngine` effect lifecycle/stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests"` passes with 69 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 162 (May 21, 2026)
- Wired Java `TaskId.ITEM_USE` delayed execution into `CM_MANASTONE` action `2` manastone socketing: the start animation is sent immediately, and the socket plan persistence, source/supplement packets, target item update, success/failure message, equipped stats refresh, and final animation now run after the Java 2s use delay.
- Reused the pending item-use movement-cancel path for manastone socketing, emitting Java end-state `3` item-use animation and `STR_GIVE_ITEM_OPTION_CANCELED` when movement cancels the pending socket operation.
- Current gaps in this cluster: stigma charge, charge actions, idian polish, soul-bind, godstone socketing, and amplification still use immediate or branch-specific timing; item cooldown abort cleanup and full Java `SkillEngine` effect lifecycle/stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests|EnchantServiceTests"` passes with 88 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 163 (May 21, 2026)
- Created `docs/Phase-6L-Completion.md` as the next handoff after the 6K continuation, summarizing the five committed enchant-stone, exceed-buff, scheduler, and delayed item-use units, the current 404-test validation baseline, and the next focused unit queue.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 404 tests from Session 162.

### Session 164 (May 21, 2026)
- Migrated Java `CM_MANASTONE` action `4` godstone socketing onto the C# one-shot `TaskId.ITEM_USE` scheduler: start animation is still sent immediately, and source consume/delete, target godstone persistence, success messaging, target item-info refresh, and finish animation now run after the Java 2s delay.
- Extended the shared pending item-use cancellation slot with a godstone-specific cancel message, preserving Java `ItemSocketService.socketGodstone` movement abort behavior with end-state `3` item-use animation plus `STR_MSG_GIVE_PROC_CANCEL`.
- Added `SM_SYSTEM_MESSAGE.STR_MSG_GIVE_PROC_CANCEL` packet coverage (`1402238`) beside the existing enchant/manastone cancel messages.
- Current gaps in this cluster: stigma charge, charge actions, idian polish, soul-bind, and amplification still need migration onto delayed `TaskId.ITEM_USE`; item cooldown abort cleanup and full Java `SkillEngine` effect lifecycle/stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests|ItemSocketServiceTests"` passes with 79 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 165 (May 21, 2026)
- Migrated Java stigma charge (`CM_MANASTONE` stigma source + stigma target) onto the C# one-shot `TaskId.ITEM_USE` scheduler: the Java 5s start animation remains immediate, while source charge-stone consume/delete, target enchant/delete, temporary stigma skill add/remove packets, success/failure messages, target item-info refresh, and stats refresh now run after the scheduled delay.
- Generalized the pending item-use cancellation record so branches can carry Java-specific cancel animation target/source/end-state fields; stigma charge movement abort now sends the Java-shaped target-stigma/charge-stone animation with end state `2` plus generic `STR_ITEM_CANCELED`.
- Added `SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED` packet coverage (`1300427`) beside the branch-specific item-use cancel messages.
- Current gaps in this cluster: charge actions, idian polish, soul-bind, and amplification still need migration onto delayed `TaskId.ITEM_USE`; item cooldown abort cleanup and full Java `SkillEngine` effect lifecycle/stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests|ItemSocketServiceTests|StigmaServiceTests"` passes with 88 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 166 (May 21, 2026)
- Migrated Java item `ChargeAction` conditioning items onto the C# one-shot `TaskId.ITEM_USE` scheduler: the source conditioning item now sends the Java 3s start animation, then source consume/delete, equipped-item charge persistence, charge bar updates, success messages, stats refresh, and all-complete messages run after the scheduled delay.
- Added Java charge movement-cancel parity to the shared pending item-use slot with end state `1` item-use animation plus `STR_MSG_ITEM_CHARGE_CANCELED` / `STR_MSG_ITEM_CHARGE2_CANCELED` depending on charge way.
- Confirmed Java `EnchantService.amplifyItem` is immediate and does not schedule `TaskId.ITEM_USE`; keep C# amplification immediate unless future Java source inspection finds a different branch.
- Current gaps in this cluster: idian polish and soul-bind still need migration onto delayed `TaskId.ITEM_USE`; item cooldown abort cleanup and full Java `SkillEngine` effect lifecycle/stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests|ItemChargeServiceTests"` passes with 72 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 167 (May 21, 2026)
- Migrated Java `PolishAction` idian polish onto the C# one-shot `TaskId.ITEM_USE` scheduler: valid polish attempts now send the Java 5s start animation immediately, then source consume/delete, random-bonus failure or target idian replacement, success/failure messages, target item-info update, and equipped stat refresh run after the scheduled delay.
- Reused the generic item-use cancellation path for idian polish movement aborts, preserving Java end state `2` item-use animation plus `STR_ITEM_CANCELED`.
- Current gaps in this cluster: soul-bind still needs migration onto delayed `TaskId.ITEM_USE`; item cooldown abort cleanup and full Java `SkillEngine` effect lifecycle/stat fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests|IdianPolishServiceTests"` passes with 74 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 168 (May 21, 2026)
- Migrated soul-bind confirmation accept flow onto the C# one-shot `TaskId.ITEM_USE` scheduler: accepting the Java `SM_QUESTION_WINDOW` now starts the 5s soul-bind animation immediately, then confirmed soul-bound equipment mutation, success end state `6`, success message, inventory/equipment persistence, appearance fanout, skill/stat side effects, and final equip packets run after the scheduled delay.
- Added soul-bind movement-cancel parity to the shared pending item-use slot with end state `8` item-use animation plus `STR_SOUL_BOUND_ITEM_CANCELED`.
- Current gaps in this cluster: soul-bind stance-denial messages are still pending, item cooldown abort cleanup is still not wired for generic item-use aborts, and full Java `SkillEngine` effect lifecycle/stat fanout remains pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePacketTests|GameServerBootstrapTests|EquipmentServiceTests"` passes with 89 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

### Session 169 (May 21, 2026)
- Created `docs/Phase-6M-Completion.md` as the next handoff after the 6L delayed-item-use continuation, summarizing Sessions 164-168, the five committed delayed item-use units, the current 404-test validation baseline, and the next focused unit queue.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 404 tests from Session 168.

### Session 170 (May 21, 2026)
- Added Java `Equipment.soulBindItem` invalid-stance guard parity before the C# soul-bind question path: dead, ride, chair, resting, gliding, flying, and weapon-equipped checks now return Java `STR_SOUL_BOUND_INVALID_STANCE(ChatUtil.l10n(...))` message IDs in the Java order.
- Added C# player creature-state breadcrumbs for Java `CreatureState` bit checks plus `PlayerMode.RIDE`, and wired `CM_MOVE` gliding updates into that state so movement can block soul-bind requests like Java.
- Added `ChatUtil.L10n(int)` for Java-shaped client l10n parameters and packet coverage for `STR_SOUL_BOUND_INVALID_STANCE`.
- Current gaps in this cluster: item cooldown abort cleanup is still not wired for generic item-use aborts, power-shard/quest/summon observers and exact speed/emotion fanout remain pending, and full Java `SkillEngine` effect lifecycle/stat fanout is still future work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 27 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 411 tests.

### Session 171 (May 21, 2026)
- Added Java `ItemUseLimits.usedelay/usedelayid` parsing to `ItemTemplateSummary`, with static-data coverage proving food-style item templates load `usedelay="5000"` / `usedelayid="21"`.
- Added C# player item-cooldown helpers mirroring Java `Player.addItemCoolDown` / `removeItemCoolDown`, including Java millisecond reuse time and integer-second use delay storage.
- Extended the shared `TaskId.ITEM_USE` scheduler path to set/clear Java-style per-player `usingItem` state and to support action-specific cooldown removal on abort; idian `PolishAction` now removes its item cooldown on movement cancel while `ChargeAction` keeps the Java no-remove behavior.
- Current gaps in this cluster: broader `CM_USE_ITEM` action routing still needs Java cooldown application as more item actions are ported; power-shard emotion side effects, quest/summon observers, and exact speed/emotion fanout remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "StaticDataLoadingTests|PlayerStateTests|GameServerBootstrapTests"` passes with 11 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 412 tests.

### Session 172 (May 21, 2026)
- Added Java `CM_EMOTION` opcode `43` parsing and `SM_EMOTION` opcode `37` serialization foundation, including the Java `EmotionType` IDs and no-tail `POWERSHARD_ON` / `POWERSHARD_OFF` packet shape.
- Wired the Java power-shard emotion side effects into the C# connection path: dead players are ignored, pending item-use is cancelled like Java `PlayerController.cancelUseItem`, missing equipped power shards send `STR_WEAPON_BOOST_NO_BOOSTER_EQUIPED`, and successful toggles set/unset Java `CreatureState.POWERSHARD` before broadcasting `SM_EMOTION` to visible players including self.
- Added Java `STR_WEAPON_BOOST_*` system-message coverage (`1300490`-`1300492`) and packet-factory tests for power-shard, emote, and chair emotion payload branches.
- Current gaps in this cluster: broader `CM_EMOTION` behavior still needs abnormal-state/stance guards, sit/stand/fly/weapon/walk/sprint side effects, quest/summon observers, and exact movement/attack speed fanout; broader `CM_USE_ITEM` action routing still needs Java cooldown application as more item actions are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesEmotionPacket"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 413 tests.

### Session 173 (May 21, 2026)
- Extended Java `CM_EMOTION` parity beyond power shards for the state-only branches C# can represent today: sit/stand, chair sit/up, weapon draw/sheath, and walk/run now mutate Java-shaped `CreatureState` bits and broadcast `SM_EMOTION` after pending item-use cancellation.
- Added Java creature-state bit coverage for `WALK_MODE`, `POWERSHARD`, `CHAIR`, and `PRIVATE_SHOP`, including exact-match semantics for Java multibit states and `setState(state, replace=true)` chair replacement behavior.
- Added chair `SM_EMOTION` payload coverage so the coordinate/heading tail is pinned before broader emotion fanout work.
- Current gaps in this cluster: `CM_EMOTION` still needs abnormal-state/stance guards, fly/land/fly-teleport/sprint controller behavior, sit observers, emote ownership checks, quest/summon observers, and exact movement/attack speed fanout.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesEmotionPacket|Player_CreatureStateMatchesJavaBitAndExactMultibitSemantics"` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 414 tests.

### Session 174 (May 21, 2026)
- Wired `CM_EMOTION` custom emote broadcast parity through Java `EmotionList.canUse`: default 1-35 emotes, housing-style `>10000` emotes, and explicitly learned `Player.Emotions` now fan out via `SM_EMOTION` with Java target-object resolution.
- Added `SM_EMOTION` emote-tail packet coverage (`targetObjectId`, `emotion`, `1`) alongside the previously pinned power-shard and chair payloads.
- Current gaps in this cluster: the exact `EmotionLearnAction` static learnable-id table is still approximated until item action static-data parsing is broader; `CM_EMOTION` still needs abnormal-state/stance guards, fly/land/fly-teleport/sprint controller behavior, sit observers, quest/summon observers, and exact movement/attack speed fanout.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesEmotionPacket"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 414 tests.

### Session 175 (May 21, 2026)
- Added Java `Equipment.unEquipItem` power-shard side-effect parity to the C# equipment mutation path: unequipping a `POWER_SHARDS` item now carries a `PowerShardDeactivated` flag, and the connection unsets `CreatureState.POWERSHARD` after successful persistence before sending owner-only `SM_EMOTION(POWERSHARD_OFF)`.
- Kept the mutation service side-effect-free until apply time so a failed C# persistence write does not clear player state earlier than the committed inventory change.
- Added equipment-service coverage proving power-shard unequip requests the off emotion while leaving the live player state untouched until `ApplyEquipmentChangeAsync`.
- Current gaps in this cluster: power-shard burn-out and automatic stack replacement from Java `Equipment.usePowerShard` remain future combat/observer work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "ChangeEquipment_UnequippingPowerShardRequestsPowerShardOff|CharacterSelectionServerPackets_WriteJavaShapedPayloads"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 415 tests.

### Session 176 (May 21, 2026)
- Parsed Java `<learnemotion emotionid="...">` item actions into `ItemTemplateTable.LearnableEmotionIds`, mirroring `EmotionLearnAction.afterUnmarshal`'s global learnable-id set.
- Replaced the temporary `CM_EMOTION` custom-emote range shortcut with Java `EmotionList.canUse` parity: if static data is loaded, only exact learnable emotion IDs require the player to own the emotion; non-learnable/default/housing IDs pass like Java.
- Added real static-data coverage for the 4.8 learnable-emotion set, including the 91 unique IDs, the `64`/`155` endpoints, and the missing `140` gap from Java data.
- Current gaps in this cluster: `CM_EMOTION` still needs abnormal-state/stance guards, select-target/jump/open-door pass-through behavior, fly/land/fly-teleport/sprint controller behavior, sit observers, quest/summon observers, and exact movement/attack speed fanout.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "StaticDataLoadingTests|ClientPacketFactory_ParsesEmotionPacket"` passes with 4 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 415 tests.

### Session 177 (May 21, 2026)
- Added Java `CM_EMOTION` pass-through parity for the no-mutation branches C# can represent today: `SELECT_TARGET` now cancels pending item use and returns without broadcasting, while `JUMP`, `OPEN_DOOR`, and `CLOSE_DOOR` flow through the same guard/cancel path and broadcast Java-shaped `SM_EMOTION` packets.
- Preserved Java's drawn-weapon guard for `JUMP` beside the existing chair-sit guard, so attack-mode players cannot jump-emote from this path.
- Added packet coverage for no-tail `SM_EMOTION(JUMP)` serialization plus `CM_EMOTION` parsing for select-target, jump, and open-door payloads.
- Current gaps in this cluster: `CM_EMOTION` still needs abnormal-state/stance guards, fly/land/fly-teleport/sprint controller behavior, sit observers, quest/summon observers, and exact movement/attack speed fanout.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|ClientPacketFactory_ParsesEmotionPacket"` passes with 1 test.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 415 tests.

### Session 178 (May 21, 2026)
- Added the first Java `FlyController.startFly/endFly` state side effects to `CM_EMOTION`: `FLY` now sets `CreatureState.FLYING` and ride-mode `FLOATING_CORPSE`, while `LAND` clears `FLYING`, `GLIDING`, and `FLOATING_CORPSE` before broadcasting `SM_EMOTION`.
- Kept explicit Java breadcrumbs in the handler to mark that zone eligibility, fly cooldown/reuse time, FP reduce/restore timers, and visual stat refresh remain future controller work.
- Expanded packet/state coverage for no-tail `SM_EMOTION(FLY)`, `CM_EMOTION` fly/land parsing, and the Java bit values for `FLYING`, `FLOATING_CORPSE`, and `GLIDING`.
- Current gaps in this cluster: `CM_EMOTION` still needs abnormal-state/stance guards, full fly/land eligibility/cooldown/FP/stat-controller behavior, fly-teleport/sprint controller behavior, sit observers, quest/summon observers, and exact movement/attack speed fanout.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|ClientPacketFactory_ParsesEmotionPacket|Player_CreatureStateMatchesJavaBitAndExactMultibitSemantics"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 415 tests.

### Session 179 (May 21, 2026)
- Created `docs/Phase-6N-Completion.md` as the next handoff after the 6M/6N emotion continuation, summarizing Sessions 170-178, the current 415-test validation baseline, important limits, and the next focused unit queue.
- Validation: not rerun for this docs-only handoff. Latest full validation remains `dotnet test dotnetConversion\AionServer.slnx --no-restore` passing with 415 tests from Session 178.

### Session 180 (May 21, 2026)
- Added Java `AbnormalState` bit/mask parity for player abnormal flags and wired the `CM_EMOTION` abnormal movement guard before item-use cancellation, matching Java's bypass list for target select and weapon-mode toggles.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 416 tests.

### Session 181 (May 21, 2026)
- Added first-pass Java stance state to `Player` plus `STR_SKILL_CAN_NOT_CHANGE_MODE__WHILE_IN_CURRENT_STANCE` / `STR_SKILL_CAN_NOT_TAKE_OFF__WHILE_IN_CURRENT_STANCE` packets, and wired the `CM_EMOTION` stance guard after item-use cancellation.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 417 tests.

### Session 182 (May 21, 2026)
- Added Java ride sprint runtime state (`PlayerRideInfo`, sprint FP task intent, parser coverage for `START_SPRINT`/`END_SPRINT`) and wired `CM_EMOTION` ride sprint start/end guards and side effects.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 418 tests.

### Session 183 (May 21, 2026)
- Added Java fly-teleport landing state for transporter vs windstream paths, including windstream gliding/FP-reduce side effects and parser/state coverage.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 419 tests.

### Session 184 (May 21, 2026)
- Moved first-pass Java fly/land state mutation into `Player.StartFlying` / `Player.EndFlying`, keeping FP reduce/restore task intent and ride-mode `FLOATING_CORPSE` behavior in one documented state helper.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 420 tests.

### Session 185 (May 21, 2026)
- Added Java `FlyController.onStopGliding` parity to `CM_MOVE`: stopping glide now clears `GLIDING`, chooses FP reduce/restore intent by flying state, and broadcasts `SM_EMOTION(STOP_GLIDE)` only for the walking-glider branch.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 421 tests.

### Session 186 (May 21, 2026)
- Loaded Java `ride/ride.xml` into a new `RideTable`, keyed by ride NPC id, and validated real ride speed/FP/sprint metadata from the Java static data.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 421 tests.

### Session 187 (May 21, 2026)
- Parsed Java item `<actions><ride npc_id="...">` metadata into `ItemTemplateSummary.RideNpcId`, with static-data coverage for real ride-card item templates.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 421 tests.

### Session 188 (May 21, 2026)
- Ported Java `RideAction` for `CM_USE_ITEM`: mount uses the Java 3s item-use animation and delayed task, dismount is immediate, ride state normalizes resting/floating/sprint state, and `CHANGE_SPEED` / `RIDE` / `RIDE_END` emotions broadcast through the existing visible-player fanout.
- Added ride canAct messages for resting and abnormal-state denial plus state coverage for mount/dismount behavior.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 422 tests.

### Session 189 (May 21, 2026)
- Preserved Java's `CM_EMOTION` exception for active ride item use: emotions no longer cancel the pending mount timer, while movement still cancels through the shared pending item-use path.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 422 tests.

### Session 190 (May 21, 2026)
- Corrected `CM_EMOTION.SIT` to call the Java-shaped ride dismount helper before applying resting state, so sit-triggered dismount now emits the same speed/ride-end side effects as `PlayerActions.unsetPlayerMode(PlayerMode.RIDE)`.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 422 tests.

### Session 191 (May 21, 2026)
- Ported Java `CraftLearnAction` for `CM_USE_ITEM`: recipe-card items now validate recipe count, recipe existence, race, already-known recipes, required craft skill, and skillpoint before consuming the source item.
- Added `SM_LEARN_RECIPE`, Java craft-recipe system messages, DB-backed `player_recipes` insertion plus source item consume/delete mutation, and static-data validation against a real ELYOS morph recipe.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 422 tests.

### Session 192 (May 21, 2026)
- Extended Java `<learnemotion>` item-action parsing so `ItemTemplateSummary` preserves both the card's `emotionid` and optional `minutes`, while retaining the global Java `EmotionLearnAction.isLearnable` set used by custom-emote validation.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 422 tests.

### Session 193 (May 22, 2026)
- Ported Java `EmotionLearnAction.act` for `CM_USE_ITEM`: emotion-card items now validate missing/duplicate emotion IDs, add Java item cooldowns, persist `player_emotions`, consume/delete or decrement the source card, broadcast the instant `SM_ITEM_USAGE_ANIMATION`, and send Java-shaped `SM_EMOTION_LIST(action=1)`.
- Added Java expiration math for temporary emotion cards (`minutes == 0` permanent, otherwise epoch seconds plus `minutes * 60`) and pinned `STR_ITEM_COLOR_ERROR` / `STR_TOOLTIP_LEARNED_EMOTION` system-message IDs.
- Current gaps in this cluster: temporary emotion expiration timers and timeout removal messages are not yet wired into a C# `ExpireTimerTask` equivalent.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 423 tests.

### Session 194 (May 22, 2026)
- Ported Java `TitleAddAction` for `CM_USE_ITEM`: title-card items now parse `titleid` plus optional `minutes`, validate missing/duplicate titles, preserve Java's post-animation race denial branch, persist `player_titles`, consume/delete or decrement the source card, send `STR_MSG_GET_CASH_TITLE`, and refresh the owner with Java-shaped full-list `SM_TITLE_INFO`.
- Added Java expiration math for temporary title cards, `STR_TOOLTIP_LEARNED_TITLE` packet coverage, and static-data coverage against the real Effervescent title-card templates.
- Current gaps in this cluster: temporary title expiration timers and `STR_MSG_DELETE_CASH_TITLE_BY_TIMEOUT` removal messages remain pending with the broader expirable-task bridge.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 424 tests.

### Session 195 (May 22, 2026)
- Ported Java `SkillLearnAction` for `CM_USE_ITEM`: skill-book items now parse `skillid`, required level, and class metadata; validate player level/class/race/already-known guards; add Java item cooldowns; cancel pending item use before the instant animation; persist learned `player_skills`; emit Java-shaped `SM_SKILL_LIST` message IDs; and consume/delete or decrement the source book.
- Added a C# `SkillLearnService.learnSkillBook` equivalent that uses the existing Java-shaped skill-tree projection and preserves Java's normal/stigma/profession learn-message selection, with real static-data coverage against the ELYOS ranger White Tiger skill book.
- Current gaps in this cluster: passive SkillEngine effect application, profession level-up action animations, recipe autolearn side effects, and nearby-quest refreshes remain future SkillEngine/profession slices.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 425 tests.

### Session 196 (May 22, 2026)
- Ported Java `ExpandInventoryAction` for `CM_USE_ITEM`: item templates now parse `<expandinventory level="..." storage="CUBE|WAREHOUSE"/>`, cube tickets validate `CubeExpandService.canExpandByTicket`, warehouse tickets validate `WarehouseService.canExpandByTicket` including completed warehouse quest offsets, and successful use consumes/deletes or decrements the source ticket.
- Added DB-backed expansion persistence for `players.item_expands` and `players.wh_bonus_expands`, Java expansion system messages, instant `SM_ITEM_USAGE_ANIMATION` broadcast, `SM_CUBE_UPDATE.cubeSize` for cube tickets, and `WarehouseService.sendWarehouseInfo(player, false)`-shaped regular-warehouse update packets for warehouse tickets.
- Current gaps in this cluster: NPC-paid cube/warehouse expansion dialog flows still depend on fuller NPC/dialog function validation and are not part of item-ticket parity.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 426 tests.

### Session 197 (May 22, 2026)
- Ported the item-target branch of Java `DyeAction` for `CM_USE_ITEM`: item templates now parse `<dye color="...">` plus optional minutes, target skin templates honor Java's `ItemMask.DYEABLE` bit, and successful dye/removal consumes or decrements the source dye item.
- Added DB-backed `inventory.item_color` / `color_expires` persistence, Java dye success/error system-message IDs, full target `SM_INVENTORY_UPDATE_ITEM`, and equipped-item `SM_UPDATE_PLAYER_APPEARANCE` refresh through the visible-player broadcaster when available.
- Current gaps in this cluster: house-object painting remains pending until house object edit/spawn state is broad enough to mirror `DyeAction.dyeHouseObject`.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 427 tests.

### Session 198 (May 22, 2026)
- Ported Java `AnimationAddAction` for `CM_USE_ITEM`: item templates now parse `<animation idle/run/jump/rest/shop minutes>`, motion cards start the Java 1s item-use animation, consume/delete or decrement the source card on completion, persist learned `player_motions`, and replace active motions by Java motion type.
- Added Java-shaped `SM_MOTION(action=2)` add-motion packet coverage, owner add-motion sends for each learned motion, and visible-player active-motion refresh after the completion animation.
- Current gaps in this cluster: Java `ExpireTimerTask` timeout removal for temporary motions is still pending with the shared expirable-task bridge.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 428 tests.

### Session 199 (May 22, 2026)
- Added a C# `ExpirableTaskService` bridge for Java `ExpireTimerTask`: it starts a 1s periodic scan through `ThreadPoolManager`, registers loaded player emotions/titles/motions on enter-world, registers newly learned temporary emotion/title/motion entries from `CM_USE_ITEM`, unregisters the player on leave-world, and preserves Java's `remainingSeconds < 0` expiry threshold.
- Ported timeout removal side effects for temporary emotions, titles, and motions: expired emotions remove `player_emotions`, send full `SM_EMOTION_LIST(action=0)`, and send `STR_MSG_DELETE_CASH_SOCIALACTION_BY_TIMEOUT`; expired titles remove `player_titles`, clear active display/bonus title state with Java-shaped `SM_TITLE_INFO` packets, refresh the title list, and send `STR_MSG_DELETE_CASH_TITLE_BY_TIMEOUT`; expired motions remove `player_motions`, send Java `SM_MOTION(action=6)`, and send `STR_MSG_DELETE_CASH_CUSTOMANIMATION_BY_TIMEOUT`.
- Added repository delete methods and packet coverage for the three timeout system messages plus motion removal, with focused lifecycle tests covering expiry, exact-threshold retention, and logout unregistration.
- Current gaps in this cluster: the bridge currently covers the Phase 6 temporary social unlocks only; Java also registers expirable inventory/equipment items, pets, and house objects on login, which should be widened in a later lifecycle slice.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "ExpirableTaskServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|PlayerEnterWorldServiceTests"` passes with 9 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 430 tests.

### Session 200 (May 22, 2026)
- Ported Java `CM_APPEARANCE` type `2` cosmetic item use: opcode `197` now parses the Java packet layout, resolves cube cosmetic items by object ID, reads item-template `<cosmetic name="..."/>` metadata, validates against loaded `cosmetic_items.xml` templates, and preserves Java race/gender/ride guard messages.
- Added `CosmeticItemService` for Java `CosmeticItemAction.act`, including hair/face/voice/color/tattoo/deco mutations and the Java preset branch behavior where `skin_rgb` is set from `eye_color`.
- Added DB-backed `player_appearance` persistence plus cosmetic inventory-object deletion in one mutation, Java `STR_MSG_ITEM_RESTRICTION_RIDE` packet coverage, and a Java `PlayerController.onChangedPlayerAttributes`-style visible `SM_PLAYER_INFO` refresh after successful mutation.
- Current gaps in this cluster: `CM_APPEARANCE` type `0` character rename and type `1` legion rename coupons remain pending until rename services, world cached-name updates, and legion history/rename fanout are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "Cosmetic|ClientPacketFactory_ParsesAppearancePackets|StaticDataLoadingTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|PlayerEnterWorldServiceTests"` passes with 12 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 432 tests.

### Session 201 (May 22, 2026)
- Added a typed `DecomposableItemTable` for Java `decomposable_items.xml`, preserving `DecomposableItemsData` normal vs selectable lookup behavior, Java first-group selectable rewards, empty decomposable entries, group chance/min/max level gates, fixed rewards, random reward types, race restrictions, class restrictions, and Java min/max count defaults.
- Parsed item-template `<actions><decompose/>` markers into `ItemTemplateSummary.HasDecomposeAction`, giving the next `DecomposeAction` runtime slice a Java-shaped source/action lookup without re-reading XML.
- Added real static-data coverage for Juicy Pepento fixed rewards, selectable `[Event] Cold Box` rewards, level/race-gated Unknown Bundle rewards, class-restricted Koakoa chest rewards, and random enchantment sack rewards.
- Current gaps in this cluster: `CM_USE_ITEM` still needs the Java `DecomposeAction` runtime flow: canAct guards, inventory-full handling, selectable-reward packet, 3s item-use animation, movement abort, source-item consumption, and fixed/random reward creation.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter StaticDataLoadingTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 432 tests.

### Session 202 (May 22, 2026)
- Ported Java decomposition runtime for the current item-use surface: `CM_USE_ITEM` now routes `<decompose/>`, validates Java normal/selectable decomposable metadata, sends `SM_FIRST_SHOW_DECOMPOSABLE` for selectable boxes, and schedules normal decompositions with the Java 3s `SM_ITEM_USAGE_ANIMATION`.
- Added `CM_SELECT_DECOMPOSABLE` opcode `236` plus `SM_SECONDARY_SHOW_DECOMPOSABLE`: selectable reward choice filters by Java race/class rules, consumes/deletes or decrements the source item, sends `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`, and persists the selected reward as a Java `DECOMPOSABLE` inventory add.
- Added `DecomposeService` for Java reward behavior: level-gated and chance-weighted collection selection, fixed reward count rolls, random reward resolver branches for enchantment stones, manastone grades, special manastones, ancient items, chunks, scrolls, potions, illusion godstones, and race-specific Ophidan recipes.
- Added DB-backed decompose mutations for source item update/delete plus reward inventory insertion, Java decompose system messages (`1300445`-`1300450` and `1400452`), packet coverage for decompose UI packets, and focused selectable/normal reward service tests.
- Current gaps in this cluster: reward insertion currently creates fresh cube rows; Java `ItemService.addItem` stack merging, full special-cube capacity semantics, and richer item-add failure behavior still need a general item-service port.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "DecomposeServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|ClientPacketFactory_ParsesSelectDecomposablePacket|StaticDataLoadingTests"` passes with 6 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 435 tests.

### Session 203 (May 22, 2026)
- Added a reusable C# `InventoryAddService` / `InventoryItemFactory` bridge for Java `ItemService.addItem` and `ItemFactory.newItem`: stackable rewards merge into existing cube stacks before creating fresh rows, new stack rows are split by `max_stack_count`, and non-overflow mode reports remaining count when the cube is full.
- Wired decomposition rewards through that planner, so selectable and normal decomposables now persist updated reward stacks plus inserted reward rows in the same source-item mutation and send Java `SM_INVENTORY_UPDATE_ITEM(INC_ITEM_COLLECT)` for stack merges.
- Extended decompose persistence to save reward stack count updates as well as new reward rows, while keeping Java `DECOMPOSABLE` add packets for newly created rows.
- Current gaps in this cluster: Java special-cube capacity checks (`extraInventoryId`) and richer partial-add failure messaging remain pending with the broader item-service port.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "InventoryAddServiceTests|DecomposeServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|ClientPacketFactory_ParsesSelectDecomposablePacket|StaticDataLoadingTests|PlayerEnterWorldServiceTests"` passes with 16 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 438 tests.

### Session 204 (May 22, 2026)
- Parsed Java item-template `<inventory id="...">` metadata into `ItemTemplateSummary.ExtraInventoryId`, preserving the Java `-1` no-special-cube default and real data such as Expert Essencetapping Ring's special-cube id `2`.
- Added template-aware normal/special cube slot counters to `InventoryCapacity`, matching Java `ItemStorage.getCubeItems` and `getSpecialCubeItems` behavior for callers with static item metadata.
- Tightened `DecomposeService.canAct` to mirror Java's `inventory.isFull() || inventory.isFullSpecialCube() && containsSpecialCubeItems(...)`: normal cube fullness ignores special-cube items, and full special cube only blocks decomposables whose level-suitable fixed rewards include obtainable special-cube items.
- Current gaps in this cluster: the reusable item-add planner still needs template-aware special-slot capacity for future non-overflow callers and richer partial-add behavior outside the decompose guard.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "DecomposeServiceTests|StaticDataLoadingTests|InventoryAddServiceTests"` passes with 9 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 439 tests.

### Session 205 (May 22, 2026)
- Extended `InventoryAddService.CreateAddItemPlan` with optional item-template metadata so non-overflow new rows now choose Java normal cube vs special cube free slots from the reward template's `ExtraInventoryId`.
- Added list-based template-aware normal/special cube capacity helpers to `InventoryCapacity`, letting planners operate on simulated inventory states before persistence.
- Added planner coverage for a full normal cube still accepting a special-cube item, and a full special cube rejecting a special-cube item with remaining count preserved.
- Current gaps in this cluster: the planner still reports partial remaining count but does not yet model all Java `ItemService.addItem` caller messaging around partial adds.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "InventoryAddServiceTests|DecomposeServiceTests|StaticDataLoadingTests"` passes with 11 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 441 tests.

### Session 206 (May 22, 2026)
- Parsed Java item-template `<remodel type="..." minutes="..."/>` metadata into `ItemTemplateSummary.RemodelAction`, with real static-data coverage for the Expert Essencetapping Ring remodel action.
- Added Java opcode `90` `CM_ITEM_REMODEL` parser coverage (`npcId`, keep item object id, extract item object id, trailing unknown) and registered it for in-game clients.
- Left the packet handler as an explicit documented no-op until the next runtime slice ports Java `services/item/ItemRemodelService.remodelItem`.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "StaticDataLoadingTests|ClientPacketFactory_ParsesItemRemodelPacket|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 4 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 442 tests.

### Session 207 (May 22, 2026)
- Added a first-pass C# `ItemRemodelService` for Java `services/item/ItemRemodelService.remodelItem`: level, gender, Kinah, compatibility, remodelable, pattern-reshaper, and extract-removal guards now produce a deterministic mutation plan.
- Wired `CM_ITEM_REMODEL` runtime through `GameServerConnection`, including target skin/color mutation, pattern-reshaper skin removal, Kinah decrement, extract item decrement/delete, Java-shaped inventory update/delete packets, success/failure system messages, and DB-backed persistence in one transaction.
- Added `ItemTemplateSummary.IsRemodelable` from Java `ItemMask.REMODELABLE`, `inventory.item_skin`/color persistence for remodel, and service coverage for normal remodel success, pattern reshaper removal, and low-level rejection.
- Current gaps in this cluster: Java NPC distance/function validation, full preview/cosmetic restrictions, and any timed skin-expiration removal from `RemodelAction.minutes` remain future slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "ItemRemodelServiceTests|ClientPacketFactory_ParsesItemRemodelPacket|PlayerEnterWorldServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 11 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 445 tests.

### Session 208 (May 22, 2026)
- Tightened the reusable C# `InventoryAddService` bridge for Java `ItemService.addStackableItem`: `POWER_SHARDS` rewards now merge into equipped shard stacks before normal cube stacks, matching Java's equipment-first shard branch.
- Added planner coverage proving the equipped-shard path is used only for `POWER_SHARDS`; normal stackable items still ignore equipped stacks and create/merge cube rows only.
- Current gaps in this cluster: broader Java partial-add caller messaging and wider reuse of the planner outside decompose/remodel-adjacent reward flows remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter InventoryAddServiceTests` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 447 tests.

### Session 209 (May 22, 2026)
- Broadened the C# `ExpirableTaskService` bridge for Java `ExpireTimerTask` and `Item.onExpire`: loaded cube/equipment items, regular-warehouse items, and account-warehouse items now register on enter-world; newly created non-stackable decompose reward rows register through the same service.
- Added Java cash-item warning/timeout side effects for expirable items: minute-threshold `STR_MSG_CASH_ITEM_TIME_LEFT`, cube `SM_DELETE_ITEM`, warehouse `SM_DELETE_WAREHOUSE_ITEM`, storage-size `SM_CUBE_UPDATE`, inventory row plus `item_stones` deletion, and cube/warehouse cash-item timeout messages.
- Added packet coverage for `SM_DELETE_WAREHOUSE_ITEM` and the cash-item timeout/warning messages, plus lifecycle coverage for loaded item expiry, warning thresholds, exact-threshold retention, logout unregistration, and newly registered reward items.
- Current gaps in this cluster: Java pet and house-object expirable rows remain pending, and equipped-item expiration still lacks fuller Java stat/equipment observer fanout beyond the delete/storage refresh packets.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "ExpirableTaskServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|PlayerEnterWorldServiceTests"` passes with 12 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 450 tests.

### Session 210 (May 22, 2026)
- Added a typed `AssemblyItemTable` for Java `assembly_items.xml`, preserving `AssemblyItemsData.getAssemblyItem` lookup by assembled result item ID and the JAXB `parts` integer list.
- Parsed item-template `<actions><assemble item="..."/>` metadata into `ItemTemplateSummary.AssemblyItemId`, matching Java `model/templates/item/actions/AssemblyItemAction.item`.
- Added real static-data coverage for the `186000018` assembly recipe and its five material parts, plus a parity check that every loaded `<assemble>` action is represented on an item template.
- Current gaps in this cluster: `CM_USE_ITEM` still needs Java `AssemblyItemAction` runtime: canAct all-part lookup, 1s item-use animation/abort observer, part consumption by item ID, success message, and Java `ItemService.addItem` reward insertion.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter StaticDataLoadingTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 450 tests.

### Session 211 (May 22, 2026)
- Ported Java `AssemblyItemAction` runtime into `CM_USE_ITEM`: source items with `AssemblyItemId` now validate that every Java recipe part exists in the cube, start the 1s `SM_ITEM_USAGE_ANIMATION`, and use Java's generic item-cancel message/end-state on movement or replacement cancellation.
- Added `AssemblyItemService` to mirror Java part consumption by item ID and default `ItemService.addItem` reward behavior, including stack/new-row reward planning, newly-created expirable reward registration, and Java's full-inventory quirk where parts remain consumed while the reward is not added.
- Added DB-backed assembly mutation persistence for multiple part count updates/deletes plus reward stack updates/new rows in one transaction, and added `STR_ASSEMBLY_ITEM_SUCCEEDED` plus `STR_MSG_DICE_INVEN_ERROR` packet helpers.
- Current gaps in this cluster: this still lacks Java quest item-removed observer callbacks and richer end-to-end client validation of the scheduled packet ordering.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "AssemblyItemServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|PlayerEnterWorldServiceTests"` passes with 10 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 453 tests.

### Session 212 (May 22, 2026)
- Parsed Java item-template `<actions><expextract item_id="..." percent="..." cost="..."/>` metadata into `ItemTemplateSummary.ExpExtractAction`, matching `model/templates/item/actions/ExpExtractAction`.
- Added real static-data coverage for fixed-cost and percent-cost XP extraction items, and asserted that every loaded `<expextract>` element is represented on an item template.
- Current gaps in this cluster: `CM_USE_ITEM` still needs Java `ExpExtractAction` runtime, including 5s item-use scheduling/cancel, inventory-full and not-enough-exp guards, EXP mutation persistence, source item consumption by item ID, reward add, `SM_STATS_INFO`/state refresh needs, and `STR_MSG_EXP_EXTRACTION_USE` messaging.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter StaticDataLoadingTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 453 tests.

### Session 213 (May 22, 2026)
- Ported Java `ExpExtractAction` runtime into `CM_USE_ITEM`: extraction items now validate inventory capacity and available EXP, start Java's 5s self item-use animation, use the decompose-style cancel message/end-state, and revalidate before completion.
- Added `ExpExtractService` to mirror Java fixed/percent EXP cost calculation from the current level's EXP need, level-floor guard, source item consumption by item ID, and default `ItemService.addItem` reward planning, including Java's post-source-consumption dice-inventory failure behavior when the reward cannot fit.
- Added DB-backed extraction mutation persistence for `players.exp`, source count/delete, and reward stack/new-row changes; added `SM_STATUPDATE_EXP` plus Java extraction system messages and packet coverage for the EXP update payload.
- Current gaps in this cluster: Java `PlayerCommonData.setExp` level-change side effects and quest item-removed observer callbacks remain deferred, and the scheduled ordering still needs real-client validation with the broader item-use queue.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|ExpExtractServiceTests"` passes with 4 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 456 tests.

### Session 214 (May 22, 2026)
- Tightened `InventoryAddService` for Java `ItemService.addItem` partial-add behavior: capacity-limited add plans now preserve any stack merges or created rows, retain the leftover count, and separately flag inventory-full remainders.
- Updated assembly and XP extraction reward callers to persist/send partial reward mutations before Java dice-inventory failure messaging, matching `ItemService.addItem`'s "apply what fits, then return remainder" contract.
- Added planner coverage for a full cube with a partially mergeable stack, plus inventory-full assertions for normal and special cube failures.
- Current gaps in this cluster: broader reuse is still pending for future reward/item services that create inventory rows outside the current decompose, assembly, and XP extraction paths.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "InventoryAddServiceTests|AssemblyItemServiceTests|ExpExtractServiceTests"` passes with 14 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 457 tests.

### Session 215 (May 22, 2026)
- Parsed Java item-template `<actions><composition/>` metadata into `ItemTemplateSummary.HasCompositionAction`, with real static-data coverage for the lone combination tool `165010000`.
- Added opcode `208` `CM_COMPOSITE_STONES` parser coverage and routed it through a C# `CompositionService` matching Java `CompositionAction`: combination tool validation, enchantment-stone validation, level cap, Java reward-ID calculation, sequential `decreaseByItemId` semantics, 5s self-only animation/cancel, and item-add planner reward insertion.
- Reused the existing multi-consume/reward persistence shape through a composition-specific service wrapper, keeping Java breadcrumbs while avoiding another copy of the same inventory transaction.
- Current gaps in this cluster: Java casting/protection interruption and full real-client ordering validation remain deferred with the broader item-use observer work; `ExtractAction`/`ApExtractAction` still need their own service dependencies before runtime parity.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CompositionServiceTests|ClientPacketFactory_ParsesCompositeStonesPacket|StaticDataLoadingTests"` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 461 tests.

### Session 216 (May 22, 2026)
- Parsed the remaining small extraction action metadata: Java `<actions><extract/>` now sets `ItemTemplateSummary.HasExtractAction`, and Java `<actions><apextract rate="..." target="..."/>` now maps to `ItemTemplateSummary.ApExtractAction`.
- Added real static-data coverage for the lone extraction tool `165000001` plus representative AP extraction tools `165005000` and `165005001`, and asserted that every loaded `<extract>`/`<apextract>` element is represented on an item template.
- Current gaps in this cluster: runtime remains intentionally deferred until `EnchantService.breakItem` reward/breakdown behavior, target equipment deletion, AP rank/common-data persistence, and AP stat packets are ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter StaticDataLoadingTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 461 tests.

### Session 217 (May 22, 2026)
- Added a C# `EnchantService.CreateBreakItemPlan` bridge for Java `services/EnchantService.breakItem`, including weapon/armor target validation, Java effective-level calculation, stone-tier selection, weapon +5 level bump, Java random reward-count ranges, target deletion, source consume/decrement, and reward insertion through the reusable item-add planner.
- Added planner coverage for weapon extraction, armor extraction, and canAct-style guard failures.
- Current gaps in this cluster: this was planner-only; runtime scheduling/cancel, persistence, inventory packet ordering, and real-client validation remained for the next slice.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter EnchantServiceTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 464 tests.

### Session 218 (May 22, 2026)
- Ported Java `<extract/>` item-use runtime through `CM_USE_ITEM`: canAct now validates source/target cube rows, self-only 5s item-use animation, Java cancel messaging, target/source mutation persistence, reward stack/new-row persistence, and Java success/failure animation end states.
- Added Java-shaped decomposition failure messages for missing target, non-decomposable targets, and equipped target attempts, plus inventory delete/update/add packets and dice-inventory failure behavior when the reward cannot fit after source/target consumption.
- Registered newly-created non-stackable extraction rewards with the expirable item bridge, matching the Java `ItemService.addNonStackableItem` side effect already used by decomposition and XP extraction.
- Current gaps in this cluster: quest item-removed observer callbacks and exact real-client ordering validation remain deferred with the broader scheduled item-use queue.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 464 tests.

### Session 219 (May 22, 2026)
- Parsed Java `<acquisition ap="...">` metadata into `ItemTemplateSummary.RequiredAbyssPoints`, preserving the value consumed by `ApExtractAction.act`.
- Added Java `ItemMask.CAN_AP_EXTRACT` support as `ItemTemplateSummary.CanApExtract`, enabling AP extraction target validation against real item masks instead of guessing from item type alone.
- Added real static-data coverage for AP-extractable item `100000363` and its required AP value.
- Current gaps in this cluster: AP extraction runtime still needed source/target guard order, AP rank mutation persistence, AP gain packets, and side-effect fanout.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 464 tests.

### Session 220 (May 22, 2026)
- Ported Java `ApExtractAction.canAct + act` into a C# `ApExtractService` and `CM_USE_ITEM` runtime: source/target lookup, source action validation, target `CAN_AP_EXTRACT`, tool level/quality/target-type checks, required AP lookup, AP calculation from Java rate, target deletion, source consume/decrement, and AP rank update.
- Added DB-backed persistence for AP extraction in one transaction: target delete, source save/delete, and `abyss_rank` save through the Java-shaped `AbyssRankDAO.storeAbyssRank` bridge.
- Added Java `STR_MSG_COMBAT_MY_ABYSS_POINT_GAIN` system message, AP extraction service tests, packet coverage, and runtime packets for consumed items plus owner `SM_ABYSS_RANK`.
- Current gaps in this cluster: Java rank-change side effects beyond the owner rank packet remained pending: `SM_ABYSS_RANK_UPDATE`, rank-limited equipment checks, `AbyssSkillService.updateSkills`, legion contribution, and siege callbacks.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 466 tests.

### Session 221 (May 22, 2026)
- Added C# `SM_ABYSS_RANK_UPDATE` opcode `136` parity for Java `network/aion/serverpackets/SM_ABYSS_RANK_UPDATE`, covering rank-change, team-object, and mentor-status payload shapes.
- Wired AP extraction rank changes to broadcast `SM_ABYSS_RANK_UPDATE(action=0)` to visible players after the owner receives `SM_ABYSS_RANK`, matching Java `AbyssPointsService.onRankChanged` packet fanout within the current known-list approximation.
- Current gaps in this cluster: Java `Equipment.checkRankLimitItems`, `AbyssSkillService.updateSkills`, legion contribution fanout, and siege callback handling remain future slices because their supporting systems are not fully ported.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "GamePackets_AreSerializedWithExpectedOpcodesAndPayloads|ApExtractServiceTests"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 466 tests.

### Session 222 (May 22, 2026)
- Ported the first AP rank-change equipment side effect from Java `Equipment.checkRankLimitItems`: C# now scans equipped items after an AP extraction rank change, checks main and fusioned item rank limits, unequips failing items, persists the equipment slot state, refreshes inventory/stat/appearance packets through the existing equipment path, and sends Java `STR_MSG_UNEQUIP_RANKITEM`.
- Added packet coverage for system message `1401329` plus equipment service coverage for regular rank-limited items and fusioned item rank limits.
- Current gaps in this cluster: Java `AbyssSkillService.updateSkills`, legion contribution fanout, siege callback handling, and full retail behavior around full-inventory rank unequip edge cases remain future slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "EquipmentServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 30 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 468 tests.

### Session 223 (May 22, 2026)
- Ported Java `services/abyss/AbyssSkillService.updateSkills` into C#: AP rank changes now remove all same-race abyss transform skills, add the current race/rank temporary transform skills at or above `STAR5_OFFICER`, and emit Java-shaped `SM_SKILL_REMOVE` plus `SM_SKILL_LIST` packets after the rank-limited equipment pass.
- Added service coverage for Elyos minimum-rank grants, old-rank removal before new-rank grants, below-minimum cleanup, and Asmodian rank-specific skill sets.
- Current gaps in this cluster: the C# path still uses Java's default `STAR5_OFFICER` transform threshold and does not yet load `RankingConfig.XFORM_MIN_RANK`; passive SkillEngine effect apply/remove fanout remains deferred with the broader SkillEngine slice.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "AbyssSkillServiceTests|GamePackets_AreSerializedWithExpectedOpcodesAndPayloads"` passes with 4 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 472 tests.

### Session 224 (May 22, 2026)
- Loaded Java `RankingConfig.XFORM_MIN_RANK` from `gameserver.topranking.xform.min_rank`, mapping the Java `AbyssRankEnum` names to C# rank IDs and honoring `mygs.properties`/environment overrides through the existing config loader.
- Wired AP rank-change transform skill updates to use the loaded threshold instead of the hardcoded `STAR5_OFFICER` default, while keeping `STAR5_OFFICER` as the fallback for invalid or absent values.
- Added option coverage for the default Java `STAR5_OFFICER` value, `mygs.properties` override to `COMMANDER`, and service coverage for configured minimum-rank suppression.
- Current gaps in this cluster: legion contribution fanout, siege callback handling, and passive SkillEngine effect apply/remove fanout remain future slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "AbyssSkillServiceTests|GameServerOptionsTests"` passes with 9 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 473 tests.

### Session 225 (May 22, 2026)
- Added C# `SM_HOUSE_UPDATE` opcode `61` parity for Java `SM_HOUSE_UPDATE`/`AbstractHouseInfoPacket.writeCommonInfo`, including the three-header words, address/owner/building/type IDs, owner-state/door fields, owner name, legion ID/emblem bytes, show-owner flag, sign notice, and empty decor placeholders.
- Wired `CM_HOUSE_SETTINGS` to broadcast `SM_HOUSE_UPDATE` after the owner `SM_HOUSE_ACQUIRE`, matching Java `house.getController().updateAppearance()` within the current player-position visibility approximation.
- Added packet coverage for the house update payload, including Java house type lookup from housing templates and sign/legion fields.
- Current gaps in this cluster: full house known-list membership, GeoService door updates, visitor kick side effects, and populated decor/house-object registry data remain future housing slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter CharacterSelectionServerPackets_WriteJavaShapedPayloads` passes with 1 test.

### Session 226 (May 22, 2026)
- Added C# `SM_HOUSE_RENDER` opcode `271` and `SM_DELETE_HOUSE` opcode `272` parity for Java `PlayerController.see/notSee` house known-list packets.
- Factored the Java `AbstractHouseInfoPacket.writeCommonInfo` layout into a shared C# writer so `SM_HOUSE_UPDATE` and `SM_HOUSE_RENDER` stay byte-aligned when populated decor/registry support is added later.
- Added packet coverage proving `SM_HOUSE_RENDER` writes the same common house payload as `SM_HOUSE_UPDATE` after the update header, and that `SM_DELETE_HOUSE` writes the Java address ID body.
- Current gaps in this cluster: the packets are ready, but full known-list membership still needs a real C# house world object/store before enter/leave visibility can send render/delete automatically.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter CharacterSelectionServerPackets_WriteJavaShapedPayloads` passes with 1 test.

### Session 227 (May 22, 2026)
- Added Java housing visitor-kick system message helpers `STR_MSG_HOUSING_ORDER_OUT_WITHOUT_FRIENDS`, `STR_MSG_HOUSING_ORDER_OUT_ALL`, `STR_MSG_HOUSING_REQUEST_OUT`, and `STR_MSG_HOUSING_CHANGE_OWNER`.
- Wired `CM_HOUSE_SETTINGS` close-door branches to send the Java owner-facing `HouseController.kickVisitors` "out" confirmation before the existing door-change confirmation for friends-only and fully closed states.
- Current gaps in this cluster: C# still lacks real house-zone membership, friend/legion visitor filtering, and teleport-out side effects, so visitor recipient messages and movement remain future housing-known-list work.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter CharacterSelectionServerPackets_WriteJavaShapedPayloads` passes with 1 test.

### Session 228 (May 22, 2026)
- Wired Java `services/player/PlayerEnterWorldService` login equipment cleanup: after login house-owner info and before expirable registration, C# now runs `EquipmentService.CheckRankLimitItems` to remove rank-limited gear when the player's stored AP rank changed while offline.
- Reused the existing equipment mutation fanout so login cleanup persists unequipped slots, sends inventory updates, rank-limit system messages, stats refresh, appearance broadcast, and power-shard deactivation when applicable.
- Current gaps in this cluster: Java's full login service fanout still has unported legion/siege/pet/house-object services, but rank-limited equipment cleanup now runs on both AP rank changes and login.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter EquipmentServiceTests` passes with 30 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 473 tests.

### Session 229 (May 22, 2026)
- Loaded Java `model/templates/housing/HouseAddress` map position and optional exit coordinates into `HousingAddressSummary`, preserving `map`, `x`, `y`, `z`, `exit_map`, `exit_x`, `exit_y`, and `exit_z` from `housing/houses.xml`.
- Added real static-data assertions for an open-world house address and an instanced studio address with exit coordinates, preparing the C# housing known-list and visitor teleport-out slices to use Java address data instead of hardcoded placeholders.
- Current gaps in this cluster: no behavior consumes these coordinates yet; house known-list membership, door GeoService updates, and visitor teleport-out remain future slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter StaticDataLoadingTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 473 tests.

### Session 230 (May 22, 2026)
- Loaded Java `HousingConfig.VISIBILITY_DISTANCE` from `gameserver.housing.visibility.distance`, preserving the 200m default and existing override/environment precedence.
- Added option coverage for the Java default so future house known-list work does not accidentally use the generic 95m creature visible distance.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter GameServerOptionsTests` passes with 4 tests.

### Session 231 (May 22, 2026)
- Added `WorldHouse` snapshots for Java `model/house/House` visibility state, deriving map position from loaded `HouseAddress` coordinates and preserving owner, legion, door, sign, inactive, and building fields used by house info packets.
- Added `HousingVisibilityService`, which tracks per-player known house addresses and returns Java-style `SM_HOUSE_RENDER`/`SM_DELETE_HOUSE` deltas using the housing visibility distance.
- Extended the C# `World` container with a house snapshot store parallel to the generic object map.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter HousingVisibilityServiceTests` passes with 3 tests.

### Session 232 (May 22, 2026)
- Wired first-pass house known-list packet flow: enter-world registers loaded player houses into the world snapshot store, refreshes housing visibility for online players, movement refreshes the moving player's house-known deltas, and disconnect clears the tracked house-known set.
- Extended `SM_HOUSE_RENDER` and `SM_HOUSE_UPDATE` to serialize either loaded owner/player state or persistent `WorldHouse` snapshots through the same Java `AbstractHouseInfoPacket.writeCommonInfo` bridge.
- Updated `CM_HOUSE_SETTINGS` appearance broadcast to refresh the world-house snapshot and broadcast `SM_HOUSE_UPDATE` from the house position when coordinates are available.
- Current gaps in this cluster: C# still lacks region-bucketed KnownList storage, populated house-object/decor packets, GeoService door state updates, and visitor zone filtering/teleport-out.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "HousingVisibilityServiceTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads|GameClientSocketServer"` passes with 6 tests.

### Session 233 (May 22, 2026)
- Added DB-backed persistent custom-house loading for Java `HousesDAO.loadHouses(..., false)`: C# now loads `houses` rows, joins owner/player legion display fields, excludes studio addresses `2001`/`3001`, derives inactive custom houses by owner/acquire order, applies Java `House.resetDoorState` fallback semantics, and stores snapshots into `World` during bootstrap.
- Added `HousingWorldService` as a game-engine bootstrap slice so persistent houses are visible even before owners log in; owner login/settings updates can still refresh those snapshots with current loaded state.
- Added service coverage for persistent-house bootstrap storage.
- Current gaps in this cluster: unowned static custom houses that do not have DB rows are not synthesized yet, studios are not spawned per personal instance, deleted-owner revocation is not ported, and house-object/registry/decor persistence remains pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "HousingWorldServiceTests|HousingVisibilityServiceTests|GameServerBootstrap_LoadsDataInitializesWorldAndStartsGameTime"` passes with 5 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 477 tests.

### Session 234 (May 22, 2026)
- Parsed Java housing-land default building references into `HousingAddressSummary`, preserving `HousingLand.getDefaultBuilding` fallback semantics and global `Building.type` values from `housing/house_buildings.xml`.
- Extended `HousingWorldService` to synthesize ownerless custom-house world snapshots for static `HouseAddress` rows missing from the `houses` table, matching Java `HousingService.spawnHouses` creation of `new House(address, instanceId)` with default building, owner id `0`, visible map coordinates, `showOwnerName=true`, and closed ownerless doors.
- Skipped `PERSONAL_INS` studio addresses during global synthesis, preserving Java's per-personal-instance studio spawning split.
- Current gaps in this cluster: synthesized ownerless custom houses are world-visible but still are not persisted until the future acquisition/auction path, studios are not spawned per personal instance, deleted-owner revocation is not ported, and house-object/registry/decor persistence remains pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "HousingWorldServiceTests|StaticDataLoadingTests|HousingVisibilityServiceTests"` passes with 8 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 235 (May 22, 2026)
- Ported Java `HousingService.revokeOwnershipOfDeletedPlayers` startup behavior into the C# world-house load: persistent custom-house rows with a nonzero `player_id` but no matching `players` row are treated as ownerless before visibility snapshots are built.
- Persisted the Java `changeOwner(house, 0)` reset for those deleted-owner rows: default building, `player_id=0`, null acquire/next-pay/sign fields, `showOwnerName=true`, and closed door settings.
- Cleared owner, legion, inactive, and sign fields from the world snapshot after revocation so `SM_HOUSE_RENDER`/`SM_HOUSE_UPDATE` no longer advertise deleted players.
- Current gaps in this cluster: the revocation path has compile/full-suite coverage but still needs opt-in live-DB validation with an orphaned `houses.player_id`; studios are still excluded from global world-house load and remain pending for personal-instance flow.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "HousingWorldServiceTests|StaticDataLoadingTests"` passes with 5 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 236 (May 22, 2026)
- Parsed Java `housing/house_buildings.xml` default building part IDs into `HousingBuildingSummary`, preserving the `Building.partsByType` data that Java `HouseRegistry.getUsedDecorId` uses as its fallback decor source.
- Updated the shared `SM_HOUSE_UPDATE`/`SM_HOUSE_RENDER` common-info writer to emit the 19 Java `PartType` decor lines from the house building defaults instead of zero placeholders, while retaining zero fallback for missing parts such as `ADDON`.
- Added static-data coverage for real Java building `353000` default parts and packet coverage proving both house update/render paths carry the populated decor line sequence.
- Current gaps in this cluster: player-registered decor and house-object registry persistence/packets remain pending, so the packet lines now match Java defaults but do not yet reflect owner-placed decorations.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "StaticDataLoadingTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads"` passes with 4 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 237 (May 22, 2026)
- Added Java `SM_HOUSE_REGISTRY` opcode `116` parity scaffolding for decoration mode registry lists: action `1` writes registered-but-not-spawned object rows, and action `2` writes default building part rows plus unused registered decorations.
- Split Java `Building.getDefaultPartIds` data from the 19-line render decor sequence so action `2` sends unique default part IDs in `PartType` order, matching `Building.partsByType`/`EnumMap` behavior instead of repeating room lines.
- Added packet coverage for `SM_HOUSE_REGISTRY` action `1` object rows and action `2` default-plus-unused decoration rows, plus static-data coverage for the real `353000` default part ID order.
- Current gaps in this cluster: C# now has the registry packet shell and default-part source, but player-registered item loading, `CM_HOUSE_EDIT` wiring, object spawn/despawn persistence, and useable-object extra data remain future slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "StaticDataLoadingTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads"` passes with 4 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 238 (May 22, 2026)
- Added Java `CM_HOUSE_EDIT` opcode `82` parsing for decoration add/delete/spawn/move/despawn and renovation building actions, preserving the Java read shapes for item object IDs, coordinates, rotation, and target building ID.
- Added simple `SM_HOUSE_EDIT` opcode `82` mode-action responses and wired active-house enter decoration mode to send Java's `SM_HOUSE_EDIT(1)`, `SM_HOUSE_REGISTRY(1)`, and `SM_HOUSE_REGISTRY(2)` sequence using the default-part registry shell.
- Wired exit decoration mode plus enter/exit renovation mode to echo `SM_HOUSE_EDIT` action bytes, matching Java's mode branches while leaving mutation-heavy add/move/delete/renovate behavior for later slices.
- Current gaps in this cluster: action `3`/`4`/`5`/`6`/`7` registry mutations, renovation coupon/building switch behavior, player-registered row loading, and spawned object visibility packets remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesHousingPackets"` passes with 2 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 239 (May 22, 2026)
- Loaded Java `housing/housing_objects.xml` into a C# `HousingObjectTemplateTable`, keyed like `dataholders/HousingObjectData` so future `HouseObjectFactory` and registry load work can resolve template metadata by housing-object template ID.
- Preserved the Java concrete `PlaceableHouseObject.getTypeId` mappings for passive/picture/move/movie objects (`0`), use item (`1`), storage (`2`), postbox (`3`), chair (`5`), jukebox (`6`), NPC (`7`), and emblem (`11`).
- Captured the fields needed by the next registry/object slices: placement area/location/limit/category, `use_days`, dyeability, NPC ID, storage warehouse ID, use-item owner/cooldown/delay/count/required-item data, and emblem level.
- Added real static-data coverage for the 1,511 Java housing-object templates plus representative chair, storage, NPC, and use-item records.
- Current gaps in this cluster: loaded template metadata is not yet consumed by DB registry loading, `CM_HOUSE_EDIT` add/register flows, spawned object visibility packets, or useable-object usage data serialization.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter StaticDataLoadingTests` passes with 3 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 240 (May 22, 2026)
- Added Java `SM_HOUSE_OBJECT` opcode `268` parity scaffolding for spawned house-object visibility rows, including address/owner/object/template IDs, coordinates, rotation, cooldown, expiration, dye info, type ID, and NPC object ID tail data.
- Added Java `SM_HOUSE_OBJECTS` opcode `270` parity scaffolding for the compact owned-object list packet that writes template IDs plus XYZ coordinates for each spawned object.
- Introduced `PlacedHouseObjectSummary` and a shared `HouseObjectPacketWriter` so future `PlayerController.see` and `HouseObjectFactory` work can feed the same packet shape without duplicating the Java dye-info block.
- Current gaps in this cluster: these packets are byte-shaped and tested, but no C# registry loader or house-object known-list path emits them yet; useable-object extra usage data remains a future packet-tail slice.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter CharacterSelectionServerPackets_WriteJavaShapedPayloads` passes with 1 test.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

### Session 241 (May 22, 2026)
- Loaded Java `player_registered_items` registry rows into C# `HouseRegistrySummary` data through `IHousingRepository`, preserving Java `PlayerRegisteredItemsDAO.loadRegistry` separation of `area='DECOR'` decoration rows from placeable house-object rows.
- Parsed Java `housing/house_parts.xml` `HousePart` IDs, types, and `building_tags`, preserved `Building.parts_match`, and applied Java deleted-decor validation for building-tag mismatch and non-palace room rows before exposing `HouseRegistry.getUnusedDecors` packet rows.
- Wired `CM_HOUSE_EDIT` decoration-mode entry to lazy-load and cache the active house registry, then send Java-shaped `SM_HOUSE_REGISTRY(1)` not-spawned objects and `SM_HOUSE_REGISTRY(2)` default parts plus unused registered decorations.
- Added registry model coverage for not-spawned/spawned object splitting, use-item usage-data bytes, expiration seconds, and invalid decor filtering, plus static-data coverage for real Java house part counts and tag/building compatibility.
- Current gaps in this cluster: spawned registry objects are loaded into summaries but are not yet emitted through a house-object known-list path; house-object cooldown persistence, richer useable-object action check-type tails, registry mutations, and renovation actions remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "PlayerHouseTests|StaticDataLoadingTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesHousingPackets|HousingWorldServiceTests"` passes with 10 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 479 tests.

### Session 242 (May 22, 2026)
- Added C# `SM_DELETE_HOUSE_OBJECT` opcode `269` parity for Java `SM_DELETE_HOUSE_OBJECT`, writing the house-object item ID used by `PlayerController.notSee`.
- Converted loaded registry objects with stored coordinates/headings into `PlacedHouseObjectSummary` rows, preserving Java `HouseObject.isSpawnedByPlayer`, heading-to-rotation, NPC tail ID, dye/expiration/type fields, and use-item usage-data bytes.
- Wired Java `CM_LEVEL_READY` active-house behavior so owners receive compact `SM_HOUSE_OBJECTS` for spawned active-house registry objects before the baseline self `SM_PLAYER_INFO` path.
- Attached registries to persistent `WorldHouse` snapshots during housing bootstrap and refreshed owner world-house snapshots after lazy registry load, allowing house known-list appearance to send individual `SM_HOUSE_OBJECT` packets and disappearance to send `SM_DELETE_HOUSE_OBJECT` before `SM_DELETE_HOUSE`.
- Current gaps in this cluster: known-list delivery is still tied to the first-pass world-house distance scan instead of true region-bucketed `KnownList`; house-object cooldown DB load/save and useable-object action check-type serialization are still pending, and mutation actions still need persistence.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "PlayerHouseTests|HousingWorldServiceTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads|GameClientSocketServer|StaticDataLoadingTests"` passes with 12 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

### Session 243 (May 22, 2026)
- Added DB-backed Java `HouseObjectCooldownsDAO` parity to player enter/logout: C# now loads active `house_object_cooldowns` rows into `Player.HouseObjectCooldowns` and stores future reuse times on logout after deleting stale rows.
- Wired `SM_HOUSE_REGISTRY` action `1` and known-list `SM_HOUSE_OBJECT` creation to compute cooldown seconds from the receiving player's loaded house-object cooldowns, matching Java `Cooldowns.remainingSeconds`.
- Added coverage for enter-world loading of house-object cooldowns and registry/placed-object cooldown second calculation.
- Current gaps in this cluster: the cooldown values can now round-trip and serialize, but actual useable house-object interaction still needs `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` handling before C# mutates those cooldowns itself; richer useable-object action check-type packet tails remain pending.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "PlayerEnterWorldServiceTests|PlayerHouseTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads|GameClientSocketServer|HousingWorldServiceTests"` passes with 16 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

### Session 244 (May 22, 2026)
- Expanded `SM_HOUSE_EDIT` opcode `82` beyond simple mode bytes: action `3` now writes registered-object add-back rows, action `4` writes edit-inventory removal rows, action `5` writes spawn/move object placement payloads, and action `7` writes despawn object IDs in the Java packet shapes.
- Added C# `CM_HOUSE_EDIT` placement/move/despawn handling for already registered house objects: action `5` updates stored XYZ/heading and sends Java `SM_HOUSE_EDIT(5)` plus `SM_HOUSE_EDIT(4, 1, objectId)`, action `6` sends Java `SM_HOUSE_EDIT(7, 0, objectId)` then `SM_HOUSE_EDIT(5)`, and action `7` clears placement and sends Java `SM_HOUSE_EDIT(7, 0, objectId)` then `SM_HOUSE_EDIT(3, 1, objectId)`.
- Added `IHousingRepository.SaveHouseObjectPlacementAsync` to persist the Java `player_registered_items` placement columns (`x`, `y`, `z`, `h`, `area`) for spawned/moved/despawned objects, using template area for placed objects and `NONE` for despawned objects.
- Refreshed the active player's cached registry and world-house snapshot after placement mutations so later registry packets and house known-list scans see the new object state.
- Current gaps in this cluster: action `3` add/register from inventory, action `4` delete/unregister, renovation action `16`, quest event callbacks, and broadcast/update side effects for already sighted non-owner players remain pending; this slice covers registered-object placement persistence and owner responses first.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesHousingPackets|PlayerHouseTests|HousingWorldServiceTests|GameClientSocketServer"` passes with 10 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

### Session 245 (May 22, 2026)
- Parsed Java `UseItemAction.check_type` from `housing/housing_objects.xml` nested `<use_item><action>` rows into `HousingObjectTemplateSummary.UseActionCheckType`.
- Updated `UseableItemObject.writeUsageData` parity so registered useable house objects now write total use count plus the Java action check-type byte instead of always writing zero.
- Added static-data coverage for real Java use-item template `3190001` check type `2` and registry usage-data coverage for the nonzero check-type tail.
- Current gaps in this cluster: the packet-tail metadata is now present, but `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` still need behavior, reward/item checks, cooldown mutation, and object-use update packets.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "StaticDataLoadingTests|PlayerHouseTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads"` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

### Session 246 (May 22, 2026)
- Implemented Java `CM_HOUSE_EDIT` action `4` delete/unregister for already registered house objects: C# now validates the object exists in the active-house registry, deletes the `player_registered_items` row, removes it from the cached registry/world-house snapshot, and sends the Java duplicate `SM_HOUSE_EDIT(4, 1, objectId)` responses.
- Added `IHousingRepository.DeleteHouseRegisteredObjectAsync` as the C# equivalent of `PlayerRegisteredItemsDAO.DELETE_QUERY`.
- Added registry model coverage for removing object summaries from the loaded registry.
- Current gaps in this cluster: action `3` register-from-inventory still needs item action parsing/ID allocation/inventory deletion, decoration delete/set-used behavior is still pending, and object ID release is not modeled yet.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter "PlayerHouseTests|CharacterSelectionServerPackets_WriteJavaShapedPayloads|ClientPacketFactory_ParsesHousingPackets|HousingWorldServiceTests"` passes with 8 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

### Session 247 (May 22, 2026)
- Parsed Java item-template `<houseobject>` and `<housedeco>` actions into `ItemTemplateSummary`, preserving the Java `DecorateAction` behavior where a missing decoration `id` still means an action exists with template ID `0`.
- Implemented Java `CM_HOUSE_EDIT` action `3` register-from-inventory for cube furniture/decor items: C# now validates the source cube item, allocates the registered item ID through `IDFactory`, creates either a `RegisteredHouseObjectSummary` from `SummonHouseObjectAction`/`HousingObjectData` or a `RegisteredHouseDecorationSummary` from `DecorateAction`, deletes the inventory row plus `item_stones`, inserts the `player_registered_items` row, refreshes the cached house registry/world-house snapshot, and sends Java-shaped `SM_HOUSE_EDIT(3, 1, object)` or `SM_HOUSE_EDIT(3, 2, decor)` responses.
- Added DB transaction coverage in `IHousingRepository` for inventory-to-registry object/decor registration, including Java `PlayerRegisteredItemsDAO.INSERT_QUERY` column shape and `HouseObjectFactory` `use_days` expiration handling.
- Expanded `SM_HOUSE_EDIT` action `3` serialization to support decoration store rows, and changed registry upsert helpers so newly registered objects/decorations are appended to the cached registry.
- Current gaps in this cluster: decoration set-used/delete mutations are still pending, action `16` renovation is still pending, quest callbacks/broadcasts for object placement are still pending, object ID release after registered-item deletion remains to be modeled, and future `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` behavior still needs reward/use-count/cooldown mutation.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

### Session 248 (May 22, 2026)
- Registered and parsed Java `CM_HOUSE_DECORATE` opcode `75`, including object ID, ignored template ID, and unsigned line-number fields.
- Added Java `PartType.getForLineNr` parity for decoration line mapping, including the client-line distinction for `ADDON`, plus render/update packet decoration resolution that now overlays used registered decorations on top of building defaults in Java `PartType.values()` room order.
- Implemented DB-backed decoration apply/default-revert mutations: object ID `0` deletes any applied custom decoration for the part/room, nonzero object IDs apply a registered decoration to the room, delete any previous decoration of the same part/room, delete the source row when applying a default decor, refresh the cached registry/world-house snapshot, send Java duplicate `SM_HOUSE_EDIT(4, 2, objectId)` responses for nonzero applies and one response for default revert, and broadcast `SM_HOUSE_UPDATE` appearance refreshes.
- Added `IHousingRepository.SaveHouseDecorationMutationAsync` as the C# equivalent of `PlayerRegisteredItemsDAO.storeDecors` update/delete branches.
- Current gaps in this cluster: renovation action `16`, `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT`, quest callbacks, object/decor ID release after delete, and richer sighted-player house-object update side effects remain pending.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

### Session 249 (May 22, 2026)
- Added Java `CM_USE_HOUSE_OBJECT` opcode `224` and `CM_RELEASE_OBJECT` opcode `225` parsers, preserving their single object-ID payloads.
- Added Java `SM_USE_OBJECT` opcode `197` and `SM_OBJECT_USE_UPDATE` opcode `264` packet serializers for the house-object gauge/update shapes used by `UseableItemObject`, `StorageObject`, and `PostboxObject`.
- Expanded `HousingObjectTemplateSummary` to carry Java `UseItemAction` runtime metadata (`remove_count`, `reward_id`, and `final_reward_id`) alongside the previously loaded `check_type`, so the next runtime slice has the required item-use mutation fields.
- Current gaps in this cluster: the parsers/packets/action metadata are ready, but `CM_USE_HOUSE_OBJECT` runtime still needs visibility/talk-range guards, occupant tracking, owner-only/storage/postbox behavior, required-item checks, delayed reward/remove-count mutation, use-count persistence, cooldown mutation, cancel/release handling, and object deletion on final use.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

### Session 250 (May 22, 2026)
- Implemented Java `CM_HOUSE_EDIT` action `16` renovation runtime: C# now validates the target building, consumes the Java race/type-specific renovation coupon from cube inventory, persists the `houses.building_id` update and coupon mutation in one housing transaction, reloads the registry against the new building defaults, refreshes the cached player/world-house snapshot, and broadcasts `SM_HOUSE_UPDATE`.
- Added `IHousingRepository.SaveHouseRenovationAsync` as the C# equivalent of the `removeRenovationCoupon` plus `HousesDAO.storeHouse` mutation boundary.
- Current gaps in this cluster: `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` runtime, quest callbacks, object/decor ID release after delete, studio house spawning, GeoService door state updates, live-DB orphan-house validation, and richer sighted-player house-object update side effects remain pending.
- Validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

### Session 251 (May 22, 2026)
- Implemented the first Java `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` runtime branch for useable storage/postbox house objects: C# now resolves spawned registry objects from visible world-house snapshots or the owner's active registry, enforces Java `talking_distance + 1` range checks, owner-only storage access, atomic useable-object occupants, and release cleanup on object release/logout.
- Parsed Java house-object `name_id` and `talking_distance` metadata into `HousingObjectTemplateSummary`, allowing object-use system messages to send `ChatUtil.L10n(name_id)` and the runtime to match `PositionUtil.isInTalkRange(player, HouseObject)`.
- Added Java `SM_DIALOG_WINDOW` opcode `60` for the postbox `DialogPage.MAIL` branch, plus housing object occupied/use/cancel/too-far/owner-only `SM_SYSTEM_MESSAGE` helpers.
- Storage now sends Java-shaped `SM_OBJECT_USE_UPDATE` after successful owner use, and postboxes set `Player.MailboxState` to regular, send the mail dialog window, then send the same use update; release clears the transient occupant and sends the Java postbox cancel message.
- Current gaps in this cluster: the larger `UseableItemObject` branch is still pending, including owner/visitor use-count checks, required-item/remove-count consumption, delayed `SM_USE_OBJECT` gauge completion, reward/final-reward grants, use-count persistence, cooldown mutation, and final-use deletion/cooldown cleanup.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 275 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

### Session 252 (May 22, 2026)
- Implemented the first Java `UseableItemObject.onUse` runtime path for normal use-item house objects with no final-reward handoff: C# now validates owner-only access, object cooldowns, owner/visitor use-count caps, required equipped/cube item checks, inventory-full guards, atomic occupants, delayed `SM_USE_OBJECT` gauge start/completion, and release/logout cancellation.
- Added `IHousingRepository.SaveHouseObjectUseAsync` to persist the Java scheduled-use mutation boundary: required-item count updates/deletes, reward stack updates/inserts, `player_registered_items` owner/visitor use-count updates, and final-use row deletion are stored in one transaction.
- Added `RegisteredHouseObjectSummary.WithUseCounts` so `UseableItemObject.writeUsageData` bytes are regenerated after owner/visitor count changes, matching Java's total-use-count plus action-check-type tail.
- Normal reward completion now consumes the required item, adds the reward through the existing Java `ItemService.addItem`-style planner, sends Java house-object reward messages, broadcasts `SM_OBJECT_USE_UPDATE`, applies cooldowns, and deletes exhausted no-final-reward objects with the Java `HouseObject.despawnAndRemoveHouseObject(false)` packet sequence.
- Current gaps in this cluster: final-reward / `mustGiveLastReward` behavior for expired useable items is intentionally blocked rather than approximated; cooking-placement duplicate-reward denial, delete-expire-time messaging, owner-only final-reward recovery, and fuller house-object expiration scheduling still need a dedicated Java-parity slice.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 275 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

### Session 253 (May 22, 2026)
- Finished the Java `UseableItemObject` final-reward handoff for house use items: C# now allows final-reward templates through, restores `mustGiveLastReward` from expired `player_registered_items.expire_time`, blocks visitors with `STR_MSG_HOUSING_OBJECT_DELETE_EXPIRE_TIME`, and lets the owner claim the final reward on the follow-up use.
- Preserved absolute registered-object `expire_time` alongside packet-facing remaining `ExpirationSeconds`, and updated `SaveHouseObjectUseAsync` so reaching `use_count` on a final-reward object persists the Java `setExpireTime(now)` handoff instead of deleting the row immediately.
- Reworked reward selection to match Java's scheduled task: normal rewards are granted at each use, final-reward objects send the flowerpot goal and mark final pending at `use_count`, no-final-reward objects delete at `use_count`, and final rewards delete at `use_count + 1`.
- Added Java cooking-placement duplicate-reward denial through `STR_MSG_CANNOT_USE_ALREADY_HAVE_REWARD_ITEM`.
- Current gaps in this cluster: full `ExpireTimerTask` handling for registered house objects is still pending, including automatic `HouseObject.onExpire` deletion with `STR_MSG_HOUSING_OBJECT_DELETE_EXPIRE_TIME`, cooldown cleanup for all players, and object-ID release after registry deletion.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 275 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

### Session 254 (May 22, 2026)
- Extended the C# `ExpireTimerTask` bridge to loaded active-house registry objects, matching Java `PlayerEnterWorldService` registration once `HouseRegistry` data is available.
- Added house-object `canExpireNow` parity for the important loaded cases: emblems never expire automatically, and expired final-reward `UseableItemObject` rows remain registered so the owner can recover the final reward instead of losing the object.
- Wired expired registered house objects through `HouseObject.onExpire`-style deletion: the C# path deletes the `player_registered_items` row, sends Java `SM_HOUSE_EDIT(7)` / `SM_DELETE_HOUSE_OBJECT` / `SM_HOUSE_EDIT(4)` plus `STR_MSG_HOUSING_OBJECT_DELETE_EXPIRE_TIME`, refreshes the cached player/world house registry, clears the discarded object cooldown from online players, and releases the object ID.
- Broadened registered-object discard cleanup so final-use deletion and `CM_HOUSE_EDIT` object deletion also remove online-player house-object cooldowns and release IDs.
- Current gaps in this cluster: the C# house-object expiration bridge does not yet model `NpcObject.canExpireNow` target checks or useable-object occupant checks beyond final-reward deferral; studio spawning and richer NPC/dialog known-list validation remain separate housing/NPC slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore` passes with 276 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 483 tests.

### Session 255 (May 22, 2026)
- Added a runtime `canExpireNow` callback to the loaded house-object `ExpireTimerTask` bridge so C# can preserve Java's live object guards without baking connection state into `ExpirableTaskService`.
- Wired the game runtime callback to Java `UseableHouseObject.canExpireNow` semantics for use-item, storage, and postbox objects by deferring expiration while `CM_USE_HOUSE_OBJECT` occupant state is still held.
- Added the Java `NpcObject.canExpireNow` target guard against the currently available housing-NPC object ID representation, deferring expiration while any online player has that NPC selected.
- Added coverage that expired use-item, storage, postbox, and NPC house objects remain registered while runtime state reports them busy, then expire on the next tick once that callback allows it.
- Current gaps in this cluster: studio spawning, GeoService door state updates, live-DB orphan-house validation, visitor kick side effects, and fuller NPC/dialog known-list/function validation remain separate housing/NPC slices.
- Validation: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore --filter ExpirableTaskServiceTests` passes with 7 tests.
- Full validation: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 484 tests.

---

## Next Steps

1. Continue AP rank-change side effects beyond the current owner/visible-player packets, AP/login rank-limited equipment passes, configured abyss transform skill updates, and rank config load: add legion contribution fanout and `SiegeService.onAbyssPointsAdded` callback coverage once those supporting systems have C# homes.
2. Broaden the expirable lifecycle bridge to Java's remaining registered expirable types, pets and house objects, once the missing pet/house-object models and persistence surfaces exist.
3. Finish the remaining stigma/effect slice: full `SkillEngine` effect application after temporary skill mutations and the corresponding stat/effect removal fanout.
4. Continue `CM_EMOTION` only if the next slice first introduces one missing support model: full fly-zone/cooldown/FP timers, stance observers, sit observers, quest/summon observers, or reusable stat-speed calculation.
5. Wire charge, power-shard, and idian burn triggers into the future skill/combat observer paths: `ChargeInfo`, `PolishChargeCondition`, `Equipment.usePowerShard`, `IdianStone.onEquip` attack/defend observers, low-charge update packets, zero-charge deletion, and stat refresh fanout.
6. Continue housing from the new world-house baseline: fill the remaining house-object/NPC edges, especially studio spawning, GeoService door state updates, live-DB orphan-house validation, visitor kick side effects, or full NPC/dialog known-list/function validation.
7. Real-client validate scheduled item-use ordering for decompose, assembly, XP extraction, composition, extraction, and AP extraction once the readiness pass begins.
