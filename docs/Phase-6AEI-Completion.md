# Phase 6AEI Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1303
Latest Commit: included in the UOW-1303 unit commit
Status: Internal deterministic `SM_PET.writePetData` helper complete; public `LOAD_PETS` / `ADOPT` constructors remain disabled.

## What Changed

- Modified `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`.
- Modified `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`.
- Added `docs/Phase-6-BindPointTeleport-KnownListPetWritePetDataHelper.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- `SmPetDataSnapshot`
- `SmPetFunctionSnapshot`
- internal `SmPet.WritePetData(PacketBuffer, SmPetDataSnapshot)`
- Java-ordered packet function serialization:
  - warehouse;
  - loot;
  - doping;
  - food.
- Java `NONE` padding for zero/one function cases.
- Java appearance block after pet data.
- Guard for more than two writable functions.

Public `LOAD_PETS` and `ADOPT` packet constructors are still not enabled.

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 22 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 390 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.

No Java runtime packet capture was executed. No live `GameServerConnection` dispatch was enabled.

## Parallel Work Completed

One read-only explorer audited Java `CM_PET` parser/runtime behavior and was closed.

Key side-audit findings:

- No C# `CM_PET` parser/runtime exists yet.
- Recommended future parser slice: table-driven parse tests for `ADOPT`, `SURRENDER`/`SPAWN`/`DISMISS`, `FOOD` sub-shapes, `RENAME`, and `MOOD`, explicitly excluding already-audited `EXTEND_EXPIRATION`.
- Runtime side effects are much broader than parsing: inventory mutation, pet list/DAO, world spawning, feed timers, mood state, refeed tasks, doping bag, and broadcasts.

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

## Suggested Parallel Batch For Next Session

Use one writer for `SmPet.cs` / `GamePacketTests.cs`.

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | `SM_PET` `ADOPT` constructor using `SmPetDataSnapshot` | `SmPet.cs`, `GamePacketTests.cs`, docs | Medium | Writer only | Smallest next server-packet shape using new helper. |
| B | `CM_PET` parser metadata DTO/tests | client packet file/test file if isolated | Medium | Yes if not touching `SmPet.cs` | Use explorer audit; no runtime mutation. |
| C | `PetFeedProgress` bit-pack helper | isolated helper/test or docs | Medium | Yes if separate files | Needed before live feed projection. |
| D | Java pet vector generator planning | docs/read-only | Low | Yes | Useful while Maven remains unavailable. |

Recommended next batch: Candidate A locally, with Candidate B or D as a read-only/isolated side task only if file ownership is clear.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/Templates/Pet/PetFunctionType.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetWritePetDataHelper.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetFunctionStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
