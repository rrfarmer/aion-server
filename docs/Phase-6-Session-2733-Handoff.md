# Phase 6 Session 2733 Handoff

## Completed UOW

[Phase 6] UOW-2733: Block full legion warehouse item moves.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live item move/split into a full legion warehouse now sends Java's warehouse-full message and avoids mutating item state.
- Java source/runtime path: ItemMoveService.moveItem, ItemSplitService.splitItem, Player.getStorage(StorageType.LEGION_WAREHOUSE), IStorage.getStorageIsFullMessage, and LegionWarehouse.updateLimit.
- C# runtime artifact wired: InventoryCapacity legion warehouse limit/free-slot helpers and GameServerConnection.CreateStorageFullMessage for destination storage type `3`.
- Client-visible/state/persistence effect: live CM_MOVE_ITEM/CM_SPLIT_ITEM into a full legion warehouse sends system-message id `1300421`; move unlocks the source item, and neither path changes item owner/location/count nor writes persistence/history.
- Why this is runtime progress: it sends real server packets from live client packet paths and prevents incorrect live inventory mutation.
```

## Commit

`[Phase 6][UOW-2733] Block full legion warehouse moves`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/InventoryCapacity.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2733-Completion.md`
- `docs/Phase-6-Session-2733-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.InventoryCapacity`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageFullMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToFullLegionWarehouseSendsJavaFullMessageWithoutMutation|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 289
- Failed: 0
- Skipped: 0

Attempted wider edited-class C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --logger "console;verbosity=minimal"
```

Result:

- Failed: 4
- Passed: 146
- The failures were outside the new legion warehouse full-path tests:
  - `HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap`
  - `HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava`
  - `HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets`
  - `HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava`

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged; no narrow Java fixture exists for this capacity branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was live inventory mutation path, but focused dispatch tests compiled the affected project and proved the exact storage type `3` behavior.

## Conservative Parity Status

- `CM_MOVE_ITEM` and `CM_SPLIT_ITEM` now have partial Java parity for full legion warehouse destination handling.
- Full `ItemMoveService`/`ItemSplitService` parity is not claimed because C# still lacks a true Java `LegionWarehouse` aggregate and other storage types remain outside current live scope.
- `IStorage.getStorageIsFullMessage` parity is partial; C# covers live cube/warehouse/account/legion cases only.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Destination legion warehouse full branch now sends `1300421`, unlocks source, and avoids move/persistence/history mutation. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Destination legion warehouse full branch now sends `1300421` before split-item creation and avoids persistence/history mutation. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Utility / live packet selection | Partial | Unit Tested | Partial Parity | CUBE, regular warehouse, account warehouse, and legion warehouse are represented; other storage types remain out of current C# live scope. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse.updateLimit` | `InventoryCapacity.GetLegionWarehouseLimit` | Runtime capacity helper | Partial | Unit Tested through live handlers | Partial Parity | Java `(3 + expansions) * 8` formula is represented through `Player.LegionWarehouseExpansions`; no full Java aggregate yet. |

## Known Gaps / Watchouts

- Live item movement restrictions still do not consume `GameServerOptions.Legion.WarehouseEnabled`.
- Full edited-class validation currently has unrelated existing failures outside this UOW. Use the focused recipe unless working directly on those failing areas.
- C# still stores legion warehouse rows as flattened `InventoryItems` location `3`.
- Exact Java golden bytes for `1300421` were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire the Java legion warehouse enabled config into live item movement restrictions for storage type `3`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: disabling `gameserver.legion.warehouse` should block live item movement to/from legion warehouse, not just dialog opening.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo and isItemRestrictedFrom; LegionConfig.LEGION_WAREHOUSE; ItemMoveService.moveItem; ItemSplitService.splitItem.
- C# runtime artifact likely involved: GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage, GameServerOptions.Legion.WarehouseEnabled, live CM_MOVE_ITEM/CM_SPLIT_ITEM/CM_REPLACE_ITEM branches, and existing SmSystemMessage helpers.
- Client-visible/state/persistence effect expected: move/split/replace to disabled legion warehouse sends `STR_MSG_WAREHOUSE_CANT_LEGION_DEPOSIT`; move/split/replace from disabled legion warehouse sends `STR_GUILD_WAREHOUSE_NO_RIGHT`; item state and persistence remain unchanged.
- Why this is runtime progress: it sends real server packets from live inventory packet handlers and prevents incorrect live inventory mutation when Java config disables legion warehouse.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToDisabledLegionWarehouseSendsJavaDepositRestriction|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMoveWhenDisabledSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToDisabledLegionWarehouseSendsJavaDepositRestriction|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Behavior under validation: disabled legion warehouse config blocks live inventory movement with Java message ids and no state mutation. Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: live inventory mutation path; start with the focused command above and skip broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Persist Java-equivalent legion warehouse item state from live save/logout paths if the existing database shape and C# item owner/location model can be safely wired.
- Revisit legion leave/kick/member-removal lock release only after CM_LEGION or an equivalent membership-removal path becomes live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `cf4eb10b1 [Phase 6][UOW-2732] Honor legion warehouse config`
  - `3dbdf6451 [Phase 6][UOW-2731] Deny disbanding legion warehouse opens`
  - `0912734a4 [Phase 6][UOW-2730] Release legion warehouse lock on logout`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
