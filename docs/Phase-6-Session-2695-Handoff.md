# Phase 6 Session 2695 Handoff

## Completed UOW

[Phase 6] UOW-2695: Unlock split warehouse source.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cross-storage restriction failures now use Java-equivalent source-storage update packets for non-cube source storage.
- Java source/runtime path: ItemSplitService.splitItem cross-storage restriction branch -> ItemPacketService.sendStorageUpdatePacket(player, sourceStorage.getStorageType(), sourceItem).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync.
- Client-visible/state/persistence effect: a warehouse-source split rejected by destination restrictions now sends SM_WAREHOUSE_ADD_ITEM followed by regular-warehouse SM_CUBE_UPDATE and leaves item state/persistence unchanged.
- Why this is runtime progress: it changes live packet emission from CM_SPLIT_ITEM on a restriction branch and prevents mutation; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2695] Unlock split warehouse source`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2695-Completion.md`
- `docs/Phase-6-Session-2695-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava" --logger "console;verbosity=minimal"
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

- The covered warehouse-source `CM_SPLIT_ITEM` restriction branch now restores the source UI with source-storage packet semantics.
- Full `CM_SPLIT_ITEM`, full `ItemSplitService`, and full restriction service parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cross-storage restriction unlock now uses source storage fanout for cube and regular warehouse source. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Restriction unlock source add plus storage-size packet is covered for warehouse source. Other split branches still have local packet fanout to inspect. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `ItemTemplateSummary.IsStorableInWarehouse` / `InventoryItem.IsStorableInAccountWarehouse` checks in `HandleSplitItemAsync` | Restriction logic | Partial | Unit Tested indirectly | Partial Parity | Destination restriction branches are modeled for warehouse/account warehouse. `isItemRestrictedFrom` remains unmodeled. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Split-to-empty-slot cross-storage packet fanout still has hand-written source/destination cube-size packets and should be inspected for non-cube source/destination behavior.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, and legion warehouse history remain incomplete.
- Account warehouse/legion warehouse storage-size variants are not fully covered.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_SPLIT_ITEM` split-to-empty-slot cross-storage packet fanout and implement source/destination storage-specific `SM_CUBE_UPDATE` packets if current C# differs from Java.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_SPLIT_ITEM split-to-empty-slot should send Java-equivalent storage-specific SM_CUBE_UPDATE packets after source decrease and destination add.
- Java source/runtime path: ItemSplitService.splitItem targetItem == null branch -> sourceStorage.decreaseItemCount -> SM_CUBE_UPDATE.cubeSize(sourceStorage) -> destStorage.add(newItem) -> ItemPacketService.sendStorageUpdatePacket(destinationStorage).
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync targetItem == null branch.
- Client-visible/state/persistence effect expected: cross-storage splits involving regular warehouse should report the Java storage ordinal/count after source decrease and destination add while preserving split persistence/state mutation.
- Why this is runtime progress: proceed only if source review confirms current C# sends the wrong live packet; the fix would change packets emitted by CM_SPLIT_ITEM from live code after a real split mutation.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect `CM_SPLIT_ITEM` merge-into-stack full-source delete packet sequence for source warehouse/cube combinations.
- Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
- Inspect account warehouse storage-size semantics in move/split branches only if Java source review confirms a live C# packet mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `4a284695b [Phase 6][UOW-2694] Delete merged warehouse source`
  - `7c0ace56a [Phase 6][UOW-2693] Send move storage updates`
  - `9baf76378 [Phase 6][UOW-2692] Switch replace storage items`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
