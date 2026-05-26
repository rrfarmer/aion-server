# Phase 6ADU Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1289
Status: Phase 6 continues; Java pet packet fields needed for known-list pet visibility have been audited. Live known-list player-see dispatch remains disabled because C# pet packet serializers, pet snapshot DTOs, golden vectors, live pet visibility integration, socket dispatch, and Java runtime validation are still missing.

## Session Summary

UOW-1289 added a docs-only audit for `SM_PET` and `SM_PET_EMOTE`.

Files changed:

- `docs/Phase-6-BindPointTeleport-KnownListPetPacketAudit.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ADU-Completion.md`

## Validation

- Documentation-only unit; no code tests were run for this audit.
- No Java runtime packet capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1289

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | future `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Not Started | No Tests | Needs Verification | Audit identifies known-list spawn/dismiss fields and many unsupported action layouts. Serializer/golden tests are still missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | future `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Serializer | Not Started | No Tests | Needs Verification | Audit identifies fly-start default branch fields. Movement/emotion branches remain future work. |
| `com.aionemu.gameserver.model.gameobjects.PetAction` | future C# enum | Enum | Not Started | No Tests | Needs Verification | All action ids listed. Unknown/default handling must match Java map lookup. |
| `com.aionemu.gameserver.model.gameobjects.PetEmote` | future C# enum | Enum | Not Started | No Tests | Needs Verification | Relevant ids listed. Full enum and unknown handling need port/tests. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future pet common-data snapshot DTO | DTO / Model | Not Started | No Tests | Needs Verification | Spawn needs name/template/object/master/decoration. Full common-data behaviors for feed/mood/doping/listing remain unported and include live time calculations. |
| `com.aionemu.gameserver.model.templates.pet.PetFunctionType` | future C# enum or packet constants | Enum / Packet Metadata | Not Started | No Tests | Needs Verification | Appearance id and optional function ids audited. Full function packet blocks are future work. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | future DTO/constants | DTO / Packet Metadata | Not Started | No Tests | Needs Verification | Required only for full `LOAD_PETS`/doping layouts, not first known-list spawn/dismiss serializer. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 packet audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 1 `SM_PET` serializer, 1 `SM_PET_EMOTE` serializer, 1 `PetAction` enum, 1 `PetEmote` enum, 1 pet common-data snapshot DTO, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- This unit is documentation only; no serializer exists yet.
- Java `writeS` string encoding and frame/opcode behavior must be covered by future packet tests.
- Spawn layout depends on live `Pet`, `PetCommonData`, position, move-controller target, heading, master object id, and appearance data.
- Full `SM_PET` action coverage is broad and should not be assumed from a narrow known-list serializer.
- Pet feed/mood/doping layouts use live timers and mutable common data.
- Java runtime golden vectors were not generated.
- Reflection and threading behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add minimal C# pet packet prerequisites for known-list visibility only.
- Scope:
  - add `PetAction` values for `SPAWN` and `DISMISS`;
  - add `PetEmote.FLY_START`;
  - add snapshot DTOs for pet spawn/dismiss/fly-start packet fields;
  - add `SmPet` spawn/dismiss serializer and `SmPetEmote` fly-start serializer;
  - add source-derived packet tests for deterministic payload field order;
  - do not implement full `SM_PET` action coverage or live pet visibility dispatch.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Minimal pet packet serializers | new packet/model/test files | Medium | Best next executable prerequisite; keep scope to spawn/dismiss/fly-start. |
| B | Snapshot-entry list composition | new isolated abnormal-effect service/test files | Medium | Independent of pet packet files. |
| C | Full `SkillTargetSlot` enum mapping audit | docs/read-only or isolated enum | Low/Medium | Helps reduce abnormal-effect slot caller-supplied risk. |
| D | Java pet golden-vector plan | docs/read-only | Low | Useful before runtime packet capture tooling is available. |

### Do Not Parallelize

- Multiple agents editing pet packet serializer/model files at once.
- Pet visibility planner integration and packet serializer implementation in shared files at the same time.
- Full `SM_PET` action coverage with known-list serializer slice.
- Live known-list dispatch or socket executor changes.
- Shared progress/handoff docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`
- Latest completed commits:
  - `a565a2e5e [Phase 6][UOW-1287] Add abnormal effect snapshot factory`
  - `60c69a26a [Phase 6][UOW-1288] Add pet visibility order plan`
  - UOW-1289 should be committed as `[Phase 6][UOW-1289] Audit pet packet fields`
- Next commit after this handoff should be `[Phase 6][UOW-1290] ...` for minimal pet packet prerequisites, snapshot-entry list composition, or another selected safe unit.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.
