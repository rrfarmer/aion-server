# Phase 6 Session 1677 Completion - Stagger/Stumble Forced-Move Planner

Date: 2026-05-28
Unit of Work: UOW-1677
Status: Complete

## Scope

Extend the existing non-live forced-move start-effect planner to cover Java `StaggerEffect.startEffect` and `StumbleEffect.startEffect`, reusing the `SM_FORCED_MOVE` packet-plan boundary.

## Completed Work

- Extended `dotnetConversion/src/Aion.GameServer/Services/ForcedMoveStartEffectPlanService.cs`.
- Added `ForcedMoveEffectKind.Stagger`.
- Added `ForcedMoveEffectKind.Stumble`.
- Added `ShouldRemoveStunEffects` to `ForcedMoveStartEffectPlan`.
- Modeled Java start-effect ordering metadata for stagger/stumble:
  - current-skill cancellation from effector
  - paralyze-effect removal
  - stumble-only stun-effect removal
  - player stop-glide and stop-move intents
  - world-position update intent
  - player-only `SM_FORCED_MOVE` packet intent
  - matching abnormal-state set intents
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMoveStartEffectPlanServiceTests.cs`.

## Validation

Executed:

`dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ForcedMoveStartEffectPlanServiceTests|FullyQualifiedName~ForcedMovePacketPlanServiceTests|FullyQualifiedName~SmForcedMovePacketTests"`

Result:

- 11 tests passed.
- Build succeeded.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.skillengine.effect.StaggerEffect.startEffect`
- `com.aionemu.gameserver.skillengine.effect.StumbleEffect.startEffect`
- Dependency reuse confirmed against `com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE`
- Dependency intent remains tied to `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacketAndReceive`
- World-mutation intent remains tied to `com.aionemu.gameserver.world.World.updatePosition`

## Migration Parity Table - UOW-1677

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.StaggerEffect` | `Aion.GameServer.Services.ForcedMoveStartEffectPlanService` | Effect Boundary | Partial | Unit Tested | Partial Parity | Models non-live `startEffect` ordering for cancel-current-skill, remove-paralyze intent, optional player stop-glide/stop-move intents, world-position update intent, player-only forced-move packet intent, and `STAGGER` abnormal-state set intent. `calculate`, `applyEffect`, `endEffect`, geo collision, resistance checks, sub-effect typing, and live controller/world/packet integration remain absent. |
| `com.aionemu.gameserver.skillengine.effect.StumbleEffect` | `Aion.GameServer.Services.ForcedMoveStartEffectPlanService` | Effect Boundary | Partial | Unit Tested | Partial Parity | Models non-live `startEffect` ordering for cancel-current-skill, remove-paralyze intent, remove-stun intent, optional player stop-glide/stop-move intents, world-position update intent, player-only forced-move packet intent, and `STUMBLE` abnormal-state set intent. `calculate`, `applyEffect`, `endEffect`, geo collision, resistance checks, sub-effect typing, TODO no-send special cases, and live controller/world/packet integration remain absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE` | `Aion.GameServer.Network.Aion.ServerPackets.SmForcedMove`; `ForcedMovePacketPlanService` | Server Packet / Dependency | Complete packet, Partial workflow | Regression Tested | Partial Parity | Existing packet bytes remain covered and the planner composes the packet-plan service for player branches. No Java runtime frame capture or live dispatch integration was added in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacketAndReceive` | `ForcedMovePacketPlan.ShouldBroadcastAndReceive`; `ForcedMoveStartEffectPlan.ForcedMovePacketPlan` | Utility Boundary | Partial | Unit Tested boundary only | Needs Verification | C# records broadcast-and-receive intent only. Recipient selection, ordering, source inclusion, visibility, encryption, response handling, and threading remain unverified. |
| `com.aionemu.gameserver.world.World.updatePosition` | `ForcedMoveStartEffectPlan.ShouldUpdateWorldPosition`; `UpdatedPosition` metadata | World Mutation Boundary | Partial | Unit Tested boundary only | Needs Verification | C# records intended world position update but performs no live world, known-list, instance, or movement-controller mutation. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_ForStaggerPlayer_RemovesParalyzeAndCreatesForcedMovePacketLikeJava` | Stagger player ordering and packet payload | Reviewed Java `StaggerEffect.startEffect` source | Unit | Intent-only; no live controller/world/packet execution |
| `CreatePlan_ForStumbleNpc_RemovesParalyzeAndStunButSkipsPlayerPacketLikeJava` | Stumble NPC ordering and no-player-packet branch | Reviewed Java `StumbleEffect.startEffect` source | Unit | Intent-only; no live effect/world execution |
| `CreatePlan_ForStaggerPlayerWithInvalidEffector_BlocksBeforeWorldAndAbnormalMutation` | Invalid packet source guard | C# safety boundary | Unit | Guard is C#-specific, not a direct Java branch |

## Risks / Gaps

- No live `StaggerEffect` or `StumbleEffect` integration was added.
- Planner output is intent-only; `cancelCurrentSkill`, `removeParalyzeEffects`, `removeStunEffects`, `onStopGliding`, `onStopMove`, `World.updatePosition`, packet broadcast, and abnormal-state mutation are not executed.
- `calculate`, resistance checks, heading/angle calculation, geo collision, sub-effect classification, `applyEffect`, and `endEffect` are outside this unit.
- `StumbleEffect` Java TODO notes some skills do not send anything; this planner does not model skill-specific no-send exceptions.
- Java runtime/golden vectors for `SM_FORCED_MOVE` are still desirable to strengthen packet evidence.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ForcedMoveStartEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMoveStartEffectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1677-Completion.md`
- `docs/Phase-6-Session-1677-Handoff.md`
