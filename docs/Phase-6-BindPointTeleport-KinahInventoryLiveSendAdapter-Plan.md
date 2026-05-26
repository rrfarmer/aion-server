# Phase 6 Bind-Point Teleport Kinah Inventory Live Send Adapter Plan

Date: May 26, 2026
Unit of Work: UOW-1246
Scope: Disabled/opt-in inventory packet send adapter seam for scheduled bind-point Kinah.
Source of truth: Java project.

## Summary

UOW-1246 adds an opt-in C# send adapter for the scheduled bind-point Kinah inventory update packet. The adapter consumes the existing `BindPointTeleportKinahInventoryUpdatePacketPlan`, calls `IGameClientConnectionRegistry.SendPacketToPlayerAsync` only when explicitly enabled, and returns the existing `BindPointTeleportKinahInventorySendResult` shape.

Default behavior remains disabled and non-sending. The adapter is not wired into `GameServerConnection` dispatch.

## Java Source Findings

- Java scheduled callback re-checks and mutates Kinah through `player.getInventory().tryDecreaseKinah(price, DEC_KINAH_FLY)`.
- `Storage.decreaseItemCount` mutates the Kinah item, keeps zero-count Kinah instead of deleting it, sends the update packet, then marks storage dirty.
- Cube Kinah sends `SM_INVENTORY_UPDATE_ITEM(player, item, DEC_KINAH_FLY)`.
- `DEC_KINAH_FLY` uses mask `0x4B`.
- This inventory update send happens before `addCooldown`, `SM_BIND_POINT_TELEPORT` action `3` fanout, and final teleport scheduling.

## C# Changes

- Extended `BindPointTeleportKinahInventorySendAdapterStatus` with `MissingConnection`, `Sent`, and `Failed`.
- Added `BindPointTeleportKinahInventorySendAdapterService`.
- Preserved `CreateDisabledPlan(...)` as the default no-send path.
- Added tests for disabled, no-intent, missing-registry, registry false, registry true, registry exception, and cancellation behavior.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKinahInventorySendAdapterPlanServiceTests" --nologo` passed 10 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 186 tests.
- No Java runtime packet capture was executed.
- No `GameServerConnection` dispatch path was enabled.

## Migration Parity Table - UOW-1246

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.BindPointTeleportKinahInventorySendAdapterService`; `BindPointTeleportKinahInventorySendAdapterPlanService` | Packet Utility / Send Adapter | Partial | Unit Tested | Needs Verification | Opt-in adapter can call registry send for prepared `SmInventoryUpdateItem`; default remains disabled. No Java packet capture or live client comparison. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; `BindPointTeleportKinahInventorySendAdapterService` | Storage / Count Mutation Send Boundary | Partial | Unit Tested | Intentional Difference | Java sends during mutation and then marks storage dirty. C# uses staged packet intent plus explicit opt-in send result after persistence policy. Exact zero-count Kinah update remains packet intent, not delete. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | owner mutation/persistence/send adapter chain | Storage / Kinah Mutation | Partial | Unit Tested | Needs Verification | C# chain can now produce opt-in send results, but live owner mutation plus dispatch are still not wired. Threading/locking remains in owner service, not this adapter. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `BindPointTeleportKinahInventorySendAdapterService`; `BindPointTeleportKinahInventorySendResultPlanService`; `BindPointTeleportKinahSendBeforeRuntimeOrderingService` | Service / Callback Send Gate | Partial | Regression Tested | Needs Verification | Send result now can come from an opt-in adapter and still gates cooldown/action `3` fanout. Scheduled callback live dispatch and final movement remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via packet intent | Packet / Serialization | Partial | Regression Tested | Needs Verification | Existing packet intent uses `DecreaseKinahFly`/mask `0x4B`; this unit did not add golden-byte Java comparison. Serialization remains source-derived only. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_DisabledAdapterDoesNotCallRegistry` | Unit | C# live-safety gate | Packet-ready plans remain no-send by default. | C# guard. | Java has no disabled equivalent. |
| `ExecuteAsync_EnabledWithoutPacketIntentDoesNotCallRegistry` | Unit | Java send only happens after successful mutation/persistence path | Missing packet intent does not call the registry. | Source-derived staging guard. | No live callback dispatch. |
| `ExecuteAsync_EnabledWithoutRegistryReturnsMissingConnection` | Unit | C# online-player boundary | Enabled adapter without registry maps to `MissingConnection`. | C# runtime boundary. | Java `PacketSendUtility` assumes player connection context. |
| `ExecuteAsync_EnabledMapsRegistryFalseToMissingConnection` | Unit | C# registry send boundary | Registry false maps to `MissingConnection` and send-result gate blocks fanout. | Deterministic registry result. | Java runtime send failure semantics not captured. |
| `ExecuteAsync_EnabledMapsRegistryTrueToSentAndUnlocksSendDecision` | Unit | Java packet-send-before-cooldown order | Registry true maps to `Sent` and unlocks the send-result gate. | Source-derived order plus C# gate. | No known-list fanout or movement execution. |
| `ExecuteAsync_EnabledMapsRegistryExceptionToFailed` | Unit | C# send failure policy | Non-cancellation registry exceptions map to `Failed`. | C# staging guard. | Java exception behavior not runtime-compared. |
| `ExecuteAsync_CanceledTokenStopsBeforeRegistryCall` | Unit | C# async boundary | Cancellation propagates and does not call registry. | Deterministic cancellation assertion. | Java threading semantics are not comparable here. |

## Remaining Risks

- Adapter is opt-in but not registered or called from `GameServerConnection`.
- Java runtime packet bytes were not captured for `SM_INVENTORY_UPDATE_ITEM`.
- Live known-list fanout, cooldown storage execution, final teleport scheduling, and movement remain disabled.
- C# staged persistence-before-send policy remains intentionally different from Java dirty storage timing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, known-list fanout, and movement parity remain `Needs Verification`.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 opt-in send adapter service plus 7 focused tests added to the existing adapter test fixture
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 known-list fanout gate, 1 live movement adapter, 1 `GameServerConnection` dispatch path, 1 Java packet capture path, and 1 live scheduled task path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a known-list fanout parity design or characterization slice for bind-point action `3` before any live scheduled callback dispatch. It should distinguish Java self-first known-list broadcast behavior from the current C# registry/distance fanout approximation.

## Update After UOW-1247

The current C# visible-distance fanout approximation is now executable as a characterization test. It confirms source inclusion plus same-world/95m filtering, and it explicitly remains `Needs Verification` against Java known-list behavior.
