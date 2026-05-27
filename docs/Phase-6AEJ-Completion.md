# Phase 6AEJ Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1304
Latest Commit: included in the UOW-1304 unit commit
Status: `SM_PET` adopt packet support complete from supplied snapshot; live adoption runtime remains unported.

## What Changed

- Modified `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`.
- Modified `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`.
- Added `docs/Phase-6-BindPointTeleport-KnownListPetAdoptPacket.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- Public `SmPetDataSnapshot`.
- Public `SmPetFunctionSnapshot`.
- `SmPet.Adopt(SmPetDataSnapshot)`.
- `PetAction.Adopt` serialization branch.
- `SmPet_AdoptWritesPetDataLikeJava`.

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 23 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 391 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.

No Java runtime packet capture was executed. No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1304

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | `ADOPT` now writes action id plus `writePetData` from supplied snapshot. `LOAD_PETS`, `FOOD`, `MOOD`, `SPECIAL_FUNCTION`, and live dispatch remain unsupported. No Java runtime vector exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(PetCommonData, boolean)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Adopt`; `SmPet(SmPetSurrenderSnapshot)` | Packet Constructor / Factory | Partial | Unit Tested | Needs Verification | C# now covers both `isAdopt=true` and `isAdopt=false` packet shapes through explicit APIs. It does not mirror Java's bool constructor directly and does not hydrate live `PetCommonData`. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetDataSnapshot` | Model Projection / DTO | Partial | Unit Tested | Needs Verification | Snapshot is public because adopt packet construction now consumes it. It still carries supplied packet facts only and does not model Java mutable common-data behavior, timers, DAO state, or thread-safety. |
| `com.aionemu.gameserver.model.templates.pet.PetFunctionType` | `Aion.GameServer.Model.Templates.Pet.PetFunctionType`; `SmPetFunctionSnapshot` | Enum / Packet Function DTO | Partial | Unit Tested | Partial Parity | Adopt test exercises LOOT function plus `NONE` padding and appearance. Other supported function records are covered by UOW-1303 tests, not Java runtime vectors. |
| `com.aionemu.gameserver.services.toypet.PetAdoptionService.adoptPet` | future live adoption runtime | Service / Runtime | Not Started | Manual Only | Needs Verification | Java validates egg item/template/name, decreases inventory, computes expire epoch seconds, inserts pet, sends `SM_PET(commonData,true)`, and registers `ExpireTimerTask`. None of those live side effects were implemented. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_AdoptWritesPetDataLikeJava` | Packet Unit | `SM_PET.writeImpl` `ADOPT` branch | Writes `H ADOPT`, common-data header, LOOT function record, one `NONE` pad, and appearance. | Source-derived byte/field-order assertions. | No Java runtime golden vector or live adoption flow. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- Live adoption runtime and persistence remain unported.
- Public `LOAD_PETS` remains unsupported.
- Snapshot facts are supplied and may diverge from Java live state until a resolver/hydrator exists.
- Date/time behavior for expiration/birthday remains supplied-value only.
- Feed/doping live projections remain partial, and `CM_PET` parser/runtime is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 adopt packet factory/branch plus 1 focused packet test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime vector generation, live adoption runtime, inventory mutation, pet list/DAO insertion, expiration timer registration, `LOAD_PETS`, full `CM_PET`, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add public `SM_PET` `LOAD_PETS` support from a supplied ordered list of `SmPetDataSnapshot` values:

- `H LOAD_PETS(0)`;
- `C 0`;
- `H pets.Count`;
- repeated `writePetData`.

Keep live login/pet-list hydration and socket dispatch out of scope.

## Suggested Parallel Batch For Next Session

Use one writer for `SmPet.cs` / `GamePacketTests.cs`.

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | `SM_PET` `LOAD_PETS` packet from supplied snapshots | `SmPet.cs`, `GamePacketTests.cs`, docs | Medium | Writer only | Main next packet slice; uses already-tested helper. |
| B | `CM_PET` parser metadata DTO/tests | client packet file/test file if isolated | Medium | Yes if separate files | Use UOW-1303 explorer audit; no runtime mutation. |
| C | `PetFeedProgress` bit-pack helper | isolated helper/test or docs | Medium | Yes if separate files | Needed before live feed projection. |
| D | Java pet vector generator planning | docs/read-only | Low | Yes | Useful while Maven remains unavailable. |

Recommended next batch: Candidate A locally, with Candidate B or D only if file ownership is clear.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetAdoptionService.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetAdoptPacket.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetWritePetDataHelper.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
