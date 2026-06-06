# Phase 6 Session 2699 Handoff

## Completed UOW

[Phase 6] UOW-2699: Reject replace during shutdown.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM rejection now handles GameServer.isShuttingDownSoon with Java-equivalent item unlock packets and STR_MSG_DISABLE("Shutdown Progress").
- Java source/runtime path: ItemMoveService.switchItemsInStorages restriction branch -> sendItemUnlockPacket(sourceItem) + sendItemUnlockPacket(replaceItem) + SM_SYSTEM_MESSAGE.STR_MSG_DISABLE("Shutdown Progress").
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync restriction branch and SmSystemMessage.
- Client-visible/state/persistence effect: replace-item attempts during shutdown now unlock both client-side item slots, send the shutdown disable system message, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_REPLACE_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2699] Reject replace during shutdown`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2699-Completion.md`
- `docs/Phase-6-Session-2699-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava" --logger "console;verbosity=minimal"
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

- The covered `CM_REPLACE_ITEM` shutdown rejection branch now sends both Java unlock packets and Java `STR_MSG_DISABLE("Shutdown Progress")`.
- Full `CM_REPLACE_ITEM`, full `ItemMoveService`, and full restriction parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Shutdown-soon rejection now unlocks both items and sends the Java disable message. Full restriction coverage remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source and replace unlock packets are asserted for cube and regular warehouse during shutdown rejection. Legion variants remain limited. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_DISABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.Disable` | Server packet factory | Partial | Unit Tested indirectly | Partial Parity | Message id and string parameter are asserted through the live handler regression. Other generated message coverage is outside this UOW. |

## Known Gaps / Watchouts

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom` remains incomplete in live replace handling because C# still defers legion warehouse runtime/history/permission integration.
- Java `isItemRestrictedTo` legion warehouse branches remain incomplete for the same reason.
- Account and legion warehouse storage-size variants are not fully covered.

## Next Recommended Runtime UOW

Recommended candidate: inspect remaining `CM_REPLACE_ITEM` rejection behavior for regular/account warehouse `isItemRestrictedTo` denial messages and implement only if current C# silently unlocks without Java's denial system message.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_REPLACE_ITEM rejection for regular/account warehouse storage restrictions should send Java-equivalent denial system messages before unlocking both items.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> STR_WAREHOUSE_CANT_DEPOSIT_ITEM or STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT, then ItemMoveService.switchItemsInStorages -> sendItemUnlockPacket(sourceItem) + sendItemUnlockPacket(replaceItem).
- C# runtime artifact likely involved: GameServerConnection.HandleReplaceItemAsync restriction branch and IsRestrictedToStorage helper.
- Client-visible/state/persistence effect expected: rejected replace attempts for non-storable regular/account warehouse moves should send the Java denial message plus unlock packets without mutating/persisting inventory state.
- Why this is runtime progress: proceed only if source review confirms current C# omits a live denial packet; the fix would change packets emitted by CM_REPLACE_ITEM from live code.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksBothLikeJava|FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: rejected replace-item storage switches emit the Java denial message and source/replace unlock packets without mutating item locations or calling persistence.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared restriction helpers or packet primitives are changed.

## Other Safe Runtime Candidates

- Inspect account warehouse storage-size semantics in move/split/replace branches only if Java source review confirms a live C# packet mismatch.
- Inspect legion warehouse replace/split history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
- Inspect `CM_SPLIT_ITEM` remaining account warehouse branches only if source review finds live packet or persistence mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `a9be11478 [Phase 6][UOW-2698] Persist replace storage switches`
  - `110f7822c [Phase 6][UOW-2697] Delete split warehouse source`
  - `36b2cecf6 [Phase 6][UOW-2696] Split empty-slot storage updates`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
