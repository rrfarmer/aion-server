# Phase 6HD Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HC and covers Session 700.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "DataManager_LoadsRealJavaStaticDataManifestCounts|StaticData_LoadsStorageExpansionTemplatesByNpcId"`
  - Result: Passed, 2 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1267 tests.

## Recent Work Completed

### Session 700 - Real Storage Expander Static Data Coverage

- Extended `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` with real repository XML assertions for storage expansion data.
- Source files pinned:
  - `game-server/data/static_data/storage_expander/cube_expander.xml`,
  - `game-server/data/static_data/storage_expander/warehouse_expander.xml`.
- Asserted real-data counts:
  - merged `expansion_npc` element count: `6`,
  - cube template group count: `3`,
  - cube flattened NPC-id count: `7`,
  - warehouse template group count: `3`,
  - warehouse flattened NPC-id count: `254`.
- Asserted representative cube lookups:
  - NPC `798008`: level `1`, price `1000`,
  - NPC `798011`: levels `1..4`, level `4` price `180000`,
  - NPC `279022`: level `5`, price `360000`.
- Asserted representative warehouse lookups:
  - NPC `203199`: levels `1..5`, level `5` price `363000`,
  - NPC `203221`: levels `2..3`, level `2` price `24000`,
  - NPC `810015`: levels `1..3`, level `3` price `72600`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.CubeExpandData` | `Aion.GameServer.Dataholders.StaticData.CubeExpansionTemplates` / `StorageExpansionTemplateTable` | Dataholder | Partial | Regression Tested | Needs Verification | Real repository XML load now asserts 3 cube template groups and 7 flattened NPC ids, plus representative NPC-id lookups and min/max/price values. Java JAXB runtime execution, duplicate-id overwrite behavior, schema validation, and Java-generated golden count output remain unverified. |
| `com.aionemu.gameserver.dataholders.WarehouseExpandData` | `Aion.GameServer.Dataholders.StaticData.WarehouseExpansionTemplates` / `StorageExpansionTemplateTable` | Dataholder | Partial | Regression Tested | Needs Verification | Real repository XML load now asserts 3 warehouse template groups and 254 flattened NPC ids, plus representative NPC-id lookups and min/max/price values. Java JAXB runtime execution, duplicate-id overwrite behavior, schema validation, and Java-generated golden count output remain unverified. |
| `com.aionemu.gameserver.model.templates.StorageExpansionTemplate` | `Aion.GameServer.Dataholders.StorageExpansionTemplateSummary` | DTO / Static Data Template | Partial | Regression Tested | Needs Verification | Real XML assertions cover NPC id flattening, min/max expansion levels, and price lookup for representative templates. Malformed XML behavior, duplicate level behavior, serialization differences, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.expand.Expand` | `Aion.GameServer.Dataholders.StorageExpansionPrice` | DTO | Partial | Regression Tested | Needs Verification | Real XML assertions cover integer `level` and `price` values for representative cube and warehouse rows. Precision/rounding issues are not expected for int values, but overflow/malformed XML behavior and JAXB comparison remain unverified. |
| `com.aionemu.gameserver.dataholders.DataManager` | `Aion.GameServer.Dataholders.DataManager` / `StaticData.LoadFromCacheAsync` | Static Data Loader | Partial | Regression Tested | Needs Verification | The merged repository static-data load now includes storage-expander count/lookup coverage. Java DataManager startup order, JAXB `afterUnmarshal`, schema validation timing, background validation, threading behavior, and reflection behavior remain unverified for this specific dataholder. |
| `game-server/data/static_data/storage_expander/cube_expander.xml` | `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` assertions | Static XML Source | Partial | Regression Tested | Needs Verification | Source file values are pinned in C# tests. Changes to Java XML will intentionally require C# parity-test updates. No Java runtime load or client behavior was compared. |
| `game-server/data/static_data/storage_expander/warehouse_expander.xml` | `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` assertions | Static XML Source | Partial | Regression Tested | Needs Verification | Source file values are pinned in C# tests. Changes to Java XML will intentionally require C# parity-test updates. No Java runtime load or client behavior was compared. |

## Tests Added Or Updated

- `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts`
  - Validates real storage-expander merged element count, template-group counts, flattened NPC-id lookup counts, representative NPC-id lookups, min/max expansion levels, and prices from repository XML.
  - Source-derived from Java XML and Java dataholder behavior.
  - Does not compare against Java runtime execution, Java-generated golden count output, JAXB runtime behavior, or live client behavior.
- `StaticDataLoadingTests.StaticData_LoadsStorageExpansionTemplatesByNpcId`
  - Rerun to preserve focused parser coverage for the compact XML fixture.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 real storage-expander static-data coverage slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: Java JAXB/runtime comparison, duplicate-id runtime comparison, storage limit recalculation, golden/encrypted packet comparison, and live-client/MySQL validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Duplicate NPC id overwrite behavior remains source-inferred but not exercised against Java runtime.
- Java JAXB runtime behavior, schema-validation timing, and DataManager startup ordering remain unverified for storage expander data.
- Storage object cube/warehouse limit recalculation remains represented only by fields and outgoing packets.
- Java golden packet bytes, encrypted frames, localized client rendering, and live client validation remain unperformed.
- Live MySQL write/readback for accepted NPC expansions remains unverified.

## Next Recommended Unit of Work

Continue storage expansion parity by modeling Java `player.setCubeLimit()` / `setWarehouseLimit()` effects beyond outgoing packet fields if the C# storage model can support it, or pivot back to another Phase 6 core gap from `## Next Steps` such as kisk lifecycle cleanup, charge/power-shard/idiani burn hooks, or loot/drop handler-side quest/event paths.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HC-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
