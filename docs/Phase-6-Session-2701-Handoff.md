# Phase 6 Session 2701 Handoff

## Completed UOW

[Phase 6] UOW-2701: Unlock restricted move source.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM regular/account storage rejection now sends Java-equivalent source unlock packets after the denial message.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> denial system message, then ItemMoveService.moveItem restriction branch -> ItemPacketService.sendItemUnlockPacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync cross-storage rejection branch.
- Client-visible/state/persistence effect: rejected move attempts for non-storable regular/account warehouse destinations now send the denial packet, restore the source slot with ALL_SLOT storage update and SM_CUBE_UPDATE, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_MOVE_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2701] Unlock restricted move source`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2701-Completion.md`
- `docs/Phase-6-Session-2701-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateRestrictedToStorageMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksSourceLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize" --logger "console;verbosity=minimal"
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

- The covered regular warehouse `CM_MOVE_ITEM` restriction branch now sends Java's denial message plus source unlock/storage-size packet.
- Full `CM_MOVE_ITEM`, full `ItemMoveService`, and full restriction parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Regular warehouse restriction now sends Java denial plus source unlock/storage-size before mutation/persistence. Shutdown-soon and from-restriction handling remain partial. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateRestrictedToStorageMessage` | Service helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse denial is asserted through live move handling; account warehouse shares the helper but lacks a separate focused regression. Legion warehouse restrictions remain deferred. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleMoveItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source unlock packet and cube size are asserted for cube source during restriction rejection. Non-cube source variants remain limited. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Account warehouse zero-size ordinal behavior was reviewed as already modeled; this UOW asserts cube source size after unlock. |

## Known Gaps / Watchouts

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Account warehouse restriction shares the helper but was not independently asserted in this UOW.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, and legion warehouse history/permission integration remain incomplete for move handling.
- Account and legion warehouse storage-size variants are not fully covered by live tests.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_MOVE_ITEM` shutdown-soon rejection against Java and wire the Java unlock plus `STR_MSG_DISABLE("Shutdown Progress")` path if current C# differs.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM shutdown-soon rejection should unlock the source item and send Java's shutdown disable system message.
- Java source/runtime path: ItemMoveService.moveItem restriction branch -> sendItemUnlockPacket(item) + SM_SYSTEM_MESSAGE.STR_MSG_DISABLE("Shutdown Progress").
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync cross-storage rejection branch and SmSystemMessage.Disable.
- Client-visible/state/persistence effect expected: move attempts during shutdown should restore the source client slot, send storage size, send STR_MSG_DISABLE, and avoid mutation/persistence.
- Why this is runtime progress: proceed only if source review confirms current C# differs; the fix would change packets emitted by live CM_MOVE_ITEM rejection handling.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_ShutdownSoonUnlocksSourceLikeJava|FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseRestrictionSendsDenialAndUnlocksSourceLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if a fix is needed: shutdown-soon move rejection emits source unlock/storage-size and Java disable message without mutating item location or calling persistence.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives are changed.

## Other Safe Runtime Candidates

- Inspect `CM_MOVE_ITEM` account warehouse rejection with soulbound/non-storable items if a separate live regression is needed beyond the shared helper.
- Inspect legion warehouse replace/split/move history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
- Inspect `CM_SPLIT_ITEM` remaining account warehouse branches only if source review finds live packet or persistence mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `d77d6e5c5 [Phase 6][UOW-2700] Send replace restriction denials`
  - `15c9b2a37 [Phase 6][UOW-2699] Reject replace during shutdown`
  - `a9be11478 [Phase 6][UOW-2698] Persist replace storage switches`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
