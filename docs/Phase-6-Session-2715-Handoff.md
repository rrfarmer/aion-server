# Phase 6 Session 2715 Handoff

## Completed UOW

[Phase 6] UOW-2715: Send legion warehouse move withdrawal denial.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_MOVE_ITEM source LEGION_WAREHOUSE now runs Java's restriction denial branch instead of returning before permission checks.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemRestrictionService.isItemRestrictedFrom(LEGION_WAREHOUSE) -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT -> sendItemUnlockPacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, CreateLegionWarehouseMoveRestrictionMessage, Player legion permission masks, SmSystemMessage.GuildWarehouseNoRight, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect: a legion member lacking WH_WITHDRAWAL receives the authority-denial system message and source unlock; item state is not mutated.
- Why this is runtime progress: it sends a real Java-equivalent server packet sequence from a live client packet handler.
```

## Commit

`[Phase 6][UOW-2715] Send legion warehouse move denial`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2715-Completion.md`
- `docs/Phase-6-Session-2715-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection.HasLegionWarehouseRight`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
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

- `CM_MOVE_ITEM` now has partial runtime parity for source-legion-warehouse missing-withdrawal denial and source unlock.
- Full legion warehouse item movement parity is not claimed. Successful storage mutation, persistence, and legion history remain deferred.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_MOVE_ITEM.runImpl` | `GameServerConnection.HandleMoveItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Source legion warehouse denial/unlock is wired for failed WH_WITHDRAWAL. |
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Item movement service | Partial | Unit Tested | Partial Parity | Restriction branch is represented; successful legion warehouse movement and history are deferred. |
| `ItemRestrictionService.isItemRestrictedFrom` | `CreateLegionWarehouseMoveRestrictionMessage` | Restriction helper | Partial | Unit Tested indirectly | Partial Parity | Source `LEGION_WAREHOUSE` missing-withdrawal sends `STR_GUILD_WAREHOUSE_NO_RIGHT`; config-disabled behavior remains unmodeled. |

## Known Gaps / Watchouts

- `CM_SPLIT_ITEM` still returns before legion warehouse restriction denial/unlock.
- `CM_REPLACE_ITEM` still returns before legion warehouse restriction denial/unlock.
- Successful legion warehouse item move/split/replace still needs live legion storage, owner mapping, persistence, packets, and history.
- Java `LegionConfig.LEGION_WAREHOUSE` is not modeled in this C# handler yet.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire `CM_SPLIT_ITEM` legion warehouse restriction denial/unlock before the current deferred return.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: CM_SPLIT_ITEM with source or destination LEGION_WAREHOUSE should run Java's cross-storage restriction branch and unlock the source item instead of silently returning.
- Java source/runtime path: CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem -> ItemRestrictionService.isItemRestrictedTo / isItemRestrictedFrom -> sendStorageUpdatePacket.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync, CreateLegionWarehouseMoveRestrictionMessage or a split-specific shared restriction helper, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect expected: client receives Java-equivalent denial and source unlock packets; no split, merge, or persistence mutation occurs.
- Why this is runtime progress: it wires a deferred live client packet path and sends real server packets from live code.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none if the UOW only wires the denial/unlock branch.

## Other Safe Runtime Candidates

- Wire `CM_REPLACE_ITEM` legion warehouse restriction denial and both-item unlock.
- Add Java/C# golden capture for the denial/unlock packet sequence if a narrow Java runtime capture is available.
- Scope successful legion warehouse item movement only after storage/persistence/history prerequisites are identified.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `bac8cb770 [Phase 6][UOW-2714] Send legion warehouse Kinah denial`
  - `255364fdd [Phase 6][UOW-2713] Create missing Kinah move targets`
  - `f65ce73b0 [Phase 6][UOW-2712] Add moved items to regular warehouse`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
