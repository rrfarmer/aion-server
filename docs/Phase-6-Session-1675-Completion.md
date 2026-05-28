# Phase 6 Session 1675 Completion - Forced Move Packet Parity

Date: 2026-05-28  
Unit of Work: UOW-1675  
Status: Complete

## Scope

Port the missing Java `SM_FORCED_MOVE` packet surface into C# and add a non-live packet-plan helper for the Java `broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))` branch used by forced-move callers.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmForcedMove.cs`.
- Added `ForcedMoveSnapshot` payload metadata for source object id, target object id, and x/y/z coordinates.
- Added `dotnetConversion/src/Aion.GameServer/Services/ForcedMovePacketPlanService.cs`.
- Added `ForcedMovePacketPlan` and `ForcedMovePacketPlanStatus`.
- Modeled Java `PacketSendUtility.broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))` as a non-live planning boundary with explicit broadcast intent.
- Added focused tests in:
  - `dotnetConversion/tests/Aion.GameServer.Tests/SmForcedMovePacketTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMovePacketPlanServiceTests.cs`
- Updated `docs/PHASE-6-PROGRESS.md` with the new unit, parity table, and next-work recommendation.

## Validation

Executed:

`dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmForcedMovePacketTests|FullyQualifiedName~ForcedMovePacketPlanServiceTests"`

Result:

- 4 tests passed.
- Build succeeded.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE`
- `com.aionemu.gameserver.skillengine.effect.PulledEffect.startEffect`
- `com.aionemu.gameserver.skillengine.effect.OpenAerialEffect.startEffect`
- Neighboring call sites reviewed for packet reuse/context: `StaggerEffect.startEffect`, `StumbleEffect.startEffect`, `CM_MOVE`, `AntiHackService`

## Migration Parity Table - UOW-1675

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE` | `Aion.GameServer.Network.Aion.ServerPackets.SmForcedMove` | Server Packet | Complete | Unit Tested | Verified Parity | Unit test covers opcode `195`, source object id, target object id, literal byte `16`, and x/y/z payload ordering from Java `writeImpl`. C# uses a snapshot record instead of Java's `Creature` convenience constructor overload. |
| `com.aionemu.gameserver.skillengine.effect.PulledEffect.startEffect` packet branch | `Aion.GameServer.Services.ForcedMovePacketPlanService` | Effect Boundary Utility | Partial | Unit Tested | Partial Parity | Planner records only packet broadcast-and-receive intent. Reflection source selection, cancel-skill ordering, stop-glide/stop-move, world update, and abnormal-state mutation remain unmodeled. |
| `com.aionemu.gameserver.skillengine.effect.OpenAerialEffect.startEffect` packet branch | `Aion.GameServer.Services.ForcedMovePacketPlanService` | Effect Boundary Utility | Partial | Unit Tested | Partial Parity | Planner records packet intent only. Java `removeParalyzeEffects`, stop-glide/stop-move, world update, and abnormal-state mutation remain outside this unit. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `SmForcedMove_WritesJavaPayloadShape` | `SM_FORCED_MOVE` payload field order and literal byte `16` | Reviewed Java `writeImpl` source | Unit | No live Java frame capture comparison |
| `CreateBroadcastReceivePlan_CreatesSmForcedMoveAndBroadcastReceiveIntent` | Planner emits `SmForcedMove` and Java broadcast-and-receive intent metadata | Reviewed packet call sites in `PulledEffect`/`OpenAerialEffect` | Unit | Intent-only, no live dispatch |
| `CreateBroadcastReceivePlan_BlocksInvalidSourceBeforePacketCreation` | Invalid source id blocks planner output | C# safety boundary | Unit | Guard is C#-specific, not a Java direct runtime branch |
| `CreateBroadcastReceivePlan_BlocksInvalidTargetBeforePacketCreation` | Invalid target id blocks planner output | C# safety boundary | Unit | Guard is C#-specific, not a Java direct runtime branch |

## Risks / Gaps

- No live effect/controller/world integration was added.
- `PacketSendUtility.broadcastPacketAndReceive` remains intent-only in this slice.
- `PulledEffect` reflected-source behavior still needs explicit planner/runtime modeling.
- No Java runtime/golden capture currently confirms `SM_FORCED_MOVE` beyond reviewed source and C# unit serialization.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmForcedMove.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ForcedMovePacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmForcedMovePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMovePacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`