# Phase 6 Bind-Point Teleport Known-List Pet writePetData Helper

Date: May 27, 2026
Unit of Work: UOW-1303
Status: Internal deterministic `writePetData` helper complete; public `LOAD_PETS` / `ADOPT` constructors remain disabled.

## Scope

This unit ports Java `SM_PET.writePetData(PetCommonData)` as an internal packet-facing helper backed by deterministic snapshots.

Included:

- `SmPetDataSnapshot`;
- `SmPetFunctionSnapshot`;
- internal `SmPet.WritePetData(PacketBuffer, SmPetDataSnapshot)`;
- source-derived packet tests for no, one, and two writable function cases;
- a guard for more than two packet-writable functions.

Out of scope:

- public `LOAD_PETS` constructor;
- public `ADOPT` constructor;
- live `PetCommonData` hydration;
- live pet template/static-data resolver;
- `CM_PET` parser/runtime;
- Java runtime vector capture.

## Parallel Work

| Agent | Task | Type | Files | Result |
|---|---|---|---|---|
| Orchestrator | Implement internal `writePetData` helper and tests | Implementation | `SmPet.cs`, `GamePacketTests.cs`, docs | Completed. |
| Explorer | Audit Java `CM_PET` parser/runtime excluding already-audited action `15` | Java Analysis | read-only | Completed and closed; output integrated into next-work notes. |

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java` (read-only side audit)

## Behavior Added

`SmPet.WritePetData` writes the Java packet-data sequence:

1. name;
2. template id;
3. pet object id;
4. master object id;
5. two zero placeholders;
6. birthday epoch seconds;
7. seconds until expiration;
8. packet-writable function records in Java order;
9. `NONE` padding when zero or one packet-writable function is present;
10. appearance block.

Function record support:

| Function | C# Input | Java Packet Shape |
|---|---|---|
| `WAREHOUSE` | `SmPetFunctionSnapshot(PetFunctionType.Warehouse)` | `C 0`, `C 0` |
| `LOOT` | `SmPetFunctionSnapshot(PetFunctionType.Loot)` | `C 3`, `C 1`, `C 0` |
| `DOPING` | `DopingItemIds` | `C 2`, `C 32`, then 8 padded `D` item ids |
| `FOOD` | `FeedProgressData`, `RefeedDelaySeconds` | `C 1`, `C 8`, `D feedProgressData`, `D refeedDelaySeconds` |

The helper sorts supplied functions into Java's hard-coded packet order: warehouse, loot, doping, food. It throws if more than two packet-writable functions are supplied, matching the UOW-1302 static-data audit and making future static-data drift explicit.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 22 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 390 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1303

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET.writePetData` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.WritePetData` | Packet Serializer Helper | Partial | Unit + Regression Tested | Partial Parity | Internal helper writes Java source-derived common-data header, function records, `NONE` padding, and appearance block. Public `LOAD_PETS`/`ADOPT` packet constructors remain disabled, and no Java runtime vector comparison exists. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetDataSnapshot` | Model Projection / DTO | Partial | Unit Tested | Needs Verification | Snapshot carries packet-facing values only: name, ids, birthday seconds, seconds-to-expire, functions, and decoration. It does not hydrate live common data, mutate refeed time, compute mood/feed state, or model DAO/threading behavior. |
| `com.aionemu.gameserver.model.templates.pet.PetFunctionType` | `Aion.GameServer.Model.Templates.Pet.PetFunctionType`; `SmPetFunctionSnapshot` | Enum / Packet Function DTO | Partial | Unit Tested | Partial Parity | Helper supports Java packet-writable `WAREHOUSE`, `LOOT`, `DOPING`, and `FOOD` records and ignores XML order by writing Java hard-coded order. `BUFF`, `MERCHANT`, `BAG`, and `WING` remain unsupported in `writePetData` because Java does not write them there. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `SmPetFunctionSnapshot.DopingItemIds` | Fixed Slot Projection | Partial | Unit Tested | Needs Verification | C# pads to exactly 8 packet slots and rejects more than 8 supplied item ids. Live synchronized bag mutation and dirty-state behavior are not ported. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | `SmPetFunctionSnapshot.FeedProgressData` | Bit-Packed Feed Projection | Not Started | Unit Tested as supplied value | Needs Verification | Helper consumes supplied packet-facing feed progress and refeed seconds. Java bit-packing and wall-clock `getRefeedDelay()` mutation are not ported. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` | future `CmPet` parser/metadata | Client Packet / Handler | Not Started | Manual Only | Needs Verification | Read-only explorer mapped ADOPT, SURRENDER, SPAWN, DISMISS, FOOD, RENAME, and MOOD parser/runtime behavior. No C# parser/runtime work was implemented. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_WritePetDataNoWritableFunctionsPadsNoneLikeJava` | Packet Unit | `SM_PET.writePetData` no writable functions path | Writes common-data header, two `NONE` pads, and appearance. | Source-derived byte/field-order assertions. | No Java runtime vector. |
| `SmPet_WritePetDataOneDopingFunctionPadsItemsAndNoneLikeJava` | Packet Unit | `SM_PET.writePetData` DOPING branch | Writes doping function id, byte length 32, two supplied item ids, six zero pads, one `NONE`, and appearance. | Source-derived byte/field-order assertions. | No live `PetDopingBag` projection or Java runtime vector. |
| `SmPet_WritePetDataTwoFunctionsUsesJavaOrderAndNoNonePad` | Packet Unit | `SM_PET.writePetData` hard-coded function order | Supplied FOOD before WAREHOUSE still serializes WAREHOUSE then FOOD, with no `NONE` pad. | Source-derived byte/field-order assertions plus UOW-1302 static-data audit. | No public list/adopt packet constructor yet. |
| `SmPet_WritePetDataRejectsMoreThanTwoWritableFunctions` | Unit / Guard | UOW-1302 static-data audit; Java comment | C# fails explicitly if a future snapshot violates current Java data's two-writable-function assumption. | Deterministic guard assertion. | This is a C# safety guard, not Java runtime behavior. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- Public `LOAD_PETS` and `ADOPT` constructors remain unsupported.
- `SmPetDataSnapshot` consumes supplied seconds and feed data rather than computing Java wall-clock or bit-packed values.
- Java `PetTemplate.getPetFunctions()` mutation side effects are not modeled.
- Live pet/common-data/template/feed/doping hydration and DAO persistence remain unported.
- Full `CM_PET` parser/runtime is still missing and has significant asynchronous feed/mood/doping behavior.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 internal serializer helper, 2 packet-facing snapshots, and 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: public `LOAD_PETS`, public `ADOPT`, Java runtime vector generation, live pet common-data/template hydration, feed bit-packing, doping bag live state, full `CM_PET`, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add public `SM_PET` `ADOPT` support using `SmPetDataSnapshot`, because it is the smallest packet shape that reuses the new helper without list iteration. Keep live adoption runtime and `LOAD_PETS` out of scope.

Alternative: port `CM_PET` parser metadata first, using the explorer audit as source notes, with no runtime mutation.
