# Phase 6 Bind-Point Teleport Known-List Pet Spawn Snapshot Provider

Date: May 26, 2026
Unit of Work: UOW-1293
Scope: Add a disabled/non-live provider shape for Java `SM_PET(Pet)` spawn snapshots.
Source of truth: Java project.

## Summary

UOW-1293 adds `PlayerKnownListPetSpawnSnapshotProviderService`, a conservative provider that validates the Java-required pet fields before producing `SmPetSpawnSnapshot`.

The provider remains non-live. It consumes supplied packet-facing values because C# still lacks active toy-pet, pet common-data, pet template, pet position, pet move target, and live master/pet object relationships.

Validated fields:

- active pet object id;
- pet name from Java `PetCommonData.getName()`;
- pet template id from `pet.getObjectTemplate().getTemplateId()`;
- pet position XYZ;
- pet move-controller target XYZ;
- pet heading;
- pet master object id;
- pet common-data decoration.

The provider also carries `MasterIsInFlyingState` metadata and exposes `CanCreateFlyStartEmote`, intentionally matching Java's `Player.isInFlyingState()` distinction rather than a generic flying/gliding predicate.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerKnownListPetSpawnSnapshotProvider|PlayerKnownListPetVisibility|SmPet"` passed 25 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PlayerVisualStatsUpdate"` passed 373 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1293

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Services.PlayerKnownListPetSpawnSnapshotProviderService` | Packet Snapshot Provider | Partial | Unit + Regression Tested | Partial Parity | Provider validates all fields needed by the Java `SM_PET(Pet)` spawn constructor before creating `SmPetSpawnSnapshot`. It does not read live Java-equivalent pet objects. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `PlayerKnownListPetSpawnSnapshotProviderInput` | Snapshot Input DTO | Partial | Unit Tested | Needs Verification | Input models packet-facing live pet fields. Active pet reference, template binding, position, move-controller target, heading, and master relationships must still be hydrated by future live adapters. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `PlayerKnownListPetSpawnSnapshotProviderInput.PetName/CommonDataDecoration` | Common Data Projection | Partial | Unit Tested | Needs Verification | Name and decoration are represented. Birthday, expiry, feed, mood, doping, timestamps, and mutable timers remain unsupported. |
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `PlayerKnownListPetSpawnSnapshotProviderResult.CanCreateFlyStartEmote` | Controller Metadata | Partial | Unit Tested | Partial Parity | Result preserves the Java `Player.isInFlyingState()` decision input for fly-start metadata. It does not execute `PlayerController.see(Pet)` or send packets. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `CanCreateFlyStartEmote` metadata feeding `SmPetEmote` | Packet Metadata | Partial | Unit Tested | Partial Parity | Provider records whether fly-start can be planned from Java flying-state metadata. It does not construct or send the emote packet itself. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_WithCompleteSuppliedPetSnapshotBuildsSmPetSpawnSnapshot` | Unit / Provider | `SM_PET(Pet)` | Complete supplied pet fields produce an exact `SmPetSpawnSnapshot`. | Source-derived field assertions. | No live pet object hydration. |
| `Create_UsesJavaFlyingStateNotGenericGlidingForFlyStartMetadata` | Unit / Provider | `PlayerController.see(Pet)` | Fly-start metadata follows supplied `isInFlyingState` only. | Source-derived metadata assertion. | No live fly-state adapter. |
| `Create_MissingJavaRequiredFieldBlocksSnapshot` | Unit / Provider | `SM_PET(Pet)` reads live pet/template/common-data/move fields | Missing required fields produce explicit blockers and no snapshot. | Deterministic blocker assertions. | No Java runtime comparison. |
| `TryCreate_ReturnsFalseAndNullOutputForBlockedSnapshot` | Unit / Provider | Future adapter shape | Try-create helper returns false when snapshot creation is blocked. | C# API guard assertion. | No live caller yet. |

## Remaining Risks

- Java runtime packet captures were not generated; parity remains source-derived.
- Provider input is supplied metadata, not live pet hydration.
- C# has no active toy-pet model, pet common-data model, pet template binding, or pet move-controller target state.
- Full `SM_PET` action coverage and `SM_PET_EMOTE` movement branches remain unsupported.
- Live known-list mutation, dependent pet retry execution, and socket dispatch remain disabled.
- Java mutable pet/common-data reads are not modeled with locks; C# provider copies supplied values only.
- Date/time behavior is intentionally avoided for spawn packets.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live pet spawn snapshot provider plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live active pet model, live pet common-data hydration, live pet template/position/move-target hydration, Java runtime packet capture, live known-list dispatch, and full pet packet coverage
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Wire the non-live provider output into the existing population pet diagnostics as an optional provider-result source, or write the Java pet golden-vector design note. Keep all live dispatch disabled.
