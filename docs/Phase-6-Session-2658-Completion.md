# Phase 6 Session 2658 Completion

## UOW

[Phase 6] UOW-2658: Persist live periodic player item stones.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: the live periodic player item save now persists item-stone rows instead of only flushing dirty/deleted inventory rows.
- Java source/runtime path: PlayerEnterWorldService.ItemUpdateTask.run -> InventoryDAO.store(player) -> ItemStoneListDAO.save(player), with Player.getAllItems supplying inventory, warehouse, account warehouse, pet bag, cabinet, and equipment items.
- C# runtime artifact wired: MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync now calls item_stones replacement helpers for the currently modeled player item collections.
- Client-visible/state/persistence effect: modeled live manastone, fusion stone, godstone proc count, and idian polish state on player inventory/equipment/warehouse/account-warehouse items can survive periodic item saves without requiring logout.
- Why this is runtime progress: this mutates the existing item_stones database shape from the live scheduled periodic item persistence callback; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
  - `ItemUpdateTask.run` looks up the player from `World`, then calls `InventoryDAO.store(player)` and `ItemStoneListDAO.save(player)`.
- `game-server/src/com/aionemu/gameserver/dao/ItemStoneListDAO.java`
  - `save(Player player)` delegates to `save(player.getAllItems())`.
  - `save(List<Item> items)` persists categories `0` manastone, `1` godstone, `2` fusion stone, and `3` idian.
  - Java tracks stone persistent state and issues insert/update/delete SQL, then marks stones updated.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getAllItems()` includes inventory, regular warehouse, account warehouse, pet bags, cabinets, and equipped items.

## C# Changes

- Extended `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` so the live periodic item callback:
  - flushes dirty/deleted inventory rows as before,
  - then snapshots item-stone rows for modeled current player items,
  - then marks dirty item state persisted only after both steps succeed.
- Added `GetPlayerItemStoneSnapshotItems(Player)` as the modeled C# equivalent of the Java `Player.getAllItems()` item-stone source:
  - cube/equipment items,
  - regular warehouse items,
  - account warehouse items,
  - excluding deleted/no-action item states and deduping by item object id.
- Added `ReplaceInventoryItemStonesAsync`, reusing existing Java-category row serialization from `BuildItemStonePersistenceRows`.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetPlayerItemStoneSnapshotItems_ReturnsModeledCurrentPlayerItems` | Unit/runtime helper | `Player.getAllItems()` | The C# snapshot source includes modeled current inventory/equipment/warehouse/account-warehouse items and excludes deleted rows. | Directly exercises the item source used by `SavePeriodicPlayerItemsAsync`. | Does not cover Java pet bags or cabinets because this C# player model path does not expose them here. |
| `SavePeriodicPlayerItemsAsync_ReplacesCurrentItemStonesEvenWhenItemRowIsCleanAgainstJavaSchema_WhenEnabled` | Gated DB integration | `ItemUpdateTask.run -> ItemStoneListDAO.save(player.getAllItems())` | A clean item row can still have its `item_stones` rows replaced by periodic item persistence. | Uses the existing Java schema table and all four item-stone categories. | The DB body is guarded by `AION_GAMESERVER_DB_INTEGRATION=1`; in this environment it compiled but returned early. |

## Validation Decision

```text
- Changed surface: live periodic inventory persistence repository implementation and item-stone SQL helper reuse.
- Specific behavior/contract: Java periodic item save persists item_stones after InventoryDAO.store(player), even when item-stone state changes are separate from dirty inventory-row fields.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryItemStonePersistenceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests.SavePeriodicPlayerItemsAsync_ReplacesCurrentItemStonesEvenWhenItemRowIsCleanAgainstJavaSchema_WhenEnabled" --no-restore
- Focused Java/Maven command: not run; no narrow Java test fixture exists for ItemUpdateTask/ItemStoneListDAO in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: repository persistence surface changed.
- Broad .NET decision: skipped after focused validation because the filtered suite compiled the changed repository and exercised the item-stone snapshot helper plus the gated database contract.
- Why this scope is sufficient: the service scheduling path was validated in UOW-2657; this UOW changed the repository method invoked by that live callback and targeted that method's item-stone contract.
```

Results:

- Focused C# validation passed: 4/4 tests.
- The gated database test returned early because `AION_GAMESERVER_DB_INTEGRATION` was not enabled in this environment.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerEnterWorldService.ItemUpdateTask.run` item-stone save call | `PlayerEnterWorldService` scheduled item callback plus `IPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Scheduler callback/persistence | Partial | Unit Tested | Partial Parity | UOW-2657 schedules the callback; UOW-2658 persists item stones inside the repository method invoked by that callback. |
| `ItemStoneListDAO.save(Player)` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Persistence | Partial | Unit Tested / DB Contract Compiled | Partial Parity | C# snapshots modeled player item-stone rows through the existing `item_stones` table. Java per-stone persistent-state deltas are represented as delete+insert snapshots in C#. |
| `Player.getAllItems()` | `GetPlayerItemStoneSnapshotItems(Player)` | Runtime item source | Partial | Unit Tested | Partial Parity | Includes modeled cube/equipment, regular warehouse, and account warehouse items. Java pet bag and cabinet items remain outside this C# path. |
| `ItemStoneListDAO.ItemStoneType` category mapping | `BuildItemStonePersistenceRows` / `ItemStonePersistenceRow` | SQL row mapping | Implemented | Unit Tested | Partial Parity | Existing tests cover categories 0/1/2/3, godstone proc count, and idian polish fields. |

## Known Gaps

- Java pet bag and cabinet items are included by `Player.getAllItems()` but are not exposed in the currently modeled C# `Player` item collections used by this repository method.
- C# snapshots all item-stone rows per modeled item instead of Java's per-stone persistent-state insert/update/delete decisions.
- Periodic general save still lacks Java abyss rank, full skill list, full quest list, and house save parity.
- Real MySQL validation for the new periodic method was not run because `AION_GAMESERVER_DB_INTEGRATION` was not enabled.
- Real client validation was not run.

## Next Runtime UOW Candidates

1. Add live periodic abyss-rank persistence to `SavePeriodicPlayerGeneralAsync`, matching Java `GeneralUpdateTask -> AbyssRankDAO.storeAbyssRank(player)`, using the existing abyss-rank SQL helper already present in the repository.
2. Add live periodic skill-list persistence to `SavePeriodicPlayerGeneralAsync`, matching Java `GeneralUpdateTask -> SkillListDAO.storeSkills(player)`, if the C# skill runtime and database row contracts are present enough for a focused write path.
3. Extend modeled periodic item-stone coverage to pet bag or cabinet item collections only after those runtime item collections are loaded into the C# `Player` model and used by live code.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
