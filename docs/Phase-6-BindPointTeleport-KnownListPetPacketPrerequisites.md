# Phase 6 Bind-Point Teleport Known-List Pet Packet Prerequisites

Date: May 26, 2026
Unit of Work: UOW-1290
Scope: Add minimal C# packet prerequisites for Java known-list pet visibility.
Source of truth: Java project.

## Summary

UOW-1290 ports the narrow packet subset needed by `PlayerController.see(Pet)` and `PlayerController.notSee(Pet)` without enabling live known-list dispatch.

Implemented C# surfaces:

- `PetAction` ids and Java-style unknown fallback;
- `PetEmote` ids and Java-style unknown fallback;
- `PetFunctionType` packet ids needed by the appearance block;
- `SmPetSpawnSnapshot` for the known-list spawn packet fields;
- `SmPet` spawn and dismiss serializers;
- `SmPetEmoteSnapshot` for default-branch emotes;
- `SmPetEmote` fly-start/default-branch serializer.

Out of scope:

- full `SM_PET` action coverage;
- pet feed, mood, doping, rename, adopt/surrender, special-function, and load-list layouts;
- `SM_PET_EMOTE` movement branches;
- live pet object/common-data hydration;
- live `PlayerController` or socket dispatch integration.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmPet|PetActionAndEmoteResolvers"` passed 5 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PlayerVisualStatsUpdate"` passed 355 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1290

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Known-list `SPAWN` and `DISMISS` payloads are ported from Java source order. Full Java action coverage remains unsupported. No Java runtime golden vector was captured. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Default-branch emote payload supports fly-start packet shape. Java `MOVE_STOP` and `MOVETO` movement branches are explicitly unsupported in this slice. |
| `com.aionemu.gameserver.model.gameobjects.PetAction` | `Aion.GameServer.Model.GameObjects.PetAction` | Enum | Partial | Unit Tested | Partial Parity | Ids and unknown fallback are ported. Only spawn/dismiss are consumed by the new serializer; other actions remain future serializer work. |
| `com.aionemu.gameserver.model.gameobjects.PetEmote` | `Aion.GameServer.Model.GameObjects.PetEmote` | Enum | Partial | Unit Tested | Partial Parity | Ids and unknown fallback are ported. Only fly-start/default-branch serialization is exercised. |
| `com.aionemu.gameserver.model.templates.pet.PetFunctionType` | `Aion.GameServer.Model.Templates.Pet.PetFunctionType` | Enum / Packet Metadata | Partial | Unit Tested through `SmPet` | Partial Parity | Packet ids are ported, including Java's duplicate `FOOD`/`APPEARANCE` id of 1. Player-function semantics are not modeled. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetSpawnSnapshot` | DTO / Snapshot | Partial | Unit Tested | Needs Verification | Snapshot carries packet-facing name/template/object/position/target/heading/master/decoration fields. It does not hydrate live Java `Pet`, move controller, or common data. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `SmPetSpawnSnapshot.Decoration` | DTO / Common Data Projection | Partial | Unit Tested through `SmPet` | Needs Verification | Only the decoration field needed by the spawn appearance block is represented. Feed, mood, expiry, birthday, doping, function list, and mutable timers remain unported. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_SpawnWritesKnownListSubsetLikeJava` | Packet Unit | `SM_PET(Pet)` | Spawn action id, string/template/object ids, current position, move target, heading, master id, and appearance block order. | Source-derived C# byte/field-order assertions. | No Java runtime golden vector; live pet hydration missing. |
| `SmPet_DismissWritesPetObjectIdAndDeleteAnimationLikeJava` | Packet Unit | `SM_PET(int, ObjectDeleteAnimation)` | Dismiss action id, pet object id, and delete-animation byte. | Source-derived C# field-order assertions. | No live not-see dispatch. |
| `SmPetEmote_FlyStartWritesDefaultBranchLikeJava` | Packet Unit | `SM_PET_EMOTE(Pet, PetEmote.FLY_START)` | Pet object id, fly-start id, and zero default branch params. | Source-derived C# field-order assertions. | Movement branches remain unsupported. |
| `PetActionAndEmoteResolversPreserveJavaUnknownFallbacks` | Enum Unit | `PetAction.getActionById`; `PetEmote.getEmoteById` | Known ids and unknown fallback behavior. | Deterministic source-derived assertions. | Reflection/map implementation differs intentionally from Java static maps. |

## Remaining Risks

- Java runtime packet captures were not generated, so parity is source-derived rather than runtime verified.
- Full `SM_PET` action coverage remains broad and unsupported.
- `SM_PET_EMOTE` movement branches need coordinates, heading, and move-controller targets before they can be ported.
- Live pet/common-data hydration is missing; packet callers must supply snapshot values.
- Pet feed, mood, doping, expiration, function-list, and special-function behavior include mutable time/state that remains unported.
- Live known-list integration and socket dispatch ordering are still disabled.
- Reflection differs intentionally: C# resolver switches replace Java static `HashMap` lookup while preserving ids and unknown fallback.
- Threading behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 5 code artifacts plus 2 packet-facing snapshot DTOs
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: full `SM_PET` serializer coverage, `SM_PET_EMOTE` movement serializer branches, live pet/common-data hydration, pet visibility packet construction from planner descriptors, Java runtime packet capture, live known-list dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Bridge the non-live pet visibility descriptors from `PlayerKnownListPetVisibilityOrderPlanService` to concrete packet-construction metadata using the new `SmPet`/`SmPetEmote` serializers. Keep the bridge non-sending and snapshot-based until live pet hydration and Java runtime packet captures exist.
