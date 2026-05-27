# Phase 6 Bind-Point Teleport Known-List Pet Packet Construction Bridge

Date: May 26, 2026
Unit of Work: UOW-1291
Scope: Bridge non-live pet visibility descriptors to concrete packet-construction metadata.
Source of truth: Java project.

## Summary

UOW-1291 connects the UOW-1288 pet visibility descriptors to the UOW-1290 packet serializer subset without enabling live socket dispatch.

Implemented:

- `PlayerKnownListPetVisibilityPacketConstructionService`;
- non-sending construction plans for `SmPet` spawn/dismiss and `SmPetEmote` fly-start;
- explicit blockers for missing spawn snapshots, pet object-id mismatch, missing delete animation, and unsupported descriptors;
- descriptor metadata updates from missing to partial C# support for known-list pet packet subsets.

The bridge consumes supplied `SmPetSpawnSnapshot` values. It does not hydrate Java-equivalent live `Pet`, `PetCommonData`, or move-controller state.

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

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_SeeWithFlyingMasterBuildsSmPetThenFlyStartEmote` | Unit / Bridge | `PlayerController.see(Pet)` | Constructs spawn then fly-start packets in descriptor order. | Source-derived packet type/order assertions. | No live dispatch or Java runtime capture. |
| `Construct_NotSeeBuildsSmPetDismissWithAnimation` | Unit / Bridge | `PlayerController.notSee(Pet)` | Constructs dismiss packet from descriptor delete animation. | Source-derived packet type assertion. | Viewer spawn guard is upstream metadata. |
| `Construct_MissingSpawnSnapshotBlocksOnlySpawnDescriptor` | Unit / Blocker | Java reads live `Pet` and `PetCommonData` | Missing spawn snapshot blocks spawn while fly-start can still be constructed from pet id. | Deterministic blocker assertion. | No live hydration. |
| `Construct_SpawnSnapshotPetMismatchBlocksSpawn` | Unit / Blocker | Java packet pet object identity | Mismatched supplied snapshot does not construct a spawn packet. | Deterministic blocker assertion. | No live identity lookup. |
| `Construct_NoDescriptorsReportsNoDescriptors` | Unit / Guard | Java no-pet guard | No pet descriptors produce a no-descriptors construction plan. | C# guard assertion. | No live known-list state. |

## Remaining Risks

- Java runtime packet captures were not generated, so parity remains source-derived.
- Live pet/common-data hydration is missing and required before descriptors can become live packet sends.
- Full `SM_PET` action coverage and `SM_PET_EMOTE` movement branches remain unsupported.
- Socket dispatch ordering is still disabled.
- Java known-list mutation and dependent pet retry execution are not wired to the bridge.
- Reflection differences are intentional: C# switches and records replace Java static maps/live objects.
- Threading behavior did not change; Java callback ordering is modeled as metadata only.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live packet-construction bridge plus descriptor metadata updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: live pet/common-data hydration, operation/population-level pet construction attachment, live known-list pet retry execution, full pet packet coverage, Java runtime packet capture, socket dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Attach pet packet-construction plans to population/operation-side diagnostics in a non-live way, or add a live pet snapshot hydration audit before attachment. Keep live dispatch disabled until Java runtime packet captures and live hydration exist.
