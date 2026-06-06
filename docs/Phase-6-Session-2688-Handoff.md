# Phase 6 Session 2688 Handoff

## Completed UOW

[Phase 6] UOW-2688: Merge stackable CM_MOVE_ITEM auto-slot targets.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM cross-storage moves now execute Java's slot == -1 stackable auto-merge path before normal move fallback.
- Java source/runtime path: ItemMoveService.moveItem -> ItemSplitService.mergeStacks -> Storage.increaseItemCount/decreaseItemCount.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync plus SaveItemMergeMutationAsync persistence.
- Client-visible/state/persistence effect: moving a stackable item into storage with an existing same-item stack mutates destination/source counts, persists the merge, emits destination increase then source delete/update packets, and avoids creating a separate moved stack when the source is fully consumed.
- Why this is runtime progress: this changes live inventory state, packet emission, and database persistence from a client packet handler; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2688] Merge stackable move targets`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2688-Completion.md`
- `docs/Phase-6-Session-2688-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`

## C# Artifacts Touched

- `GameServerConnection.HandleMoveItemAsync`
- `PlayerEnterWorldService.SaveItemMergeMutationAsync` via existing service call
- `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync`
- `EmptyPlayerEnterWorldRepository`
- `GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command attempted first:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Failed at compile due to a test assertion using `.Value` after `Assert.NotNull` returned the tuple value.
- Product code compiled during this attempt.

Focused C# command used after correction:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 1
- Failed: 0
- Skipped: 0

Adjacent focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmMoveItemTests|FullyQualifiedName~SmWarehouseUpdateItem_SerializesObjectIdTypeAndUpdateType|FullyQualifiedName~SmDeleteItem_MoveDeleteTypeMatchesJava" --logger "console;verbosity=minimal"
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

- The covered full-source stackable auto-merge branch now matches Java-reviewed behavior for state, persistence-path selection, and packet sequence.
- Full `CM_MOVE_ITEM`, `ItemMoveService`, `ItemSplitService`, and storage persistence parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_MOVE_ITEM.runImpl` | `CmMoveItem` plus `GameServerConnection.HandleMoveItemAsync` | Client packet / live handler | Partial | Unit Tested | Partial Parity | Parser and stackable auto-slot full-merge branch are covered. Full move-item parity is not claimed. |
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Slot `-1` stackable merge branch now executes before normal cross-storage move. Legion warehouse, shutdown, full-storage, partial multi-stack scenarios, and some restriction-from-source behavior remain incomplete or unverified. |
| `ItemSplitService.mergeStacks` | `GameServerConnection.HandleMoveItemAsync` merge branch and `PlayerEnterWorldService.SaveItemMergeMutationAsync` | Service / persistence | Partial | Unit Tested indirectly | Partial Parity | Full-source auto-merge packet/state behavior is covered through live handler. Generic split merge and partial merge fallback need more coverage. |
| `Storage.decreaseItemCount` | `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Persistence behavior | Partial | Manual source review | Needs Verification | Non-Kinah source rows with count `<= 0` are now deleted inside merge transaction. No live MySQL integration test was run in this UOW. |

## Known Gaps / Watchouts

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Partial auto-merge followed by normal cross-storage move is not covered.
- Multiple target stacks are source-reviewed but not tested.
- Target-storage full behavior is still not modeled in this C# handler.
- Legion warehouse history/permissions and shutdown unlock/message behavior remain deferred.
- Source restriction checks remain narrower than Java's `ItemRestrictionService.isItemRestrictedFrom`.
- Real MySQL merge-delete persistence was source-reviewed but not integration tested.

## Next Recommended Runtime UOW

Recommended candidate: continue `CM_MOVE_ITEM` with partial auto-merge followed by normal move of the remaining source stack.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: when slot == -1 and destination same-item stacks have limited free space, live CM_MOVE_ITEM should merge as much as possible, then move the remaining source stack to destination like Java.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 loop and normal cross-storage move fallback after ItemSplitService.mergeStacks.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync and SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect expected: destination stack count increases, source count decreases, remaining source item changes storage/slot, and packets show destination increase/source decrease followed by source delete/destination add for the remaining moved item.
- Why this is runtime progress: it mutates live inventory state, persists merge plus move state, and sends real server packets from CM_MOVE_ITEM.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava|FullyQualifiedName~<new-partial-merge-test-name>|FullyQualifiedName~CmMoveItemTests" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless the next UOW changes shared packet primitives, repository interfaces, or storage capacity infrastructure.

## Other Safe Runtime Candidates

- Inspect Java target-storage full handling for `CM_MOVE_ITEM` and only implement if C# has enough live storage capacity state to avoid guesswork.
- Inspect `CM_SPLIT_ITEM` full-source merge/delete packet and persistence behavior now that merge persistence deletes non-Kinah zero-count sources.
- Inspect `CM_REPLACE_ITEM` Java delete/add packet ordering only if a concrete live C# mismatch is confirmed.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The current UOW changed a live handler and existing merge persistence; do not broaden validation without a named trigger.
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
