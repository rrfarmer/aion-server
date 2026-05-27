# Phase 6 Bind-Point Teleport Known-List Pet Vector Artifact Reader

Date: May 27, 2026
Unit of Work: UOW-1297
Status: Complete for guarded C# reader/comparator scaffolding; Java runtime vector generation remains blocked locally because Maven is unavailable.

## Scope

This unit adds a guarded C# test reader for future Java known-list pet packet vector artifacts.

The reader does not claim parity by itself. It provides the test-side contract that will compare generated Java `SM_PET` / `SM_PET_EMOTE` artifact bodies against existing C# packet serializers once Java artifacts are present.

No live known-list dispatch, pet hydration, or Java packet generation was enabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java pet packet vector reader/comparator | `SM_PET`, `SM_PET_EMOTE`, `PlayerController.see/notSee` | `PetJavaVectorArtifactReaderTests.cs` | Test Creation | Yes, orchestrator-owned | Medium | Adds guarded evidence path without requiring Java tooling. |
| B | Full `SM_PET` action audit | `SM_PET`, `PetCommonData`, `PetFeedProgress`, `PetDopingBag` | read-only notes | Java Analysis | Yes | Low | Read-only and independent from C# reader work. |
| C | `CM_PET_EMOTE` runtime audit | `CM_PET_EMOTE`, `World`, `CreatureMoveController`, `KnownList` | read-only notes | Java Analysis | Yes | Low | Read-only and independent from C# reader work. |
| D | `SkillTargetSlot` enum audit | abnormal-effect target-slot Java/C# surfaces | docs or isolated enum/test files | Java Analysis / Enum Audit | Yes | Low/Medium | Separate subsystem; deferred to keep this unit pet-focused. |

Selected parallel batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Add guarded pet vector artifact reader/comparator | Test Creation | `dotnetConversion/tests/Aion.GameServer.Tests/PetJavaVectorArtifactReaderTests.cs`; shared docs at integration | Production packet files; Java source writes | Existing `SmPet`, `SmPetEmote`, UOW-1295 vector design | Test reader and docs. |
| Explorer A | Full `SM_PET` action audit | Java Analysis | read-only Java/C# inspection | all writes | none | Branch/dependency map and safest next slices. |
| Explorer B | `CM_PET_EMOTE` runtime audit | Java Analysis | read-only Java inspection | all writes | none | Parser/runtime behavior notes and future C# slice map. |

Both explorers completed read-only and were closed after reporting.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetSpecialFunction.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`

## Behavior Added

Added `PetJavaVectorArtifactReaderTests` with:

- schema-v1 parsing for known-list pet vector artifacts;
- semantic validation for:
  - `SM_PET` `pet-spawn`;
  - `SM_PET_EMOTE` `pet-fly-start`, `pet-move-stop`, and `pet-move-to`;
  - `SM_PET` `pet-dismiss`;
- guarded artifact discovery under `parity-artifacts/known-list-pet/java`;
- optional comparison of generated `bodyHex` and `canonicalPayloadHex` against C# serializers when artifacts are present.

The guarded test logs `Needs Verification` and returns if no generated Java artifacts exist.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetJavaVectorArtifactReader"` passed 2 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetJavaVectorArtifactReader|SmPet|PetActionAndEmoteResolvers|PlayerKnownListPetVisibility"` passed 20 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 380 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed.
- `java -version` reports Java 8 locally.
- `mvn -version` failed because Maven is not available on PATH.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

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

