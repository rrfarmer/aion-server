# Phase 6 Bind-Point Teleport Known-List Pet Action-Only Packets

Date: May 27, 2026
Unit of Work: UOW-1298
Status: Complete for the selected action-only `SM_PET` packet subset; Java runtime vector comparison remains unavailable.

## Scope

This unit adds the lowest-state remaining Java `SM_PET(PetAction)` packet subset to C#.

Included actions:

- `TALK_WITH_MERCHANT(6)`
- `TALK_WITH_MINDER(7)`
- `H_ADOPT(16)`
- `H_ABANDON(17)`

Java `SM_PET.writeImpl` writes the action id first and has no switch branch for these actions, so their packet body is exactly the two-byte action id. C# now supports this as an allow-listed action-only constructor.

Out of scope:

- `LOAD_PETS`
- `ADOPT`
- `SURRENDER`
- `FOOD`
- `RENAME`
- `MOOD`
- `SPECIAL_FUNCTION`
- `EXTEND_EXPIRATION`
- live pet management dispatch or socket sends

`EXTEND_EXPIRATION` also has no Java write branch, but it was left unsupported in this unit because its call sites and client expectations still need verification before expanding the allow-list.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`

## Behavior Added

- Added `SmPet(PetAction action)` for the selected Java action-only packet shapes.
- Kept the constructor guarded with an explicit allow-list so unsupported Java branches cannot be emitted accidentally.
- Added a table-driven packet test asserting that each selected action writes only `H actionId`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader"` passed 16 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 384 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1298

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Selected action-only `SM_PET(PetAction)` payloads now emit only the Java action id for merchant, minder, house adopt, and house abandon. Java runtime vectors are still missing. `LOAD_PETS`, `ADOPT`, `SURRENDER`, `FOOD`, `RENAME`, `MOOD`, `SPECIAL_FUNCTION`, `EXTEND_EXPIRATION`, and `writePetData` remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.PetAction` | `Aion.GameServer.Model.GameObjects.PetAction` | Enum | Partial | Unit Tested | Partial Parity | Existing C# enum ids are reused for the selected action-only packets. Unknown fallback is represented by a C# switch rather than Java's static map. `EXTEND_EXPIRATION` is intentionally not accepted by `SmPet(PetAction)` yet pending call-site/client-behavior verification. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(PetAction)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet(PetAction)` | Packet Constructor | Partial | Unit Tested | Needs Verification | Constructor is allow-listed to avoid accidentally claiming unsupported branches. Reflection behavior does not apply. Serialization is source-derived; no Java runtime body/canonical payload comparison ran. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_ActionOnlyWritesActionIdOnlyLikeJava` | Packet Unit / Theory | `SM_PET.writeImpl`; `PetAction` ids | `TALK_WITH_MERCHANT`, `TALK_WITH_MINDER`, `H_ADOPT`, and `H_ABANDON` serialize as only `H actionId`. | Source-derived field-order assertion. | No Java runtime golden vector or live dispatch comparison. |

## Remaining Risks

- Java runtime packet vectors still do not exist locally because Maven is unavailable.
- Action-only packet support is intentionally limited to four actions; `EXTEND_EXPIRATION` remains unverified despite having no Java write branch.
- Full Java `SM_PET` management branches remain unported and depend on pet common-data, static pet functions, feed progress, mood timers, doping bags, DAO/scheduler state, and serializer-side mutations.
- No live pet management handler or socket dispatch uses these packets yet.
- Date/time, threading, serialization beyond the selected two-byte bodies, and mutable pet-state behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 focused serializer constructor plus 1 packet theory test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime vector generator, `EXTEND_EXPIRATION` call-site verification, full `SM_PET` action coverage, pet common-data/template/feed/mood/doping snapshots, live dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Port the next low-state `SM_PET` branch:

- `RENAME`: `D petObjectId`, `S petName`; or
- `SURRENDER(PetCommonData)`: `D templateId`, `D objectId`, `D 0`, `D 0` from a supplied common-data snapshot.

Keep `writePetData`, feed, mood, doping, pet list/adopt, and live handler dispatch out of scope until common-data snapshot contracts are designed.
