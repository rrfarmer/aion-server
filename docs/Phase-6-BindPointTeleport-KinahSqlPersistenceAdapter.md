# Phase 6 Bind-Point Teleport Kinah SQL Persistence Adapter

Date: May 26, 2026
Unit of Work: UOW-1245
Scope: Disabled/opt-in SQL persistence adapter seam for scheduled bind-point Kinah.
Source of truth: Java project.

## Summary

UOW-1245 adds a live-capable but disabled-by-default C# SQL seam for the scheduled bind-point Kinah persistence plan. The adapter consumes `BindPointTeleportKinahPersistenceOperationPlan`, calls an opt-in repository only when enabled, and maps affected rows or repository exceptions through the existing `BindPointTeleportKinahPersistenceResult` statuses.

The seam remains unwired from `GameServerConnection`. Live scheduled Kinah teleport execution is still blocked by inventory send, known-list fanout, and movement side-effect parity.

## Java Source Findings

- Java `InventoryDAO.updateItems` persists dirty inventory rows with a broad `UPDATE inventory SET ... WHERE item_unique_id=?`.
- Java binds `item_count` first, owner through `getItemOwnerId`, and `item_unique_id` as the final `WHERE` key.
- Java ignores affected-row counts from `executeBatch()` and commits the update batch if JDBC does not throw.
- Java dirty storage persistence is later than the scheduled Kinah mutation and packet send path.

The C# seam intentionally differs by executing the existing owner-checked SQL shape:

```sql
UPDATE inventory SET item_count = ? WHERE item_unique_id = ? AND item_owner = ?
```

That tighter row contract is kept disabled by default until live dispatch has a complete rollback/send/movement strategy.

## C# Changes

- Added `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService`.
- Added `Aion.GameServer.Data.IBindPointTeleportKinahPersistenceRepository`.
- Added `EmptyBindPointTeleportKinahPersistenceRepository`.
- Added `MySqlBindPointTeleportKinahPersistenceRepository`.
- Added focused unit tests for disabled, no-SQL, saved, missing-row, failed, SQL order, and cancellation-token behavior.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKinahSqlPersistenceAdapterServiceTests" --nologo` passed 7 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 179 tests.
- No Java runtime comparison was executed.
- No real database integration test was executed.

## Migration Parity Table - UOW-1245

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.IBindPointTeleportKinahPersistenceRepository`; `Aion.GameServer.Data.EmptyBindPointTeleportKinahPersistenceRepository`; `Aion.GameServer.Data.MySqlBindPointTeleportKinahPersistenceRepository`; `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService` | Repository / Persistence Adapter | Partial | Unit Tested | Needs Verification | Java dirty-row update is broad, batched, later, and ignores affected rows. C# seam is owner-checked, single-row, affected-row aware, disabled by default, and unwired from dispatch. No DB integration or Java runtime comparison yet. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService` consuming `BindPointTeleportKinahPersistenceOperationPlan` | Storage / Persistence Boundary | Partial | Unit Tested | Intentional Difference | Java mutation marks item/storage dirty and persists later. C# adapter consumes a staged mutation/persistence plan and can require rollback on missing/failed persistence. Threading/locking behavior remains the owner service boundary, not this adapter. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahPersistenceOperationPlanService`; SQL adapter seam | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# preserves zero-count Kinah and updates count only; it does not delete exact-price Kinah. Persistence is owner-checked and gated. Precision/rounding is integer/long only in this unit. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahSqlPersistenceAdapterService` | Service / Callback Persistence Gate | Partial | Regression Tested | Needs Verification | Adapter can map saved/missing/failed persistence results, but scheduled callback live dispatch remains disabled and no Java 10-second task/runtime comparison was run. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | Future live inventory send adapter; existing bind-point packet intent/send-result planners | Packet Utility / Send Boundary | Partial | Regression Tested | Needs Verification | Discovered dependency for next live gate. This unit does not send packets. Serialization and packet-order parity remain unverified at runtime. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_NoSqlOperationReturnsSatisfiedResultWithoutRepositoryCall` | Unit | Java no-fee/no-mutation branches | No-SQL plans produce a satisfied non-live result without calling the repository. | Source-derived only. | No live callback dispatch. |
| `ExecuteAsync_DisabledAdapterDoesNotCallRepository` | Unit | C# live-safety gate | Update-ready SQL plans are not executed when the adapter is disabled. | C# guard. | Java has no disabled equivalent. |
| `ExecuteAsync_EnabledAdapterMapsSingleAffectedRowToSaved` | Unit | Java `InventoryDAO.updateItems` count persistence fields | Enabled adapter executes the owner-checked SQL plan, preserves parameter order, and maps one affected row to `Saved`. | Deterministic SQL/parameter assertions. | SQL differs intentionally from Java broad dirty update. No DB execution. |
| `ExecuteAsync_EnabledAdapterMapsNonSingleAffectedRowsToMissingRow` | Unit | C# owner-checked persistence policy | Zero or multiple affected rows map to `MissingRow` and require rollback. | C# staging guard. | Java ignores affected-row counts. |
| `ExecuteAsync_EnabledAdapterMapsExceptionToFailed` | Unit | JDBC exception failure branch | Repository exceptions map to `Failed` and require rollback. | Source-derived exception handling. | No transaction/rollback DB test. |
| `ExecuteAsync_PropagatesCancellationTokenToRepository` | Unit | C# async boundary | Cancellation token reaches the repository call. | Deterministic token assertion. | Java threading semantics are not comparable here. |

## Remaining Risks

- `MySqlBindPointTeleportKinahPersistenceRepository` is live-capable but not registered or called from `GameServerConnection`.
- No real MySQL integration test has validated parameter binding against the actual `inventory` table.
- Java's broad dirty-row update and C#'s owner-checked single-row update intentionally differ.
- Java ignores affected-row counts, while C# treats non-single rows as rollback-worthy failures.
- Packet send, known-list fanout, cooldown/action fanout, final movement, and scheduled task execution remain disabled.
- Reflection behavior did not change. Serialization, threading, date/time, packet-order, dirty-state persistence, and movement parity remain `Needs Verification`.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 adapter service, 1 repository interface, 2 repository implementations, and 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live packet send adapter, 1 known-list fanout gate, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 DB integration path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled/opt-in bind-point Kinah inventory packet send adapter that can consume the existing packet intent and send-result policy without enabling `GameServerConnection` dispatch. It should prove that send failures block cooldown/action `3` fanout and final movement, and it should remain separate from SQL execution and movement.
