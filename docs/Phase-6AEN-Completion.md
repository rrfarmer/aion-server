# Phase 6AEN Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1308
Latest Commit: included in the UOW-1308 unit commit
Status: `SM_PET.SPECIAL_FUNCTION` autoloot/autosell packet shapes exist from supplied snapshots; live pet service mutation and doping response packets remain unported.

## What Changed

- Added `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetSpecialFunction.cs`.
- Modified `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`.
- Modified `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`.
- Added `docs/Phase-6-BindPointTeleport-KnownListPetSpecialFunctionPacket.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- `PetSpecialFunction` enum and nullable resolver for Java ids.
- `SmPetSpecialFunctionSnapshot`.
- `SmPet.SpecialFunction(...)`.
- `SPECIAL_FUNCTION` serialization for:
  - autoloot activation/deactivation;
  - autosell activation/deactivation;
  - autoloot NPC notification.
- Guard rejecting the unsupported doping special-function shape.

## Validation Completed

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

## Suggested Parallel Batch For Next Session

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | Doping special-function packet shape | `SmPet.cs`, `GamePacketTests.cs`, docs | Medium | Writer only | Keep supplied-snapshot only; avoid live item state. |
| B | Pet runtime dependency map | Java read-only/docs | Low | Yes | Map `CM_PET` and `CM_PET_EMOTE.runImpl` side effects. |
| C | Java pet vector generator retry | docs/tooling read-only | Low | Yes | Useful if Maven/tooling becomes available. |
| D | Food/mood packet audit | Java read-only/docs | Low | Yes | Scope future `SM_PET.FOOD` and `MOOD` snapshots before code. |

Recommended next batch: Candidate A as the only writer, optionally paired with read-only B or D. Do not parallelize multiple writers on `SmPet.cs`, `GamePacketTests.cs`, or shared progress docs.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetSpecialFunction.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetSpecialFunction.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPetEmote.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetSpecialFunctionPacket.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
