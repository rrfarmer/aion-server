# Phase 6 Session 2721 Handoff

## Completed UOW

[Phase 6] UOW-2721: Deposit kinah into legion warehouse.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_LEGION_WH_KINAH action 1 now executes Java's successful kinah deposit mutation after permission checks.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> inventory.tryDecreaseKinah -> LEGION_WAREHOUSE.increaseKinah -> LegionStorageProxy -> ItemPacketService update fanout.
- C# runtime artifact wired: GameServerConnection.HandleLegionWarehouseKinahAsync / HandleLegionWarehouseKinahDepositAsync plus existing merge persistence and kinah packet fanout.
- Client-visible/state/persistence effect: permitted deposit reduces cube kinah, creates/increases legion-owned location 3 kinah, persists both rows, and sends inventory/warehouse update packets.
- Why this is runtime progress: it mutates live currency state, persists runtime inventory rows, and sends real packets from live code.
```

## Commit

`[Phase 6][UOW-2721] Deposit kinah into legion warehouse`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2721-Completion.md`
- `docs/Phase-6-Session-2721-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahDepositAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 2
- Failed: 0
- Skipped: 0
- Existing warnings only.

Validation notes:

- Initial focused attempt failed because `IDFactory([9001])` marks 9001 used and generates object id `1`; the test expectation was corrected.
- Second focused attempt failed because captured packet serialization saw the live target count after the zero-count add packet step; the runtime now sends a zero-count snapshot for the warehouse-add packet.

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

- `CM_LEGION_WH_KINAH` now has partial runtime parity for permitted action 1 deposits into a modeled legion warehouse kinah row.
- Full legion kinah parity is not claimed. Withdrawal, history persistence, shared legion warehouse storage, direct DB integration, and exact Java bytes remain gaps.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Action 1 deposit is live; action 0 withdrawal remains deferred. |
| `Storage.tryDecreaseKinah` / `Storage.increaseKinah` | `GameServerConnection.HandleLegionWarehouseKinahDepositAsync` | Storage mutation | Partial | Unit Tested | Partial Parity | Positive deposit mutation is represented with rollback; Java history side effect is missing. |
| `LegionStorageProxy` / `LegionWarehouse` | `Player.InventoryItems` location 3 model | Storage proxy / aggregate | Partial | Unit Tested indirectly | Partial Parity | C# uses modeled location 3 rows instead of shared legion storage. |
| `ItemPacketService.ItemUpdateType` | `SmInventoryUpdateItem` / `SmWarehouseUpdateItem` | Packet update masks | Partial | Unit Tested | Partial Parity | Deposit emits `DEC_KINAH_BUY`, zero-count add, and `INC_KINAH_COLLECT`; no exact byte golden. |

## Known Gaps / Watchouts

- Successful action 0 withdrawal remains deferred.
- `LegionService.addHistory(KINAH_DEPOSIT/KINAH_WITHDRAW)` is not implemented.
- Modeled legion warehouse kinah still lives in `Player.InventoryItems` with `Location = 3`; Java uses shared `LegionWarehouse`.
- Legion warehouse expansion count is not loaded into the C# player/legion model.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire successful `CM_LEGION_WH_KINAH` action 0 withdrawal.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: CM_LEGION_WH_KINAH action 0 with WH_WITHDRAWAL should decrease modeled legion warehouse kinah and increase cube kinah instead of returning after permission checks.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> LEGION_WAREHOUSE.tryDecreaseKinah -> inventory.increaseKinah -> LegionStorageProxy -> ItemPacketService update fanout -> LegionService.addHistory(KINAH_WITHDRAW).
- C# runtime artifact likely involved: GameServerConnection.HandleLegionWarehouseKinahAsync, modeled location 3 kinah source row, cube kinah destination row/create path, SaveItemMergeMutationAsync persistence, kinah packet fanout.
- Client-visible/state/persistence effect expected: legion kinah count decreases, cube kinah count increases or is created, persistence updates both rows, and real warehouse/inventory update packets are sent.
- Why this is runtime progress: it fills the remaining successful live packet action, mutates currency state, persists runtime rows, and sends real packets.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is found during discovery. Broad-validation trigger: none if the UOW stays within this live kinah branch.

## Other Safe Runtime Candidates

- Implement live legion warehouse history persistence if the existing legion history schema can be wired directly.
- Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
- Port another small deferred live packet branch from `GameServerConnection` after checking that it mutates state or sends real packets.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `67e44a33e [Phase 6][UOW-2720] Split cube items into legion warehouse`
  - `486884578 [Phase 6][UOW-2719] Replace cube and legion warehouse items`
  - `7eb5f728b [Phase 6][UOW-2718] Move cube items into legion warehouse`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
