# Phase 6AQG Completion - StaticData Nearby Quest Templates

Date: 2026-05-28
Unit of Work: UOW-1613
Status: Complete after focused validation

## Scope

This unit exposed nearby quest template summaries through `StaticData` so future nearby-refresh composition can use real Java quest-template data without enabling live `SM_NEARBY_QUESTS` sends. It also tightened the extractor to Java's `QuestsData` holder shape: only `<quest>` elements directly under `<quests>` are quest templates, while merged-cache event quest references are ignored.

This is a static-data prerequisite only. It does not wire production `PlayerController.updateNearbyQuests`, quest-start mutation, quest-finish reward execution, or live packet dispatch.

## Completed Work

- Added `StaticData.NearbyQuestTemplates`.
- Loaded `NearbyQuestTemplateTable` from the merged static-data cache in `StaticData.LoadFromCacheAsync`.
- Tightened `NearbyQuestTemplateXmlExtractor` to direct `<quests><quest>` rows.
- Updated fixture and real-data tests proving event `<quest>` references are ignored and 8,043 real quest templates are exposed.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused nearby/static-data tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestFinishRewardProjectionStaticDataBridgeTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~QuestFinishSocketGuardedInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests"
```

Result: 24 passed, 0 failed.

Additional static-data smoke passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~QuestFinishRewardProjectionStaticDataBridgeTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests"
```

Result: 27 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Nearby quest template StaticData exposure | `StaticData.cs`, nearby extractor/tests | Medium | Yes | Moves nearby-refresh prerequisites forward without live sends. |
| ItemCharge storage-location audit | read-only Java/C# charge files | Low | No | Safe supporting work after UOW-1611. |
| ItemPurification repository payload/rollback guard | ItemPurification persistence/execution tests | Medium | No | Separate subsystem; avoid mixing with static-data changes. |
| Java protection serializer implementation | Java serializer/observer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because `StaticData` constructor shape and extractor scope are shared surfaces that need one owner.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `Extract_StreamInputFeedsNearbyQuestTemplateTableAndPredicate` | Updated | Event `<quest>` references in merged static-data XML are ignored; real quest templates under `<quests>` are indexed. | Source-derived from Java `QuestsData` holder structure. |
| `LoadFromCacheAsync_ExposesQuestFinishRewardProjectionLookupTableWithoutSocketWiring` | Updated | `StaticData.NearbyQuestTemplates` exposes only real quest templates and preserves `CanReport` / reward metadata. | Deterministic merged-cache fixture regression. |
| `LoadStaticDataAsync_RealDataExposesQuestFinishRewardProjectionLookupTable` | Updated | Real static data exposes 8,043 nearby quest templates and keeps reward projection counts intact. | C# real XML load; no Java runtime object comparison. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.StaticData.NearbyQuestTemplates`; `NearbyQuestTemplateTable` | Static Data Repository | Partial | Regression Tested | Partial Parity | StaticData now exposes 8,043 quest-template summaries from the merged cache. Full Java `QuestTemplate` graph, script hooks, JAXB lifecycle, and QuestEngine integration remain missing. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `NearbyQuestTemplateXmlExtractor` | Static Template DTO / Extractor | Partial | Unit Tested + Regression Tested | Partial Parity | Extractor scopes templates to direct `<quests><quest>` rows and ignores event quest references. Many Java fields and nested reward details remain unported. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | future consumers of `StaticData.NearbyQuestTemplates`; existing `NearbyQuestRefreshPlanService` | Controller / Nearby Refresh Dependency | Partial | Existing Unit Tested + StaticData Regression Tested | Needs Verification | Real quest-template table is available to future refresh composition, but no live `SM_NEARBY_QUESTS` send or production controller refresh was enabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests` | Packet Dependency | Partial | Existing Regression Tested | Needs Verification | This unit only provides static template data. Packet serialization/live socket ordering was not touched or compared to Java runtime bytes. |

## Remaining Risks

- `StaticData.NearbyQuestTemplates` contains staged summaries, not full Java `QuestTemplate` objects.
- Live `PlayerController.updateNearbyQuests`, production world-instance lookup, and `SM_NEARBY_QUESTS` sends remain disabled.
- Java JAXB defaults/lifecycle, script hooks, full reward/work item content, and unsupported quest-template fields remain incomplete.
- Packet bytes, socket ordering, threading, reflection/dynamic handler behavior, date/time handling, and serialization remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 1 StaticData nearby quest-template exposure plus 1 extractor scope tightening and focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live nearby refresh dispatch, map-region/world-instance lookup, full Java `QuestTemplate` object graph, JAXB runtime comparison, packet-byte comparison, production QuestEngine integration.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Compose `StaticData.NearbyQuestTemplates` into a non-live consumer:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| XP level-change context/static-data adapter | `QuestXpLevelChangeContextFactoryService` or a small wrapper/test | Keep non-live; no production sends. |
| Guarded nearby-refresh input plan | new small service/test or existing nearby refresh tests | Use `StaticData.NearbyQuestTemplates` as explicit input source. |
| ItemCharge storage-location audit | read-only Java `Inventory`/`Equipment` lifecycle and C# selected-charge lookup | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1613] Expose nearby quest templates in static data
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardProjectionStaticDataBridgeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQG-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
