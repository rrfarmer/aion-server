# Phase 6 Session 2700 Completion

## UOW

[Phase 6] UOW-2700: Send replace restriction denials.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM regular/account storage rejection now sends Java-equivalent denial messages before unlocking both items.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> STR_WAREHOUSE_CANT_DEPOSIT_ITEM or STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT, then ItemMoveService.switchItemsInStorages -> sendItemUnlockPacket(sourceItem) + sendItemUnlockPacket(replaceItem).
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync restriction branch and restricted-storage helper.
- Client-visible/state/persistence effect: rejected replace attempts for non-storable regular/account warehouse moves now send the Java denial packet, unlock both client-side slots, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_REPLACE_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Regular warehouse rejection sends `STR_WAREHOUSE_CANT_DEPOSIT_ITEM`.
  - Account warehouse rejection sends `STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Restriction checks happen before item mutation; rejected replacement unlocks both items.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemUnlockPacket` routes to `sendStorageUpdatePacket(..., ItemAddType.ALL_SLOT)`.

## C# Changes

- Replaced the boolean replace-storage restriction helper with `CreateRestrictedToStorageMessage`, returning the Java denial message for regular/account warehouse restrictions.
- Updated `HandleReplaceItemAsync` to send the first restriction denial message before the existing source/replace unlock fanout.
- Added a focused non-storable regular warehouse replacement regression and a local fixture item template with warehouse mask bit disabled.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReplaceItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksBothLikeJava` | Unit / live connection handler | `ItemRestrictionService.isItemRestrictedTo`, `ItemMoveService.switchItemsInStorages`, and `ItemPacketService.sendItemUnlockPacket` source review | A non-storable cube item rejected from a regular warehouse replacement sends `STR_WAREHOUSE_CANT_DEPOSIT_ITEM`, unlocks both items with `ALL_SLOT` packets, and does not mutate or persist item locations. | Socket-backed connection fixture invoking the live private handler, runtime-loaded restricted item template, repository mutation counters, in-memory item assertions, and packet byte decoding. | Account warehouse denial is covered by shared helper review but not a separate focused test; legion warehouse restrictions remain deferred. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM regular/account storage restriction rejection packet fanout.
- Specific behavior/contract: Java sends the storage denial message as a side effect of isItemRestrictedTo, then unlocks both items without mutation/persistence.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksBothLikeJava|FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and a test fixture template.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped rejection behavior plus adjacent shutdown rejection.
- Why this scope is sufficient: the regression exercises the branch that previously silently unlocked after a regular warehouse restriction without Java's denial packet.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateRestrictedToStorageMessage` | Service helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse denial is asserted through live replace handling; account warehouse shares the helper but lacks a separate focused regression. Legion warehouse restrictions remain deferred. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Regular warehouse restriction now sends Java denial plus both unlock packets before mutation/persistence. Full restriction coverage remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source and replace unlock packets are asserted for cube and regular warehouse during restriction rejection. Legion variants remain limited. |

## Known Gaps

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- Account warehouse denial is implemented through the shared helper but not independently asserted in this UOW.
- Java `ItemRestrictionService.isItemRestrictedFrom` and legion warehouse `isItemRestrictedTo` remain incomplete in live replace handling because C# still defers legion warehouse runtime/history/permission integration.
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
2. Inspect account warehouse replace rejection with soulbound/non-storable items if a separate runtime regression is needed beyond the shared helper.
3. Inspect legion warehouse replace/split history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
