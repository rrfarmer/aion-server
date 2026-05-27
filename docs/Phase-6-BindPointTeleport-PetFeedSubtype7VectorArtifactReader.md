# Phase 6 - Pet Feed Subtype 7 Vector Artifact Reader

Date: May 27, 2026
Unit of Work: UOW-1334

## Scope

This unit adds a guarded C# reader/test shape for future Java runtime-vector artifacts from `docs/Phase-6-BindPointTeleport-PetFeedSubtype7RuntimeVectorDesign.md`.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData`
- `com.aionemu.gameserver.model.gameobjects.player.PetFeedProgress`

## Implemented

- Added `PetFeedSubtype7JavaVectorArtifactReaderTests`.
- Added schema-v1 parsing coverage for future pet-feed subtype `7` Java vector artifacts.
- Validated the expected rewarded-feed semantic sequence:
  - `pet-feed-progress`
  - `pet-feed-reward-item`
  - `pet-feed-end`
  - `end-feeding-emotion`
  - `pet-feed-refeed-notification`
- Validated required decoded `SM_PET` FOOD fields for subtypes `2`, `5`, `6`, and `7`.
- Guarded future artifact discovery under `parity-artifacts/pet-feed-subtype7/java`.
- Compared generated `SM_PET` body/canonical payload hex against C# `SmPet.Food(...)` only when future Java artifacts provide bytes.
- Kept `SM_EMOTION` byte comparison out of scope for this reader; the schema only validates its role in the feed sequence.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader"` passed 2 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 141 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

## Migration Parity Table - UOW-1334

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rewarded full branch | `PetFeedSubtype7JavaVectorArtifactReaderTests` | Artifact Reader / Test | Partial | Unit Tested | Needs Verification | Reader validates future artifact schema and sequence semantics, but no Java runtime artifacts exist yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtypes `2`, `5`, `6`, and `7` | `SmPet.Food`; `PetFeedSubtype7JavaVectorArtifactReaderTests` | Packet / Artifact Comparator | Partial | Unit Tested source-derived | Needs Verification | C# body/canonical comparison runs only when future Java artifact hex is present. Current test validates schema and source-derived field requirements. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` end-feeding packet | `PetFeedSubtype7JavaVectorArtifactReaderTests` schema semantics | Packet / Sequence Marker | Not Started | Unit Tested schema only | Needs Verification | Reader validates semantic position but does not compare `SM_EMOTION` bytes. Concrete emotion vector comparison remains separate work. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.getRefeedDelay` | artifact decoded `refeedDelaySeconds`; `PetCommonDataTiming` | Timing / Artifact Field | Partial | Unit Tested schema only | Needs Verification | Schema requires subtype `7` delay field, but no Java runtime capture determines queue-time vs serialization-time value. |
| `com.aionemu.gameserver.model.gameobjects.player.PetFeedProgress.reset` | artifact pre/post state snapshots; `PetFeedProgress` | Progress / Artifact Field | Partial | Unit Tested schema only | Needs Verification | Schema requires distinct pre/post progress values. Runtime artifacts are still needed to identify what subtype `7` serialized. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ParsePetFeedSubtype7Artifact_ReadsSchemaV1PacketFields` | Unit / Artifact Schema | UOW-1333 vector design and Java packet source review | Parses representative schema-v1 JSON and validates packet sequence, decoded subtype fields, state snapshots, and required subtype `7` zero item/trailing field. | Deterministic schema validation only. | Does not compare generated Java bytes. |
| `FindPetFeedSubtype7JavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Guarded Unit / Artifact Discovery | UOW-1333 vector design | Scans `parity-artifacts/pet-feed-subtype7/java` and validates/compares artifacts when present; reports needs-verification when absent. | Guarded comparison path exists. | No artifacts are present, so no runtime parity is claimed. |

## Remaining Risks

- No Java runtime subtype `7` vector artifacts exist yet.
- `SM_EMOTION` byte comparison is intentionally out of scope for this reader.
- Artifact schema cannot prove Java send-time serialization timing until production Java vectors are generated.
- Java wall-clock and reward randomness still need controlled fixture inputs.
- Live C# scheduler, reward creation, DAO persistence, inventory mutation, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 guarded artifact reader test class with 2 tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime packet observer, generated subtype `7` artifacts, deterministic feed fixture, `SM_EMOTION` byte comparator, live common-data wiring, scheduler, reward creation, DAO persistence, inventory mutation, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Investigate whether the existing Java packet-vector approach can hook production `PacketSendUtility.sendPacket` timing for pet feed packets, or add a small docs-only Java observer implementation plan for subtype `7` artifact generation. Keep live feed dispatch disabled until Java artifacts exist and compare cleanly.
