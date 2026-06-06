# Phase 6 Session 2735 Handoff

## Completed UOW

[Phase 6] UOW-2735: Record legion item move history before full-storage denial.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live `CM_MOVE_ITEM` involving legion warehouse now records Java's legion warehouse item history immediately after movement restrictions pass.
- Java source/runtime path: `ItemMoveService.moveItem` calls `LegionService.addWHItemHistory` after `ItemRestrictionService`/trading/shutdown checks and before auto-merge and full-storage handling.
- C# runtime artifact wired: `GameServerConnection.HandleMoveItemAsync` moved `AddLegionWarehouseItemHistoryAsync` to the Java-equivalent ordering point.
- Client-visible/state/persistence effect: a move into a full legion warehouse still sends `STR_WAREHOUSE_DEPOSIT_FULL_BASKET` and leaves item state unchanged, but now also persists Java's `ITEM_DEPOSIT` legion history row.
- Why this is runtime progress: it changes persisted live legion history from a live client packet handler and keeps the packet/state branch aligned with Java runtime ordering.
```

## Commit

`[Phase 6][UOW-2735] Record legion move history before full denial`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2735-Completion.md`
- `docs/Phase-6-Session-2735-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.AddLegionWarehouseItemHistoryAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched C# files.

Java/Maven:

- Not run. Java source was reviewed unchanged; no narrow Java fixture exists for this branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was live handler ordering/persistence side effect, but focused live packet dispatch compiled the affected project and proved the exact move-history branches touched by this UOW.

## Conservative Parity Status

- `CM_MOVE_ITEM` now has closer partial Java parity for legion warehouse item-history ordering.
- Full `ItemMoveService` parity is not claimed because other move branches, persistence failure behavior, and the true Java `LegionWarehouse` aggregate remain incomplete.
- `CM_SPLIT_ITEM` was reviewed but not changed because Java split records legion warehouse item history after the full-storage check.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Legion warehouse item history now follows Java ordering before auto-merge/full-storage handling. |
| `com.aionemu.gameserver.services.LegionService.addWHItemHistory` | `GameServerConnection.AddLegionWarehouseItemHistoryAsync` | Live persistence side effect | Partial | Unit Tested through live handler | Partial Parity | Deposit/withdraw descriptions and action names are covered through live move tests; full Java `Legion` aggregate behavior is not modeled. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Reviewed to avoid applying move ordering to split; Java split records history after the full-storage check. |

## Known Gaps / Watchouts

- C# still stores legion warehouse rows as flattened `InventoryItems` location `3`, not a full Java `LegionWarehouse`.
- Auto-merge into/out of legion warehouse shares Java's pre-merge history ordering after this UOW, but no dedicated auto-merge assertion was added.
- C# persistence-failure rollback can still differ from Java's dirty-state flow.
- Exact Java database row output was not captured; source review and focused live repository assertions are the evidence.

## Next Recommended Runtime UOW

Recommended candidate: fix same-storage legion warehouse slot persistence owner.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: same-storage `CM_MOVE_ITEM` slot moves inside legion warehouse should persist against the legion-owned inventory row, not the player-owned row.
- Java source/runtime path: `ItemMoveService.moveInSameStorage` marks the item dirty, and `InventoryDAO.getItemOwnerId` stores `LEGION_WAREHOUSE` rows with the legion id.
- C# runtime artifact likely involved: `PlayerEnterWorldService.SaveInventoryItemSlotAsync`, `PlayerEnterWorldRepository.SaveInventoryItemSlotAsync`, and live `GameServerConnection.HandleMoveItemAsync` same-storage branch.
- Client-visible/state/persistence effect expected: moving a legion warehouse item to another slot updates the live item slot and persists using `player.LegionId` as `item_owner`; packets remain silent like Java same-storage moves.
- Why this is runtime progress: it fixes live persistence from a live client packet handler using the existing database shape.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_LegionWarehouseSameStorageSlotPersistsWithLegionOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or fixtures change; source review should be enough for this narrow owner-id branch. Broad-validation trigger: live persistence helper touched; start with the focused command above and skip broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Discover whether cross-storage switch persistence owner handling for legion warehouse is complete through `SaveItemStorageSwitchMutationAsync`; wire only if a concrete runtime gap exists.
- Persist or restore Java-equivalent legion warehouse item state from live save/logout/load paths if discovery finds a concrete runtime gap in the current C# owner/location persistence.
- Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `ea4240257 [Phase 6][UOW-2734] Honor disabled legion warehouse moves`
  - `3f2808224 [Phase 6][UOW-2733] Block full legion warehouse moves`
  - `cf4eb10b1 [Phase 6][UOW-2732] Honor legion warehouse config`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
