# Phase 6 Bind-Point Teleport Known-List Pet Load-Pets Packet

Date: May 27, 2026
Unit of Work: UOW-1305
Status: `SM_PET` load-pets packet shape complete from supplied ordered snapshots; live login pet-list hydration remains unported.

## Scope

This unit adds public C# support for Java `SM_PET(Collection<PetCommonData>)` by exposing `SmPet.LoadPets(IReadOnlyList<SmPetDataSnapshot>)`.

Included:

- `SmPet.LoadPets(...)`;
- `PetAction.LoadPets` branch in `SmPet.WritePayload`;
- source-derived packet test for count prefix and repeated `writePetData`.

Out of scope:

- live player pet-list hydration;
- Java `PetList.loadPets` behavior;
- DAO reads;
- login/enter-world socket dispatch;
- Java runtime vector capture.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetList.java`

## Behavior Added

`SmPet.LoadPets(snapshots)` serializes:

1. `H LOAD_PETS(0)`;
2. `C 0`;
3. `H pets.Count`;
4. repeated Java `writePetData` payloads in supplied order.

The packet is source-derived and intentionally non-live. The supplied order is treated as authoritative because Java iterates the `Collection<PetCommonData>` it receives.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 24 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 392 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1305

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | `LOAD_PETS` now writes action id, zero byte, count, and repeated `writePetData` from supplied snapshots. `FOOD`, `MOOD`, `SPECIAL_FUNCTION`, and live dispatch remain unsupported. No Java runtime vector exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(Collection<PetCommonData>)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.LoadPets` | Packet Factory | Complete for supplied-snapshot shape | Unit Tested | Needs Verification | C# consumes an ordered list of supplied packet snapshots. Java collection ordering depends on caller collection; live C# pet-list ordering and hydration are not implemented. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetDataSnapshot` | Model Projection / DTO | Partial | Unit Tested | Needs Verification | Snapshot values are supplied. No live common-data hydration, mutable feed/refeed calculations, birthday/expiration calculation, or DAO state is ported. |
| `com.aionemu.gameserver.model.gameobjects.player.PetList` | future C# live pet-list hydration | Model / Collection | Not Started | Manual Only | Needs Verification | Java load/list order and `lastUsedPetTemplateId` behavior remain unported. UOW-1303 explorer noted a possible Java bug where `lastUsedPetTemplateId` may be set from object id during load. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_LoadPetsWritesCountAndPetDataListLikeJava` | Packet Unit | `SM_PET.writeImpl` `LOAD_PETS` branch | Writes `H LOAD_PETS`, `C 0`, `H count`, and two ordered `writePetData` entries. | Source-derived byte/field-order assertions. | No Java runtime golden vector or live pet-list hydration. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- Live login/enter-world pet-list hydration is not implemented.
- Java collection ordering is caller-dependent; C# currently preserves supplied order only.
- `PetList.loadPets` and DAO behavior remain unported.
- Feed/doping/timing values remain supplied packet facts.
- `FOOD`, `MOOD`, `SPECIAL_FUNCTION`, full `CM_PET`, and socket dispatch remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 load-pets packet factory/branch plus 1 focused packet test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime vector generation, live pet-list hydration, pet DAO reads, collection-order confirmation, feed/doping/timing projection, `FOOD`, `MOOD`, `SPECIAL_FUNCTION`, full `CM_PET`, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Move to parser metadata before the remaining complex server packet branches:

- add `CmPet` parser DTO/tests for Java `CM_PET` actions `ADOPT`, `SURRENDER`, `SPAWN`, `DISMISS`, `FOOD`, `RENAME`, and `MOOD`;
- keep runtime mutation and `EXTEND_EXPIRATION` out of scope for the first parser slice.

Alternative: audit/port the lowest-risk `SPECIAL_FUNCTION` packet subtypes for autoloot/autosell activation, but avoid doping retries and live item state.
