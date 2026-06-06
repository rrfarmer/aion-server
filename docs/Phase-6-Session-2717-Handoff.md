# Phase 6 Session 2717 Handoff

## Completed UOW

[Phase 6] UOW-2717: Send legion warehouse replace withdrawal denial.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_REPLACE_ITEM involving LEGION_WAREHOUSE now runs Java's restriction denial branch instead of returning before client unlock packets.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> ItemRestrictionService.isItemRestrictedFrom / isItemRestrictedTo -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT -> sendItemUnlockPacket(sourceItem) and sendItemUnlockPacket(replaceItem).
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync, CreateLegionWarehouseMoveRestrictionMessage, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect: a legion member lacking WH_WITHDRAWAL receives the authority-denial system message and both item unlock updates; item state is not mutated.
- Why this is runtime progress: it wires a deferred live client packet branch and sends real Java-equivalent server packets from live code.
```

## Commit

`[Phase 6][UOW-2717] Send legion warehouse replace denial`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2717-Completion.md`
- `docs/Phase-6-Session-2717-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection.SendStorageUpdatePacketAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
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

- `CM_REPLACE_ITEM` now has partial runtime parity for source-legion-warehouse missing-withdrawal denial and both-item unlock.
- Full legion warehouse replace parity is not claimed. Successful storage mutation, persistence, and legion history remain deferred.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_REPLACE_ITEM.runImpl` | `GameServerConnection.HandleReplaceItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Source legion warehouse denial/unlock is wired for failed WH_WITHDRAWAL. |
| `ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Item replace service | Partial | Unit Tested | Partial Parity | Restriction branch is represented; successful legion warehouse replace and history are deferred. |
| `ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` | Packet fanout | Partial | Unit Tested indirectly | Partial Parity | Both source and replacement items are unlocked with `ALL_SLOT`; exact Java packet bytes were not captured. |

## Known Gaps / Watchouts

- Successful legion warehouse move/split/replace still needs live legion storage, owner mapping, persistence, packets, and history.
- Destination-legion-warehouse denial paths need focused live tests when selected as runtime scope.
- Java `LegionConfig.LEGION_WAREHOUSE` is not modeled in this C# handler yet.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: scope the smallest successful legion warehouse item movement prerequisite that mutates real runtime state, likely live legion storage loading/owner mapping or one successful move branch paired with persistence/history.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: a successful legion warehouse item move/split/replace should move beyond denial-only paths and mutate real runtime storage state.
- Java source/runtime path: ItemMoveService.moveItem / splitItem / switchItemsInStorages -> LegionService.addWHItemHistory -> ItemPacketService storage packet fanout.
- C# runtime artifact likely involved: GameServerConnection item movement handlers, player/legion storage model, PlayerEnterWorldRepository persistence, and packet fanout helpers.
- Client-visible/state/persistence effect expected: item location/slot/count changes, relevant storage packets are sent, and runtime state is persisted/restored using existing database shape.
- Why this is runtime progress: it mutates live storage state and advances a deferred successful legion warehouse runtime path.
```

Suggested focused validation starting point depends on the chosen branch. Keep any next UOW narrow and avoid preview/readiness-only scaffolding.

## Other Safe Runtime Candidates

- Add focused destination-legion-warehouse denial coverage for move/split/replace only if paired with same-session live handler behavior.
- Add Java/C# golden capture for denial/update packets if a narrow Java runtime capture becomes available and directly unblocks live behavior.
- Continue replacing silent legion warehouse returns with Java-equivalent live denial/unlock behavior where an untested source/destination branch remains.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `4180c7d5f [Phase 6][UOW-2716] Send legion warehouse split denial`
  - `e6aa8a8e3 [Phase 6][UOW-2715] Send legion warehouse move denial`
  - `bac8cb770 [Phase 6][UOW-2714] Send legion warehouse Kinah denial`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
