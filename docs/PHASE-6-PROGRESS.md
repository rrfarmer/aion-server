# Phase 6: Port Game Core - Detailed Plan & Progress

**Status**: IN PROGRESS (started May 20, 2026)  
**Target**: Port gameplay systems in Java dependency order while keeping database and packet behavior compatible.  
**Validation Approach**: Parity/unit/integration tests first; real-client validation remains an end-of-port readiness step.  
**Code Trace Convention**: GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior they mirror.

---

## Resume Snapshot

Last updated: May 20, 2026

- Phase 5 is complete for automated infrastructure parity. Real-client validation is intentionally deferred to later readiness validation.
- Current active work is Phase 6c enter-world. The C# path handles `CM_ENTER_WORLD`, validates missing/online/reentry/duplicate-world cases, loads the player common row and player-owned inventory rows, marks the character online, stores it in the world container, transitions the connection to `InGame`, and sends `SM_ENTER_WORLD_CHECK`.
- Character creation is DB-backed and writes `players`, `player_appearance`, `player_skills`, and starter `inventory` rows. It uses Java-style starter items, equipment-slot selection, level-1 autolearn skills, old-name reservation checks, and membership character limits.
- Startup now preloads `IDFactory` from Java-equivalent used-ID tables before gameplay allocation.
- Next implementation slice should expand enter-world object loading with skills, quests, recipes, settings, life stats, cooldowns, bind point, warehouse/account data, then begin the Java retail packet sequence after `SM_ENTER_WORLD_CHECK`.
- Latest validation before this housekeeping pass: `dotnet test dotnetConversion\AionServer.slnx` passed with 258 tests.

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
- [ ] Load full player object graph (partial: common player row plus inventory/equipment item rows)
- [x] Java-shaped `CM_ENTER_WORLD` gate checks for missing character, online/reentry state, duplicate world presence
- [x] Mark player online, update `last_online`, transition connection to in-game, and send `SM_ENTER_WORLD_CHECK`
- [ ] Inventory/equipment load and stat application (partial: typed inventory rows loaded; stat application pending)
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

---

## Next Steps

1. Expand enter-world object loading with skills, quests, recipes, settings, life stats, cooldowns, bind point, and warehouse/account data.
2. Start the Java retail enter-world packet sequence after `SM_ENTER_WORLD_CHECK`, beginning with skill list and inventory info.
3. Add focused live-DB opt-in coverage for creation and enter-world once local schema fixtures are ready.
