# Phase 6 Session 2698 Handoff

## Completed UOW

[Phase 6] UOW-2698: Persist replace storage switches atomically.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM cross-storage item switches now persist both swapped item locations through one storage-switch mutation.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> swap equipmentSlot values -> remove both items -> delete packets -> add both items, with the changed inventory state persisted as a coherent storage update.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync cross-storage branch, PlayerEnterWorldService, and PlayerEnterWorldRepository.
- Client-visible/state/persistence effect: a cube/warehouse item switch now rolls back both in-memory item locations and sends no packets if the two-row persistence fails; on success it keeps the existing Java delete/add packet order.
- Why this is runtime progress: it changes live CM_REPLACE_ITEM persistence and rollback behavior after a real inventory state mutation; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2698] Persist replace storage switches`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2698-Completion.md`
- `docs/Phase-6-Session-2698-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository`
- `Aion.GameServer.Data.EmptyPlayerEnterWorldRepository`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems" --logger "console;verbosity=minimal"
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

- The covered cube/regular-warehouse `CM_REPLACE_ITEM` cross-storage switch now persists both changed item rows through one repository mutation and preserves Java packet order on success.
- Full `CM_REPLACE_ITEM`, full `ItemMoveService`, and all storage-family parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REPLACE_ITEM` | `Aion.GameServer.Network.Aion.ClientPackets.CmReplaceItem` / `GameServerConnection.HandleReplaceItemAsync` | Client packet / live handler | Partial | Unit Tested | Partial Parity | Cross-storage switch persistence and packet order are covered. Parser parity existed previously; full handler restrictions are not complete. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cross-storage cube/regular-warehouse switches now persist both swapped rows through one mutation and roll back both on save failure. Legion warehouse, shutdown-soon, and full restriction checks remain gaps. |
| `com.aionemu.gameserver.dao.InventoryDAO.store` | `MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync` | Repository / persistence | Partial | Unit Tested indirectly | Partial Parity | Two item-location updates are wrapped in a MySQL transaction. No live MySQL integration test was run for this UOW. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` / `sendStorageUpdatePacket` | `SendItemDeletePacketAsync` / `SendStorageUpdatePacketAsync` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Existing Java delete/delete/add/add order remains covered for cube and regular warehouse. Other storage families remain limited. |

## Known Gaps / Watchouts

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, and legion warehouse history/permission integration remain incomplete.
- The new MySQL two-row storage-switch mutation was compiled and reviewed, but not exercised against a live database fixture.
- Account and legion warehouse storage-size variants are not fully covered.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_REPLACE_ITEM` restriction/shutdown branch against Java `ItemRestrictionService.isItemRestrictedFrom`, `isItemRestrictedTo`, and `GameServer.isShuttingDownSoon`; implement only if current C# differs from Java in live unlock/system-message behavior.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_REPLACE_ITEM rejection should unlock both involved items and, if shutdown-soon is represented in C#, send the Java shutdown disable system message.
- Java source/runtime path: ItemMoveService.switchItemsInStorages restriction branch -> sendItemUnlockPacket(sourceItem) + sendItemUnlockPacket(replaceItem) + optional SM_SYSTEM_MESSAGE.STR_MSG_DISABLE("Shutdown Progress").
- C# runtime artifact likely involved: GameServerConnection.HandleReplaceItemAsync restriction branch and storage update helpers.
- Client-visible/state/persistence effect expected: rejected replace attempts should send Java-equivalent item unlock/update packets for both source and replace items without mutating/persisting inventory state.
- Why this is runtime progress: proceed only if source review confirms current C# omits a live rejection packet or message; the fix would change packets emitted by CM_REPLACE_ITEM from live code.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_RestrictionUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: rejected replace-item storage switches emit source and replace unlock/update packets without mutating item locations or calling persistence.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared restriction helpers, packet primitives, or repository interfaces are changed.

## Other Safe Runtime Candidates

- Inspect account warehouse storage-size semantics in move/split/replace branches only if Java source review confirms a live C# packet mismatch.
- Inspect legion warehouse replace/split history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
- Inspect `CM_SPLIT_ITEM` remaining account warehouse branches only if source review finds live packet or persistence mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `110f7822c [Phase 6][UOW-2697] Delete split warehouse source`
  - `36b2cecf6 [Phase 6][UOW-2696] Split empty-slot storage updates`
  - `df25b7af6 [Phase 6][UOW-2695] Unlock split warehouse source`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
