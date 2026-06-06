# Phase 6 Session 2691 Completion

## UOW

[Phase 6] UOW-2691: Unlock restricted split source storage.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cross-storage restriction failures now restore the source storage view before split/merge mutation.
- Java source/runtime path: ItemSplitService.splitItem cross-storage restriction branch -> ItemRestrictionService.isItemRestrictedTo/isItemRestrictedFrom -> ItemPacketService.sendStorageUpdatePacket(player, sourceStorage.getStorageType(), sourceItem).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync.
- Client-visible/state/persistence effect: restricted cross-storage split/merge requests send a source item storage update plus cube-size, avoid merge/split persistence, and leave source/destination counts unchanged instead of mutating an invalid destination stack or sending a warehouse error message.
- Why this is runtime progress: it changes live packet emission and prevents live inventory/persistence mutation from CM_SPLIT_ITEM; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Performs cross-storage restriction checks before kinah, split-to-empty-slot, and same-item merge branches.
  - On restriction failure, calls `sendStorageUpdatePacket(player, sourceStorage.getStorageType(), sourceItem)` and returns.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendStorageUpdatePacket` sends an item collect/add packet for the source storage and a cube-size packet.

## C# Changes

- Moved cross-storage destination storability checks before the split branch decision so they apply to both empty-slot splits and same-item merge targets.
- Replaced C# system-message rejection for this split path with Java-style source storage update packet plus cube-size.
- Added a focused regression for a non-warehouse-storable stackable item targeting an existing warehouse stack.
- Extended the local inventory-add packet assertion helper with an expected slot parameter for restored source items.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava` | Unit / live connection handler | `ItemSplitService.splitItem` cross-storage restriction branch and `ItemPacketService.sendStorageUpdatePacket` | A non-warehouse-storable stackable source item attempting to split/merge into regular warehouse keeps source and target counts unchanged, skips merge/move persistence, and sends source `SM_INVENTORY_ADD_ITEM` with `ITEM_COLLECT` plus cube-size. | Socket-backed connection fixture invoking the live private handler through reflection, runtime-loaded item template, repository mutation counters, inventory assertions, and packet byte decoding. | Only destination storability restrictions are modeled; C# still lacks Java's full `ItemRestrictionService.isItemRestrictedFrom` coverage. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM cross-storage restriction branch plus focused connection test helper assertion.
- Specific behavior/contract: Java restores the source storage view and returns on cross-storage split restrictions before any split or merge mutation.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava|FullyQualifiedName~CmSplitItemTests" --logger "console;verbosity=minimal"
- First result: failed only because the existing test helper assumed restored add packets always had slot 65535; the live handler already sent the expected source add plus cube-size. Helper was corrected with an expected slot parameter.
- Focused C# command after correction: same command.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and directly adjacent split parser/handler regressions.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, parser, and packet behavior.
- Why this scope is sufficient: the regression exercises the branch where old C# could merge into a destination stack despite Java restriction failure.
```

Result:

- Focused C# validation passed: 4/4 after helper correction.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Destination storability restriction branch now sends source storage update before split/merge mutation. Full split-item parity is not claimed. |
| `ItemRestrictionService.isItemRestrictedTo` | `ItemTemplateSummary.IsStorableInWarehouse` / `InventoryItem.IsStorableInAccountWarehouse` checks in `HandleSplitItemAsync` | Restriction logic | Partial | Unit Tested indirectly | Partial Parity | Warehouse/account destination storage checks are modeled. Full Java restriction service and source-restriction checks remain incomplete. |
| `ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateItemCollect` / `SmWarehouseAddItem` plus `SmCubeUpdate.CubeSize` in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Cube source restriction unlock packet is covered. Non-cube source unlock packet is source-reviewed but not separately tested. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom` remains unmodeled for this handler.
- Non-cube source restriction unlock packet shape is source-reviewed but not separately tested.
- Legion warehouse behavior remains deferred.
- Cross-storage full-source split-merge delete is source-aligned from UOW-2690 but not separately tested.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect non-cube source cross-storage split restriction unlock packet behavior if a safe fixture can represent warehouse source.
2. Inspect cross-storage `CM_SPLIT_ITEM` full-source merge-delete packet sequence for source warehouse/cube combinations.
3. Inspect `CM_REPLACE_ITEM` Java delete/add packet ordering and C# live packet sequence for a confirmed mismatch.
