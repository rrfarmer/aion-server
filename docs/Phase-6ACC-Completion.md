# Phase 6ACC Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1245
Status: Phase 6 continues; bind-point scheduled Kinah now has a disabled/opt-in owner-checked SQL persistence adapter seam. Live packet sends, `GameServerConnection` dispatch, known-list fanout, and movement remain disabled.

## Session Summary

UOW-1245 added a live-capable but disabled-by-default SQL adapter seam for scheduled bind-point Kinah persistence. The seam consumes `BindPointTeleportKinahPersistenceOperationPlan`, maps affected rows and exceptions through the existing persistence statuses, and stays unwired from live dispatch.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahSqlPersistenceAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/BindPointTeleportKinahPersistenceRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportKinahSqlPersistenceAdapterServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KinahSqlPersistenceAdapter.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACC-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKinahSqlPersistenceAdapterServiceTests" --nologo` passed 7 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 179 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.
- No real DB integration test was run.

## Migration Parity Table - UOW-1245

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.IBindPointTeleportKinahPersistenceRepository`; `Aion.GameServer.Data.EmptyBindPointTeleportKinahPersistenceRepository`; `Aion.GameServer.Data.MySqlBindPointTeleportKinahPersistenceRepository`; `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService` | Repository / Persistence Adapter | Partial | Unit Tested | Needs Verification | Java dirty-row update is broad, batched, later, and ignores affected rows. C# seam is owner-checked, single-row, affected-row aware, disabled by default, and unwired from dispatch. No DB integration or Java runtime comparison yet. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService` consuming `BindPointTeleportKinahPersistenceOperationPlan` | Storage / Persistence Boundary | Partial | Unit Tested | Intentional Difference | Java mutation marks item/storage dirty and persists later. C# adapter consumes a staged mutation/persistence plan and can require rollback on missing/failed persistence. Threading/locking behavior remains the owner service boundary, not this adapter. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahPersistenceOperationPlanService`; SQL adapter seam | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# preserves zero-count Kinah and updates count only; it does not delete exact-price Kinah. Persistence is owner-checked and gated. Precision/rounding is integer/long only in this unit. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService` | Service / Callback Persistence Gate | Partial | Regression Tested | Needs Verification | Adapter can map saved/missing/failed persistence results, but scheduled callback live dispatch remains disabled and no Java 10-second task/runtime comparison was run. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | Future live inventory send adapter; existing bind-point packet intent/send-result planners | Packet Utility / Send Boundary | Partial | Regression Tested | Needs Verification | Discovered dependency for next live gate. This unit does not send packets. Serialization and packet-order parity remain unverified at runtime. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 adapter service, 1 repository interface, 2 repository implementations, and 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live packet send adapter, 1 known-list fanout gate, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 DB integration path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- `MySqlBindPointTeleportKinahPersistenceRepository` is live-capable but not registered or called from `GameServerConnection`.
- No real MySQL integration test has validated parameter binding against the actual `inventory` table.
- Java's broad dirty-row update and C#'s owner-checked single-row update intentionally differ.
- Java ignores affected-row counts, while C# treats non-single rows as rollback-worthy failures.
- Packet send, known-list fanout, cooldown/action fanout, final movement, and scheduled task execution remain disabled.
- Reflection behavior did not change. Serialization, threading, date/time, packet-order, dirty-state persistence, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled/opt-in bind-point Kinah inventory packet send adapter.
- Scope:
  - Consume existing packet intent and send-result policy.
  - Prove send failures block cooldown/action `3` fanout and final movement.
  - Keep adapter separate from SQL execution and movement.
  - Keep `GameServerConnection` dispatch disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Disabled inventory packet send adapter | new send adapter service/test pair | Medium | Best next executable seam. |
| B | Known-list-backed fanout design | docs only | Low | Needed before registry/distance fanout can claim Java parity. |
| C | DB integration test design | docs or gated test skeleton | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION`; do not make default CI require MySQL. |
| D | Movement side-effect readiness audit | docs only | Low/Medium | Helpful before a future live movement adapter. |

### Do Not Parallelize

- Live packet send adapter and live movement adapter in the same unit.
- `GameServerConnection` dispatch with any remaining disabled adapter.
- SQL persistence and movement side effects.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahSqlPersistenceAdapterService.cs`
  - `dotnetConversion/src/Aion.GameServer/Data/BindPointTeleportKinahPersistenceRepository.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahPersistenceOperationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahSendBeforeRuntimeOrderingService.cs`
  - `docs/Phase-6-BindPointTeleport-KinahSqlPersistenceAdapter.md`
- Latest completed commits:
  - `474a7495d [Phase 6][UOW-1244] Add bind point teleport scheduled Kinah live adapter readiness`
  - next commit should be `[Phase 6][UOW-1245] Add bind point teleport Kinah SQL persistence adapter`

Keep live bind-point behavior disabled until live inventory packet sending, Java known-list fanout, final movement packet ordering, and persistent database behavior each have focused parity slices.
