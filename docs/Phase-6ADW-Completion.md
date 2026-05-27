# Phase 6ADW Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1291
Status: Phase 6 continues; non-live pet visibility packet construction now exists, but live known-list pet dispatch remains disabled.

## Session Summary

UOW-1291 bridges pet visibility descriptors to concrete packet-construction metadata using the UOW-1290 `SmPet` and `SmPetEmote` serializer subset.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityPacketConstructionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPetVisibilityOrderPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPetVisibilityPacketConstructionServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListPetPacketConstructionBridge.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ADW-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerKnownListPetVisibility"` passed 11 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PlayerVisualStatsUpdate"` passed 360 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed before the final focused test rerun.
- One parallel test/build attempt failed with a transient compiler output lock; rerunning the test command alone passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1291

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `Aion.GameServer.Services.PlayerKnownListPetVisibilityPacketConstructionService` | Controller Packet Construction Bridge | Partial | Unit + Regression Tested | Partial Parity | Bridge constructs `SmPet` spawn and optional `SmPetEmote` fly-start in descriptor order from supplied snapshots. It does not execute live controller callbacks or socket sends. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)` | `PlayerKnownListPetVisibilityPacketConstructionService` | Controller Packet Construction Bridge | Partial | Unit + Regression Tested | Partial Parity | Bridge constructs `SmPet` dismiss from descriptor pet id and delete animation. Viewer spawned guard still belongs to the upstream planner. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updatePetVisibility` | `PlayerKnownListPetVisibilityOrderPlanService` plus construction bridge | Known-List Dependent Packet Metadata | Partial | Unit + Regression Tested | Partial Parity | Descriptor support now reports partial C# packet support. Live known-list membership mutation and dependent retry execution remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Bridge consumes serializer for spawn/dismiss only. Full Java action coverage remains unsupported and no runtime golden vector exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Serializer | Partial | Unit + Regression Tested | Partial Parity | Bridge consumes fly-start/default branch only. Movement branches remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetSpawnSnapshot` | DTO / Snapshot | Partial | Unit Tested | Needs Verification | Snapshot must be supplied by caller. Live `Pet`, position, move-controller target, heading, master, and common-data hydration are not implemented. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `SmPetSpawnSnapshot.Decoration` | DTO / Common Data Projection | Partial | Unit Tested through bridge/serializer | Needs Verification | Only decoration is represented. Feed, mood, expiry, birthday, doping, functions, and live mutable state remain unported. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live packet-construction bridge plus descriptor metadata updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: live pet/common-data hydration, operation/population-level pet construction attachment, live known-list pet retry execution, full pet packet coverage, Java runtime packet capture, socket dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java runtime packet captures were not generated, so parity remains source-derived.
- Live pet/common-data hydration is missing and required before descriptors can become live packet sends.
- Full `SM_PET` action coverage and `SM_PET_EMOTE` movement branches remain unsupported.
- Java known-list mutation, dependent pet retry execution, and socket dispatch are not wired to the bridge.
- Reflection differences are intentional: C# switches and records replace Java static maps/live objects.
- Threading behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Attach pet packet-construction metadata to population/operation diagnostics or audit live pet snapshot hydration.
- Scope:
  - keep attachment non-sending and snapshot-based;
  - preserve player packet construction order first, then dependent pet packet metadata;
  - report missing pet spawn snapshots as blockers;
  - do not enable socket sends.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Pet construction diagnostic attachment | new service/test files plus docs | Medium | Good next executable prerequisite. |
| B | Live pet snapshot hydration audit | docs/read-only or isolated adapter shape | Medium | Needed before live dispatch can ever be enabled. |
| C | Java pet golden-vector design note | docs/read-only | Low | Useful before runtime capture tooling is available. |
| D | Full `SkillTargetSlot` enum mapping audit | docs/read-only or isolated enum | Low/Medium | Helps abnormal-effect slot parity. |

### Do Not Parallelize

- Shared progress/handoff docs.
- Live socket dispatch changes with diagnostic attachment.
- Full `SM_PET` action coverage with known-list pet construction attachment.
- Multiple agents editing the same pet visibility service/test files.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityOrderPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPetVisibilityPacketConstructionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPetEmote.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPetVisibilityPacketConstructionServiceTests.cs`
- Latest completed commits:
  - `c3daecf69 [Phase 6][UOW-1290] Add pet packet prerequisites`
  - UOW-1291 should be committed as `[Phase 6][UOW-1291] Bridge pet packet construction`
- Next commit after this handoff should be `[Phase 6][UOW-1292] ...`.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.
