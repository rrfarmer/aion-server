# Phase 6ADT Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1288
Status: Phase 6 continues; a non-live pet visibility/order prerequisite now models Java's dependent pet retry after player visibility callbacks. Live known-list player-see dispatch remains disabled because pet packet serializers, pet object/common-data models, live pet visibility integration, socket dispatch, and Java runtime validation are still missing.

## Session Summary

UOW-1288 added `PlayerKnownListPetVisibilityOrderPlanService`.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPetVisibilityOrderPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListPetVisibilityOrder.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ADT-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPetVisibilityOrderPlanServiceTests|PlayerKnownListPlayerSideEffectPlanServiceTests|PlayerKnownListOperationSideEffectAttachmentServiceTests|PlayerKnownListPopulationPlanServiceTests" --nologo` passed 31 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|PlayerKnownListPetVisibility|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 351 tests.
- No Java runtime packet capture was run.
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

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live pet visibility/order planner plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 1 `SM_PET` serializer, 1 `SM_PET_EMOTE` serializer, 1 pet object/common-data model, 1 full pet visibility predicate, 1 live known-list pet update integration point, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- This is metadata only; no live known-list callbacks, membership mutation, or socket sends occur.
- `SM_PET`, `SM_PET_EMOTE`, `PetAction`, `PetEmote`, pet common-data, pet movement, and pet appearance serialization are missing.
- Full pet visibility predicate is not ported; master visibility is a supplied flag.
- Java `ConcurrentHashMap` candidate ordering remains unspecified and not modeled.
- Java runtime packet capture was not performed.
- Threading behavior is not modeled beyond documenting synchronous ordering.
- Reflection and date/time behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Audit and/or port the minimal `SM_PET`/`SM_PET_EMOTE` packet prerequisite.
- Scope:
  - inspect Java `SM_PET`, `SM_PET_EMOTE`, `PetAction`, `PetEmote`, `Pet`, and pet common-data fields;
  - decide whether a docs-only packet field audit is needed before code;
  - if code is safe, add serializers for a narrow spawn/dismiss/fly-start subset with blocked metadata for unsupported layouts;
  - add golden or source-derived packet tests where deterministic inputs are available;
  - do not wire live pet visibility dispatch.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `SM_PET`/`SM_PET_EMOTE` field audit | docs/read-only or new audit doc | Low/Medium | Best next step before serializer work. |
| B | Minimal pet packet serializers | new packet/model/test files | Medium/High | Only after field audit is clear; avoid shared live planner changes. |
| C | Snapshot-entry list composition | new isolated abnormal-effect service/test files | Medium | Independent of pet packet files. |
| D | Full `SkillTargetSlot` enum mapping audit | docs/read-only or isolated enum | Low/Medium | Helps reduce abnormal-effect slot caller-supplied risk. |

### Do Not Parallelize

- Multiple agents editing pet packet serializer/model files at once.
- Pet visibility planner integration and packet serializer implementation in shared files at the same time.
- Live known-list dispatch or socket executor changes.
- Shared progress/handoff docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/Pet.java`
  - pet action/emote/common-data classes discovered by audit
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPetVisibilityOrderPlanServiceTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Latest completed commits:
  - `15e8c35c7 [Phase 6][UOW-1286] Add abnormal effect remaining time helper`
  - `a565a2e5e [Phase 6][UOW-1287] Add abnormal effect snapshot factory`
  - UOW-1288 should be committed as `[Phase 6][UOW-1288] Add pet visibility order plan`
- Next commit after this handoff should be `[Phase 6][UOW-1289] ...` for pet packet field audit, minimal pet packet prerequisite, snapshot-entry list composition, or another selected safe unit.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.
