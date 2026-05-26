# Phase 6 Bind-Point Teleport Known-List Player Side-Effect Planner

Date: May 26, 2026
Unit of Work: UOW-1262
Scope: Model Java `PlayerController.see(Player)` and `notSee(Player)` packet side effects as non-live descriptors.
Source of truth: Java project.

## Summary

UOW-1262 adds `PlayerKnownListPlayerSideEffectPlanService`, a descriptor-only planner for player-player known-list packet side effects. It does not send packets, mutate known-list membership, wire `GameServerConnection`, or execute live world lifecycle callbacks.

The planner records Java packet order and current C# packet-surface readiness:

- `SM_PLAYER_INFO`
- `SM_MOTION`
- optional ride `SM_EMOTION`
- optional `SM_PLAYER_STANCE`
- optional `SM_ABNORMAL_EFFECT` after creature-specific see packets
- `SM_DELETE` for player `notSee` when the viewing player is still spawned

## Java Source Findings

- `PlayerController.see(VisibleObject)` calls `super.see(object)` first.
- For a seen `Player`, Java delegates to `sendPlayerInfoPackets(player)`.
- `sendPlayerInfoPackets(Player)` sends `SM_PLAYER_INFO`, `SM_MOTION`, optional ride `SM_EMOTION`, and optional `SM_PLAYER_STANCE` in that order.
- The Java `SM_PLAYER_INFO` enemy/aggro flag is `!player.equals(getOwner()) && getOwner().isAggroIconTo(player)`, so self-view suppresses the flag.
- After the creature branch, Java sends `SM_ABNORMAL_EFFECT` when the seen creature has active effects.
- `PlayerController.notSee(VisibleObject,ObjectDeleteAnimation)` calls `super.notSee(...)`, skips deletion packets when the owner/viewer is not spawned, and otherwise sends `SM_DELETE(object, animation)` for player objects.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPlayerSideEffectPlanService.cs`:

- `PlanSee` creates ordered descriptors for Java player see packets.
- `PlanNotSee` creates a delete descriptor or the Java teleport/unspawned skip result.
- Descriptors include Java packet name, optional C# packet type name, support status, object ids, Java source breadcrumbs, and packet-specific metadata.
- `SmPlayerInfo` is marked `Partial` because current C# does not expose Java's enemy/aggro constructor flag.
- `SmPlayerStance` and `SmAbnormalEffect` are marked `Missing`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPlayerSideEffectPlanServiceTests" --nologo` passed 6 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 255 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1262

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.see` | `Aion.GameServer.Services.PlayerKnownListPlayerSideEffectPlanService.PlanSee` | Controller Side-Effect Planner | Partial | Unit Tested | Partial Parity | Models player-player see packet ordering as descriptors only. Does not call `super.see`, send packets, execute NPC/pet/house/gatherable branches, or dispatch live known-list callbacks. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListPlayerSideEffectPlanService.PlanSee` descriptors | Packet Side-Effect Planner | Partial | Unit Tested | Partial Parity | Models `SM_PLAYER_INFO`, `SM_MOTION`, ride `SM_EMOTION`, and stance order. C# `SmPlayerInfo` lacks Java enemy/aggro flag behavior; C# has no `SmPlayerStance`. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee` | `PlayerKnownListPlayerSideEffectPlanService.PlanNotSee` | Controller Side-Effect Planner | Partial | Unit Tested | Partial Parity | Models viewer-unspawned skip and player fallback `SM_DELETE(object, animation)`. Does not execute `super.notSee`, target cleanup, non-player delete branches, or live packet sends. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerInfo` plus side-effect descriptor | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Packet class exists, but Java constructor flag for enemy/aggro icon and viewer-sensitive creature type/race behavior are not represented in the descriptor or serializer. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmMotion` plus side-effect descriptor | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Packet class exists and descriptor records Java ordering. No runtime send or Java packet capture was executed in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` plus ride descriptor | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Packet class exists and ride NPC id is represented. Does not instantiate or compare Java bytes in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | missing C# `SmPlayerStance`; descriptor marks missing support | Packet / Serialization Dependency | Not Started | Unit Tested | Needs Verification | Discovered missing packet. Descriptor preserves Java order and stance state `1`; serializer and live send are blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | missing C# `SmAbnormalEffect`; descriptor marks missing support | Packet / Serialization Dependency | Not Started | Unit Tested | Needs Verification | Discovered missing post-creature see packet. Descriptor records ordering after player-specific packets; effect serialization and live send are blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE` | `Aion.GameServer.Network.Aion.ServerPackets.SmDelete` plus side-effect descriptor | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Packet class exists and descriptor records supplied animation. No runtime send or Java byte comparison was executed. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | player side-effect descriptors plus non-live known-list population stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future known-list fanout can now preserve player `see`/`notSee` packet intent metadata. Live scheduled callbacks, sockets, movement, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlanSee_AlwaysPlansPlayerInfoThenMotion` | Unit / Planner | `PlayerController.sendPlayerInfoPackets` | Baseline player see descriptor order is `SM_PLAYER_INFO`, then `SM_MOTION`; no live send occurs. | Source-derived ordering. | No Java runtime comparison; `SmPlayerInfo` enemy flag still partial. |
| `PlanSee_RideAndStanceAppendAfterMotionInJavaOrder` | Unit / Planner | `PlayerController.sendPlayerInfoPackets` | Ride `SM_EMOTION` and stance `SM_PLAYER_STANCE` append after motion in Java order. | Source-derived ordering. | `SM_PLAYER_STANCE` packet class is missing. |
| `PlanSee_AbnormalEffectsAppendAfterPlayerSpecificPackets` | Unit / Planner | `PlayerController.see` creature effect branch | `SM_ABNORMAL_EFFECT` is represented after player-specific see packets. | Source-derived ordering. | `SM_ABNORMAL_EFFECT` packet class is missing. |
| `PlanSee_SelfSuppressesAggroFlagLikeJavaPlayerEqualsOwnerCheck` | Unit / Planner | Java `!player.equals(getOwner())` guard | Self-view clears the aggro icon descriptor flag even when caller supplies aggro true. | Source-derived condition. | C# `SmPlayerInfo` cannot yet serialize the Java enemy flag. |
| `PlanNotSee_WhenViewerSpawnedPlansDeleteWithAnimation` | Unit / Planner | `PlayerController.notSee` fallback branch | Spawned viewer gets `SM_DELETE` descriptor with supplied animation. | Source-derived branch. | No live packet send or Java byte comparison. |
| `PlanNotSee_WhenViewerNotSpawnedSkipsDeletePacket` | Unit / Planner | Java teleport/unspawned guard | Unspawned viewer produces no delete descriptor. | Source-derived branch. | Does not execute `super.notSee` target cleanup. |

## Remaining Risks

- Planner is non-live and unwired.
- `SmPlayerInfo` lacks Java enemy/aggro constructor behavior.
- `SmPlayerStance` and `SmAbnormalEffect` are missing C# packet classes.
- Java `super.see`, `super.notSee`, target cleanup, NPC/pet/house/gatherable/summon side effects, abnormal-effect serialization, and `notKnow` behavior are not executed.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and serialization behavior remain unverified for live known-list packet dispatch.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 1 descriptor planner service plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 `SmPlayerInfo` enemy/aggro packet gap, 1 `SmPlayerStance` packet, 1 `SmAbnormalEffect` packet, 1 live world known-list callback path, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled composition step that attaches `PlayerKnownListPlayerSideEffectPlanService` descriptors to `PlayerKnownListTwoWayOperationPlanService` see/notSee steps. Keep it descriptor-only and do not wire `GameServerConnection`, live socket sends, world lifecycle mutation, or bind-point scheduled callbacks.

## Update After UOW-1263

`PlayerKnownListOperationSideEffectAttachmentService` now performs that descriptor-only attachment. It maps owner/candidate `see` and `notSee` operation steps to directional player packet side-effect plans while deriving viewer/subject ids from the operation plan.

## Update After UOW-1265

`SmPlayerInfo` now has a focused C# constructor path for Java's `enemy` flag and writes the attackable creature-type byte `0x00` when requested. The player side-effect planner note has been updated, but viewer-sensitive race projection remains unverified and live packet sends remain disabled.

## Update After UOW-1266

`SmPlayerInfo` now has a focused viewer-context input model for Java's active-viewer race projection and neutral-to-all-player override. The known-list side-effect planner remains descriptor-only and does not compute or pass live viewer context yet, so future dispatch work still needs an explicit descriptor-to-packet input bridge before any socket sends.

## Update After UOW-1267

`SmPlayerStance` now exists as a concrete C# packet serializer and the stance descriptor reports available C# support with state `1`. The planner still does not instantiate or send the packet, does not compute live `isUnderStance`, and `SmAbnormalEffect` remains missing.
