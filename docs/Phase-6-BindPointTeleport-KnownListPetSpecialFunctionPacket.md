# Phase 6 UOW-1308 - `SM_PET` Special-Function Packet Slice

Date: May 27, 2026

## Scope

This unit adds a narrow, source-derived C# packet surface for Java `SM_PET` `SPECIAL_FUNCTION` autoloot/autosell branches from supplied snapshots. It avoids live pet service state and leaves the doping constructor shape unported.

Java source of truth:

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetSpecialFunction.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`

C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetSpecialFunction.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Implemented

- Added `PetSpecialFunction` enum and resolver for Java ids:
  - `DOPING = 2`
  - `AUTOLOOT = 3`
  - `AUTOSELL = 4`
- Added `SmPetSpecialFunctionSnapshot`.
- Added `SmPet.SpecialFunction(...)`.
- Added Java-shaped serialization for:
  - autoloot activation/deactivation: `C 3`, `C 0`, `C active`;
  - autosell activation/deactivation: `C 4`, `C 0`, `C active`;
  - autoloot NPC notification: `C 3`, `C 1|2`, `D npcObjectId`.
- Added tests for all supported shapes and the resolver.

## Explicitly Deferred

- Java doping special-function constructor `SM_PET(int dopeAction, int itemId, int slot)` remains unported and is explicitly rejected by `SmPet.SpecialFunction`.
- Live `PetService.activateLoot`, `activateAutoSell`, looting-NPC runtime calls, pet common-data state, inventory state, and socket dispatch were not added.
- Java `PetSpecialFunction.getById` returns null for unknown ids. C# mirrors this with nullable resolver output rather than an `Unknown` enum value.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|CmPet|CmPetEmote|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 54 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|CmPet|CmPetEmote|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 422 tests.

No Java runtime packet capture was executed. No live `GameServerConnection` pet special-function dispatch was enabled.

## Migration Parity Table - UOW-1308

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | `SPECIAL_FUNCTION` now writes autoloot/autosell activation and autoloot NPC notification shapes from supplied snapshots. Doping, food, mood, live dispatch, and Java runtime vectors remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(PetSpecialFunction, boolean, int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.SpecialFunction` | Packet Factory | Partial | Unit Tested | Partial Parity | Supports Java autoloot/autosell activation and autoloot NPC notification. C# rejects `DOPING` because Java uses a separate constructor with `dopeAction`, template id/slot, and slot-switch semantics. |
| `com.aionemu.gameserver.model.gameobjects.PetSpecialFunction` | `Aion.GameServer.Model.GameObjects.PetSpecialFunction` / `PetSpecialFunctionResolver` | Enum / Resolver | Complete for known ids | Unit Tested | Partial Parity | Java ids are preserved. Unknown ids return null like Java `getById`; no serialization of unknown values is supported. |
| `com.aionemu.gameserver.services.toypet.PetService.activateLoot` | future C# live pet autoloot runtime | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for activation packets and looting-NPC notifications. No pet common-data mutation, NPC loot state, or fanout was added. |
| `com.aionemu.gameserver.services.toypet.PetService.activateAutoSell` | future C# live pet autosell runtime | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for activation packets. No pet common-data mutation, item filtering, inventory sale, or persistence was added. |
| `com.aionemu.gameserver.services.toypet.PetService.useDoping` | future C# live pet doping runtime | Service | Not Started | Manual Only | Needs Verification | Doping response packet branch remains blocked because Java uses the dedicated dope-action constructor and live doping bag/item state. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_SpecialFunctionActivationWritesJavaShape` | Packet Unit | `SM_PET.writeImpl` `SPECIAL_FUNCTION` subtypes `3`/`4` | Writes Java action id, subtype, zero mode byte, and active flag for autoloot/autosell activation/deactivation. | Source-derived byte/field-order assertions. | No live service state or Java runtime vector. |
| `SmPet_SpecialFunctionAutoLootNpcNotificationWritesJavaShape` | Packet Unit | `SM_PET.writeImpl` autoloot `lootNpcObjId > 0` branch | Writes Java action id, autoloot subtype, state byte `1`/`2`, and NPC object id. | Source-derived byte/field-order assertions. | No live NPC loot runtime or Java runtime vector. |
| `SmPet_SpecialFunctionRejectsDopingShapeUntilDedicatedConstructorIsPorted` | Unit / Guard | Java separate `SM_PET(int dopeAction, int itemId, int slot)` constructor | Ensures the generic special-function API does not silently serialize the wrong doping shape. | Source-derived guard assertion. | Doping packet support remains missing. |
| `PetActionAndEmoteResolversPreserveJavaUnknownFallbacks` | Unit | `PetSpecialFunction.getById` | Adds resolver assertions for ids `2`, `3`, and unknown `99`. | Source-derived resolver assertions. | Does not exercise runtime special-function lookup. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- Live pet service mutation and dispatch are absent.
- Doping special-function packets and live doping bag state are unported.
- Autoloot NPC object-id behavior is source-derived but not runtime validated.
- Threading differences around pet common-data state and service mutation remain unknown.
- Serialization is covered by source-derived byte tests only, not Java golden output.
- Date/time/precision behavior is not involved in this packet slice.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 enum/resolver, 1 packet snapshot/factory branch, and 4 focused tests/assertion groups
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: doping response packet shape, live autoloot/autosell/doping services, pet common-data mutation, NPC loot state, inventory item sale/filtering, persistence, Java runtime vectors, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Choose one of two small pet slices:

- port the Java doping special-function packet constructor shape from supplied snapshots, still without live item-state mutation; or
- create a read-only pet runtime dependency map for `CM_PET` and `CM_PET_EMOTE.runImpl` before enabling any live pet mutation.

