# Phase 6 Session 2692 Handoff

## Completed UOW

[Phase 6] UOW-2692: Switch cross-storage replace items.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM now executes Java-style cross-storage item switches instead of returning early.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync.
- Client-visible/state/persistence effect: two item rows swap storage/slot through existing inventory persistence, then the client receives source delete, replacement delete, replacement add, and source add packets in Java order.
- Why this is runtime progress: it mutates live inventory item storage/slot state, persists both affected rows, and sends real server packets from the live CM_REPLACE_ITEM handler; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2692] Switch replace storage items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2692-Completion.md`
- `docs/Phase-6-Session-2692-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Data.EmptyPlayerEnterWorldRepository`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~CmReplaceItemTests" --logger "console;verbosity=minimal"
```

First result:

- Failed at compile because the new cross-storage locals reused names later used by the same-storage branch.

Final result after local rename:

- Passed: 1
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

- The covered regular warehouse/cube cross-storage `CM_REPLACE_ITEM` path now matches the reviewed Java order for runtime mutation and packet fanout.
- Full `CM_REPLACE_ITEM`, full `ItemMoveService`, and full `ItemPacketService` parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REPLACE_ITEM` | `Aion.GameServer.Network.Aion.ClientPackets.CmReplaceItem` / `GameServerConnection.HandleReplaceItemAsync` | Client packet / live handler | Partial | Unit Tested indirectly | Partial Parity | Packet read shape is represented and cross-storage live handler path now executes. No standalone parser test class exists. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cross-storage regular warehouse/cube switch now mutates state, persists both rows, and sends Java-ordered packets. Same-storage path existed. Legion warehouse, shutdown, and full restriction service behavior remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `SmDeleteItem`, `SmDeleteWarehouseItem`, `SmInventoryAddItem`, `SmWarehouseAddItem`, `SmCubeUpdate` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Replace-item cross-storage delete/delete/add/add order is covered. Broader move/split packet-size helper parity remains under review. |

## Known Gaps / Watchouts

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- C# still does not model Java `ItemRestrictionService.isItemRestrictedFrom`.
- Shutdown-soon behavior and shutdown system message are not wired.
- Legion warehouse replace remains deferred.
- Two-row replace persistence reuses the existing single-row move update twice; it is not yet atomic at the database layer.
- Existing `CM_MOVE_ITEM` and `CM_SPLIT_ITEM` branches still have hand-written storage update packets that should be inspected against Java storage-specific cube-size behavior.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_MOVE_ITEM` cross-storage delete/add storage-size packets and implement the smallest confirmed Java mismatch in live `HandleMoveItemAsync`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM cross-storage moves should send Java-equivalent storage-specific SM_CUBE_UPDATE packets after warehouse/cube delete and add packets.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemPacketService.sendItemDeletePacket/sendStorageUpdatePacket -> SM_CUBE_UPDATE.cubeSize(storageType, player).
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync packet fanout after cross-storage mutation.
- Client-visible/state/persistence effect expected: warehouse-source or warehouse-destination moves should report the Java storage ordinal/count in SM_CUBE_UPDATE after live delete/add packets while preserving the existing persisted item row move.
- Why this is runtime progress: proceed only if source review confirms current C# sends the wrong live packet; the fix would change packets emitted by CM_MOVE_ITEM from live code.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
- Inspect non-cube source cross-storage `CM_SPLIT_ITEM` restriction unlock packet behavior if a safe warehouse-source fixture can represent the live path.
- Inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence for source warehouse/cube combinations.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `0adbee576 [Phase 6][UOW-2691] Unlock restricted split source`
  - `44e35b258 [Phase 6][UOW-2690] Delete split merge source stack`
  - `1c6e124c3 [Phase 6][UOW-2689] Move remaining stack after merge`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
