# Phase 6 Bind-Point Teleport Known-List Pet Surrender Packet

Date: May 27, 2026
Unit of Work: UOW-1300
Status: Complete for the selected `SM_PET` surrender packet shape; Java runtime vector comparison remains unavailable.

## Scope

This unit ports the Java `SM_PET(PetCommonData, false)` surrender packet shape to C# from a supplied packet-facing snapshot.

Java payload:

1. `H actionId` where action is `SURRENDER(2)`;
2. `D commonData.getTemplateId()`;
3. `D commonData.getObjectId()`;
4. `D 0`;
5. `D 0`.

No live pet surrender handler, pet common-data hydration, persistence, validation, or socket send was added.

The Java `SM_PET(int petId, int petObjectId)` overload remains intentionally unsupported because Java `writeImpl` still reads `commonData` for surrender; serializing that overload appears unsafe unless call-site evidence proves otherwise.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`

## Behavior Added

- Added `SmPetSurrenderSnapshot`.
- Added `SmPet(SmPetSurrenderSnapshot surrender)`.
- Added a `PetAction.Surrender` serializer branch that writes template id, object id, and the two Java zero placeholders.
- Added a packet test for the Java field order and no trailing payload.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 18 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 386 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1300

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Surrender branch now writes Java field order from source review: action id, template id, object id, zero, zero. Java runtime vectors are still missing. `LOAD_PETS`, `ADOPT`, `FOOD`, `MOOD`, `SPECIAL_FUNCTION`, `EXTEND_EXPIRATION`, and `writePetData` remain unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(PetCommonData, boolean)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet(SmPetSurrenderSnapshot)` | Packet Constructor / Snapshot | Partial | Unit Tested | Needs Verification | C# ports only the `isAdopt=false` surrender shape from supplied template/object ids. The `isAdopt=true` adopt path calls Java `writePetData` and remains unsupported. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetSurrenderSnapshot` | Model Projection / DTO | Partial | Unit Tested | Needs Verification | Snapshot includes only `templateId` and `objectId`, the fields needed for surrender. Birthday, expiration, feed, mood, doping, functions, timestamps, and timer behavior remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.PetAction` | `Aion.GameServer.Model.GameObjects.PetAction` | Enum | Partial | Unit Tested | Partial Parity | Existing `SURRENDER(2)` id is now consumed by the serializer. Unknown fallback still differs internally: Java static map, C# switch. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(int, int)` | Not ported | Packet Constructor | Not Started | Manual Only | Needs Verification | Java overload sets action and object id but does not populate `commonData`; `writeImpl` reads `commonData` for surrender. Treat as unsafe/unverified until call sites are audited. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_SurrenderWritesCommonDataIdsLikeJava` | Packet Unit | `SM_PET.writeImpl` `SURRENDER` branch | Writes `H SURRENDER`, `D templateId`, `D objectId`, `D 0`, and `D 0` with no trailing payload. | Source-derived field-order assertion. | No Java runtime golden vector, live surrender handler, persistence, or socket comparison. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- The Java `SM_PET(int, int)` constructor remains unaudited for live call-site use and appears unsafe if serialized.
- Surrender handler-side validation, persistence, pet deletion cleanup, and socket dispatch are not ported in this unit.
- Full Java `writePetData` coverage remains unported and blocks `LOAD_PETS` and `ADOPT`.
- Date/time, threading, mutable pet-state behavior, and serializer-side pet common-data mutations remain unverified for the broader Java pet system.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 focused surrender snapshot/constructor/branch plus 1 packet test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime vector generator, unsafe Java `SM_PET(int,int)` call-site audit, live surrender handler/persistence, `writePetData`, full `SM_PET` action coverage, live dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit Java `SM_PET(int petId, int petObjectId)` and `EXTEND_EXPIRATION` call sites before adding more no-state packet shapes, or start the `writePetData` design/snapshot contract needed for `LOAD_PETS` and `ADOPT`.
