# Phase 6AEC Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1297
Latest Commit: included in the UOW-1297 unit commit
Status: Guarded pet vector artifact reader/comparator complete; Java runtime vector generation remains blocked locally.

## What Changed

- Added `dotnetConversion/tests/Aion.GameServer.Tests/PetJavaVectorArtifactReaderTests.cs`.
- Added `docs/Phase-6-BindPointTeleport-KnownListPetVectorArtifactReader.md`.
- Updated `docs/Phase-6-BindPointTeleport-KnownListPetGoldenVectorDesign.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

The new guarded reader parses schema-v1 Java known-list pet vector artifacts from `parity-artifacts/known-list-pet/java`. When artifacts exist, it validates semantic fields and compares Java `bodyHex` / `canonicalPayloadHex` to C# `SmPet` and `SmPetEmote` serializers. When artifacts do not exist, it logs the missing evidence and returns without claiming parity.

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetJavaVectorArtifactReader"` passed 2 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetJavaVectorArtifactReader|SmPet|PetActionAndEmoteResolvers|PlayerKnownListPetVisibility"` passed 20 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 380 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- `java -version` reports Java 8.
- `mvn -version` failed because Maven is not available on PATH.

No Java runtime packet capture was executed. No live `GameServerConnection` dispatch was enabled.

## Parallel Work Notes

Two read-only explorers completed and were closed:

- Full Java `SM_PET` audit:
  - C# currently covers only spawn/dismiss for `SmPet`.
  - Java branches still missing in C#: `LOAD_PETS`, `ADOPT`, `SURRENDER`, action-only merchant/minder/house adopt/abandon, `FOOD`, `RENAME`, `MOOD`, `SPECIAL_FUNCTION`, and `EXTEND_EXPIRATION` no-payload behavior.
  - Safest next code slices are action-only packets or `RENAME` / `SURRENDER`.
- Java `CM_PET_EMOTE` runtime audit:
  - Parser reads movement fields for `MOVE_STOP`, `MOVE_POSITION_UPDATE`, and `MOVETO`.
  - Negative current coordinates are rejected.
  - `MOVE_STOP` and `MOVETO` update position and broadcast to sighted players excluding master.
  - Known non-movement emotes broadcast default two-byte payload; `PetEmote.EMOTION` includes master.
  - Inbound `MOVE_POSITION_UPDATE(8)` has movement input, but outbound `SM_PET_EMOTE` uses the default server-packet branch.

## Migration Parity Table - UOW-1297

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Tests.PetJavaVectorArtifactReaderTests`; `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Artifact Reader | Partial | Unit + Regression Tested | Needs Verification | Reader can validate future Java spawn/dismiss artifact schema and compare generated body/canonical payload bytes when present. No Java artifacts exist yet, full `SM_PET` action coverage remains unsupported, and parity is not verified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `PetJavaVectorArtifactReaderTests`; `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Artifact Reader | Partial | Unit + Regression Tested | Needs Verification | Reader supports fly-start, move-stop, and move-to artifact semantics. No Java runtime vectors exist yet; source-derived serializer tests remain the only packet evidence. |
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `PetJavaVectorArtifactReaderTests` artifact scenario contract | Controller Packet Flow | Partial | Manual Only | Needs Verification | Artifact schema models spawn then optional fly-start sequence, but no live Java controller callback or socket send was captured. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)` | `PetJavaVectorArtifactReaderTests` artifact scenario contract | Controller Packet Flow | Partial | Manual Only | Needs Verification | Artifact schema models dismiss with delete animation. Viewer-spawned guard and live socket path remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET_EMOTE` | future C# parser/runtime plan; explorer audit | Client Packet / Runtime Source | Not Started | Manual Only | Unknown | Read-only audit captured Java parser shapes, guards, negative-coordinate behavior, movement side effects, and broadcast filtering. No C# parser/runtime work was implemented. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future common-data packet snapshot; explorer audit | Common Data / Snapshot Source | Partial | Manual Only | Needs Verification | Audit mapped full `SM_PET` dependencies: birthday, expiration, feed progress, refeed timers, mood timers, doping bag, template functions, and serializer-side mutations. Not ported in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ParseKnownListPetArtifact_ReadsSchemaV1PacketFields` | Unit / Artifact Schema | UOW-1295 vector design and Java packet source review | Parses a representative known-list pet vector schema and validates packet semantic fields. | Deterministic schema validation only. | Does not compare generated Java bytes. |
| `FindPetJavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Guarded Regression / Artifact Comparison | Future Java vector artifacts under `parity-artifacts/known-list-pet/java` | If artifacts exist, validates schema and compares generated Java body/canonical payload bytes to C# serializers; otherwise logs `Needs Verification`. | Guard path passed locally with no artifacts. | No Java artifacts exist yet, so no byte comparison ran. |

## Remaining Risks

- Maven is unavailable locally, so Java vector generation could not be attempted.
- Java runtime packet captures are still missing.
- Guarded tests do not prove parity until generated Java artifacts exist.
- Artifact schema may need adjustment once the Java generator emits real payloads.
- Full `SM_PET` action coverage remains blocked on common-data/template/feed/mood/doping state.
- `CM_PET_EMOTE` parser/runtime behavior remains unported in C#.
- Live pet hydration, known-list mutation, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 guarded vector artifact reader/comparator plus 2 tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime vector generator, Maven/tooling execution, full `SM_PET` action coverage, `CM_PET_EMOTE` runtime path, live pet/common-data hydration, live known-list dispatch, and socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Implement the lowest-state `SM_PET` packet slices next:

- action-only packets for `TALK_WITH_MERCHANT`, `TALK_WITH_MINDER`, `H_ADOPT`, and `H_ABANDON`; or
- `RENAME` and `SURRENDER(PetCommonData)` packet shapes with source-derived tests.

Keep feed, mood, doping, pet-list/adopt `writePetData`, and live dispatch out of scope until common-data/template snapshots are designed.

## Suggested Parallel Batch For Next Session

Use at most one writer on `SmPet.cs` / `GamePacketTests.cs`.

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | Action-only `SM_PET` packets | `SmPet.cs`, `GamePacketTests.cs`, docs | Low/Medium | Writer only | Lowest state and no common-data snapshots. |
| B | `SM_PET` `RENAME` / `SURRENDER` packet shapes | `SmPet.cs`, `GamePacketTests.cs`, docs | Medium | Not with A | Touches same serializer/test files as A. |
| C | `CM_PET_EMOTE` parser/runtime design | docs/read-only Java/C# inspection | Low | Yes | Good explorer task while orchestrator edits a serializer slice. |
| D | `SkillTargetSlot` audit | abnormal-effect Java/C# surfaces | Low/Medium | Yes | Separate subsystem, useful if pet writer work is small. |
| E | Full `writePetData` design | docs/read-only Java inspection | Medium | Yes | Do before porting pet list/adopt/load packets. |

Recommended next batch: Orchestrator implements Candidate A or B; one read-only explorer handles Candidate C or E.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPetEmote.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetJavaVectorArtifactReaderTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetVectorArtifactReader.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetGoldenVectorDesign.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetEmoteMovementBranches.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
