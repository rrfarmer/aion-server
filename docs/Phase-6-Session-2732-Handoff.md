# Phase 6 Session 2732 Handoff

## Completed UOW

[Phase 6] UOW-2732: Honor Java legion warehouse enabled config.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live OPEN_LEGION_WAREHOUSE now rejects warehouse opens when `gameserver.legion.warehouse` is disabled.
- Java source/runtime path: LegionConfig.LEGION_WAREHOUSE, game-server/config/main/legions.properties, and LegionService.canOpenWarehouse `!LEGION_WAREHOUSE || !npc.supportsAction(...)` -> STR_CANT_USE_GUILD_STORAGE.
- C# runtime artifact wired: GameServerOptions.LoadFromJavaConfig, GameServerLegionOptions.WarehouseEnabled, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, and existing SmSystemMessage.CantUseGuildStorage.
- Client-visible/state/persistence effect: disabled C# config sends system-message id `1300279` instead of opening or locking the live legion warehouse.
- Why this is runtime progress: it loads Java config into runtime C# options and sends a real server packet from live dialog dispatch.
```

## Commit

`[Phase 6][UOW-2732] Honor legion warehouse config`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/Phase-6-Session-2732-Completion.md`
- `docs/Phase-6-Session-2732-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/config/main/legions.properties`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Configuration.GameServerOptions`
- `Aion.GameServer.Configuration.GameServerLegionOptions`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenLegionWarehouseDialogAsync`
- `Aion.GameServer.Tests.GameServerOptionsTests`
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 316
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source and config were reviewed unchanged; no narrow Java fixture exists for this config guard branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was live config loading plus live dialog dispatch, but the focused command compiled the affected project and exercised the exact live packet branch.

## Conservative Parity Status

- `OPEN_LEGION_WAREHOUSE` now has partial runtime parity for success, no-legion denial, config-disabled denial, unsupported-action denial, disbanding denial, no-permission denial, in-use denial, same-user reopen, close release, and logout release.
- Full `LegionService` and `LegionWarehouse` parity is not claimed because C# does not model the full Java aggregate, all member-removal paths, or all store/move behavior.
- `LegionConfig` parity is partial; only `LEGION_WAREHOUSE` was added.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.LegionConfig.LEGION_WAREHOUSE` | `Aion.GameServer.Configuration.GameServerLegionOptions.WarehouseEnabled` | Config | Partial | Unit Tested | Partial Parity | Java key/default and `mygs.properties` override order are covered; other `LegionConfig` fields remain unported here. |
| `com.aionemu.gameserver.services.LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Config-disabled branch now live; full Java `LegionWarehouse` aggregate behavior remains incomplete. |
| `SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE` | `SmSystemMessage.CantUseGuildStorage` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300279` covered by packet and live dispatch tests; no Java golden bytes. |

## Known Gaps / Watchouts

- C# still stores legion warehouse contents as player `InventoryItems` location `3`, not as Java `LegionWarehouse`.
- Live item move/split capacity for destination storage type `3` appears incomplete: C# `CreateStorageFullMessage` covers storage types `0`, `1`, and `2`; Java `IStorage.getStorageIsFullMessage` includes `LEGION_WAREHOUSE` with `STR_WAREHOUSE_DEPOSIT_FULL_BASKET`.
- Java `LegionWarehouse.updateLimit` uses `(3 + warehouseExpansions) * 8`; C# already exposes `Player.LegionWarehouseExpansions`, but a destination capacity guard still needs source review and tests.
- CM_LEGION leave/kick/member-removal remains deferred in C#; avoid that until a live path exists.
- Exact Java golden bytes for `1300279` were not captured.

## Next Recommended Runtime UOW

Recommended candidate: add Java-derived legion warehouse capacity checks to live item move/split into storage type `3`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live item move/split into a full legion warehouse should send Java's warehouse-full system message and avoid mutating item location/owner.
- Java source/runtime path: ItemMoveService.moveItem; Player.getStorage(StorageType.LEGION_WAREHOUSE); LegionStorageProxy/LegionWarehouse; IStorage.getStorageIsFullMessage -> STR_WAREHOUSE_DEPOSIT_FULL_BASKET; LegionWarehouse.updateLimit.
- C# runtime artifact likely involved: GameServerConnection.CreateStorageFullMessage, move/split handlers around destination storage type `3`, InventoryCapacity or a new helper for legion warehouse free slots, and existing SmSystemMessage.WarehouseDepositFullBasket.
- Client-visible/state/persistence effect expected: when storage type `3` has no free slots, live move/split sends system-message id for `STR_WAREHOUSE_DEPOSIT_FULL_BASKET` and does not update inventory item location, owner, count, or persistence calls.
- Why this is runtime progress: it sends a real server packet from live CM_MOVE_ITEM/CM_SPLIT_ITEM paths and prevents incorrect live inventory state mutation.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Behavior under validation: full legion warehouse destination blocks live move/split with Java warehouse-full message and no state mutation. Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: live inventory mutation path; start with the focused command above and skip broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Persist Java-equivalent legion warehouse item state from a live save/logout path if the current database shape and C# item location `3` behavior can be safely wired.
- Revisit legion leave/kick/member-removal lock release only after CM_LEGION or an equivalent membership-removal path becomes live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `3dbdf6451 [Phase 6][UOW-2731] Deny disbanding legion warehouse opens`
  - `0912734a4 [Phase 6][UOW-2730] Release legion warehouse lock on logout`
  - `1c5ee008b [Phase 6][UOW-2729] Lock live legion warehouse opens`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
