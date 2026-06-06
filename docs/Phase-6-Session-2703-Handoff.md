# Phase 6 Session 2703 Handoff

## Completed UOW

[Phase 6] UOW-2703: Reject full split destinations.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM split-to-empty-slot now rejects full cube and regular warehouse destinations before allocating an item id or mutating inventory.
- Java source/runtime path: ItemSplitService.splitItem targetItem == null branch -> destStorage.isFull() -> PacketSendUtility.sendPacket(player, destStorage.getStorageIsFullMessage()).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync targetItem == null branch, plus SmSystemMessage factories for Java full-storage message ids.
- Client-visible/state/persistence effect: full cube destination sends STR_WAREHOUSE_FULL_INVENTORY (1390149), full regular warehouse destination sends STR_WAREHOUSE_DEPOSIT_FULL_BASKET (1300421), and split state/persistence packets are suppressed.
- Why this is runtime progress: it changes packets and mutation gating in the live CM_SPLIT_ITEM handler; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2703] Reject full split destinations`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2703-Completion.md`
- `docs/Phase-6-Session-2703-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/ItemStorage.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageFullMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection.GetRegularWarehouseFreeSlots`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullCubeDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleSplitItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize" --logger "console;verbosity=minimal"
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

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- The covered cube and regular warehouse `CM_SPLIT_ITEM` full destination branches now send Java's storage-full message and return before mutation/persistence.
- Full `CM_SPLIT_ITEM`, full `IStorage.getStorageIsFullMessage`, and full storage fullness parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full cube and regular warehouse split-to-empty-slot destinations now reject before mutation/persistence with Java message ids. Full split flow remains partial. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Storage helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Cube and regular warehouse messages are wired. Account, legion, pet, and house storage full messages remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFull` | `InventoryCapacity` plus `GameServerConnection.GetRegularWarehouseFreeSlots` | Storage capacity helper | Partial | Unit Tested indirectly | Partial Parity | Uses existing Java-derived cube/warehouse limits; regular warehouse counts live flattened storage rows. Special cube and non-regular storage families remain deferred here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Packet factory | Partial | Unit Tested indirectly | Partial Parity | Added factories for `1300421` and `1390149`; packet serialization is covered through decoded live handler packets. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Account warehouse destination fullness is still not wired because the current C# runtime model does not yet restore/count account warehouse rows for this handler.
- Legion warehouse, pet warehouse, house storage, special cube, and legion history behavior remain deferred.
- `CM_MOVE_ITEM` and `CM_REPLACE_ITEM` destination-full handling still need source review before any runtime UOW.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_MOVE_ITEM` empty-slot destination-full handling against Java and proceed only if source review finds a live mismatch after the recent split fix.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM empty-slot move should reject full destination storage with Java's storage-family full message before moving or persisting an item.
- Java source/runtime path: ItemMoveService.moveItem destination-empty branch, destination storage capacity guard, and IStorage.getStorageIsFullMessage if present.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync and the existing CreateStorageFullMessage helper.
- Client-visible/state/persistence effect expected: move attempts into a full cube or regular warehouse should send the Java full-storage system message and avoid source delete/add, storage-size packets, item location mutation, and persistence.
- Why this is runtime progress: proceed only if source review confirms current C# differs; the fix would change packets and mutation gating in live CM_MOVE_ITEM handling.
```

Suggested focused validation starting point if a fix is needed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullCubeDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: full destination move rejection emits the Java storage-full message without mutating item location, deleting/adding storage packets, storage-size packets, or persistence; adjacent successful cross-storage move still works.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or storage capacity helpers are broadened beyond the live handler branch.

## Other Safe Runtime Candidates

- Inspect `CM_REPLACE_ITEM` destination/full-storage guards if Java source shows a live packet/state mismatch independent of broad legion scaffolding.
- Inspect account warehouse runtime item restore/counting before attempting account warehouse full-destination parity.
- Inspect legion warehouse replace/split/move history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `6505d4840 [Phase 6][UOW-2702] Send split restriction denials`
  - `cd125ef3b [Phase 6][UOW-2701] Unlock restricted move source`
  - `d77d6e5c5 [Phase 6][UOW-2700] Send replace restriction denials`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
