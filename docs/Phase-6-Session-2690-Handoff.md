# Phase 6 Session 2690 Handoff

## Completed UOW

[Phase 6] UOW-2690: Delete source stack on full split merge.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM same-item merge now deletes the source stack when the merge consumes it completely.
- Java source/runtime path: ItemSplitService.splitItem targetItem same-item branch -> mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.ItemDeleteType.fromUpdateType.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync and SmDeleteItem split delete mask.
- Client-visible/state/persistence effect: source stack is removed from live inventory, merge persistence records a zero-count/deleted source, and packets are destination increase, source delete, and cube-size instead of a zero-count source update.
- Why this is runtime progress: it mutates live inventory state, persists through the existing merge mutation path, and sends real server packets from CM_SPLIT_ITEM; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2690] Delete split merge source stack`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDeleteItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2690-Completion.md`
- `docs/Phase-6-Session-2690-Handoff.md`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava|FullyQualifiedName~CmSplitItemTests|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 4
- Failed: 0
- Skipped: 0

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- The covered same-storage full-source split-merge branch now matches Java-reviewed state, persistence-path, and packet behavior.
- Full `CM_SPLIT_ITEM`, `ItemSplitService`, and storage persistence parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_SPLIT_ITEM.runImpl` | `CmSplitItem` plus `GameServerConnection.HandleSplitItemAsync` | Client packet / live handler | Partial | Unit Tested | Partial Parity | Parser exists and full-source same-storage merge-delete branch is covered. Complete split-item parity is not claimed. |
| `ItemSplitService.mergeStacks` | `GameServerConnection.HandleSplitItemAsync` merge branch and `PlayerEnterWorldService.SaveItemMergeMutationAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-storage full-source merge now deletes source and emits Java delete type. Cross-storage full-source delete not separately tested. |
| `Storage.decreaseItemCount` | `GameServerConnection.HandleSplitItemAsync` delete branch and `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Runtime state / persistence | Partial | Unit Tested indirectly | Partial Parity | Live handler removes zero-count non-Kinah source; repository merge deletion was added in UOW-2688. Real MySQL integration remains untested here. |
| `ItemPacketService.ItemDeleteType` | `SmDeleteItem.SplitDeleteType` | Packet enum/constant | Partial | Unit Tested indirectly | Partial Parity | `SPLIT = 0x04` is covered through live packet decoding. Full enum parity is not claimed. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Cross-storage full-source split-merge delete path is source-aligned but not separately tested.
- Partial split-merge and target-full behavior remain only existing coverage/source review.
- Legion warehouse history/permissions remain deferred.
- Real MySQL merge-delete behavior was not integration tested in this UOW.

## Next Recommended Runtime UOW

Recommended candidate: inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence and cube-size behavior, then add a narrow live regression or fix if a mismatch remains.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_SPLIT_ITEM cross-storage same-item full merge should delete the source stack with Java MOVE delete type and emit Java-equivalent source/destination storage packets.
- Java source/runtime path: ItemSplitService.splitItem cross-storage same-item branch -> mergeStacks -> Storage.decreaseItemCount with DEC_ITEM_SPLIT_MOVE.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync and SmDeleteWarehouseItem/SmDeleteItem packet emission.
- Client-visible/state/persistence effect expected: source stack runtime removal, merge persistence deletion, and source delete packet/cube-size behavior for cross-storage split-merge.
- Why this is runtime progress: it mutates live inventory state, persists existing database rows, and sends real server packets from CM_SPLIT_ITEM.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~<new-cross-storage-split-full-merge-test-name>|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava|FullyQualifiedName~CmSplitItemTests" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless repository interfaces, shared packet primitives, or storage capacity infrastructure are changed.

## Other Safe Runtime Candidates

- Inspect `CM_SPLIT_ITEM` restriction failure branch: Java sends storage update unlock for cross-storage restricted source, while C# currently sends warehouse/cant-deposit messages in some cases.
- Inspect `CM_REPLACE_ITEM` Java delete/add packet ordering and C# live packet sequence for a confirmed mismatch.
- Inspect Java target-storage full handling for `CM_MOVE_ITEM` only if C# has enough live storage capacity state.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `f92496cfb [Phase 6][UOW-2688] Merge stackable move targets`
  - `1c6e124c3 [Phase 6][UOW-2689] Move remaining stack after merge`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
