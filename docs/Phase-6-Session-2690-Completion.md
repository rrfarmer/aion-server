# Phase 6 Session 2690 Completion

## UOW

[Phase 6] UOW-2690: Delete source stack on full split merge.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM same-item merge now deletes the source stack when the merge consumes it completely.
- Java source/runtime path: ItemSplitService.splitItem targetItem same-item branch -> mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.ItemDeleteType.fromUpdateType.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync and SmDeleteItem split delete mask.
- Client-visible/state/persistence effect: source stack is removed from live inventory, merge persistence records a zero-count/deleted source, and packets are destination increase, source delete, and cube-size instead of a zero-count source update.
- Why this is runtime progress: it mutates live inventory state, persists through the existing merge mutation path, and sends real server packets from CM_SPLIT_ITEM; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`
  - Reads source object id, amount, source storage, destination object id, destination storage, and signed slot, then calls `ItemSplitService.splitItem`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Same-item target branch delegates to `mergeStacks`.
  - `mergeStacks` increases destination first, then decreases source with `DEC_ITEM_SPLIT` for same storage or `DEC_ITEM_SPLIT_MOVE` for cross storage.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `decreaseItemCount` deletes non-Kinah items when count reaches zero.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `ItemDeleteType.fromUpdateType(DEC_ITEM_SPLIT)` maps to `SPLIT` (`0x04`), and `DEC_ITEM_SPLIT_MOVE` maps to `MOVE` (`0x14`).

## C# Changes

- Added `SmDeleteItem.SplitDeleteType = 0x04` with Java breadcrumb.
- Updated `HandleSplitItemAsync` same-item merge branch to:
  - keep the existing destination increase packet,
  - remove the source item from live `player.InventoryItems` when its count reaches zero,
  - emit cube or warehouse delete with `SPLIT` for same-storage and `MOVE` for cross-storage,
  - emit cube-size after source deletion.
- Added a focused live connection regression and split-packet helper/invoker.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava` | Unit / live connection handler | `ItemSplitService.mergeStacks`, `Storage.decreaseItemCount`, `ItemPacketService.ItemDeleteType.fromUpdateType` | A same-storage split-merge of source count 3 into target count 97 removes the source, raises target to 100, persists merge, sends target `INC_ITEM_MERGE`, source delete type `SPLIT`, and cube-size count 1. | Socket-backed connection fixture invoking the live private handler through reflection, runtime-loaded template, repository mutation counters, live inventory assertions, and packet byte decoding. | Cross-storage full-source split-merge delete uses the same branch but was not separately tested. Partial split-merge remains existing behavior. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM same-item merge source deletion plus split delete packet mask.
- Specific behavior/contract: Java deletes non-Kinah source stacks when a split-merge consumes the full source amount and maps DEC_ITEM_SPLIT to ItemDeleteType.SPLIT.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava|FullyQualifiedName~CmSplitItemTests|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch, one packet mask constant, and directly adjacent parser/move-merge regression coverage.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, parser, and packet behavior.
- Why this scope is sufficient: the regression exercises the exact branch where old C# emitted a zero-count update and left the source item live instead of deleting it like Java.
```

Result:

- Focused C# validation passed: 4/4.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_SPLIT_ITEM.runImpl` | `CmSplitItem` plus `GameServerConnection.HandleSplitItemAsync` | Client packet / live handler | Partial | Unit Tested | Partial Parity | Parser exists and full-source same-storage merge-delete branch is covered. Complete split-item parity is not claimed. |
| `ItemSplitService.mergeStacks` | `GameServerConnection.HandleSplitItemAsync` merge branch and `PlayerEnterWorldService.SaveItemMergeMutationAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-storage full-source merge now deletes source and emits Java delete type. Cross-storage full-source delete not separately tested. |
| `Storage.decreaseItemCount` | `GameServerConnection.HandleSplitItemAsync` delete branch and `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Runtime state / persistence | Partial | Unit Tested indirectly | Partial Parity | Live handler removes zero-count non-Kinah source; repository merge deletion was added in UOW-2688. Real MySQL integration remains untested here. |
| `ItemPacketService.ItemDeleteType` | `SmDeleteItem.SplitDeleteType` | Packet enum/constant | Partial | Unit Tested indirectly | Partial Parity | `SPLIT = 0x04` is covered through live packet decoding. Full enum parity is not claimed. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Cross-storage full-source split-merge delete path is source-aligned but not separately tested.
- Partial split-merge and target-full behavior remain only existing coverage/source review.
- Legion warehouse history/permissions remain deferred.
- Real MySQL merge-delete behavior was not integration tested in this UOW.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence and cube-size behavior for a narrow live regression if safe.
2. Inspect `CM_SPLIT_ITEM` restriction failure branch: Java sends storage update unlock for cross-storage restricted source, while C# currently sends warehouse/cant-deposit messages in some cases.
3. Inspect `CM_REPLACE_ITEM` Java delete/add packet ordering and C# live packet sequence for a confirmed mismatch.
