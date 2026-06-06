# Phase 6 Session 2702 Handoff

## Completed UOW

[Phase 6] UOW-2702: Send split restriction denials.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cross-storage restriction rejection now sends Java-equivalent regular/account warehouse denial messages before unlocking the source item.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> denial system message, then ItemSplitService.splitItem restriction branch -> ItemPacketService.sendStorageUpdatePacket(sourceStorage, sourceItem).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync cross-storage restriction branch.
- Client-visible/state/persistence effect: rejected split attempts for non-storable regular/account warehouse destinations now send the denial packet, restore the source slot with storage update and SM_CUBE_UPDATE, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_SPLIT_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2702] Send split restriction denials`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2702-Completion.md`
- `docs/Phase-6-Session-2702-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateRestrictedToStorageMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava|FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize" --logger "console;verbosity=minimal"
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

- The covered regular/account destination `CM_SPLIT_ITEM` restriction branches now send Java's denial message before source unlock/storage-size packets.
- Full `CM_SPLIT_ITEM`, full `ItemSplitService`, and full restriction parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Regular/account cross-storage restriction now sends Java denial plus source unlock/storage-size before mutation/persistence. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateRestrictedToStorageMessage` | Service helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Regular and account destination denials are asserted through live split handling. Legion warehouse restrictions remain deferred. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source unlock packet and source storage size are asserted for cube and regular warehouse sources. Other storage families remain limited. |

## Known Gaps / Watchouts

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, destination full checks, and legion warehouse history remain incomplete.
- Account warehouse successful split/move/replace mutation branches are not fully covered by live tests.
- Legion warehouse storage-size/history variants remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_MOVE_ITEM` shutdown-soon rejection against Java and proceed only if source review finds a remaining live mismatch after UOW-2701.

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

- Inspect `CM_MOVE_ITEM` account warehouse rejection with soulbound/non-storable items if a separate live mismatch is confirmed beyond the shared helper.
- Inspect legion warehouse replace/split/move history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
- Inspect `CM_SPLIT_ITEM` destination-full handling only if Java source review finds a live packet or state mismatch.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `cd125ef3b [Phase 6][UOW-2701] Unlock restricted move source`
  - `d77d6e5c5 [Phase 6][UOW-2700] Send replace restriction denials`
  - `15c9b2a37 [Phase 6][UOW-2699] Reject replace during shutdown`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
