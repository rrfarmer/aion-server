# Phase 6 Bind-Point Teleport Known-List Pet Provider Diagnostics

Date: May 27, 2026
Unit of Work: UOW-1294
Status: Complete for non-live provider-result diagnostic bridge; live pet hydration and dispatch remain disabled.

## Scope

This unit connects the disabled `PlayerKnownListPetSpawnSnapshotProviderService` from UOW-1293 to the existing population pet packet-construction diagnostics from UOW-1292.

The bridge is still supplied-input only:

- direct request-level `SmPetSpawnSnapshot` values remain authoritative;
- optional provider inputs can create a snapshot when a direct snapshot is absent;
- provider blockers are carried into population diagnostics;
- no live `Pet`, `PetCommonData`, template, movement, known-list, or socket dispatch behavior is enabled.

## Java Source Breadcrumbs

- `com.aionemu.gameserver.world.knownlist.KnownList.updatePetVisibility`
- `com.aionemu.gameserver.controllers.PlayerController.see(Pet)`
- `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE`
- `com.aionemu.gameserver.model.gameobjects.Pet`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData`

## C# Artifacts

- `Aion.GameServer.Services.PlayerKnownListPopulationPlanService`
- `Aion.GameServer.Services.PlayerKnownListPopulationPlanRequest`
- `Aion.GameServer.Services.PlayerKnownListPopulationPetVisibilityPacketConstructionAttachment`
- `Aion.GameServer.Services.PlayerKnownListPopulationPacketConstructionDiagnosticService`
- `Aion.GameServer.Services.PlayerKnownListPopulationPetVisibilityPacketConstructionDiagnostic`
- `Aion.GameServer.Services.PlayerKnownListPetSpawnSnapshotProviderService`

## Behavior Added

Population planning now accepts optional `PetSpawnSnapshotProviderInputsByPetObjectId`. For each pet visibility plan:

1. If a direct `PetSpawnSnapshotsByPetObjectId` entry exists, that snapshot is used for packet construction.
2. Otherwise, if a provider input exists for the pet object id, the provider creates either a `SmPetSpawnSnapshot` or an explicit blocker result.
3. The packet-construction bridge receives the resolved snapshot, if any.
4. Diagnostics report provider result count, blocked provider result count, status counts by provider status, and per-candidate provider status.

This preserves Java-facing blockers such as missing move-controller target values instead of collapsing them into a generic missing snapshot message.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPacketConstructionDiagnostic|PlayerKnownListPetSpawnSnapshotProvider|PlayerKnownListPetVisibility"` passed 34 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PlayerVisualStatsUpdate"` passed 376 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1294

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.updatePetVisibility` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Population Composition Metadata | Partial | Unit + Regression Tested | Partial Parity | Population pet diagnostics can now consume provider-created snapshots or provider blockers. No live `KnownList` mutation, dependent retry execution, `ConcurrentHashMap` ordering, or socket dispatch is implemented. |
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `PlayerKnownListPopulationPetVisibilityPacketConstructionAttachment` | Controller Packet Metadata | Partial | Unit + Regression Tested | Partial Parity | See-path diagnostics can preserve provider-created `SM_PET` spawn input and fly-start metadata. It does not call live `PlayerController.see(Pet)` or send packets. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)` | `PlayerKnownListPopulationPetVisibilityPacketConstructionAttachment` | Controller Packet Metadata | Partial | Unit + Regression Tested | Partial Parity | Attachment still supports dismiss construction plans. This unit did not add new live not-see behavior or socket-order validation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet`; `PlayerKnownListPetSpawnSnapshotProviderService` | Packet / Snapshot Consumer | Partial | Unit + Regression Tested | Partial Parity | Provider-created snapshots can now feed population packet construction. Full Java action coverage remains unsupported and no runtime golden vector exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote`; provider fly-start metadata | Packet / Metadata Consumer | Partial | Unit + Regression Tested | Partial Parity | Population diagnostics can construct fly-start metadata after provider-created spawn snapshots. Movement branches remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `PlayerKnownListPetSpawnSnapshotProviderInput` | Snapshot Input DTO | Partial | Unit Tested | Needs Verification | Provider input remains supplied metadata. Active pet reference, template binding, position, move-controller target, heading, master relationship, locks, and live state hydration remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `PlayerKnownListPetSpawnSnapshotProviderInput.PetName/CommonDataDecoration` | Common Data Projection | Partial | Unit Tested | Needs Verification | Provider blockers now surface missing name/common-data fields through population diagnostics. Birthday, expiry, feed, mood, doping, functions, timestamps, and timers remain unsupported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Summarize_PetSpawnProviderInputFeedsPopulationPetDiagnostics` | Unit / Diagnostic | `KnownList.updatePetVisibility`; `PlayerController.see(Pet)`; `SM_PET(Pet)` | Complete supplied provider input creates a snapshot, feeds pet packet construction, and reports provider status `Created`. | Source-derived metadata assertions. | No live pet object hydration or Java runtime capture. |
| `Summarize_DirectPetSpawnSnapshotRemainsAuthoritativeOverProviderInput` | Unit / Diagnostic | Java live `SM_PET(Pet)` has one live pet source; C# request snapshot precedence policy | Direct supplied snapshots stay authoritative over provider inputs and avoid provider blocker noise. | Deterministic C# precedence regression. | This is a C# staging-policy test, not Java runtime evidence. |
| `Summarize_BlockedPetSpawnProviderResultIsReportedWithPacketBlocker` | Unit / Diagnostic | `SM_PET(Pet)` reads `pet.getMoveController().getTargetX2/Y2/Z2()` | Missing move-target data is reported as provider status `MissingPetMoveTarget` while the packet bridge also reports a blocked spawn packet. | Deterministic blocker assertion from Java source reads. | No live move-controller adapter or runtime comparison. |

## Remaining Risks

- Java runtime packet captures were not generated; parity remains source-derived.
- Provider inputs are still supplied metadata, not live `Pet`/`PetCommonData`/template/move-controller hydration.
- Direct snapshots and provider inputs are request-level C# staging surfaces; Java reads mutable live state at send time.
- Java synchronization/threading around mutable pet state is not modeled.
- Full `SM_PET` action coverage and `SM_PET_EMOTE` movement branches remain unsupported.
- Live known-list mutation, dependent pet retry execution, and socket dispatch remain disabled.
- Date/time behavior remains intentionally avoided for spawn packets, but pet common-data timers are still unported.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 provider-result population diagnostic bridge plus 3 focused diagnostic tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: live active pet model, live pet common-data hydration, live pet template/position/move-target hydration, full pet packet coverage, Java runtime packet capture, live known-list dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Draft the Java pet golden-vector design note or audit `SM_PET_EMOTE` movement branches for future serializer coverage.

Recommended next scope:

- identify exact Java harness inputs needed to instantiate `SM_PET(Pet)` and `SM_PET_EMOTE`;
- record object-id/template/name/position/move-target/heading/master/flying-state dependencies;
- keep the output as design documentation unless Java tooling is available for actual golden-vector generation.
