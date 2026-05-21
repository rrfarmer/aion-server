# Phase 6B Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 is still in progress; this document intentionally keeps only handoff context and remaining work.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 320 tests.

---

## Resume Snapshot

- Phase 5 automated infrastructure parity is complete. Real-client validation remains deferred to end-of-port readiness.
- Phase 6 active area is GameServer gameplay parity, especially enter-world, inventory/equipment, movement/known-list, housing, logout/save, and later core gameplay systems.
- Current C# GameServer already has broad enter-world coverage: common player row, appearance, inventory/warehouse item rows with item-stone detail display, skills, cooldowns, quests, titles, motions, emotions, recipes, macros, mailbox, broker settlement summary, houses, craft/portal cooldowns, life stats, social lists with friend active-house fields, abyss rank, client settings, bind point, enter-world packet sequence, movement basics, social/chat/mail/broker/housing surfaces, and logout baseline persistence.
- Most recent completed slices:
  - `CM_SHOW_RESTRICTIONS` opcode `194` now mirrors Java `/restriction` handling by returning `SM_SYSTEM_MESSAGE.STR_MSG_ACCUSE_INFO_NORMAL` (`1400076`) for the active player.
  - `CM_INSTANCE_INFO` opcode `192` now parses Java's request layout and returns `SM_INSTANCE_INFO(updateType, player)` for the current no-team player branch.
  - `PeriodicSaveService` now mirrors Java's `ServerRunTimeSaveTask` by periodically storing `server_variables.serverLastRun` and writing it again on shutdown.
  - Game-time periodic updates now broadcast Java-shaped `SM_GAME_TIME` to all online players through a new world-wide packet fanout helper before saving `server_variables.time`.
  - `GameTimeService` now mirrors Java `ServerVariablesDAO` time recovery/persistence by loading `server_variables.time`, periodically storing it, and saving again on shutdown.
  - `CM_REVIVE`, `CM_QUESTIONNAIRE`, `CM_START_LOOT`, `CM_LOOT_ITEM`, `CM_SUBZONE_CHANGE`, and `CM_CHANGE_CHANNEL` now parse Java wire layouts and route through explicit deferred handlers until revive, reward, drop, zone, and channel services are ported.
  - `CM_MACRO_CREATE`, `CM_MACRO_DELETE`, and `SM_MACRO_RESULT` now mutate loaded macros and persist Java `player_macrosses` rows.
  - `CM_REJECT_REVIVE` opcode `146` now mirrors Java's empty-payload, no-op packet.
  - `SM_PLAYER_INFO` now writes Java's legion member block with loaded legion ID/name and emblem type/color fields.
  - `CM_HEADING_UPDATE` opcode `147` now parses Java's spin/heading byte and remains a no-op like Java `runImpl`.
  - Player enter-world now loads legion membership/name from `legion_members`/`legions`, and personal `SM_CHAT_WINDOW` writes Java's legion-name field.
  - Account membership from login auth is now attached to the active C# player and serialized in Java's `SM_CHAT_WINDOW` VIP byte and `SM_PLAYER_INFO` membership marker.
  - `SM_PLAYER_INFO` now fills Java's selected-target object ID and active-house address fields from loaded player state; team and mentor fields remain explicit defaults until team/mentor systems are ported.
  - `CM_SET_NOTE` and `SM_UPDATE_NOTE` now mirror Java note updates: `players.note` loads/saves with common data, chat-window/player-info packets write the note, online friends get refreshed friend-list snapshots, and visible players receive the note update.
  - `CM_CUSTOM_SETTINGS` and `SM_CUSTOM_SETTINGS` now update and fan out Java display/deny bitmasks, sharing the settings persistence path.
  - `CM_UI_SETTINGS` now mirrors Java client setting updates for UI/shortcut/house-buddy blobs, and logout persistence writes Java `PlayerSettingsDAO.saveSettings` rows.
  - `CM_FRIEND_ADD` now honors Java `DeniedStatus.FRIEND` target-side settings loaded from `player_settings` rows, returning `STR_MSG_REJECTED_FRIEND` before question-window creation.
  - `SM_FRIEND_LIST` now writes Java `HousingService.findActiveHouse` address and door-state fields for friends, backed by loaded DB house settings and online friend snapshot refreshes.
  - Housing maintenance timing now has Java `MaintenanceTask.calculateImpoundDate` and `MailFormatter.sendHouseMaintenanceMail` stage-selection parity, and rent payment uses the shared maintenance timing service.
  - Housing auction startup-recovery timing now mirrors Java `AuctionEndTask.shouldRunOnStart`, including the 30-minute post-auction prolongation recovery window.
  - Housing login maintenance notices now mirror Java `HousingService.onPlayerLogin` for overdue active houses and final/third overdue mailbox sequester notices.
  - Active-house town level in `SM_HOUSE_OWNER_INFO` now follows Java `House.getTownLevel`, using housing address town IDs from `houses.xml` and DB-backed `towns.level` values.
  - Inactive-house grace seconds in `SM_HOUSE_OWNER_INFO` now follow Java `House.findGraceEndTime`, ending at the last configured auction-end run before the two-week cap.
  - Housing auction and maintenance timing now parse Java weekly `CronExpression` strings such as `0 0 12 ? * SUN` instead of hard-coding only the default schedules.
  - Housing auction timing now mirrors Java `AuctionEndTask.tryProlongAuction` for the default Sunday-noon auction end, including per-house five-minute prolongations capped at thirty minutes and prolonged `SM_HOUSE_BIDS` countdowns.
  - `CM_HOUSE_SETTINGS` with DB-backed door/show-owner/sign state, `SM_HOUSE_ACQUIRE`, and Java door-order system messages.
  - `CM_TITLE_SET` and `CM_BONUS_TITLE` with loaded-title validation and Java-shaped `SM_TITLE_INFO`.
  - `CM_MOTION` with Java motion-type active slots, DB-backed `player_motions.active` updates, self `SM_MOTION(action=5)`, and visible-player `SM_MOTION(action=7)` broadcast.
- Use Java files under `game-server/src/com/aionemu/gameserver/...` as the source of truth for packet layouts, guard order, persistence behavior, and side effects.

---

## Remaining Work

### Cross-Cutting
- Add focused live-DB opt-in coverage for creation and enter-world once local schema fixtures are ready.
- Keep improving packet parity tests whenever a new client/server packet surface is added.
- Preserve Java guard order even when some side effects are deferred because a dependency is not ported yet.
- Continue adding C# breadcrumbs that name the Java class/method being mirrored.

### Phase 6a: Account And Character Flow
- Add an account object/session model beyond infrastructure auth fields.

### Phase 6c: Enter World And Player Graph
- Finish full player object graph loading. Current graph is broad but still partial.
- Inventory/equipment stat application remains pending. Item rows and item-stone packet display are loaded/serialized, but equipped item stats are not applied.
- Equipment-dependent player state needs more Java parity, including exact stats, transforms, ride/stance, store state, team/mentor data, CP fields, and enemy-race viewer handling in `SM_PLAYER_INFO`.
- Broker paths still need Java's full NPC `DialogAction.OPEN_VENDOR` function validation once NPC/known-list systems are ported.
- `CM_READ_EXPRESS_MAIL` postman spawn/delete works for the owner, but full sight-range known-list fanout remains incomplete.
- Title gaps: bonus-title stat modifier application and nearby quest refresh side effects remain pending.
- Motion gaps: motion acquisition/removal item actions and expiration timers remain pending.

### Housing
- Continue housing maintenance task parity:
  - Full `MaintenanceTask` impound/auction behavior.
  - Maintenance overdue mail persistence/delivery.
- Continue auction-end settlement parity:
  - Scheduled `AuctionEndTask.endAuction` execution using the startup-recovery timing helper.
  - Auction-end settlement.
  - Seller/buyer result mail beyond failed-bid refunds.
  - Auto-fill behavior.
  - Live-client packet timing.
- Continue house sign/appearance/update parity:
  - House appearance known-list fanout.
  - GeoService door-state updates.
  - Visitor kick side effects for door settings.

### Chat And Social
- Public chat still lacks chat commands, full `PlayerRestrictions.canChat`, message name filtering, group/alliance/league/legion/commander/channel chat, per-recipient staff race-filter suppression, and full chat logging.
- Whisper still lacks no-whispers custom state, `PlayerRestrictions.canChat`, `NameRestrictionService.filterMessage`, GM/staff chat logging, exact `ChatUtil` name-tag parsing, and per-recipient staff race-filter suppression.
- Chat info still lacks real group/alliance chat-window branches, persistent known-list membership, and exact name-tag parsing.
- Chat auth still lacks chat-ban/gag follow-up, bridge reconnect replay for pending player auths, and real-client validation of the ChatServer endpoint advertised by `SM_VERSION_CHECK`.
- Social still lacks offline social request handling and generic `ResponseRequester` support beyond buddy requests.

### Movement And Known List
- Port movement anti-hack, protection, fall/glide side effects, and exact flying-state gates.
- Add persistent known-list object enter/leave/update fanout for players and NPCs.
- Complete `SM_PLAYER_INFO` dependent state for transforms, exact stats, store, ride/stance, team/mentor, CP, and viewer-specific enemy race handling.
- Broaden the known-list bridge beyond current visible-player broadcasts for movement, enter, logout, and postman spawn/delete.

### Logout, Saves, And Recovery
- Persist effects, quests, inventory dirty state, and social/group/legion logout side effects.
- Add periodic player/inventory save task and broader recovery behavior. Game-time recovery/persistence and `serverLastRun` runtime saves are now ported.
- Align dirty-save behavior with Java for houses, inventory, quest state, effects, and social/team systems as those systems come online.

### Later Phase 6 Core Gameplay
- NPC and spawn engine basics.
- Skills and effects.
- Combat and damage.
- Loot and item use.
- Quest state mutation beyond current persistence/list packet surfaces.
- Periodic world/gameplay services that Java expects around those systems.

---

## Suggested Next Units

1. Housing auction settlement/end or maintenance task parity, because housing mutation surfaces are already partially ported.
2. Equipment stat application, because inventory/equipment loading and item template summaries are already in place.
3. Persistent known-list membership and richer `SM_PLAYER_INFO`, because movement and visible-player broadcast scaffolding already exists.
4. A compact remaining chat/group packet surface if a smaller next commit is preferred.
