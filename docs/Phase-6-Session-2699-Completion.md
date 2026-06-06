# Phase 6 Session 2699 Completion

## UOW

[Phase 6] UOW-2699: Reject replace during shutdown.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM rejection now handles GameServer.isShuttingDownSoon with Java-equivalent item unlock packets and STR_MSG_DISABLE("Shutdown Progress").
- Java source/runtime path: ItemMoveService.switchItemsInStorages restriction branch -> sendItemUnlockPacket(sourceItem) + sendItemUnlockPacket(replaceItem) + SM_SYSTEM_MESSAGE.STR_MSG_DISABLE("Shutdown Progress").
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync restriction branch and SmSystemMessage.
- Client-visible/state/persistence effect: replace-item attempts during shutdown now unlock both client-side item slots, send the shutdown disable system message, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_REPLACE_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage replacement rejects on restrictions, trading, or `GameServer.isShuttingDownSoon()`, then unlocks both items and sends `STR_MSG_DISABLE("Shutdown Progress")` only for shutdown.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemUnlockPacket` routes to `sendStorageUpdatePacket(..., ItemAddType.ALL_SLOT)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_MSG_DISABLE(String)` uses message id `1390230`.

## C# Changes

- Added `SmSystemMessage.Disable(string)` for Java `STR_MSG_DISABLE`.
- Wired `_isShuttingDownSoon()` into `HandleReplaceItemAsync` rejection handling.
- Preserved existing source and replace unlock packet fanout with `ALL_SLOT`, then sends the Java shutdown system message.
- Extended the inventory test fixture to inject the connection shutdown delegate.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava` | Unit / live connection handler | `ItemMoveService.switchItemsInStorages` rejection branch and `SM_SYSTEM_MESSAGE.STR_MSG_DISABLE` source review | Shutdown-soon replace attempts do not mutate or persist item locations, unlock both items with `ALL_SLOT` storage update packets, and send message id `1390230` with `"Shutdown Progress"`. | Socket-backed connection fixture invoking the live private handler, injected shutdown delegate, repository mutation counters, in-memory item assertions, and packet byte decoding. | Full Java restriction parity is not complete; `isItemRestrictedFrom` remains limited by legion warehouse runtime gaps. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM rejection branch plus one server system-message factory.
- Specific behavior/contract: Java shutdown-soon rejection unlocks both items, sends STR_MSG_DISABLE("Shutdown Progress"), and stops before mutation/persistence.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch, one message factory, and a test fixture constructor parameter.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped rejection and adjacent success behavior.
- Why this scope is sufficient: the new regression exercises the only live handler branch using the added shutdown message.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Shutdown-soon rejection now unlocks both items and sends the Java disable message. Full restriction coverage remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source and replace unlock packets are asserted for cube and regular warehouse during shutdown rejection. Legion variants remain limited. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_DISABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.Disable` | Server packet factory | Partial | Unit Tested indirectly | Partial Parity | Message id and string parameter are asserted through the live handler regression. Other generated message coverage is outside this UOW. |

## Known Gaps

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom` remains incomplete in live replace handling because C# still defers legion warehouse runtime/history/permission integration.
- Java `isItemRestrictedTo` legion warehouse branches remain incomplete for the same reason.
- Account and legion warehouse storage-size variants are not fully covered.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect account warehouse storage-size semantics in move/split/replace branches and implement only a confirmed live packet mismatch.
2. Inspect legion warehouse replace/split history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
3. Inspect remaining `CM_REPLACE_ITEM` rejection behavior for regular/account warehouse `isItemRestrictedTo` system messages if current C# silently unlocks without Java denial messages.
