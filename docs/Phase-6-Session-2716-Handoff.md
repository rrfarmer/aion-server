# Phase 6 Session 2716 Handoff

## Completed UOW

[Phase 6] UOW-2716: Send legion warehouse split withdrawal denial.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_SPLIT_ITEM source LEGION_WAREHOUSE now runs Java's cross-storage restriction denial branch instead of returning before source item checks.
- Java source/runtime path: CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem -> ItemRestrictionService.isItemRestrictedTo / isItemRestrictedFrom -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT -> sendStorageUpdatePacket.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync, CreateLegionWarehouseMoveRestrictionMessage, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect: a legion member lacking WH_WITHDRAWAL receives the authority-denial system message and source legion warehouse update; item state is not mutated.
- Why this is runtime progress: it sends real Java-equivalent server packets from a live client packet handler.
```

## Commit

`[Phase 6][UOW-2716] Send legion warehouse split denial`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2716-Completion.md`
- `docs/Phase-6-Session-2716-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection.SendStorageUpdatePacketAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 2
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

- `CM_SPLIT_ITEM` now has partial runtime parity for source-legion-warehouse missing-withdrawal denial and source update.
- Full legion warehouse split parity is not claimed. Successful storage mutation, persistence, and legion history remain deferred.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_SPLIT_ITEM.runImpl` | `GameServerConnection.HandleSplitItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Source legion warehouse denial/update is wired for failed WH_WITHDRAWAL. |
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Item split service | Partial | Unit Tested | Partial Parity | Restriction branch is represented; successful legion warehouse split and history are deferred. |
| `ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` | Packet fanout | Partial | Unit Tested indirectly | Partial Parity | Source update uses `ITEM_COLLECT` for split restriction failure; exact Java packet bytes were not captured. |

## Known Gaps / Watchouts

- `CM_REPLACE_ITEM` still returns before legion warehouse restriction denial and both-item unlock.
- Successful legion warehouse move/split/replace still needs live legion storage, owner mapping, persistence, packets, and history.
- Destination-legion-warehouse denial paths need focused live tests when selected as runtime scope.
- Java `LegionConfig.LEGION_WAREHOUSE` is not modeled in this C# handler yet.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire `CM_REPLACE_ITEM` legion warehouse restriction denial and both-item unlock before the current deferred return.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: CM_REPLACE_ITEM involving LEGION_WAREHOUSE should run Java's restriction branch and unlock both involved items instead of silently returning.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> ItemRestrictionService.isItemRestrictedFrom / isItemRestrictedTo -> sendItemUnlockPacket(sourceItem) and sendItemUnlockPacket(replaceItem).
- C# runtime artifact likely involved: GameServerConnection.HandleReplaceItemAsync, CreateLegionWarehouseMoveRestrictionMessage, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect expected: client receives Java-equivalent denial plus source and replacement item unlock packets; no item location/slot state mutates.
- Why this is runtime progress: it wires a deferred live client packet branch and sends real server packets from live code.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none if the UOW only wires the denial/unlock branch.

## Other Safe Runtime Candidates

- Add focused destination-legion-warehouse denial coverage for move or split paths, paired with live branch behavior.
- Add Java/C# golden capture for denial/update packets if a narrow Java runtime capture becomes available.
- Scope successful legion warehouse item movement only after storage/persistence/history prerequisites are identified.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `e6aa8a8e3 [Phase 6][UOW-2715] Send legion warehouse move denial`
  - `bac8cb770 [Phase 6][UOW-2714] Send legion warehouse Kinah denial`
  - `255364fdd [Phase 6][UOW-2713] Create missing Kinah move targets`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
