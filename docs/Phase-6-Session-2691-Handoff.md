# Phase 6 Session 2691 Handoff

## Completed UOW

[Phase 6] UOW-2691: Unlock restricted split source storage.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cross-storage restriction failures now restore the source storage view before split/merge mutation.
- Java source/runtime path: ItemSplitService.splitItem cross-storage restriction branch -> ItemRestrictionService.isItemRestrictedTo/isItemRestrictedFrom -> ItemPacketService.sendStorageUpdatePacket(player, sourceStorage.getStorageType(), sourceItem).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync.
- Client-visible/state/persistence effect: restricted cross-storage split/merge requests send a source item storage update plus cube-size, avoid merge/split persistence, and leave source/destination counts unchanged instead of mutating an invalid destination stack or sending a warehouse error message.
- Why this is runtime progress: it changes live packet emission and prevents live inventory/persistence mutation from CM_SPLIT_ITEM; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2691] Unlock restricted split source`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2691-Completion.md`
- `docs/Phase-6-Session-2691-Handoff.md`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava|FullyQualifiedName~CmSplitItemTests" --logger "console;verbosity=minimal"
```

First result:

- Failed only in the test helper slot expectation for the restored source add packet.

Final result after helper correction:

- Passed: 4
- Failed: 0
- Skipped: 0

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- The covered cube-source destination-storability restriction branch now matches Java-reviewed source-unlock behavior.
- Full `CM_SPLIT_ITEM`, `ItemRestrictionService`, and source-storage restriction parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Destination storability restriction branch now sends source storage update before split/merge mutation. Full split-item parity is not claimed. |
| `ItemRestrictionService.isItemRestrictedTo` | `ItemTemplateSummary.IsStorableInWarehouse` / `InventoryItem.IsStorableInAccountWarehouse` checks in `HandleSplitItemAsync` | Restriction logic | Partial | Unit Tested indirectly | Partial Parity | Warehouse/account destination storage checks are modeled. Full Java restriction service and source-restriction checks remain incomplete. |
| `ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateItemCollect` / `SmWarehouseAddItem` plus `SmCubeUpdate.CubeSize` in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Cube source restriction unlock packet is covered. Non-cube source unlock packet is source-reviewed but not separately tested. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom` remains unmodeled for this handler.
- Non-cube source restriction unlock packet shape is source-reviewed but not separately tested.
- Legion warehouse behavior remains deferred.
- Cross-storage full-source split-merge delete is source-aligned from UOW-2690 but not separately tested.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_REPLACE_ITEM` Java delete/add packet ordering and C# live packet sequence, then implement the smallest confirmed runtime mismatch.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_REPLACE_ITEM should switch cross-storage items with Java-equivalent delete/add packet ordering and persisted slot/location mutations.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages.
- C# runtime artifact likely involved: GameServerConnection.HandleReplaceItemAsync and SaveReplaceItemStorageMutationAsync or equivalent persistence.
- Client-visible/state/persistence effect expected: item locations/slots and delete/add packet order change only if source review confirms current C# mismatch.
- Why this is runtime progress: proceed only if it mutates live inventory state, persists existing database rows, or sends corrected packets from CM_REPLACE_ITEM.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~<new-replace-item-runtime-test-name>|FullyQualifiedName~CmReplaceItemTests" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless repository interfaces, shared packet primitives, or storage capacity infrastructure are changed.

## Other Safe Runtime Candidates

- Inspect non-cube source cross-storage split restriction unlock packet behavior if a safe fixture can represent warehouse source.
- Inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence for source warehouse/cube combinations.
- Inspect Java target-storage full handling for `CM_MOVE_ITEM` only if C# has enough live storage capacity state.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `f92496cfb [Phase 6][UOW-2688] Merge stackable move targets`
  - `1c6e124c3 [Phase 6][UOW-2689] Move remaining stack after merge`
  - `44e35b258 [Phase 6][UOW-2690] Delete split merge source stack`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
