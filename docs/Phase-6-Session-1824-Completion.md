# Phase 6 Session 1824 Completion - Add Disabled Craft Inventory Packet Send Adapter

Date: 2026-05-31
Unit of Work: UOW-1824
Status: Complete

## Scope

Add a disabled adapter that consumes craft-start inventory packet intent and records the Java send boundary without dispatching live packets.

## Completed Work

- Added `CraftStartInventoryPacketSendAdapterPlanService.CreateDisabledPlan(...)`.
- Added `CraftStartInventoryPacketSendAdapterPlan`.
- Added `CraftStartInventoryPacketSendOperation`.
- Added `CraftStartInventoryPacketSendAdapterStatus`.
- Preserved packet order from `CraftStartInventoryPacketPlan`.
- Recorded the Java boundary as `ItemPacketService -> PacketSendUtility.sendPacket`.
- Added disabled send flags:
  - `WouldCallSendPacketAsync`
  - `DidCallSendPacketAsync`
  - `WouldSendPacketCount`
  - `SentPacketCount`
- Added guard behavior for missing or not-ready packet plans.
- Kept packet dispatch disabled and non-live.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 320 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4572 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.model.items.storage.Storage.delete`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## Migration Parity Table - UOW-1824

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `Storage.decreaseItemCount` update packet send boundary | `CraftStartInventoryPacketSendAdapterPlanService.CreateDisabledPlan` | Packet Adapter | Partial | Unit Tested | Partial Parity | C# consumes planned `SmInventoryUpdateItem` intent and records the Java send boundary; no live send occurs. |
| `Storage.delete` delete packet send boundary | `CraftStartInventoryPacketSendOperation` | Packet Adapter | Partial | Unit Tested | Partial Parity | C# records `SmDeleteItem` and `SmCubeUpdate` operations in existing plan order; storage removal, quest callbacks, and live dispatch remain pending. |
| `PacketSendUtility.sendPacket` craft inventory packet dispatch | `CraftStartInventoryPacketSendAdapterPlan.WouldCallSendPacketAsync` / `DidCallSendPacketAsync` | Packet Adapter | Partial | Unit Tested | Partial Parity | C# records would-send and did-send flags; disabled adapter never calls live dispatch. |

## Risks / Gaps

- The packet send adapter is disabled and does not send packets.
- No live connection registry integration is wired for craft-start inventory packet dispatch.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No item persistence is written to the database.
- No DP spend, live `CraftingTask` creation, scheduler startup, or craft completion is wired.
- Java storage delete quest callbacks/logging are not executed.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Begin a live-disabled craft inventory persistence adapter around the SQL descriptors so future live DB execution can be gated behind a disabled-by-default adapter with transaction/result boundaries.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - integrate the disabled packet send adapter into `CraftStartLiveExecutorFacadePlanService`
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1824-Completion.md`
- `docs/Phase-6-Session-1824-Handoff.md`
