# Phase 6 Session 2739 Handoff

## Completed UOW

[Phase 6] UOW-2739: Track legion warehouse deleted rows.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: location `3` legion warehouse item deletes now enter C# dirty item state and can be flushed by logout/periodic item persistence.
- Java source/runtime path: `Storage.delete`, `LegionStorageProxy.delete`, `Player.getDirtyItemsToUpdate`, and `InventoryDAO.store(player)`.
- C# runtime artifact wired: `Player.TrackDeletedItem`, `Player.GetDirtyItemsToUpdate`, and the existing `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` dirty snapshot path.
- Client-visible/state/persistence effect: deleted legion warehouse rows are removed from live flattened storage state, tracked as deleted location `3` rows, and deleted from the existing `inventory` table on snapshot save.
- Why this is runtime progress: it mutates live player/legion warehouse item state and persists that mutation through the existing DB schema.
```

## Commit

`[Phase 6][UOW-2739] Track legion warehouse deleted rows`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/Phase-6-Session-2739-Completion.md`
- `docs/Phase-6-Session-2739-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.Player`
- `Aion.GameServer.Tests.PlayerInventoryPersistentStateTests`
- `Aion.GameServer.Tests.PlayerEnterWorldRepositoryDatabaseIntegrationTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~ResolveInventoryStoreOwnerId_UsesLegionOwnerForLegionWarehouseLikeJava|FullyQualifiedName~SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePeriodicPlayerItemsAsync_DeletesTrackedLegionWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 22
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

- Not run. Java source was reviewed unchanged; no narrow Java storage-state fixture exists for this branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was shared live player inventory state and persistence path changed; focused tests compiled the affected project and covered the edited model plus adjacent repository snapshot behavior.

## Conservative Parity Status

- Location `3` deleted item tracking now has partial Java parity with `Storage.delete` and `LegionStorageProxy.delete`.
- C# still uses flattened `Player.InventoryItems` for legion warehouse rows instead of Java's full legion warehouse aggregate.
- Snapshot persistence can now consume location `3` deleted rows; DB-gated test was compiled but not run against MySQL.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `Player.TrackDeletedItem` | Runtime state mutation | Partial | Unit Tested | Partial Parity | C# now removes current rows before tracking deleted rows and handles location `3`; packet emission remains handled by live callers, not the model. |
| `com.aionemu.gameserver.model.items.storage.LegionStorageProxy.delete` | `Player.TrackDeletedItem` location-3 branch | Runtime state mutation | Partial | Unit Tested / Integration-gated | Partial Parity | Flattened C# location-3 rows now behave like a dirty legion warehouse storage for deletes; no full proxy aggregate exists. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` | `Player.GetDirtyItemsToUpdate` | Runtime dirty harvest | Partial | Unit Tested | Partial Parity | Dirty harvest now includes location-3 current/deleted rows separately from cube rows. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Repository save path | Partial | Integration-gated | Partial Parity | Existing snapshot save can now consume tracked location-3 deletes; DB execution was not run in this session. |

## Known Gaps / Watchouts

- C# still lacks a full Java `LegionWarehouse` aggregate and `LegionStorageProxy`.
- Location `3` delete packet emission remains caller-owned.
- DB integration tests were not run against MySQL in this session; they compile and are gated behind `AION_GAMESERVER_DB_INTEGRATION=1`.
- Full Java `PeriodicSaveService.LegionWarehouseSaveTask` parity remains broader than loaded player snapshot saves.

## Next Recommended Runtime UOW

Recommended candidate: discover whether live legion warehouse kinah handlers consume enter-world-loaded location `3` kinah rows for open/deposit/withdraw flows, then wire the smallest concrete runtime gap if found.

Runtime progress gate for that candidate must be confirmed from fresh discovery. Likely shape:

```text
- Deferred/live behavior to advance: live legion warehouse kinah deposit/withdraw/open paths should read, mutate, send packets for, and persist loaded location `3` kinah rows like Java storage kinah.
- Java source/runtime path: Java `Storage.increaseKinah/decreaseKinah`, `LegionStorageProxy`, `ItemPacketService.sendStorageUpdatePacket`, and `InventoryDAO.store`.
- C# runtime artifact likely involved: `GameServerConnection` warehouse kinah handlers, `Player.InventoryItems` location-3 kinah row lookup, and `PlayerEnterWorldService`/repository persistence helpers.
- Client-visible/state/persistence effect expected: legion warehouse kinah changes update client-visible warehouse packets and survive relog with `item_owner = player.LegionId`.
- Why this is runtime progress: it mutates live item/kinah state, sends storage packets, and persists through the existing DB schema.
```

Suggested focused validation if a concrete path is found:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehouse|FullyQualifiedName~HandleMoveKinah|FullyQualifiedName~SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: live inventory state/packet/persistence path if changed; start with focused C# tests and skip broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Discover whether a C# periodic cached-legion warehouse save service is needed beyond player-loaded location `3` rows; implement only if there is a live cached warehouse runtime structure to persist.
- Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.
- Inspect item split/merge deletion packet paths for location `3` only if paired with a concrete live persistence or packet mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `6b019f7d5 [Phase 6][UOW-2738] Persist legion warehouse snapshot owners`
  - `3bc1c0c0c [Phase 6][UOW-2737] Load legion warehouse rows on enter-world`
  - `69833feab [Phase 6][UOW-2736] Persist legion slot moves with legion owner`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
