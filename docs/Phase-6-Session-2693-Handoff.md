# Phase 6 Session 2693 Handoff

## Completed UOW

[Phase 6] UOW-2693: Send move storage-size updates.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM normal cross-storage moves now use Java-equivalent storage-specific SM_CUBE_UPDATE packets after delete/add packets.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemPacketService.sendItemDeletePacket/sendStorageUpdatePacket -> SM_CUBE_UPDATE.cubeSize(storageType, player).
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync.
- Client-visible/state/persistence effect: regular warehouse destination moves now send a regular-warehouse SM_CUBE_UPDATE with warehouse count/ordinal after SM_WAREHOUSE_ADD_ITEM while preserving the existing item row location/slot persistence.
- Why this is runtime progress: it changes packets emitted by the live CM_MOVE_ITEM handler after real inventory state and persistence mutation; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2693] Send move storage updates`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2693-Completion.md`
- `docs/Phase-6-Session-2693-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava" --logger "console;verbosity=minimal"
```

First result:

- Failed because fixture item id 201 was not warehouse-storable, so the handler correctly returned before mutation.

Final result after fixture correction to item id 200:

- Passed: 2
- Failed: 0
- Skipped: 0

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- The covered normal cube-to-regular-warehouse `CM_MOVE_ITEM` path now sends storage-specific Java-style `SM_CUBE_UPDATE` after delete/add packets.
- Full `CM_MOVE_ITEM`, full `ItemMoveService`, and all storage-family packet parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Normal cross-storage move now uses storage-aware delete/add packet fanout. Stack merge branches remain partially reviewed. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `SmDeleteItem`, `SmDeleteWarehouseItem`, `SmInventoryAddItem`, `SmWarehouseAddItem`, `SmCubeUpdate` usage in `HandleMoveItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Normal move delete/add storage-size updates are covered. Other item handlers still have local packet fanout to inspect. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse ordinal/count is asserted for this move path. Account/legion storage behavior remains limited to existing modeled helpers. |

## Known Gaps / Watchouts

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Auto-merge full-source delete branch still has hand-written source delete/cube-update fanout and should be inspected for warehouse source behavior.
- Java `ItemRestrictionService.isItemRestrictedFrom`, trading unlock edge cases, shutdown-soon behavior, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse storage-size behavior are not covered by this UOW.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_MOVE_ITEM` stack auto-merge full-source delete for warehouse source and implement storage-specific source-size packets if current C# differs from Java.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM stack auto-merge should send Java-equivalent source delete and storage-specific SM_CUBE_UPDATE when the source stack is fully merged from a non-cube storage.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 stack merge loop -> ItemSplitService.mergeStacks -> ItemPacketService.sendItemDeletePacket/source storage update behavior.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync auto-merge item.Count <= 0 branch.
- Client-visible/state/persistence effect expected: a warehouse-source full stack merge should delete the source warehouse item with the correct storage-size packet while preserving existing merge persistence/state mutation.
- Why this is runtime progress: proceed only if source review confirms current C# sends the wrong live packet; the fix would change packets emitted by CM_MOVE_ITEM from live code after a real merge mutation.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullWarehouseSourceAutoMergeUsesJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect non-cube source cross-storage `CM_SPLIT_ITEM` restriction unlock packet behavior if a safe warehouse-source fixture can represent the live path.
- Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
- Inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence for source warehouse/cube combinations.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `9baf76378 [Phase 6][UOW-2692] Switch replace storage items`
  - `0adbee576 [Phase 6][UOW-2691] Unlock restricted split source`
  - `44e35b258 [Phase 6][UOW-2690] Delete split merge source stack`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
