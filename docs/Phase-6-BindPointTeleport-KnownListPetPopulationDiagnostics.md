# Phase 6 Bind-Point Teleport Known-List Pet Population Diagnostics

Date: May 26, 2026
Unit of Work: UOW-1292
Scope: Attach non-live pet visibility packet construction metadata to population diagnostics.
Source of truth: Java project.

## Summary

UOW-1292 adds an optional, non-sending population-level attachment point for dependent pet visibility packet construction.

Implemented:

- optional pet visibility order requests on `PlayerKnownListPopulationCandidateFact`;
- optional supplied `SmPetSpawnSnapshot` map on `PlayerKnownListPopulationPlanRequest`;
- per-candidate `PlayerKnownListPopulationPetVisibilityPacketConstructionAttachment` records;
- population diagnostic counts for pet packet-construction plans, constructed pet packets, and blocked pet packets;
- tests for constructed dependent pet packets and missing pet spawn snapshot blockers.

This does not enable live known-list mutation or socket dispatch. It gives the existing population diagnostic surface a place to report Java's dependent pet visibility retry after the master player visibility callback.

## Validation

- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPacketConstructionDiagnostic|PlayerKnownListPetVisibility"` passed 20 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PlayerVisualStatsUpdate"` passed 362 tests.
- One parallel build/test attempt failed with a transient compiler output lock; rerunning serially passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Explorer Hydration Findings

A read-only explorer audited Java live pet snapshot hydration. No files were edited by the explorer.

Minimal Java live fields needed for `SM_PET(Pet)` spawn:

- `pet.getName()`;
- `pet.getObjectTemplate().getTemplateId()`;
- `pet.getObjectId()`;
- `pet.getPosition().getX/Y/Z()`;
- `pet.getMoveController().getTargetX2/Y2/Z2()`;
- `pet.getHeading()`;
- `pet.getMaster().getObjectId()`;
- `pet.getCommonData().getDecoration()`.

`SM_PET_EMOTE(Pet, PetEmote.FLY_START)` needs only the pet object id plus `PetEmote.FLY_START`; Java writes zero `emotionId` and zero `param1` through the default branch. The fly-start decision is based on `Player.isInFlyingState()`, not a generic gliding/flying flag.

## Migration Parity Table - UOW-1292

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.updatePetVisibility` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Population Composition Metadata | Partial | Unit + Regression Tested | Partial Parity | Population plans can now carry dependent pet visibility packet-construction metadata after player visibility metadata. No live `KnownList` mutation, callback execution, or `ConcurrentHashMap` ordering parity is implemented. |
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `PlayerKnownListPopulationPetVisibilityPacketConstructionAttachment` | Controller Packet Metadata | Partial | Unit + Regression Tested | Partial Parity | Population diagnostics count `SmPet` spawn and optional `SmPetEmote` fly-start packet construction from supplied snapshots. Live pet hydration and Java runtime packet capture are missing. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)` | `PlayerKnownListPopulationPetVisibilityPacketConstructionAttachment` | Controller Packet Metadata | Partial | Unit + Regression Tested | Partial Parity | Attachment can carry dismiss construction plans through the same pet visibility bridge, but this unit's population regression focuses on see/fly-start and missing snapshot behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer Consumer | Partial | Unit + Regression Tested | Partial Parity | Population diagnostics consume the spawn/dismiss serializer subset. Full `SM_PET` action coverage remains unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Serializer Consumer | Partial | Unit + Regression Tested | Partial Parity | Population diagnostics consume only fly-start/default branch metadata. Movement branches remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetSpawnSnapshot` | DTO / Snapshot Input | Partial | Unit Tested | Needs Verification | Snapshot remains supplied input. Live active pet, template, position, move-target, heading, master, and common-data hydration are still missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `SmPetSpawnSnapshot.Decoration` | DTO / Common Data Projection | Partial | Unit Tested | Needs Verification | Only decoration is consumed for spawn appearance. Feed, mood, expiry, birthday, doping, function-list, timestamps, and timers remain unported. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Summarize_PetVisibilityPacketConstructionMetadataCountsDependentPetPackets` | Unit / Diagnostic | `KnownList.updatePetVisibility`; `PlayerController.see(Pet)` | Population diagnostics count dependent pet spawn plus fly-start packets after supplied snapshot construction. | Source-derived C# metadata assertions. | No live dispatch or Java runtime capture. |
| `Summarize_MissingPetSpawnSnapshotSurfacesPetPacketBlocker` | Unit / Diagnostic | Java reads live `Pet` and `PetCommonData` for spawn | Missing supplied spawn snapshot is surfaced as a pet packet blocker while fly-start can still construct from pet id. | Deterministic blocker assertion. | No live pet hydration. |

## Remaining Risks

- Java runtime packet captures were not generated; parity remains source-derived.
- Live active pet and pet common-data hydration are not implemented.
- Full `SM_PET` action coverage and `SM_PET_EMOTE` movement branches remain unsupported.
- Population metadata is optional and supplied snapshot based.
- Live known-list mutation, dependent pet retry execution, and socket dispatch are disabled.
- Java synchronized/update ordering and `ConcurrentHashMap` iteration behavior are not modeled beyond descriptor order metadata.
- Reflection behavior did not change. Threading behavior did not change. Date/time behavior is intentionally avoided for spawn snapshots.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 population-level pet packet diagnostic attachment path plus 2 diagnostic regression tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: live pet/common-data hydration, live known-list pet retry execution, full pet packet coverage, Java runtime packet capture, socket dispatch, socket-order validation, and full pet visibility predicate parity
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live `SmPetSpawnSnapshot` provider/adapter shape from future live pet snapshots, or create a Java golden-vector design note for pet packets. Keep the provider disabled until C# has active pet, common-data, template, position, movement target, and master flying-state surfaces.
