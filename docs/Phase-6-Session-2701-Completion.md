# Phase 6 Session 2701 Completion

## UOW

[Phase 6] UOW-2701: Unlock restricted move source.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM regular/account storage rejection now sends Java-equivalent source unlock packets after the denial message.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> denial system message, then ItemMoveService.moveItem restriction branch -> ItemPacketService.sendItemUnlockPacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync cross-storage rejection branch.
- Client-visible/state/persistence effect: rejected move attempts for non-storable regular/account warehouse destinations now send the denial packet, restore the source slot with ALL_SLOT storage update and SM_CUBE_UPDATE, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_MOVE_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage move rejects on `ItemRestrictionService.isItemRestrictedTo`, `isItemRestrictedFrom`, trading, or shutdown-soon, then calls `sendItemUnlockPacket` and optionally sends shutdown disable.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Regular warehouse rejection sends `STR_WAREHOUSE_CANT_DEPOSIT_ITEM`; account warehouse rejection sends `STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemUnlockPacket` routes to `sendStorageUpdatePacket(..., ItemAddType.ALL_SLOT)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
  - Account warehouse `cubeSize` writes storage ordinal 2 with zero counts; current C# already maps storage id 2 to zero-size ordinal 2, so no account-size runtime mismatch was implemented in this UOW.

## C# Changes

- Replaced `HandleMoveItemAsync`'s storage-restriction early returns with the Java rejection fanout:
  - optional Java denial message,
  - source `ALL_SLOT` storage update via `SendStorageUpdatePacketAsync`,
  - optional shutdown disable message,
  - return before mutation/persistence.
- Reused `CreateRestrictedToStorageMessage` for move and replace restriction decisions.
- Added a focused regular warehouse move rejection regression using the existing restricted test item template.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksSourceLikeJava` | Unit / live connection handler | `ItemRestrictionService.isItemRestrictedTo`, `ItemMoveService.moveItem`, and `ItemPacketService.sendItemUnlockPacket` source review | A non-storable cube item rejected from a regular warehouse move sends `STR_WAREHOUSE_CANT_DEPOSIT_ITEM`, unlocks the source item with `ALL_SLOT`, sends cube size, and does not mutate or persist item location. | Socket-backed connection fixture invoking the live private handler, runtime-loaded restricted item template, repository mutation counters, in-memory item assertions, and packet byte decoding. | Account warehouse rejection shares the helper but lacks a separate focused regression; legion warehouse/from-restriction remains deferred. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM storage-restriction rejection packet fanout.
- Specific behavior/contract: Java sends the storage denial message, then sendItemUnlockPacket restores the source slot with ALL_SLOT and storage-size packet before returning.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksSourceLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped rejection behavior plus adjacent successful move behavior.
- Why this scope is sufficient: the new regression exercises the branch that previously returned after the denial without Java's source unlock packet.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Regular warehouse restriction now sends Java denial plus source unlock/storage-size before mutation/persistence. Shutdown-soon and from-restriction handling remain partial. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateRestrictedToStorageMessage` | Service helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse denial is asserted through live move handling; account warehouse shares the helper but lacks a separate focused regression. Legion warehouse restrictions remain deferred. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleMoveItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source unlock packet and cube size are asserted for cube source during restriction rejection. Non-cube source variants remain limited. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Account warehouse zero-size ordinal behavior was reviewed as already modeled; this UOW asserts cube source size after unlock. |

## Known Gaps

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Account warehouse restriction shares the helper but was not independently asserted in this UOW.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, and legion warehouse history/permission integration remain incomplete for move handling.
- Account and legion warehouse storage-size variants are not fully covered by live tests.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_MOVE_ITEM` shutdown-soon rejection against Java and wire the Java unlock plus `STR_MSG_DISABLE("Shutdown Progress")` path if current C# differs.
2. Inspect `CM_MOVE_ITEM` account warehouse rejection with soulbound/non-storable items if a separate live regression is needed beyond the shared helper.
3. Inspect legion warehouse replace/split/move history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
