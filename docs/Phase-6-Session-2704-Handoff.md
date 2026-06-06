# Phase 6 Session 2704 Handoff

## Completed UOW

[Phase 6] UOW-2704: Reject full move destinations.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM cross-storage moves now reject full cube and regular warehouse destinations after Java-style auto-merge attempts and before moving/persisting the remaining source item.
- Java source/runtime path: ItemMoveService.moveItem -> optional stack auto-merge -> targetStorage.isFull() -> send targetStorage.getStorageIsFullMessage() -> sendItemUnlockPacket(player, item).
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync reuses CreateStorageFullMessage and source SendStorageUpdatePacketAsync ALL_SLOT unlock.
- Client-visible/state/persistence effect: full cube destination sends STR_WAREHOUSE_FULL_INVENTORY (1390149) and unlocks the warehouse source; full regular warehouse destination sends STR_WAREHOUSE_DEPOSIT_FULL_BASKET (1300421) and unlocks the cube source; move mutation/persistence and destination delete/add packets are suppressed.
- Why this is runtime progress: it changes packet fanout and mutation gating in the live CM_MOVE_ITEM handler; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2704] Reject full move destinations`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2704-Completion.md`
- `docs/Phase-6-Session-2704-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageFullMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection.SendStorageUpdatePacketAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullCubeDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
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

- The covered cube and regular warehouse `CM_MOVE_ITEM` full destination branches now send Java's storage-full message and unlock the source item before mutation/persistence.
- Full `CM_MOVE_ITEM`, full `IStorage.getStorageIsFullMessage`, and full storage fullness parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full cube and regular warehouse destinations now reject after auto-merge attempts with Java message ids and source unlock. Full move flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleMoveItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Full-destination move rejection now restores the source item with `ALL_SLOT` and storage-size packet for cube/regular warehouse sources. Other storage families remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Storage helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Cube and regular warehouse full messages are used by live move and split handlers. Account, legion, pet, and house storage full messages remain incomplete. |

## Known Gaps / Watchouts

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Partial auto-merge followed by full-destination rejection is Java-reviewed but not separately asserted.
- Account warehouse destination fullness is still not wired because the current C# runtime model does not yet restore/count account warehouse rows for this handler.
- Legion warehouse history/permissions and non-regular storage families remain deferred.
- `CM_REPLACE_ITEM` restriction, shutdown, and switch ordering are partially covered, but full Java source review is still needed for remaining edge cases.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_REPLACE_ITEM` / Java `ItemMoveService.switchItemsInStorages` trading and shutdown rejection against current C# and proceed only if source review finds a live mismatch.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_REPLACE_ITEM rejection should restore both involved items and send Java's shutdown disable message when appropriate.
- Java source/runtime path: ItemMoveService.switchItemsInStorages restriction/trading/shutdown branch -> sendItemUnlockPacket(player, sourceItem) -> sendItemUnlockPacket(player, replaceItem) -> optional STR_MSG_DISABLE("Shutdown Progress").
- C# runtime artifact likely involved: GameServerConnection.HandleReplaceItemAsync and existing SendStorageUpdatePacketAsync/SmSystemMessage.Disable helpers.
- Client-visible/state/persistence effect expected: rejected replace attempts should send both source and replace unlock packets with storage-size updates, send shutdown disable when needed, and avoid item slot/location mutation plus persistence.
- Why this is runtime progress: proceed only if source review confirms current C# differs; the fix would change packets emitted by live CM_REPLACE_ITEM rejection handling.
```

Suggested focused validation starting point if a fix is needed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_TradingUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: replace rejection emits both Java source unlock packets, emits Java disable message for shutdown, and does not mutate/persist; adjacent successful switch ordering still works.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or storage helpers are changed.

## Other Safe Runtime Candidates

- Inspect `CM_MOVE_ITEM` partial auto-merge followed by full-destination rejection if a focused live mismatch is suspected beyond the source-reviewed ordering.
- Inspect account warehouse runtime item restore/counting before attempting account warehouse full-destination parity.
- Inspect legion warehouse replace/split/move history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `7a3ab5a13 [Phase 6][UOW-2703] Reject full split destinations`
  - `6505d4840 [Phase 6][UOW-2702] Send split restriction denials`
  - `cd125ef3b [Phase 6][UOW-2701] Unlock restricted move source`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
