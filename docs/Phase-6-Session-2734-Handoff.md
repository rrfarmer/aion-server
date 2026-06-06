# Phase 6 Session 2734 Handoff

## Completed UOW

[Phase 6] UOW-2734: Honor disabled legion warehouse config in live item movement.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: disabling `gameserver.legion.warehouse` now blocks live item movement to/from legion warehouse storage type `3`, not only the open-dialog path.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo, ItemRestrictionService.isItemRestrictedFrom, LegionConfig.LEGION_WAREHOUSE, ItemMoveService.moveItem, ItemSplitService.splitItem, and CM_REPLACE_ITEM's shared restriction path.
- C# runtime artifact wired: GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage now consumes GameServerOptions.Legion.WarehouseEnabled and is used by live CM_MOVE_ITEM, CM_SPLIT_ITEM, and CM_REPLACE_ITEM handling.
- Client-visible/state/persistence effect: live move/split into disabled legion warehouse sends system-message id `1400355`; live move from disabled legion warehouse sends system-message id `1300322`; blocked operations unlock the source storage view and leave item state, persistence calls, and legion history unchanged.
- Why this is runtime progress: it sends real server packets from live inventory packet handlers and prevents incorrect live item mutation when Java's legion warehouse config disables the feature.
```

## Commit

`[Phase 6][UOW-2734] Honor disabled legion warehouse moves`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2734-Completion.md`
- `docs/Phase-6-Session-2734-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage`
- `Aion.GameServer.Configuration.GameServerOptions.Legion.WarehouseEnabled`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command 1:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToDisabledLegionWarehouseSendsJavaDepositRestriction|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToDisabledLegionWarehouseSendsJavaDepositRestriction|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMoveWhenDisabledSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0

Focused C# command 2:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToFullLegionWarehouseSendsJavaFullMessageWithoutMutation|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 295
- Failed: 0
- Skipped: 0

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched C# files.

Java/Maven:

- Not run. Java source was reviewed unchanged; no narrow Java fixture exists for this config branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was live inventory mutation path, but focused live packet dispatch compiled the affected project and proved both new config branches plus adjacent legion warehouse movement behavior.

## Conservative Parity Status

- `CM_MOVE_ITEM` and `CM_SPLIT_ITEM` now have partial Java parity for disabled legion warehouse movement restrictions.
- `CM_REPLACE_ITEM` uses the same changed restriction helper and therefore consumes `GameServerOptions.Legion.WarehouseEnabled`; direct disabled-config replace coverage remains a future optional strengthening item, not a blocker to the move/split runtime path proven here.
- Full `ItemRestrictionService`, `ItemMoveService`, and `ItemSplitService` parity is not claimed because C# still lacks a true Java `LegionWarehouse` aggregate and other storage types remain outside current live scope.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage` | Live movement restriction helper | Partial | Unit Tested through live handlers | Partial Parity | Destination storage type `3` now includes Java disabled-config handling and sends `1400355`. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedFrom` | `GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage` | Live movement restriction helper | Partial | Unit Tested through live handlers | Partial Parity | Source storage type `3` now includes Java disabled-config handling and sends `1300322`. |
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Move to/from disabled legion warehouse is blocked before mutation, persistence, and legion history. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Split into disabled legion warehouse is blocked before mutation, persistence, and legion history. |

## Known Gaps / Watchouts

- C# still stores legion warehouse rows as flattened `InventoryItems` location `3`, not a full Java `LegionWarehouse`.
- Exact Java golden bytes for `1400355` and `1300322` were not captured.
- Full edited-class validation currently has unrelated existing failures outside this UOW. Use the focused recipe unless working directly on those failing areas.
- Java `CM_LEGION_WH_KINAH` was briefly checked during discovery; it does not consult `LegionConfig.LEGION_WAREHOUSE`, so do not add a kinah disabled-config guard without stronger Java evidence.

## Next Recommended Runtime UOW

Recommended candidate: discover and wire the next live Java `ItemRestrictionService` storage branch that C# still omits, only where there is an existing live C# packet path.

Runtime progress gate for that candidate must be re-established from fresh discovery. A likely shape:

```text
- Deferred/live behavior to advance: a currently unhandled Java item restriction should block a live C# item move/split/replace path before mutation.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo/isItemRestrictedFrom for the selected storage type, plus the Java item service that invokes it.
- C# runtime artifact likely involved: GameServerConnection's live inventory movement handlers and any existing C# storage option/template/right helper needed by the selected branch.
- Client-visible/state/persistence effect expected: Java-equivalent system message is sent from live dispatch and item state/persistence/history remain unchanged.
- Why this is runtime progress: it sends a real server packet from a live handler and prevents incorrect live inventory mutation.
```

## Other Safe Runtime Candidates

- Persist or restore Java-equivalent legion warehouse item state from live save/logout/load paths if discovery finds a concrete runtime gap in the current C# owner/location persistence.
- Add direct disabled-config `CM_REPLACE_ITEM` coverage only if paired with any required runtime fix found during discovery; do not do a test-only UOW.
- Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `3f2808224 [Phase 6][UOW-2733] Block full legion warehouse moves`
  - `cf4eb10b1 [Phase 6][UOW-2732] Honor legion warehouse config`
  - `3dbdf6451 [Phase 6][UOW-2731] Deny disbanding legion warehouse opens`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
