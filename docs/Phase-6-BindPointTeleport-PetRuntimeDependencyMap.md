# Phase 6 UOW-1310 - Pet Runtime Dependency Map

Date: May 27, 2026

## Scope

This read-only unit maps the Java runtime dependencies behind the newly ported pet parser/packet surfaces. It intentionally makes no C# runtime changes. The goal is to prevent the next live `CM_PET` / `CM_PET_EMOTE` slice from accidentally skipping Java side effects, ordering, timers, persistence, or mutation semantics.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetAdoptionService.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetMoodService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Pet.java`

## Runtime Dependency Map

| Java Entry | Runtime Dependencies | Packet Outputs | Persistence / Timers | C# Status |
|---|---|---|---|---|
| `CM_PET.ADOPT` -> `PetAdoptionService.adoptPet` | active player, inventory egg object lookup, item template action metadata, pet template lookup, duplicate-pet check, name conversion | `SM_PET(PetCommonData, true)` | consumes egg by object id, inserts `player_pets`, computes expire epoch seconds, registers `ExpireTimerTask` | Parser and adopt packet exist; runtime not started |
| `CM_PET.SURRENDER` -> `PetAdoptionService.surrenderPet` | player pet list, active pet controller, IDFactory | `SM_PET(PetCommonData, false)` | deletes pet from player list/DAO path, releases object id | Parser and surrender packet exist; runtime not started |
| `CM_PET.SPAWN` -> `PetSpawnService.summonPet` | current active pet, last-used pet common data, player task controller, `VisibleObjectSpawner.spawnPet`, pet template functions | `SM_PET(PetSpecialFunction.AUTOLOOT/AUTOSELL, true)` when state already active; spawn visibility packets elsewhere | schedules periodic `PetController.PetUpdateTask`; schedules refeed or sets hungry; clears mood after long despawn; updates last-used template id | Parser, spawn packet, special-function packet exist; runtime not started |
| `CM_PET.DISMISS` -> active pet controller delete | active pet controller | pet dismiss visibility through controller/known-list paths | despawn time/mood save likely via controller/update task path | Parser and dismiss packet exist; runtime not started |
| `CM_PET.FOOD` feed/cancel/not-hungry -> `PetService.removeObject` and `checkFeeding` | inventory item by object id, pet feed function/flavour static data, player level, item template level, feed result table, item service | `SM_PET` food subtypes `1`, `2`, `5`, `6`, `7`, `8`; `SM_EMOTION` start/end feeding; system messages | 2.5s scheduled feed checks; decreases item count; schedules refeed delay; persists reuse time; resets feed progress | Parser exists; `SM_PET.FOOD` packet branch and runtime not started |
| `CM_PET.FOOD` doping -> `PetService.useDoping` | pet doping bag, pet doping static template, player spawn state, inventory item by template id, item use restrictions, item reuse timers, skill-use actions, SkillEngine | `SM_PET` dope actions `0`, `1`, `2`, `3`; `SM_ITEM_USAGE_ANIMATION` for skill-use items | may schedule 5s/20s/reuse-time retries; mutates synchronized doping bag; consumes item; adds item cooldown; bag save handled separately | Parser and `SM_PET` doping packets exist; live runtime not started |
| `CM_PET.FOOD` autoloot/autosell -> `PetService.activateLoot` / `activateAutoSell` | pet function support, current team loot rules, merchant function metadata, audit logger | `SM_PET(PetSpecialFunction.AUTOLOOT/AUTOSELL, active)` plus system messages | mutates `PetCommonData.isLooting/isSelling`; persistence path not yet identified in this audit | Parser and special-function packets exist; runtime not started |
| `CM_PET.RENAME` -> `PetService.renamePet` | `NameRestrictionService`, active pet, `Util.convertName` | broadcast `SM_PET(int, String)` to visible players including self | `PlayerPetsDAO.updatePetName` | Parser and rename packet exist; runtime not started |
| `CM_PET.MOOD` -> `PetMoodService.checkMood` | active pet, mood/gift cooldowns, inventory fullness, condition reward item, mood counters | `SM_PET` mood subtypes `0`, `2`, `3`, `4`; inventory-full system message | mutates mood start/counter/cooldowns; can add reward item; mood DAO save path is separate | Parser exists; `SM_PET.MOOD` packet branch and runtime not started |
| `CM_PET.EXTEND_EXPIRATION` | Java reads item object id and pet object id | none | Java runImpl intentionally does nothing | Parser body still deferred; runtime blocked |
| `CM_PET_EMOTE` -> `World.updatePosition` / move controller / broadcast | active spawned pet, coordinate validity, pet known-list visibility, master inclusion rule for `EMOTION` | `SM_PET_EMOTE` movement/default packets | mutates pet world position and move-controller target | Parser and server packet exist; runtime not started |

## Key Java State Types

- `PetCommonData` mixes immutable ids with mutable state: decoration/name, feed progress, doping bag, cancel-feed flag, refeed time, mood timing/counters, expire time, despawn time, looting/selling flags, and a volatile refeed task.
- `PetFeedProgress.getDataForPacket()` bit-packs regular consumed count, total points, loved consumed count, and an unknown nibble. C# currently only accepts supplied packet data in snapshots.
- `PetDopingBag` has synchronized slot mutation, dynamic array length, dirty flag, and scroll-only switch rules for slots `>= 2`.
- `PlayerPetsDAO` loads/saves feed status, doping bag comma-separated item ids, pet name, mood data, despawn time, and pet rows.
- `Pet` is a `VisibleObject` with a `CreatureMoveController`, master player, template, and common data.

## Ordering / Timing Hazards

- Food feeding is asynchronous: Java sends start-feed packets, then schedules a 2.5s check that may recursively schedule more checks.
- Doping use can schedule retries at 5s, 20s, or item reuse-delay duration before sending the dope-action packet.
- Adoption computes expiration from wall-clock epoch seconds and registers the expirable task immediately after the adopt packet.
- Spawn adds a periodic pet update task before attempting `VisibleObjectSpawner.spawnPet`; invalid spawns audit and return after task scheduling.
- Mood point calculations mutate `startMoodTime` lazily and cap packet-facing points at 9000 only when requested for packet output.
- `CM_PET_EMOTE` rejects negative coordinates before world mutation; unknown emotes only log and return.

## Recommended Live-Port Order

1. Add C# pet persistence/model foundation: `PetCommonData`, pet list, DAO read/write, and packet snapshot hydration from loaded DB rows.
2. Add `SM_PET.FOOD` and `SM_PET.MOOD` packet-only snapshot branches before runtime.
3. Add no-send runtime planners for adopt/surrender/spawn/dismiss/rename that produce ordered side-effect descriptors.
4. Add feed/doping/mood planners with explicit timer descriptors rather than immediate scheduler use.
5. Only then wire a guarded `GameServerConnection` dispatch path for selected `CM_PET` branches.
6. Defer live `CM_PET_EMOTE` world mutation until pet world object, move controller, known-list membership, and visibility fanout are real.

## Migration Parity Table - UOW-1310

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` | `Aion.GameServer.Network.Aion.ClientPackets.CmPet` plus future runtime dispatch | Client Packet / Handler | Partial | Manual Only in this unit | Partial Parity | Parser exists. Runtime side effects are mapped but not ported. Missing adoption, surrender, spawn, dismiss, food, doping, autoloot/autosell, rename, mood, and expiration behavior. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ClientPackets.CmPetEmote` plus future runtime dispatch | Client Packet / Handler | Partial | Manual Only in this unit | Partial Parity | Parser exists. Runtime active-pet guard, coordinate validation, world update, move-controller mutation, logging, and fanout are unported. |
| `com.aionemu.gameserver.services.toypet.PetAdoptionService` | future C# pet adoption service/planner | Service | Not Started | Manual Only | Needs Verification | Requires inventory, static item action metadata, pet list/DAO, ID allocation, expiration timer, and adopt/surrender packet ordering. |
| `com.aionemu.gameserver.services.toypet.PetSpawnService` | future C# pet spawn service/planner | Service | Not Started | Manual Only | Needs Verification | Requires active pet controller, periodic update tasks, visible-object spawner, pet common data, feed/mood state, and special-function restore packets. |
| `com.aionemu.gameserver.services.toypet.PetService` | future C# pet service/planner | Service | Not Started | Manual Only | Needs Verification | Requires feed scheduler, feed static data, inventory mutation, item service, doping bag, item cooldowns, SkillEngine, team loot rules, trade service, autosell filtering, and DAO save paths. |
| `com.aionemu.gameserver.services.toypet.PetMoodService` | future C# pet mood service/planner | Service | Not Started | Manual Only | Needs Verification | Requires mood point/cooldown mutation, inventory-full guard, reward item add, mood packets, and DAO save path. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future C# pet common-data model plus existing packet snapshots | Model | Partial | Manual Only | Needs Verification | Packet snapshots exist, but live mutable common data, volatile cancel/refeed task semantics, wall-clock mood/refeed calculations, expiration, despawn time, and looting/selling flags are unported. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | future C# doping bag plus `SmPetDopingSpecialFunctionSnapshot` | Model / Packet DTO | Partial | Manual Only | Needs Verification | Packet snapshot exists. Live synchronized slot mutation, dirty flag, dynamic item array, and scroll-slot switch semantics remain unported. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | future C# feed progress plus `SmPetFunctionSnapshot.FeedProgressData` | Model / Bit Packing | Partial | Manual Only | Needs Verification | Packet-facing supplied value exists. Java bit packing, loved/regular counts, hungry level, reset behavior, and saved-data decode are unported. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO` | future C# pet repository | Repository | Not Started | Manual Only | Needs Verification | Requires Java-compatible SQL for pet rows, feed status, doping CSV, mood data, name update, remove, and used-id preload. |

## Tests Added

No tests were added in this read-only audit unit. Existing packet/parser tests from UOW-1306 through UOW-1309 remain the current automated coverage.

## Remaining Risks

- No code changed in this unit; all live pet runtime behavior remains unported.
- Threading differences are high risk: Java uses volatile fields, synchronized `PetDopingBag.setItem`, scheduled tasks, and controller task maps.
- Serialization gaps remain for `SM_PET.FOOD` and `SM_PET.MOOD`.
- Date/time gaps remain for expiration epoch seconds, birthday timestamps, refeed delay, mood cooldown, gift cooldown, and despawn age.
- Persistence gaps remain for `player_pets`, feed status, doping CSV, mood data, despawn time, and name changes.
- Runtime packet ordering is source-reviewed but not tested or captured from Java.
- Java runtime packet vectors are still missing.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 dependency-map document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: live pet common-data model, pet repository, pet list, adoption, surrender, spawn, dismiss, food, doping, autoloot/autosell, autosell sale flow, rename, mood, `CM_PET_EMOTE` world mutation, timers, persistence, Java runtime vectors, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Implement the next packet-only prerequisite before live mutation: audit and port `SM_PET.FOOD` from supplied snapshots, because food runtime depends on the largest number of scheduled and persistence side effects. Keep `PetFeedProgress` bit packing as a separate helper or supplied snapshot field until the model is ported.

