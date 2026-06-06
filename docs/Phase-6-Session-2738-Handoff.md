# Phase 6 Session 2738 Handoff

## Completed UOW

[Phase 6] UOW-2738: Persist legion warehouse snapshot rows with Java owner mapping.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: dirty loaded legion warehouse location `3` rows now persist through logout and periodic item snapshot saves with Java-equivalent owner mapping.
- Java source/runtime path: `InventoryDAO.store(Player)`, `InventoryDAO.getItemOwnerId`, `InventoryDAO.DELETE_QUERY`, and `PeriodicSaveService.LegionWarehouseSaveTask`.
- C# runtime artifact wired: `MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync`, `SavePeriodicPlayerItemsAsync`, and their dirty inventory snapshot helpers.
- Client-visible/state/persistence effect: modified location `3` legion warehouse rows survive logout/periodic item flush under `item_owner = player.LegionId`; snapshot deletes follow Java `item_unique_id` semantics.
- Why this is runtime progress: it persists live dirty item state through the existing `inventory` and `item_stones` database shape.
```

## Commit

`[Phase 6][UOW-2738] Persist legion warehouse snapshot owners`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryItemStonePersistenceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/Phase-6-Session-2738-Completion.md`
- `docs/Phase-6-Session-2738-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/services/PeriodicSaveService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`

## C# Artifacts Touched

- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.ResolveInventoryStoreOwnerId`
- `Aion.GameServer.Tests.PlayerEnterWorldRepositoryItemStonePersistenceTests`
- `Aion.GameServer.Tests.PlayerEnterWorldRepositoryDatabaseIntegrationTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ResolveInventoryStoreOwnerId_UsesLegionOwnerForLegionWarehouseLikeJava|FullyQualifiedName~SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~EnterWorldAsync_LoadsLegionWarehouseItemsForLegionMemberLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 4
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings were emitted outside this UOW.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched C# files.

Java/Maven:

- Not run. Java source was reviewed unchanged; no narrow Java DAO fixture exists for this branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was persistence save path changed; focused tests compiled the affected project and covered the Java-derived owner rule plus adjacent enter-world load contract.

## Conservative Parity Status

- Dirty location `3` legion warehouse row owner mapping now matches Java `InventoryDAO.getItemOwnerId` for the modeled snapshot save paths.
- Snapshot deletes now match Java `InventoryDAO.DELETE_QUERY` item-id-only behavior for `InventoryDAO.store(player)`.
- Full Java cached-legion periodic warehouse sweep is not claimed; C# still persists loaded flattened rows through player save flows.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` | `MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Repository save path | Partial | Unit Tested / Integration-gated | Partial Parity | Logout snapshot saves now recompute location `3` owner as legion id; broader dirty storage modeling remains flattened. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Repository save path | Partial | Unit Tested / Integration-gated | Partial Parity | Periodic snapshot saves now recompute location `3` owner as legion id; full Java storage aggregates are still not ported. |
| `com.aionemu.gameserver.dao.InventoryDAO.getItemOwnerId` | `MySqlPlayerEnterWorldRepository.ResolveInventoryStoreOwnerId` | Utility / repository helper | Complete for modeled storages | Unit Tested | Verified Parity | Java branch behavior for player, account, and legion storage owners is covered by focused unit assertions. |
| `com.aionemu.gameserver.dao.InventoryDAO.DELETE_QUERY` | `MySqlPlayerEnterWorldRepository.DeleteInventoryItemSnapshotRowAsync` | Repository delete path | Partial | Integration-gated through snapshot tests | Partial Parity | Snapshot deletes now use item id only like Java `InventoryDAO.store`; other explicit mutation deletes intentionally remain owner-qualified. |
| `com.aionemu.gameserver.services.PeriodicSaveService.LegionWarehouseSaveTask` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` location-3 snapshot branch | Periodic save path | Partial | Integration-gated | Partial Parity | C# still lacks a cached `LegionWarehouse` aggregate task; loaded location-3 rows persist through player periodic item saves. |

## Known Gaps / Watchouts

- C# still lacks a full Java `LegionWarehouse` aggregate and `LegionStorageProxy`.
- DB integration tests were not run against MySQL in this session; they compile and are gated behind `AION_GAMESERVER_DB_INTEGRATION=1`.
- `Player.TrackDeletedItem` ignores location `3`; do not change it without discovering a concrete live delete/withdraw path that needs it.
- Full Java `PeriodicSaveService.LegionWarehouseSaveTask` parity remains broader than loaded player snapshot saves.

## Next Recommended Runtime UOW

Recommended candidate: discover whether live legion warehouse withdrawal/delete paths can leave location `3` deleted rows untracked because `Player.TrackDeletedItem` ignores location `3`, then wire only the smallest concrete runtime gap if found.

Runtime progress gate for that candidate must be confirmed from fresh discovery. Likely shape:

```text
- Deferred/live behavior to advance: a live legion warehouse item removal path should track location `3` deleted rows for logout/periodic snapshot persistence when the item is consumed or removed rather than moved.
- Java source/runtime path: Java `Storage.delete`, `Player.getStorage(LEGION_WAREHOUSE)`, `LegionStorageProxy`, and `InventoryDAO.store(player)` / `PeriodicSaveService.LegionWarehouseSaveTask`.
- C# runtime artifact likely involved: `Player.TrackDeletedItem`, live warehouse withdraw/delete handlers in `GameServerConnection`, and dirty snapshot persistence in `MySqlPlayerEnterWorldRepository`.
- Client-visible/state/persistence effect expected: removed legion warehouse rows disappear from the existing `inventory` table and do not reappear after relog.
- Why this is runtime progress: it mutates and persists live inventory/legion warehouse state through the existing DB schema.
```

Suggested focused validation if a concrete path is found:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TrackDeletedItem|FullyQualifiedName~SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: live inventory state/persistence path if changed; start with focused C# tests and skip broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Discover whether a C# periodic cached-legion warehouse save service is needed beyond player-loaded location `3` rows; implement only if there is a live cached warehouse runtime structure to persist.
- Verify live legion warehouse kinah handlers consume enter-world-loaded location `3` kinah rows; wire only if a concrete runtime gap exists.
- Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `3bc1c0c0c [Phase 6][UOW-2737] Load legion warehouse rows on enter-world`
  - `69833feab [Phase 6][UOW-2736] Persist legion slot moves with legion owner`
  - `9fbe879df [Phase 6][UOW-2735] Record legion move history before full denial`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
