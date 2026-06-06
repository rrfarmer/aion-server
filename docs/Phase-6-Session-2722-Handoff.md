# Phase 6 Session 2722 Handoff

## Completed UOW

[Phase 6] UOW-2722: Withdraw kinah from legion warehouse.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_LEGION_WH_KINAH action 0 now executes Java's successful kinah withdrawal mutation after permission checks.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> LEGION_WAREHOUSE.tryDecreaseKinah -> inventory.increaseKinah -> LegionStorageProxy -> ItemPacketService update fanout.
- C# runtime artifact wired: GameServerConnection.HandleLegionWarehouseKinahAsync / HandleLegionWarehouseKinahWithdrawalAsync plus existing merge persistence and kinah packet fanout.
- Client-visible/state/persistence effect: permitted withdrawal reduces modeled legion warehouse kinah, increases or creates cube kinah, persists both rows, and sends warehouse/inventory update packets.
- Why this is runtime progress: it fills a deferred live packet action, mutates currency state, persists runtime rows, and sends real packets from live code.
```

## Commit

`[Phase 6][UOW-2722] Withdraw kinah from legion warehouse`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2722-Completion.md`
- `docs/Phase-6-Session-2722-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahWithdrawalAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
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

- `CM_LEGION_WH_KINAH` now has partial runtime parity for permitted action 0 withdrawals and action 1 deposits using modeled location 3 legion warehouse kinah rows.
- Full legion kinah parity is not claimed. History persistence, shared legion warehouse storage, direct DB integration, and exact Java bytes remain gaps.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Action 0 withdrawal and action 1 deposit are live; history remains deferred. |
| `Storage.tryDecreaseKinah` / `Storage.increaseKinah` | `GameServerConnection.HandleLegionWarehouseKinahWithdrawalAsync` | Storage mutation | Partial | Unit Tested | Partial Parity | Positive withdrawal mutation is represented with rollback; Java history side effect is missing. |
| `LegionStorageProxy` / `LegionWarehouse` | `Player.InventoryItems` location 3 model | Storage proxy / aggregate | Partial | Unit Tested indirectly | Partial Parity | C# uses modeled location 3 rows instead of shared legion storage. |
| `ItemPacketService.ItemUpdateType` | `SmWarehouseUpdateItem` / `SmInventoryUpdateItem` | Packet update masks | Partial | Unit Tested | Partial Parity | Withdrawal emits `DEC_KINAH_BUY` and `INC_KINAH_COLLECT`; no exact byte golden. |

## Known Gaps / Watchouts

- `LegionService.addHistory(KINAH_DEPOSIT/KINAH_WITHDRAW)` is not implemented.
- Modeled legion warehouse kinah still lives in `Player.InventoryItems` with `Location = 3`; Java uses shared `LegionWarehouse`.
- `CM_LEGION_HISTORY` cannot yet send live history because C# has no legion-history runtime data source.
- Legion warehouse expansion count is not loaded into the C# player/legion model.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: implement live legion warehouse history persistence for completed kinah deposit/withdrawal paths if the current DB shape can be wired directly.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: successful CM_LEGION_WH_KINAH action 0/1 should persist KINAH_WITHDRAW/KINAH_DEPOSIT history rows instead of leaving the Java LegionService.addHistory side effect absent.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> LegionService.addHistory -> LegionDAO.insertHistory -> Legion.addHistory; CM_LEGION_HISTORY.runImpl -> legion.getHistory(type) -> SM_LEGION_HISTORY.
- C# runtime artifact likely involved: a legion history repository/service, existing inventory mutation handlers, modeled player legion id/name/rank, and SmLegionHistory projection if rows are loaded.
- Client-visible/state/persistence effect expected: successful warehouse kinah mutation writes a durable legion_history row, and represented history can later be sent by live CM_LEGION_HISTORY.
- Why this is runtime progress: it persists runtime legion state from live packet handlers and directly unblocks a live server-packet path already ported at packet level.
```

Suggested discovery:

```powershell
rg -n "legion_history|LegionHistory|addHistory|KINAH_DEPOSIT|KINAH_WITHDRAW|CmLegionHistory|SmLegionHistory" dotnetConversion game-server/src/com/aionemu/gameserver
```

Suggested focused validation will depend on the repository seam found during discovery; it should include successful kinah deposit/withdrawal and, if live history reading is wired in the same UOW, `CM_LEGION_HISTORY` packet output from represented rows.

Java/Maven is not expected unless a narrow Java fixture is found during discovery. Broad-validation trigger: none if the UOW stays within legion history persistence/packet wiring.

## Other Safe Runtime Candidates

- Wire `CM_LEGION_HISTORY` to a runtime legion history data source and send `SmLegionHistory` for represented rows.
- Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
- Port another small deferred live packet branch from `GameServerConnection` after checking that it mutates state or sends real packets.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `73e42ae32 [Phase 6][UOW-2721] Deposit kinah into legion warehouse`
  - `67e44a33e [Phase 6][UOW-2720] Split cube items into legion warehouse`
  - `486884578 [Phase 6][UOW-2719] Replace cube and legion warehouse items`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
