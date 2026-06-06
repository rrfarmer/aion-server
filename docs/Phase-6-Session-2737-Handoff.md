# Phase 6 Session 2737 Handoff

## Completed UOW

[Phase 6] UOW-2737: Load legion warehouse rows on enter-world.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: a legion member entering world now has existing legion warehouse location `3` inventory rows restored into C# runtime state for live warehouse handlers.
- Java source/runtime path: `LegionService.loadLegionInfo` calls `InventoryDAO.loadStorage(legionId, legion.getLegionWarehouse())`, and `Player.getStorage(StorageType.LEGION_WAREHOUSE)` returns a `LegionStorageProxy` for legion members.
- C# runtime artifact wired: `IPlayerEnterWorldRepository`, `MySqlPlayerEnterWorldRepository.LoadLegionWarehouseItemsAsync`, and `PlayerEnterWorldService.EnterWorldAsync`.
- Client-visible/state/persistence effect: after enter-world, live C# move/split/kinah warehouse handlers can find existing legion-owned items/kinah by object id and location `3`; no packet is sent solely for loading.
- Why this is runtime progress: it restores runtime item state from the existing database shape for live warehouse handlers.
```

## Commit

`[Phase 6][UOW-2737] Load legion warehouse rows on enter-world`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2737-Completion.md`
- `docs/Phase-6-Session-2737-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`

## C# Artifacts Touched

- `Aion.GameServer.Data.IPlayerEnterWorldRepository`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository`
- `Aion.GameServer.Services.PlayerEnterWorldService.EnterWorldAsync`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorldAsync_LoadsLegionWarehouseItemsForLegionMemberLikeJava|FullyQualifiedName~HandleMoveItemAsync_LegionWarehouseSameStorageSlotPersistsWithLegionOwnerLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 2
- Failed: 0
- Skipped: 0

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched C# files.

Java/Maven:

- Not run. Java source was reviewed unchanged; no narrow Java fixture exists for this load branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was runtime load path and repository interface touched, but focused tests compiled the affected project and proved the new load path plus adjacent live storage consumption.

## Conservative Parity Status

- Enter-world loading now has closer partial Java parity for legion warehouse item restoration.
- Full Java `LegionWarehouse`/`LegionStorageProxy` parity is not claimed because C# still uses flattened location-3 `InventoryItems`.
- DB integration for the new load method was not run; SQL shape reuses the already-live storage loader.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.LegionService.loadLegionInfo` | `PlayerEnterWorldService.EnterWorldAsync` | Runtime load path | Partial | Unit Tested | Partial Parity | C# now restores legion-owned location-3 rows for legion members; full Legion aggregate caching/history loading remains broader. |
| `com.aionemu.gameserver.dao.InventoryDAO.loadStorage` | `MySqlPlayerEnterWorldRepository.LoadLegionWarehouseItemsAsync` | Repository load path | Partial | Unit Tested through service fake | Partial Parity | Loads rows by legion id/location 3 through the existing storage loader; exact DB integration for this branch was not run. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Player.InventoryItems` location-3 runtime model | Runtime storage model | Partial | Unit Tested | Partial Parity | C# has no full `LegionWarehouse` aggregate; restored rows are flattened into runtime inventory state. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `Player.InventoryItems` location-3 runtime model | Runtime storage access | Partial | Unit Tested | Partial Parity | Live handlers can now access restored location-3 rows through existing C# storage lookup helpers. |

## Known Gaps / Watchouts

- C# still lacks a full Java `LegionWarehouse` aggregate and `LegionStorageProxy`.
- Enter-world loads location `3` rows for any positive `LegionId`; deeper Java disband/cache semantics remain outside this UOW.
- Periodic/logout save behavior for loaded legion warehouse rows still needs focused discovery.
- Exact DB integration for `LoadLegionWarehouseItemsAsync` was not run.

## Next Recommended Runtime UOW

Recommended candidate: discover and fix periodic/logout save behavior for loaded location `3` legion warehouse rows if C# currently misses them.

Runtime progress gate for that candidate must be confirmed from fresh discovery. Likely shape:

```text
- Deferred/live behavior to advance: loaded legion warehouse location `3` rows should persist dirty state on logout/periodic save using the existing inventory table shape.
- Java source/runtime path: Java `InventoryDAO.store(player)` and `PeriodicSaveService.LegionWarehouseSaveTask`/player save flow for dirty item rows.
- C# runtime artifact likely involved: `PlayerEnterWorldRepository.SavePlayerLogoutAsync`, periodic item save handling, and inventory row selection for `Player.InventoryItems` location `3`.
- Client-visible/state/persistence effect expected: changed legion warehouse rows survive logout/periodic save with legion owner/location intact.
- Why this is runtime progress: it persists live runtime item state using the existing database schema.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~EnterWorldAsync_LoadsLegionWarehouseItemsForLegionMemberLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: persistence save path if changed; start with focused C# tests and skip broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Verify live legion warehouse kinah handlers consume enter-world-loaded location `3` kinah rows; wire only if a concrete runtime gap exists.
- Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.
- Add direct auto-merge legion warehouse history coverage only if paired with a runtime fix found during discovery; do not do a test-only UOW.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `69833feab [Phase 6][UOW-2736] Persist legion slot moves with legion owner`
  - `9fbe879df [Phase 6][UOW-2735] Record legion move history before full denial`
  - `ea4240257 [Phase 6][UOW-2734] Honor disabled legion warehouse moves`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
