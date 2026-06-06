# Phase 6 Session 2689 Handoff

## Completed UOW

[Phase 6] UOW-2689: Move remaining stack after partial auto-merge.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM now covers the Java path where slot == -1 partially merges into an existing destination stack, then moves the remaining source stack normally.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 merge loop, then normal cross-storage remove/add fallback; ItemPacketService.sendStorageUpdatePacket uses ItemAddType.ITEM_COLLECT.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync.
- Client-visible/state/persistence effect: destination stack increases, source stack decreases, remaining source item persists/moves to destination storage, and the destination add packet uses ITEM_COLLECT instead of ALL_SLOT.
- Why this is runtime progress: this changes live inventory state, persistence calls, and server packets from a client packet handler; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2689] Move remaining stack after merge`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2689-Completion.md`
- `docs/Phase-6-Session-2689-Handoff.md`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava|FullyQualifiedName~CmMoveItemTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 6
- Failed: 0
- Skipped: 0

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- The covered partial auto-merge plus remaining-stack move branch now matches Java-reviewed state and packet-mask behavior.
- Full `CM_MOVE_ITEM` and warehouse cube-size parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full-source and partial-source stackable auto-slot merge paths are now covered. Full move-item parity remains incomplete. |
| `ItemPacketService.sendStorageUpdatePacket` | `GameServerConnection.HandleMoveItemAsync` warehouse add branch | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Normal cross-storage warehouse add now uses `ITEM_COLLECT` for this live handler. Other call sites are not audited in this UOW. |
| `SM_WAREHOUSE_ADD_ITEM.writeImpl` | `SmWarehouseAddItem` | Server packet | Partial | Unit Tested indirectly | Needs Verification | Test decodes the add mask in live handler output; full packet golden parity is not claimed. |

## Known Gaps / Watchouts

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Target-storage full behavior remains unmodeled in the C# handler.
- Final warehouse cube-size packet contents are not fully verified for regular/account warehouse because this handler stores all items in `player.InventoryItems` while `SmCubeUpdate.RegularWarehouseSize` uses `player.WarehouseItems`.
- Legion warehouse history/permissions and shutdown unlock/message behavior remain deferred.
- Source restriction checks remain narrower than Java's `ItemRestrictionService.isItemRestrictedFrom`.

## Next Recommended Runtime UOW

Recommended candidate: inspect Java `CM_SPLIT_ITEM` full-source merge/delete behavior and C# `HandleSplitItemAsync` live packet sequence/persistence.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: when a split request targets a same-item stack and consumes the full source stack, live C# should delete the source item and emit Java-equivalent packets instead of leaving a zero-count runtime stack/update.
- Java source/runtime path: ItemSplitService.splitItem targetItem same-item branch -> mergeStacks -> Storage.decreaseItemCount delete behavior.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync and existing SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect expected: source stack runtime state, persistence deletion, and packet sequence change for full-source split-merge.
- Why this is runtime progress: it mutates live inventory state, persists existing database rows, and sends real server packets from CM_SPLIT_ITEM.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~<new-split-full-merge-test-name>|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava|FullyQualifiedName~CmSplitItemTests" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless repository interfaces, shared packet primitives, or storage capacity infrastructure are changed.

## Other Safe Runtime Candidates

- Inspect Java target-storage full handling for `CM_MOVE_ITEM` and only implement if C# has enough live storage capacity state.
- Inspect `CM_REPLACE_ITEM` Java delete/add packet ordering and C# live packet sequence for a confirmed mismatch.
- Inspect source restriction-from-storage behavior for `CM_MOVE_ITEM` only if C# has enough item restriction state loaded from Java XML.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits in this run:
  - `f92496cfb [Phase 6][UOW-2688] Merge stackable move targets`
  - `[Phase 6][UOW-2689] Move remaining stack after merge`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
