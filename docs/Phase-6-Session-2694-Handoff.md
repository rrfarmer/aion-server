# Phase 6 Session 2694 Handoff

## Completed UOW

[Phase 6] UOW-2694: Delete merged warehouse source.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM stack auto-merge now uses Java-equivalent source-storage delete fanout when a warehouse source stack is fully consumed.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 stack merge loop -> ItemSplitService.mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.sendItemDeletePacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync auto-merge item.Count <= 0 branch.
- Client-visible/state/persistence effect: a warehouse-source full auto-merge now sends SM_DELETE_WAREHOUSE_ITEM followed by regular-warehouse SM_CUBE_UPDATE after the merge mutation instead of a cube-size packet.
- Why this is runtime progress: it changes packets emitted by live CM_MOVE_ITEM after a real stack merge state/persistence mutation; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2694] Delete merged warehouse source`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2694-Completion.md`
- `docs/Phase-6-Session-2694-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullWarehouseSourceAutoMergeUsesJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
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

- The covered warehouse-source full auto-merge `CM_MOVE_ITEM` branch now sends the Java-style source delete storage-size packet.
- Full `CM_MOVE_ITEM`, full `ItemMoveService`, and all storage-family packet parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full-source auto-merge now uses source-storage delete fanout for warehouse sources. Other move restrictions and storage families remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` | `GameServerConnection.HandleMoveItemAsync` auto-merge branch | Service / stack merge | Partial | Unit Tested indirectly | Partial Parity | Covered for a warehouse-source stack fully merging into cube target. Partial merge and split handler variants remain separately tracked. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` / `ItemPacketService.sendItemDeletePacket` | `SendItemDeletePacketAsync` usage in `HandleMoveItemAsync` | Storage / packet service | Partial | Unit Tested indirectly | Partial Parity | Zero-count source delete uses source storage for this branch. Full storage persistence lifecycle is not claimed. |

## Known Gaps / Watchouts

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Partial auto-merge source update branch still needs non-cube source review.
- Java `ItemRestrictionService.isItemRestrictedFrom`, trading unlock edge cases, shutdown-soon behavior, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse merge/delete variants are not covered by this UOW.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_MOVE_ITEM` partial auto-merge source update for non-cube source storage and implement storage-specific update/size packets if current C# differs from Java.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM partial stack auto-merge should send Java-equivalent source update packets when the source stack remains in a non-cube storage.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 stack merge loop -> ItemSplitService.mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.sendItemPacket/sendItemUpdatePacket.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync auto-merge item.Count > 0 branch.
- Client-visible/state/persistence effect expected: a warehouse-source partial merge should update the remaining source stack using warehouse update packet semantics while preserving merge persistence and any subsequent normal move behavior.
- Why this is runtime progress: proceed only if source review confirms current C# sends the wrong live packet; the fix would change packets emitted by CM_MOVE_ITEM from live code after a real merge mutation.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_PartialWarehouseSourceAutoMergeUsesJavaStorageUpdate|FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect non-cube source cross-storage `CM_SPLIT_ITEM` restriction unlock packet behavior if a safe warehouse-source fixture can represent the live path.
- Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
- Inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence for source warehouse/cube combinations.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `7c0ace56a [Phase 6][UOW-2693] Send move storage updates`
  - `9baf76378 [Phase 6][UOW-2692] Switch replace storage items`
  - `0adbee576 [Phase 6][UOW-2691] Unlock restricted split source`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
