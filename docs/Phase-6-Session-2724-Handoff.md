# Phase 6 Session 2724 Handoff

## Completed UOW

[Phase 6] UOW-2724: Persist live legion warehouse item history.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: successful live item move/split paths involving legion warehouse now write ITEM_DEPOSIT/ITEM_WITHDRAW history rows.
- Java source/runtime path: ItemMoveService.moveItem and ItemSplitService.splitItem -> LegionService.addWHItemHistory -> LegionService.addHistory -> LegionDAO.insertHistory.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, GameServerConnection.HandleSplitItemAsync, AddLegionWarehouseItemHistoryAsync, LegionHistoryActions, existing InsertLegionHistoryAsync.
- Client-visible/state/persistence effect: durable item history rows feed the live CM_LEGION_HISTORY warehouse response path introduced in UOW-2723.
- Why this is runtime progress: existing live inventory handlers now persist Java-equivalent legion history side effects after successful runtime item mutations.
```

## Commit

`[Phase 6][UOW-2724] Persist legion warehouse item history`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2724-Completion.md`
- `docs/Phase-6-Session-2724-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`

## C# Artifacts Touched

- `Aion.GameServer.Model.Legion.LegionHistoryActions`
- `Aion.GameServer.Network.Aion.GameServerConnection.AddLegionWarehouseItemHistoryAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync` test boundary only

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 6
- Failed: 0
- Skipped: 0
- Existing warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger was live handler/persistence wiring, but the focused command built the affected project and directly exercised item move deposit, item move withdraw, split deposit, split withdraw, replace no-history, and history send adjacency.

## Conservative Parity Status

- `LegionService.addWHItemHistory` is partially represented for successful live move/split item paths involving legion warehouse.
- `ItemMoveService.moveItem` and `ItemSplitService.splitItem` now have partial runtime parity for item history side effects.
- `ItemMoveService.switchItemsInStorages` remains history-free in C# because Java has no `addWHItemHistory` call in that switch path.
- Full parity is not claimed because direct DB integration, Java in-memory legion history caching/trimming, exact Java packet bytes, and full legion warehouse aggregate behavior remain gaps.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `LegionService.addWHItemHistory` | `GameServerConnection.AddLegionWarehouseItemHistoryAsync` | Service side effect | Partial | Unit Tested indirectly | Partial Parity | Java action selection and `itemId:count` description are represented for successful C# mutations. |
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Deposit and withdrawal history rows are written from live packet dispatch. |
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Deposit and withdrawal split rows are written from live packet dispatch. |
| `ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | No item-history write, matching Java source. |
| `LegionHistoryAction` | `LegionHistoryActions` | Enum/model projection | Partial | Unit Tested indirectly | Partial Parity | `ITEM_DEPOSIT` and `ITEM_WITHDRAW` constants now exist beside kinah actions. |

## Known Gaps / Watchouts

- Direct DB integration for `legion_history` item rows was not run.
- C# still lacks Java's shared `Legion` aggregate/history cache and deletion/trimming logic.
- C# writes item history after successful persistence in rollback-sensitive handlers; Java calls before some final move mutations. This UOW preserves Java action/description semantics for successful runtime mutations.
- Legion warehouse expansion count and in-use open/close state remain incomplete.
- Existing preview/readiness-only services around legion warehouse persistence should still be ignored unless paired with live runtime wiring.

## Next Recommended Runtime UOW

Recommended candidate: load and use legion warehouse expansion count in the live runtime model that backs `SM_CUBE_UPDATE` for storage `3`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live legion warehouse size packets should use the persisted legion warehouse expansion count instead of defaulting or relying on incomplete runtime state.
- Java source/runtime path: LegionService.openLegionWarehouse / Legion.getWarehouseExpansions() -> SM_CUBE_UPDATE for StorageType.LEGION_WAREHOUSE.
- C# runtime artifact likely involved: PlayerEnterWorldRepository legion/warehouse load path, Player or legion runtime model fields, SmCubeUpdate.LegionWarehouseSizeSnapshot call sites in GameServerConnection.
- Client-visible/state/persistence effect expected: live warehouse open/move/split packet fanout reports Java-equivalent storage size/expansion values for legion warehouse.
- Why this is runtime progress: it loads persisted Java-shaped legion state into C# runtime structures used by live server packets.
```

Suggested discovery:

```powershell
rg -n "getWarehouseExpansions|setWarehouseExpansions|warehouseExpansions|LegionWarehouseSizeSnapshot|WarehouseExpansions|legion.*warehouse" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehouse|FullyQualifiedName~SmCubeUpdate" --logger "console;verbosity=minimal"
```

Narrow the filter after discovery to tests that exercise live packet paths and represented runtime expansion state. Do not make this a schema/readiness-only UOW.

## Other Safe Runtime Candidates

- Wire live legion warehouse open/close in-use state if a concrete Java runtime mutation and C# state holder are identified.
- Extend `CM_LEGION_HISTORY` reward/activity paths only when paired with live row loading and packet send coverage.
- Continue item-storage runtime parity around location `3` only where Java source shows live packet/state/persistence side effects.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `1d358d9f8 [Phase 6][UOW-2723] Persist legion warehouse history`
  - `bd4c0b1df [Phase 6][UOW-2722] Withdraw kinah from legion warehouse`
  - `73e42ae32 [Phase 6][UOW-2721] Deposit kinah into legion warehouse`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
