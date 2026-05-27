# Phase 6 UOW-1307 - `CM_PET_EMOTE` Parser Metadata

Date: May 27, 2026

## Scope

This unit adds parser-only C# coverage for Java `CM_PET_EMOTE` opcode `21`, adjacent to the UOW-1306 `CM_PET` parser. It deliberately does not port pet movement runtime mutation or visible-player fanout.

Java source of truth:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`
- `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`

C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPetEmote.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmPetEmoteTests.cs`

## Implemented

- Registered C# opcode `21` as `CmPetEmote` for `GameConnectionState.InGame`, matching Java `AionClientPacketFactory`.
- Added `CmPetEmote` parser properties for Java `CM_PET_EMOTE.readImpl`:
  - emote id;
  - resolved `PetEmote`;
  - current position and heading for `MOVE_STOP` / `MOVE_POSITION_UPDATE`;
  - current and target position for `MOVETO`;
  - emotion id and unknown byte for default branches, including unknown emotes.
- Added parser tests for:
  - factory registration and state gating;
  - current-position branches;
  - move-to branch;
  - default emotion-style branches;
  - unknown emote default-branch behavior.

## Explicitly Deferred

- Java `CM_PET_EMOTE.runImpl` is not ported. No active-pet lookup, spawned guard, coordinate validation, world position update, move-controller update, logging, or broadcast predicate was added.
- Java negative-coordinate rejection remains runtime-only and is not represented by the parser.
- Java `MOVE_POSITION_UPDATE` is parsed like `MOVE_STOP` on the client packet, but Java `SM_PET_EMOTE.writeImpl` does not serialize `MOVE_POSITION_UPDATE` as a movement branch. This existing Java asymmetry remains documented and unmodified.
- No live `GameServerConnection` dispatch is enabled.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CmPetEmote|CmPet|PetActionAndEmoteResolvers|SmPet|PetJavaVectorArtifactReader"` passed 47 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|CmPet|CmPetEmote|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 415 tests.

No Java runtime packet capture was executed. No live `GameServerConnection` pet-emote dispatch was enabled.

## Migration Parity Table - UOW-1307

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ClientPackets.CmPetEmote` | Client Packet / Parser | Partial | Unit + Regression Tested | Partial Parity | Parser-only coverage now exists for Java movement/default payload branches. Runtime active-pet lookup, coordinate rejection, `World.updatePosition`, move-controller mutation, broadcast fanout, and logging are not ported. |
| `com.aionemu.gameserver.model.gameobjects.PetEmote` | `Aion.GameServer.Model.GameObjects.PetEmote` / `PetEmoteResolver` | Enum / Resolver | Partial | Unit + Regression Tested | Partial Parity | Existing resolver maps Java ids and returns `Unknown` for unknown ids, matching Java fallback to `UNKNOWN`. This unit consumes it from the client parser. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `Aion.GameServer.Network.Aion.GameClientPacketFactory` | Packet Factory | Partial | Unit Tested | Partial Parity | Opcode `21` now registers as in-game only like Java. Reflection-based Java packet construction differs from C# delegate registration by design. Many Java opcodes remain unregistered in C#. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Server Packet Dependency | Partial | Existing Unit + Regression Tested | Needs Verification | Existing C# serializer writes `MOVE_STOP`, `MOVETO`, and default branches like Java server packet. Java client parser treats `MOVE_POSITION_UPDATE` as movement input, while Java server serializer default-serializes it; keep this asymmetry visible before runtime fanout. |
| `com.aionemu.gameserver.world.World.updatePosition` | future C# pet position update runtime | World Mutation | Not Started | Manual Only | Needs Verification | Discovered dependency for `MOVE_STOP`, `MOVE_POSITION_UPDATE`, and `MOVETO`. No pet world position mutation was added. |
| `com.aionemu.gameserver.controllers.movement.PetMoveController.setNewDirection` | future C# pet move-controller runtime | Movement Controller | Not Started | Manual Only | Needs Verification | Discovered dependency for `MOVETO` target coordinates. No C# pet move controller exists in this parser unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | future C# pet visible-player fanout | Broadcast Utility Boundary | Not Started | Manual Only | Needs Verification | Java filters visible players by pet known-list and excludes the master except for `EMOTION`. No runtime fanout was added. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryCreatePacket_RegistersJavaFunctionalPetMoveOpcodeAsInGameOnly` | Unit | `AionClientPacketFactory` opcode `21` row | C# factory creates `CmPetEmote` only for in-game state. | Source-derived state/opcode assertion. | Does not exercise encrypted socket dispatch. |
| `ReadFrom_CurrentPositionEmotesReadPositionAndHeadingLikeJava` | Unit | `CM_PET_EMOTE.readImpl` `MOVE_STOP` / `MOVE_POSITION_UPDATE` branches | Reads current x/y/z floats and heading byte. | Source-derived field assertions. | Runtime negative-coordinate rejection and world update missing. |
| `ReadFrom_MoveToReadsCurrentAndTargetPositionLikeJava` | Unit | `CM_PET_EMOTE.readImpl` `MOVETO` branch | Reads current x/y/z, heading, and target x/y/z. | Source-derived field assertions. | Runtime move-controller mutation missing. |
| `ReadFrom_DefaultEmotesReadEmotionAndUnknownLikeJava` | Unit | `CM_PET_EMOTE.readImpl` default branch | Reads emotion id and unknown byte for default emotes. | Source-derived field assertions. | Runtime broadcast-with-master behavior missing for `EMOTION`. |
| `ReadFrom_UnknownEmoteStillReadsDefaultBranchLikeJava` | Unit | Java `PetEmote.UNKNOWN` fallback and default branch | Unknown emote id maps to `Unknown` and still reads emotion/unknown bytes. | Source-derived field assertions. | Java warning/log path is runtime-only and unported. |

## Remaining Risks

- Runtime `CM_PET_EMOTE.runImpl` behavior remains unported.
- Java `MOVE_POSITION_UPDATE` has a known client/server asymmetry: input reads position, server output does not write movement fields for that enum.
- Negative-coordinate rejection, pet spawned checks, logging, world mutation, move-controller mutation, known-list predicates, and master-inclusion rules are missing.
- Threading differences remain unknown for future world/pet movement state mutation. This unit is parser-only.
- Serialization concerns remain on the existing `SM_PET_EMOTE` serializer; this unit does not add new server-packet vectors.
- No Java runtime packet vectors or live client dispatch tests were produced.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 parser class plus 1 factory registration and 5 focused parser tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: live pet movement runtime, active-pet lookup, world position mutation, pet move controller, visible-player fanout, Java runtime vectors, socket dispatch, and `MOVE_POSITION_UPDATE` runtime semantics
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit and port the lowest-risk `SM_PET.SPECIAL_FUNCTION` server-packet subtypes for autoloot/autosell activation from supplied snapshots, or draft the pet runtime dependency map for `CM_PET`/`CM_PET_EMOTE` before enabling live mutation.

