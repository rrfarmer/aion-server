# Phase 6 Bind-Point Teleport Known-List Pet Emote Movement Branches

Date: May 27, 2026
Unit of Work: UOW-1296
Status: Complete for source-derived C# `SM_PET_EMOTE` movement serializer branches; Java runtime vectors still missing.

## Scope

This unit ports the two Java `SM_PET_EMOTE.writeImpl` movement branches that were intentionally blocked in the first known-list serializer slice:

- `PetEmote.MOVE_STOP`
- `PetEmote.MOVETO`

It remains packet-only:

- no `CM_PET_EMOTE` parser/runtime path was ported;
- no Java `World.updatePosition` or `CreatureMoveController.setNewDirection` behavior was ported;
- no socket broadcast or known-list dispatch was enabled.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`

## Behavior Added

`SmPetEmoteSnapshot` now carries optional movement fields:

- current `X/Y/Z`;
- `Heading`;
- target `TargetX/TargetY/TargetZ`.

`SmPetEmote.WritePayload` now mirrors Java branch layout:

| Emote | Java Payload After Pet Id + Emote Id | C# Status |
|---|---|---|
| `MOVE_STOP(0)` | current X/Y/Z, heading | Ported and source-derived unit tested. |
| `MOVETO(12)` | current X/Y/Z, heading, target X/Y/Z | Ported and source-derived unit tested. |
| default branch including `FLY_START(129)` | emotion id, param1 | Existing behavior preserved. |

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PlayerKnownListPetVisibility"` passed 18 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PlayerVisualStatsUpdate"` passed 378 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1296

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Default, `MOVE_STOP`, and `MOVETO` payload branches are now ported from Java source order. No Java runtime golden vectors exist, and broader broadcast/runtime path is not ported. |
| `com.aionemu.gameserver.model.gameobjects.PetEmote` | `Aion.GameServer.Model.GameObjects.PetEmote` | Enum | Partial | Unit Tested | Partial Parity | Movement ids were already ported and are now consumed by serializer branches. Unknown fallback still uses C# switch rather than Java static map. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET_EMOTE` | `SmPetEmoteSnapshot` movement fields | Client Packet / Runtime Source | Not Started | Manual Only | Needs Verification | Java source reviewed to identify current-position and target-position side effects. C# does not parse client pet emotes or update live pet position/move-controller state. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `SmPetEmoteSnapshot` movement fields | Model / Snapshot Source | Partial | Unit Tested | Needs Verification | Packet-facing current position, heading, and target coordinates are represented as supplied snapshot values. Live `Pet` and move-controller hydration remain missing. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPetEmote_MoveStopWritesCurrentPositionLikeJava` | Packet Unit | `SM_PET_EMOTE.writeImpl` `MOVE_STOP` branch | Writes pet id, emote id, current XYZ, and heading. | Source-derived field-order assertions. | No Java runtime golden vector. |
| `SmPetEmote_MoveToWritesCurrentAndTargetPositionLikeJava` | Packet Unit | `SM_PET_EMOTE.writeImpl` `MOVETO` branch | Writes pet id, emote id, current XYZ, heading, and target XYZ. | Source-derived field-order assertions. | No Java runtime golden vector or move-controller adapter. |

## Remaining Risks

- Java runtime packet captures were not generated.
- C# movement values are supplied snapshot data, not live `Pet` / `CreatureMoveController` state.
- Java `CM_PET_EMOTE` negative-coordinate guard, unknown-emote warning, position update, move-controller direction update, and sighted-player broadcast remain unported.
- `MOVE_POSITION_UPDATE(8)` follows Java server-packet default branch, but C# does not yet model the client parser/runtime warning path.
- Live socket dispatch and known-list mutation remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 serializer branch expansion plus 2 focused packet tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime packet capture, live pet/move-controller hydration, `CM_PET_EMOTE` parser/runtime path, and live sighted-player broadcast dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Build the Java pet packet golden-vector harness if Java tooling/static-data fixture setup is available, or continue with a read-only full `SM_PET` action audit before adding any more toy-pet packet branches.

