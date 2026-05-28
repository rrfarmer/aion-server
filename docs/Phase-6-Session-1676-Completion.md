# Phase 6 Session 1676 Completion - Forced Move Start-Effect Planner

Date: 2026-05-28  
Unit of Work: UOW-1676  
Status: Complete

## Scope

Add a non-live C# planner for the Java `PulledEffect.startEffect` and `OpenAerialEffect.startEffect` ordering, reusing the new `SM_FORCED_MOVE` packet plan boundary from UOW-1675.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/ForcedMoveStartEffectPlanService.cs`.
- Added planner contracts:
  - `ForcedMoveEffectKind`
  - `ForcedMoveStartEffectPlanStatus`
  - `ForcedMoveStartEffectPlanInput`
  - `ForcedMoveStartEffectPlan`
- Modeled Java-side start-effect ordering metadata for:
  - current-skill cancellation source
  - `OpenAerialEffect` paralyze-removal intent
  - player stop-glide and stop-move intents
  - world-position update intent
  - player-only `SM_FORCED_MOVE` packet intent
  - abnormal-state set intents
- Preserved Java reflected-pull packet-source behavior via `originalEffected` packet-source selection.
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMoveStartEffectPlanServiceTests.cs`.

## Validation

Executed:

`dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ForcedMoveStartEffectPlanServiceTests|FullyQualifiedName~ForcedMovePacketPlanServiceTests|FullyQualifiedName~SmForcedMovePacketTests"`

Result:

- 8 tests passed.
- Build succeeded.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.skillengine.effect.PulledEffect.startEffect`
- `com.aionemu.gameserver.skillengine.effect.OpenAerialEffect.startEffect`
- Dependency reuse confirmed against `com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE`

## Migration Parity Table - UOW-1676

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.PulledEffect` | `Aion.GameServer.Services.ForcedMoveStartEffectPlanService` | Effect Boundary | Partial | Unit Tested | Partial Parity | Models non-live `startEffect` ordering for cancel-current-skill, reflected-source packet selection, optional player stop-glide/stop-move intents, world-position update intent, player-only forced-move packet intent, and abnormal-state set intent. No live controller/world/packet integration. |
| `com.aionemu.gameserver.skillengine.effect.OpenAerialEffect` | `Aion.GameServer.Services.ForcedMoveStartEffectPlanService` | Effect Boundary | Partial | Unit Tested | Partial Parity | Models non-live `startEffect` ordering for cancel-current-skill, remove-paralyze intent, optional player stop-glide/stop-move intents, world-position update intent, player-only forced-move packet intent, and abnormal-state set intent. No live controller/world/packet integration. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE` | `Aion.GameServer.Network.Aion.ServerPackets.SmForcedMove`; `ForcedMovePacketPlanService` | Server Packet / Dependency | Complete packet, Partial workflow | Unit Tested | Partial Parity | This unit reuses the packet-plan surface from UOW-1675. Packet bytes remain covered; live runtime dispatch is still pending. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_ForPulledPlayer_UsesEffectorPacketAndStopsMovement` | Non-reflected pulled player ordering and packet payload | Reviewed Java `PulledEffect.startEffect` source | Unit | Intent-only; no live movement/controller execution |
| `CreatePlan_ForReflectedPulledPlayer_UsesOriginalEffectedPacketSourceAndSkipsMotionStops` | Reflected pull packet-source and stop-branch differences | Reviewed Java `PulledEffect.startEffect` source | Unit | No live reflected effect execution |
| `CreatePlan_ForOpenAerialNpc_RemovesParalyzeAndSkipsForcedMovePacket` | Open-aerial NPC ordering and no-player-packet branch | Reviewed Java `OpenAerialEffect.startEffect` source | Unit | Intent-only; no live world/effect execution |
| `CreatePlan_BlocksInvalidEffectedBeforeWorldOrPacketPlanning` | Invalid effected object guard | C# safety boundary | Unit | Guard is C#-specific, not a direct Java branch |

## Risks / Gaps

- No live `PulledEffect` or `OpenAerialEffect` integration was added.
- Planner output is intent-only; `cancelCurrentSkill`, `removeParalyzeEffects`, `onStopGliding`, `onStopMove`, `World.updatePosition`, and abnormal-state mutation are not executed.
- `PacketSendUtility.broadcastPacketAndReceive` recipient behavior remains unverified beyond plan metadata.
- Java runtime/golden vectors for `SM_FORCED_MOVE` are still desirable to strengthen evidence.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ForcedMoveStartEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMoveStartEffectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`