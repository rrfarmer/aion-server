# Phase 6AEO Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1309
Latest Commit: included in the UOW-1309 unit commit
Status: `SM_PET` dedicated doping special-function packet shape exists from supplied snapshots; live pet doping runtime remains unported.

## What Changed

- Modified `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`.
- Modified `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`.
- Added `docs/Phase-6-BindPointTeleport-KnownListPetDopingSpecialFunctionPacket.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- `SmPetDopingSpecialFunctionSnapshot`.
- `SmPet.DopingSpecialFunction(...)`.
- `SPECIAL_FUNCTION` serialization for Java doping dope actions:
  - `0` add item;
  - `1` remove item;
  - `2` switch items between occupied slots;
  - `3` use item.
- Unknown dope-action guard.

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|CmPet|CmPetEmote|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 59 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|CmPet|CmPetEmote|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 427 tests.

No Java runtime packet capture was executed. No live `GameServerConnection` pet doping dispatch was enabled.

## Migration Parity Table - UOW-1309

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | `SPECIAL_FUNCTION` now writes the Java doping constructor shape in addition to autoloot/autosell shapes. `FOOD`, `MOOD`, live dispatch, and Java runtime vectors remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(int dopeAction, int itemId, int slot)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.DopingSpecialFunction` | Packet Factory | Complete for supplied-snapshot shape | Unit Tested | Partial Parity | Supports dope actions `0`, `1`, `2`, and `3` with Java field order. Action `2` preserves Java's overloaded `itemObjectId` slot-2 behavior. Unknown dope actions are rejected. |
| `com.aionemu.gameserver.model.gameobjects.PetSpecialFunction` | `Aion.GameServer.Model.GameObjects.PetSpecialFunction` | Enum | Complete for known ids | Existing Unit Tested | Partial Parity | Reuses UOW-1308 enum id `Doping = 2`. No new resolver behavior. |
| `com.aionemu.gameserver.services.toypet.PetService.useDoping` | future C# live pet doping runtime | Service | Not Started | Manual Only | Needs Verification | Packet serializer exists, but live item lookup, bag mutation, cooldown/buff behavior, persistence, and response dispatch are not ported. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `SmPetDopingSpecialFunctionSnapshot` / future live doping bag | Model / Packet DTO | Partial | Unit Tested for packet facts | Needs Verification | Packet snapshot carries supplied dope-action facts only. Java synchronized bag mutation and dirty-state behavior remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_DopingSpecialFunctionWritesJavaDopeActionShapes` | Packet Unit | `SM_PET.writeImpl` `SPECIAL_FUNCTION` subtype `2` | Writes add, remove, switch, and use dope-action payloads in Java field order. | Source-derived byte/field-order assertions. | No live doping bag/item state or Java runtime vector. |
| `SmPet_DopingSpecialFunctionRejectsUnknownDopeAction` | Unit / Guard | Java switch has no unknown-action body | Prevents silently writing an unsupported dope action. | Source-derived guard assertion. | Java would write action byte and no body for unknown if constructed; no in-repo caller found for unknown actions. |
| `SmPet_SpecialFunctionRejectsDopingShapeOnGenericApi` | Unit / Guard | Java separate doping constructor | Ensures generic special-function API still does not serialize doping with the wrong constructor shape. | Source-derived guard assertion. | None for packet API; live runtime remains absent. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- Live `PetService.useDoping` remains unported.
- Packet snapshot field names reflect Java's overloaded `itemObjectId` behavior for action `2`; callers must supply slot 2 in `ItemTemplateIdOrSlot2`.
- Threading differences around future live `PetDopingBag` mutation remain unknown.
- Serialization is covered by source-derived byte tests only, not Java golden output.
- Date/time/precision behavior is not involved in this packet slice.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 packet snapshot/factory branch and 3 focused tests/assertion groups
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live pet doping service, live pet doping bag state, inventory lookup/mutation, cooldown/buff behavior, persistence, Java runtime vectors, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Create a read-only pet runtime dependency map for `CM_PET` and `CM_PET_EMOTE.runImpl`, or audit the remaining `SM_PET.FOOD`/`MOOD` packet branches before introducing any live pet mutation.

## Suggested Parallel Batch For Next Session

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | Pet runtime dependency map | Java read-only/docs | Low | Yes | Recommended to avoid jumping into live mutation without service boundaries. |
| B | `SM_PET.FOOD` packet audit | Java read-only/docs | Low | Yes | Scope feed progress/refeed snapshot facts and branches. |
| C | `SM_PET.MOOD` packet audit | Java read-only/docs | Low | Yes | Needs mood point/cooldown mutation review before code. |
| D | Java pet vector generator retry | docs/tooling read-only | Low | Yes | Useful if Maven/tooling becomes available. |

Recommended next batch: start with read-only A plus either B or C. If implementing next, use one writer only for `SmPet.cs` and `GamePacketTests.cs`.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetSpecialFunction.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPetEmote.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetDopingSpecialFunctionPacket.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
