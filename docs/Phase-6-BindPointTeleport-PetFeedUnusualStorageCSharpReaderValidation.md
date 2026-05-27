# Phase 6 - Pet Feed Unusual Storage C# Reader Validation

Date: May 27, 2026
Unit of Work: UOW-1384

## Scope

This unit updates the guarded C# schema-v1 reader tests for unusual-storage Java artifacts so they understand the current Java writer shape. It does not enable warehouse-add byte comparison, require generated Java artifacts in the default test run, change Java source, or enable live unusual-storage dispatch.

Java remains the source of truth. Source breadcrumbs used in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeArtifactSchema.md`
- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeActivationPlan.md`

## Implementation

`PetFeedUnusualStorageJavaVectorArtifactReaderTests` now validates the new schema-v1 fields using its synthetic in-test artifact:

- full Java packet class names are accepted, while simple names remain supported by helper matching;
- `itemBlob.hex` includes the Java two-byte blob size prefix;
- `itemBlob.packetBodyVerification` is parsed and restricted to `matched`, `mismatched`, or `unavailable`;
- warehouse-add and cube-update packet `bodyHex` must be present in the schema sample;
- current unusual-storage `canonicalPayloadHex` is expected to equal `bodyHex`;
- cube-update body/canonical comparison remains active against the C# `SmCubeUpdate` helper;
- warehouse-add byte comparison remains guarded because full C# item-blob parity is not verified.

## Migration Parity Table - UOW-1384

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Artifact Schema / Test Reader | Partial | Unit Tested | Needs Verification | C# reader test now parses `itemBlob.hex`, `itemBlob.packetBodyVerification`, full Java packet class names, and body/canonical hex. Uses a synthetic artifact, not generated Java runtime output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet / Test Reader Dependency | Partial | Unit Tested | Needs Verification | Test validates decoded route metadata and presence of warehouse body/canonical hex, but warehouse-add byte comparison remains guarded by item-blob serializer gaps. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Packet / Test Reader Dependency | Partial | Unit Tested | Partial Parity | Test compares unusual-storage zero-size cube-update body/canonical fields against the C# helper for the synthetic artifact. Java runtime artifact comparison is still missing. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ParseUnusualStorageArtifact_ReadsSchemaV1PacketAndBlobFields` | Unit | Java capture schema/source review plus runtime activation plan | Parses schema-v1 fields including blob hex, packet-body verification status, full Java packet class names, body hex, and canonical hex. | Synthetic schema sample validates C# reader shape. | Does not use generated Java runtime output and does not compare warehouse-add bytes. |
| `FindUnusualStorageJavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Unit / Guarded Artifact Test | Java artifact output plan | Keeps generated artifact ingestion guarded when no Java artifacts exist and applies reader semantics when artifacts are present. | Guarded test pass; reports Needs Verification when no artifacts exist. | No Java artifacts are present locally. |

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~PetFeedUnusualStorageJavaVectorArtifactReaderTests --no-restore`.
- Result: Passed, 2 tests.
- Ran `git diff --check`.

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Reader validation uses a synthetic artifact, so it proves C# schema parsing only, not Java runtime parity.
- Warehouse-add byte comparison remains guarded until a real Java artifact exists and item-blob serializer gaps are closed.
- C# item-blob behavior for `STAT_BONUSES`, plume tempering stats, cleanup/seal static data, idian values, and time-dependent fields still needs verification.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 C# guarded artifact reader validation update
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, warehouse-add byte comparison, item-blob serializer gap closure
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only C# item-blob serializer gap audit focused on the fields that still block warehouse-add byte comparison: `STAT_BONUSES`, plume tempering stats, cleanup/seal static data, idian values, identification-dependent premium/enchant fields, and time-dependent expiration/dye values.
