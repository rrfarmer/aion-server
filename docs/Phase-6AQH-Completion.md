# Phase 6AQH Completion - XP Nearby StaticData Bridge

Date: 2026-05-28
Unit of Work: UOW-1614
Status: Complete after focused validation

## Scope

This unit connected UOW-1613's `StaticData.NearbyQuestTemplates` to an existing non-live XP level-change composition boundary. Java `PlayerController.onLevelChange` calls `updateNearbyQuests` after level-change side effects; C# already stages that as a `NearbyQuestRefreshPlan`. The new overload lets the context factory source nearby quest templates from `StaticData` when explicit templates are not supplied.

This remains metadata only. No production level-change hook, nearby packet send, quest-start mutation, quest-finish execution, or repository write was enabled.

## Completed Work

- Added `QuestXpLevelChangeContextFactoryService.CreateContext(Player?, QuestXpLevelChangeContextFactoryInput, StaticData?)`.
- Used `StaticData.NearbyQuestTemplates` as the fallback template source for the existing nearby-refresh sub-plan.
- Preserved explicit `NearbyQuestTemplates` input behavior.
- Added `CreateContext_WithStaticDataUsesNearbyQuestTemplatesWithoutLiveDispatch`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused XP/nearby tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestXpLevelChangeContextFactoryServiceTests|FullyQualifiedName~QuestFinishRewardProjectionStaticDataBridgeTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~QuestXpExecutionPlanServiceTests"
```

Result: 21 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| XP level-change nearby StaticData composition | context factory service/test | Low | Yes | Directly consumes UOW-1613 static data without live sends. |
| Guarded nearby-refresh input plan | possible new small service/test | Medium | No | Best next nearby follow-up. |
| ItemCharge storage-location audit | read-only Java/C# charge files | Low | No | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/observer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because the service/test pair is small and docs remain orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateContext_WithStaticDataUsesNearbyQuestTemplatesWithoutLiveDispatch` | Added | StaticData fallback supplies nearby templates to XP level-change context; event quest references are rejected as missing templates; context remains non-live metadata. | Source-derived from Java `PlayerController.onLevelChange -> updateNearbyQuests` and `QuestsData` scope. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `Aion.GameServer.Services.QuestXpLevelChangeContextFactoryService.CreateContext` | Controller / Level Change Composition | Partial | Unit Tested | Partial Parity | Context factory can source nearby templates from `StaticData` for the staged `updateNearbyQuests` sub-plan. Live player stat mutation, controller invocation, and production event ordering remain disabled. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshPlanService` consumed by `QuestXpLevelChangeContextFactoryService` | Controller / Nearby Refresh Plan | Partial | Unit Tested | Needs Verification | The new bridge creates non-live refresh metadata from real static quest templates. It does not send `SM_NEARBY_QUESTS` or resolve map regions from production world state. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.StaticData.NearbyQuestTemplates` | Static Data Repository | Partial | Unit Tested through context bridge | Partial Parity | StaticData-backed nearby templates are consumed when explicit templates are absent. Full Java `QuestTemplate` graph/JAXB/script behavior remains incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests` | Packet Dependency | Partial | Existing Regression Tested | Needs Verification | Context marks packet intent only. No live send, byte comparison, encryption/frame validation, or socket ordering was executed. |

## Remaining Risks

- XP level-change execution remains non-live metadata; production `PlayerController.onLevelChange` is not wired.
- Live nearby quest sends, real map-region/world-instance lookup, and socket packet ordering remain disabled.
- Full Java quest template object parity, JAXB lifecycle, script hooks, and unsupported fields remain incomplete.
- Threading, reflection/dynamic handler behavior, date/time handling, packet bytes, and serialization remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 1 non-live StaticData-to-XP-context composition bridge plus 1 focused unit regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live nearby refresh dispatch, production level-change hook, map-region/world-instance lookup, full Java quest-template graph, packet-byte comparison, runtime Java comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Guarded nearby-refresh input adapter | new small service/test or nearby refresh service tests | Accept `Player`, optional world instance, and `StaticData`; return non-live `NearbyQuestRefreshPlan`. |
| ItemCharge storage-location audit | read-only Java `Inventory`/`Equipment` lifecycle and C# selected-charge lookup | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1614] Compose nearby templates into XP context
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/QuestXpLevelChangeContextFactoryService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestXpLevelChangeContextFactoryServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQH-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
