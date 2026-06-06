# Phase 6 Session 2697 Handoff

## Completed UOW

[Phase 6] UOW-2697: Delete split warehouse source.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM merge-into-stack full-source deletes now use Java-equivalent source-storage delete fanout.
- Java source/runtime path: ItemSplitService.splitItem targetItem same item -> mergeStacks -> Storage.decreaseItemCount -> Storage.delete -> ItemPacketService.sendItemDeletePacket.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync targetItem same-item sourceItem.Count <= 0 branch.
- Client-visible/state/persistence effect: a warehouse-source full merge now sends SM_DELETE_WAREHOUSE_ITEM plus regular-warehouse SM_CUBE_UPDATE while preserving target-stack update, in-memory source removal, and merge persistence.
- Why this is runtime progress: it changes live CM_SPLIT_ITEM packets after a real merge mutation; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2697] Delete split warehouse source`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2697-Completion.md`
- `docs/Phase-6-Session-2697-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DELETE_WAREHOUSE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.SendItemDeletePacketAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullWarehouseSourceMergeUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
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

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- The covered warehouse-source full merge branch now sends source-storage-aware delete and size packets matching reviewed Java behavior.
- Full `CM_SPLIT_ITEM`, full `ItemSplitService`, and all storage-family packet parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-item full-source merge delete fanout now uses source-storage-aware delete/size packets. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` / `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `GameServerConnection.HandleSplitItemAsync` source decrease/delete branch | Service / storage mutation | Partial | Unit Tested indirectly | Partial Parity | Zero-count source deletion is covered for regular warehouse source into cube target. Other storage families and partial-source update branches still need review. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `SendItemDeletePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Warehouse delete plus warehouse size packet are asserted for this merge branch. Account/legion variants remain limited. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_WAREHOUSE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteWarehouseItem` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Packet is decoded in the focused live handler regression for regular warehouse delete. Broader packet parity is not claimed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse size packet is asserted for this delete branch. Account/legion variants remain limited. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Merge-into-stack partial source update for non-cube source storage still needs source review.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, destination full checks, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse storage-size variants are not fully covered.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_SPLIT_ITEM` merge-into-stack partial source update for non-cube source storage and implement storage-specific source update/size behavior only if current C# differs from Java.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_SPLIT_ITEM merge-into-stack partial source updates should send Java-equivalent source-storage update packets after decreasing a non-cube source stack.
- Java source/runtime path: ItemSplitService.splitItem targetItem same item -> mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.sendItemPacket/sendItemUpdatePacket for the source item.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync targetItem same-item sourceItem.Count > 0 branch.
- Client-visible/state/persistence effect expected: a warehouse-source partial merge should send the target stack increase plus source warehouse update packet without an incorrect cube-only update/size packet, while preserving merge persistence/state mutation.
- Why this is runtime progress: proceed only if source review confirms current C# sends the wrong live packet; the fix would change packets emitted by CM_SPLIT_ITEM from live code after a real merge mutation.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_PartialWarehouseSourceMergeUsesJavaStorageUpdate|FullyQualifiedName~HandleSplitItemAsync_FullWarehouseSourceMergeUsesJavaStorageSize" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: the partial-source same-item merge branch emits the Java storage-family source update packet while preserving target-stack mutation and merge persistence.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
- Inspect account warehouse storage-size semantics in move/split branches only if Java source review confirms a live C# packet mismatch.
- Inspect legion warehouse delete/update history only if Java source review can isolate a narrow live storage branch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `36b2cecf6 [Phase 6][UOW-2696] Split empty-slot storage updates`
  - `df25b7af6 [Phase 6][UOW-2695] Unlock split warehouse source`
  - `4a284695b [Phase 6][UOW-2694] Delete merged warehouse source`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
