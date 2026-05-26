# Phase 6ADV Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1290
Status: Phase 6 continues; minimal C# pet packet prerequisites for known-list visibility now exist, but live known-list pet dispatch remains disabled.

## Session Summary

UOW-1290 added the serializer subset needed after the UOW-1288 pet visibility planner and UOW-1289 packet field audit.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetAction.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetEmote.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Templates/Pet/PetFunctionType.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPetEmote.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListPetPacketPrerequisites.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ADV-Completion.md`

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

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 5 code artifacts plus 2 packet-facing snapshot DTOs
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: full `SM_PET` serializer coverage, `SM_PET_EMOTE` movement serializer branches, live pet/common-data hydration, pet visibility packet construction from planner descriptors, Java runtime packet capture, live known-list dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java runtime packet captures were not generated, so parity is source-derived rather than runtime verified.
- Full `SM_PET` action coverage and `SM_PET_EMOTE` movement branches remain unsupported.
- Live pet/common-data hydration is missing; packet callers must supply snapshot values.
- Live known-list integration and socket dispatch ordering are still disabled.
- Reflection differs intentionally: C# resolver switches replace Java static maps while preserving ids and unknown fallback.
- Threading behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Bridge pet visibility descriptors to concrete packet-construction metadata.
- Scope:
  - consume `PlayerKnownListPetVisibilitySideEffectDescriptor`;
  - produce non-sending construction metadata for `SmPet` spawn/dismiss and `SmPetEmote` fly-start;
  - keep callers snapshot-based and disabled for live dispatch;
  - add tests for descriptor-to-packet mapping and unsupported/missing snapshot gaps;
  - update readiness blockers after the bridge.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Pet descriptor-to-packet construction bridge | new service/test files plus docs | Medium | Best next executable prerequisite. |
| B | Full `SkillTargetSlot` enum mapping audit | docs/read-only or isolated enum | Low/Medium | Helps abnormal-effect slot parity. |
| C | Java pet golden-vector design note | docs/read-only | Low | Useful before runtime packet capture tooling is available. |

### Do Not Parallelize

- Multiple agents editing pet packet serializer/model files at once.
- Live pet dispatch/socket changes with descriptor bridge work.
- Full `SM_PET` action coverage with the known-list bridge.
- Shared progress/handoff docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPetEmote.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPetVisibilityOrderPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Latest completed commits:
  - `ca6bd5b10 [Phase 6][UOW-1289] Audit pet packet fields`
  - UOW-1290 should be committed as `[Phase 6][UOW-1290] Add pet packet prerequisites`
- Next commit after this handoff should be `[Phase 6][UOW-1291] ...`.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.
