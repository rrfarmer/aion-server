# Phase 6 Session 2713 Handoff

## Completed UOW

[Phase 6] UOW-2713: Create missing Kinah rows for account warehouse moves.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM Kinah moves between cube and account warehouse now create a missing destination Kinah runtime row instead of returning.
- Java source/runtime path: ItemSplitService.splitItem -> moveKinah -> updateKinahCount -> Storage.increaseKinah -> Storage.add(ItemFactory.newItem(KINAH, 0)) -> increaseItemCount -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection.HandleKinahMoveAsync, Player.InventoryItems, Player.AccountWarehouseItems, MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect: moving Kinah to an empty cube/account warehouse mutates both live storage lists, sends source decrease plus destination add/update packets, and persists the new Kinah row through the existing inventory table.
- Why this is runtime progress: this changes live packet handling, runtime inventory/account-warehouse state, server packets, object-id allocation/release, and database persistence.
```

## Commit

`[Phase 6][UOW-2713] Create missing Kinah move targets`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2713-Completion.md`
- `docs/Phase-6-Session-2713-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleKinahMoveAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.AddMoveStorageItem`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync`
- `Aion.GameServer.Model.GameObjects.Player.InventoryItems`
- `Aion.GameServer.Model.GameObjects.Player.AccountWarehouseItems`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CubeKinahMoveCreatesMissingAccountWarehouseKinahLikeJava|FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseKinahMoveCreatesMissingCubeKinahLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseSourceSplitsRestoredItemToCubeLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 4
- Failed: 0
- Skipped: 0
- Existing warnings only.

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

- `CM_SPLIT_ITEM` Kinah movement between cube and account warehouse now has partial runtime parity when the destination storage did not already have a Kinah row.
- Full Kinah/storage parity is not claimed. Legion warehouse Kinah, legion history, legion permissions, and broader Java `Storage` object behavior remain incomplete.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.moveKinah` | `GameServerConnection.HandleKinahMoveAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cube/account warehouse Kinah moves now handle missing destination Kinah rows. Legion warehouse Kinah remains deferred. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseKinah` | `GameServerConnection.HandleKinahMoveAsync` / `AddMoveStorageItem` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Missing destination Kinah row creation and add/update packet sequence are modeled for cube/account warehouse only. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `SendStorageUpdatePacketAsync` / `SmInventoryAddItem` / `SmWarehouseAddItem` / update packets | Packet service | Partial | Unit Tested indirectly | Partial Parity | Tests assert packet classes and decoded fields for this Kinah path; exact Java runtime bytes were not captured. |
| `com.aionemu.gameserver.dao.InventoryDAO.store` | `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Merge targets marked `New` are inserted, enabling missing destination Kinah persistence. Broader dirty-item store parity remains incomplete. |

## Known Gaps / Watchouts

- `CM_LEGION_WH_KINAH` is parsed but still no-ops in dispatch.
- Legion warehouse item move/split paths still return before Java permission/history/storage behavior.
- Exact Java wire bytes for missing-destination Kinah add/update packets were not captured.
- Packet observer assertions decode packet objects after final state mutation, while the live send serializes during `SendPacketAsync`; this UOW used field-level packet assertions, not Java byte goldens.
- Regular/account warehouse helper parity remains partial.

## Next Recommended Runtime UOW

Recommended candidate: wire the smallest `CM_LEGION_WH_KINAH` live denial path.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: parsed CM_LEGION_WH_KINAH should send Java's legion warehouse authority-denial message for players without legion membership or required rights instead of silently no-oping.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> activePlayer.getLegionMember() / hasRights(WH_DEPOSIT, WH_WITHDRAWAL) -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT.
- C# runtime artifact likely involved: GameServerConnection.ProcessPacketAsync dispatch case for CmLegionWarehouseKinah, a new HandleLegionWarehouseKinahAsync helper, Player.LegionId/LegionRank or any available legion rights model, SmSystemMessage.LegionWarehouseNoRight.
- Client-visible/state/persistence effect expected: client receives a real system message denial for unsupported/no-right legion warehouse Kinah actions; no inventory or Kinah state is mutated.
- Why this is runtime progress: it wires a deferred live client packet path and sends a real Java-equivalent server packet from live code.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionWarehouseKinah|FullyQualifiedName~HandleLegionWarehouseKinahAsync_NoLegionSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none if the UOW only wires the denial branch and packet send.

## Other Safe Runtime Candidates

- Scope the first legion warehouse item move/split runtime slice only after legion storage owner/history prerequisites are identified.
- Inspect regular warehouse replace/split restored-list behavior and proceed only if a code mismatch remains, not just missing tests.
- Inspect account warehouse Kinah existing-target packet bytes against a Java golden if a narrow runtime capture becomes available.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `f65ce73b0 [Phase 6][UOW-2712] Add moved items to regular warehouse`
  - `9b9957296 [Phase 6][UOW-2711] Move restored regular warehouse items`
  - `b1ff8d5dd [Phase 6][UOW-2710] Merge account warehouse move stacks`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
