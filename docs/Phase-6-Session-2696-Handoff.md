# Phase 6 Session 2696 Handoff

## Completed UOW

[Phase 6] UOW-2696: Split empty-slot storage updates.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM split-to-empty-slot cross-storage moves now use Java-equivalent storage-specific SM_CUBE_UPDATE packets after source decrease and destination add.
- Java source/runtime path: ItemSplitService.splitItem targetItem == null branch -> sourceStorage.decreaseItemCount -> SM_CUBE_UPDATE.cubeSize(sourceStorage) -> destStorage.add(newItem) -> ItemPacketService.sendStorageUpdatePacket(destinationStorage).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync targetItem == null branch.
- Client-visible/state/persistence effect: a warehouse-source split into an empty cube slot now sends regular-warehouse size after the source decrease, then cube add plus cube size after the destination add, while preserving split state and persistence.
- Why this is runtime progress: it changes live CM_SPLIT_ITEM packets after a real split mutation and existing persistence call; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2696] Split empty-slot storage updates`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2696-Completion.md`
- `docs/Phase-6-Session-2696-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize" --logger "console;verbosity=minimal"
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

- The covered warehouse-source to cube-destination split-to-empty-slot branch now sends source and destination storage-size packets in Java order.
- Full `CM_SPLIT_ITEM`, full `ItemSplitService`, and all storage-family packet parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Split-to-empty-slot cross-storage packet fanout now uses source/destination storage-specific size packets. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Destination add fanout is covered for cube destination from warehouse source. Other split branches still need review. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse and cube size packets are asserted for this split branch. Account/legion variants remain limited. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Merge-into-stack full-source delete branch still has hand-written source delete/cube-size fanout and should be inspected for non-cube source behavior.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, destination full checks, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse storage-size variants are not fully covered.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_SPLIT_ITEM` merge-into-stack full-source delete packet sequence for source warehouse/cube combinations and implement storage-specific source delete fanout if current C# differs from Java.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_SPLIT_ITEM merge-into-stack should delete fully consumed source stacks with Java-equivalent source-storage delete and storage-specific SM_CUBE_UPDATE.
- Java source/runtime path: ItemSplitService.splitItem targetItem same item -> mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.sendItemDeletePacket.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync targetItem same-item sourceItem.Count <= 0 branch.
- Client-visible/state/persistence effect expected: a warehouse-source full merge should send SM_DELETE_WAREHOUSE_ITEM plus regular-warehouse SM_CUBE_UPDATE while preserving merge persistence/state mutation.
- Why this is runtime progress: proceed only if source review confirms current C# sends the wrong live packet; the fix would change packets emitted by CM_SPLIT_ITEM from live code after a real merge mutation.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullWarehouseSourceMergeUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect `CM_SPLIT_ITEM` merge-into-stack partial source update for non-cube source storage and implement storage-specific source update/size behavior if needed.
- Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
- Inspect account warehouse storage-size semantics in move/split branches only if Java source review confirms a live C# packet mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `df25b7af6 [Phase 6][UOW-2695] Unlock split warehouse source`
  - `4a284695b [Phase 6][UOW-2694] Delete merged warehouse source`
  - `7c0ace56a [Phase 6][UOW-2693] Send move storage updates`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
