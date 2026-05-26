# Phase 6 Bind-Point Teleport Known-List Pet Visibility Order

Date: May 26, 2026
Unit of Work: UOW-1288
Scope: Add a non-live pet visibility/order prerequisite for player known-list side effects.
Source of truth: Java project.

## Summary

UOW-1288 adds `PlayerKnownListPetVisibilityOrderPlanService`, a metadata-only planner for Java's dependent pet visibility retry after a player's visibility callback.

The planner models:

- pet visibility side effects ordered after the master player visibility callback;
- `SM_PET(pet)` spawn intent;
- optional `SM_PET_EMOTE(pet, FLY_START)` when the master is flying;
- `SM_PET(petObjectId, animation)` dismiss intent for pet not-see;
- viewer-unspawned skip behavior for not-see;
- the “pet known before master becomes visible” repair case from Java `KnownList.updatePetVisibility`.

It does not serialize pet packets, send packets, mutate known-list state, or model live pet objects.

## Java Source Findings

- `KnownList.updateVisibility` calls `notifySee` or `notifyNotSee`, then calls `updatePetVisibility(knownObject)`.
- Java comment: pet spawn packet must be sent after `SM_PLAYER_INFO`, otherwise the pet is not displayed.
- `PlayerController.see(Pet)` sends `SM_PET(pet)` and then `SM_PET_EMOTE(pet, PetEmote.FLY_START)` when the master is flying.
- `PlayerController.notSee(Pet)` sends `SM_PET(objectId, animation)` if the viewer is spawned.
- `Creature.canSee` gates pet visibility through master visibility: a viewer can see a pet only if it is the master, or it can see and currently sees the master.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`:

- `PlayerKnownListPetVisibilityOrderRequest`;
- `PlayerKnownListPetVisibilityOrderPlan`;
- `PlayerKnownListPetVisibilitySideEffectDescriptor`;
- enums for status, transition, descriptor kind, and ordering.

Descriptors intentionally mark `CSharpSupport = Missing` because `SmPet`, `SmPetEmote`, `PetAction`, `PetEmote`, and full pet object/common-data serialization are not ported yet.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPetVisibilityOrderPlanServiceTests|PlayerKnownListPlayerSideEffectPlanServiceTests|PlayerKnownListOperationSideEffectAttachmentServiceTests|PlayerKnownListPopulationPlanServiceTests" --nologo` passed 31 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|PlayerKnownListPetVisibility|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 351 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1288

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList` | `Aion.GameServer.Services.PlayerKnownListPetVisibilityOrderPlanService` | Known-List Visibility Planner | Partial | Unit Tested | Partial Parity | Planner models Java's dependent pet visibility retry after player visibility callbacks. It does not mutate live known-list state, preserve `ConcurrentHashMap` iteration behavior, or execute callbacks. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | `PlayerKnownListPetVisibilitySideEffectDescriptor` | Controller Packet Metadata | Partial | Unit Tested | Needs Verification | Pet spawn and fly-start descriptors match Java ordering intent, but C# packet serializers are missing and no runtime packet comparison was run. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee` | `PlayerKnownListPetVisibilitySideEffectDescriptor` | Controller Packet Metadata | Partial | Unit Tested | Needs Verification | Planner models `SM_PET(objectId, animation)` dismiss intent and viewer-unspawned skip behavior. Serializer is missing. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `PlayerKnownListPetVisibilityOrderRequest` | Model / Snapshot Input | Not Started | Unit Tested | Needs Verification | Pet object data is represented only by supplied ids and flags. Full pet model, common data, special functions, appearance, and movement state are missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | descriptor `JavaPacketName = SM_PET` | Packet / Serializer | Not Started | No Tests | Needs Verification | Descriptor identifies spawn/dismiss packet requirement, but C# serializer is absent. Serialization shape and golden vectors remain blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | descriptor `JavaPacketName = SM_PET_EMOTE` | Packet / Serializer | Not Started | No Tests | Needs Verification | Descriptor identifies fly-start emote requirement, but C# `PetEmote` and packet serializer are absent. |
| `com.aionemu.gameserver.model.gameobjects.Creature.canSee` | `RequiresMasterVisibilityBeforePetVisibility` metadata | Visibility Predicate | Partial | Unit Tested | Needs Verification | Planner records master-visibility gate for pet visibility, but does not implement full Java visibility predicate or live known-list lookups. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_SeePetSpawnAfterMasterPlayerVisibilityCallback` | Unit / Planner | `KnownList.updateVisibility`; `PlayerController.see(Pet)` | Pet spawn descriptor is ordered after master player visibility and repairs known-before-master-visible cases. | Source-derived C# metadata assertions. | No live known-list or packet serialization. |
| `Plan_FlyingMasterAddsFlyStartEmoteAfterPetSpawn` | Unit / Planner | `PlayerController.see(Pet)` | Flying master adds `SM_PET_EMOTE` after `SM_PET`. | Source-derived ordering assertion. | `PetEmote` serializer missing. |
| `Plan_NotSeePetUsesSmPetDismissPacketInsteadOfDelete` | Unit / Planner | `PlayerController.notSee(Pet)` | Pet not-see uses `SM_PET(objectId, animation)` intent, not `SM_DELETE`. | Source-derived descriptor assertion. | Serializer missing. |
| `Plan_SeeDoesNotSpawnPetBeforeMasterVisible` | Unit / Visibility Gate | `Creature.canSee` pet master visibility gate | Pet spawn is blocked until master is visible. | Source-derived metadata assertion. | Full visibility predicate not ported. |
| `Plan_NotSeeSkipsDismissWhenViewerIsUnspawned` | Unit / Guard | `PlayerController.notSee` spawned check | Viewer-unspawned pet not-see sends no descriptor. | Source-derived guard assertion. | Live player spawn state is supplied. |
| `Plan_NoPetWhenMasterSnapshotHasNoPet` | Unit / Guard | `KnownList.updatePetVisibility` player-has-pet guard | No pet snapshot produces no dependent side effect. | C# guard assertion. | Java live player/pet state is not read. |

## Remaining Risks

- This is metadata only; no live known-list callbacks, membership mutation, or socket sends occur.
- `SM_PET`, `SM_PET_EMOTE`, `PetAction`, `PetEmote`, pet common-data, pet movement, and pet appearance serialization are missing.
- Full pet visibility predicate is not ported; master visibility is a supplied flag.
- Java `ConcurrentHashMap` candidate ordering remains unspecified and not modeled.
- Java runtime packet capture was not performed.
- Threading behavior is not modeled beyond documenting synchronous ordering.
- Reflection and date/time behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live pet visibility/order planner plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 1 `SM_PET` serializer, 1 `SM_PET_EMOTE` serializer, 1 pet object/common-data model, 1 full pet visibility predicate, 1 live known-list pet update integration point, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Port the minimal `SM_PET`/`SM_PET_EMOTE` packet prerequisite or add a pet packet field audit document first. Keep live pet visibility dispatch disabled until serializers and golden vectors exist.
