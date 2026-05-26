# Phase 6 Bind-Point Teleport SM_PLAYER_STANCE Packet

Date: May 26, 2026
Unit of Work: UOW-1267
Scope: Add focused C# packet support for Java `SM_PLAYER_STANCE`.
Source of truth: Java project.

## Summary

UOW-1267 adds the C# `SmPlayerStance` serializer for Java's `SM_PLAYER_STANCE` packet and updates player known-list side-effect descriptors to mark stance packet support as available.

This remains a packet prerequisite, not live stance parity. C# does not start or stop stance observers, remove effects, broadcast stance packets, or execute live known-list player-see sends in this unit.

## Java Source Findings

- Java `SM_PLAYER_STANCE.writeImpl` writes `player.getObjectId()` as `D`.
- Java then writes `state` as `C`.
- Java caller `PlayerController.sendPlayerInfoPackets` sends `new SM_PLAYER_STANCE(player, 1)` after `SM_PLAYER_INFO`, `SM_MOTION`, and optional ride `SM_EMOTION` when the visible player is under stance.
- Java `PlayerController.startStance` broadcasts `new SM_PLAYER_STANCE(getOwner(), 1)`.
- Java `PlayerController.stopStance` broadcasts `new SM_PLAYER_STANCE(getOwner(), 0)` after removing the stance observer/effect.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerStance.cs`:

- `PacketOpCode = 31`, matching Java `ServerPacketsOpcodes`.
- Constructor overloads accept a `Player` or explicit player object id plus state.
- Payload writes object id followed by state.

Updated `PlayerKnownListPlayerSideEffectPlanService`:

- stance descriptors now point to `SmPlayerStance`;
- support status changed from `Missing` to `Available`;
- descriptor remains non-live and does not instantiate/send packets.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "SmPlayerStance|PlayerKnownListPlayerSideEffectPlanServiceTests" --nologo` passed 8 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance" --nologo` passed 268 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1267

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerStance` | Packet / Serialization | Complete | Unit Tested | Partial Parity | C# writes object id then state and uses opcode 31. No Java runtime golden-byte capture was executed, so parity is source-derived but not verified parity. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListPlayerSideEffectPlanService` stance descriptor | Controller Packet Intent / Packet Dependency | Partial | Unit Tested | Partial Parity | Descriptor support now points to concrete `SmPlayerStance` and preserves Java state `1` ordering. It still does not instantiate/send packets or compute live `isUnderStance`. |
| `com.aionemu.gameserver.controllers.PlayerController.startStance` | `SmPlayerStance(player, 1)` packet prerequisite only | Controller / Broadcast Dependency | Partial | Unit Tested | Needs Verification | Packet can represent the broadcast payload, but stance observer registration, effect handling, and live broadcast are not ported in this unit. |
| `com.aionemu.gameserver.controllers.PlayerController.stopStance` | `SmPlayerStance(player, 0)` packet prerequisite only | Controller / Broadcast Dependency | Partial | Unit Tested | Needs Verification | Packet can represent the broadcast payload, but stance observer removal, effect removal, and live broadcast are not ported in this unit. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | player known-list descriptor stack with `SmPlayerStance` packet support | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future known-list fanout has one fewer packet serializer blocker. Live scheduled callbacks, sockets, movement, cooldown, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPlayerStance_WritesObjectIdAndStateLikeJava` | Unit / Packet | `SM_PLAYER_STANCE.writeImpl` | State `1` and state `0` payloads write object id followed by one-byte state. | Source-derived byte assertion. | No Java runtime packet capture or live broadcast comparison. |

Tests updated:

| Test Name | Type | What Changed | Gaps |
|---|---|---|---|
| `PlanSee_RideAndStanceAppendAfterMotionInJavaOrder` | Unit / Planner | Stance descriptor now expects `SmPlayerStance` and `Available` C# support. | Descriptor is still non-live and does not send packets. |

## Remaining Risks

- `SmPlayerStance` has no Java runtime golden-byte validation.
- Live `PlayerController.startStance` / `stopStance` observer registration, effect removal, and broadcast ordering are not ported.
- Known-list `sendPlayerInfoPackets` remains descriptor-only.
- `SmAbnormalEffect` remains missing.
- `SmPlayerInfo` still needs live active-player context computation before real player-see sends.
- Threading, reflection, date/time, precision/rounding, and broader live packet ordering remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 focused packet serializer plus 1 focused packet test theory and 1 descriptor support update
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live stance observer/broadcast path, 1 live controller side-effect dispatcher, 1 `SmAbnormalEffect` packet, 1 active-player context computation path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a focused `SmAbnormalEffect` packet serializer audit or add a non-live player-info/stance descriptor-to-packet input bridge. Prefer an audit first if the effect model is broad; do not wire live known-list sends until abnormal-effect packet readiness and Java runtime validation strategy are clearer.
