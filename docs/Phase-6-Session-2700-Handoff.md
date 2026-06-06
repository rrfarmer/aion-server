# Phase 6 Session 2700 Handoff

## Completed UOW

[Phase 6] UOW-2700: Send replace restriction denials.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM regular/account storage rejection now sends Java-equivalent denial messages before unlocking both items.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> STR_WAREHOUSE_CANT_DEPOSIT_ITEM or STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT, then ItemMoveService.switchItemsInStorages -> sendItemUnlockPacket(sourceItem) + sendItemUnlockPacket(replaceItem).
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync restriction branch and restricted-storage helper.
- Client-visible/state/persistence effect: rejected replace attempts for non-storable regular/account warehouse moves now send the Java denial packet, unlock both client-side slots, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_REPLACE_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2700] Send replace restriction denials`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2700-Completion.md`
- `docs/Phase-6-Session-2700-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateRestrictedToStorageMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksBothLikeJava|FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava" --logger "console;verbosity=minimal"
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

- The covered regular warehouse `CM_REPLACE_ITEM` restriction branch now sends Java's denial message before both unlock packets.
- Full `CM_REPLACE_ITEM`, full `ItemMoveService`, and full restriction parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateRestrictedToStorageMessage` | Service helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse denial is asserted through live replace handling; account warehouse shares the helper but lacks a separate focused regression. Legion warehouse restrictions remain deferred. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Regular warehouse restriction now sends Java denial plus both unlock packets before mutation/persistence. Full restriction coverage remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source and replace unlock packets are asserted for cube and regular warehouse during restriction rejection. Legion variants remain limited. |

## Known Gaps / Watchouts

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- Account warehouse denial is implemented through the shared helper but not independently asserted in this UOW.
- Java `ItemRestrictionService.isItemRestrictedFrom` and legion warehouse `isItemRestrictedTo` remain incomplete in live replace handling because C# still defers legion warehouse runtime/history/permission integration.
- Account and legion warehouse storage-size variants are not fully covered.

## Next Recommended Runtime UOW

Recommended candidate: inspect account warehouse storage-size semantics in move/split/replace branches and implement only a confirmed live packet mismatch.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live account warehouse move/split/replace branches should emit Java-equivalent storage-size packets for the storage being changed.
- Java source/runtime path: ItemMoveService.moveItem / ItemSplitService.splitItem / ItemPacketService.sendItemDeletePacket and sendStorageUpdatePacket for StorageType.ACCOUNT_WAREHOUSE.
- C# runtime artifact likely involved: GameServerConnection storage-size helpers and the specific live branch with confirmed mismatch.
- Client-visible/state/persistence effect expected: account warehouse client UI should receive the correct SM_CUBE_UPDATE size packet after live item mutation/deletion/addition.
- Why this is runtime progress: proceed only if source review confirms a live packet mismatch; the fix would change packets emitted by move/split/replace live handlers after real inventory mutations.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AccountWarehouseStorageSize|FullyQualifiedName~HandleReplaceItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksBothLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: the selected account warehouse live branch emits the Java storage-size packet while preserving mutation and persistence behavior.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives are changed.

## Other Safe Runtime Candidates

- Inspect account warehouse replace rejection with soulbound/non-storable items if a separate runtime regression is needed beyond the shared helper.
- Inspect legion warehouse replace/split history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
- Inspect `CM_SPLIT_ITEM` remaining account warehouse branches only if source review finds live packet or persistence mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `15c9b2a37 [Phase 6][UOW-2699] Reject replace during shutdown`
  - `a9be11478 [Phase 6][UOW-2698] Persist replace storage switches`
  - `110f7822c [Phase 6][UOW-2697] Delete split warehouse source`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
