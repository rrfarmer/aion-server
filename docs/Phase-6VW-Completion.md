# Phase 6VW Completion - UOW-1083 Quest Report Reward Metadata Projection

Date: May 26, 2026

## Unit Of Work

UOW-1083: `[Phase 6][UOW-1083] Project quest report reward metadata`

## Starting Point

- Required context docs were read at session start:
  - `docs/csharp-port.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6VV-Completion.md`
- Phase 6 remains active and large.
- Java remains the source of truth.
- Production quest-finish, reward mutation, XP mutation, custom reward execution, mail execution, persistence, and packet sends must stay disabled unless a later unit explicitly gates and proves them.

## Summary

UOW-1083 adds a narrow static-data prerequisite for the Java `QuestTemplate.can_report` branch and coarse reward/work metadata needed before the C# dialog auto-reward guard can consume real quest data.

The C# nearby quest template summary now records:

1. `CanReport`;
2. `RewardRepeatCount`;
3. `HasRewards`;
4. `HasExtendedRewards`;
5. `HasBonus`;
6. `HasQuestWorkItems`.

The extractor reads these values from Java-shaped quest XML. Tests verify fixture fields, default values, and real-data counts. No live gameplay path was enabled.

## Parallel Work Discovery

Reviewed candidates:

| Option | Candidate | Files | Risk | Notes |
|---|---|---|---|---|
| A | Static quest `can_report` and reward metadata projection prerequisite | `NearbyQuestTemplateXmlExtractor.cs`; `NearbyQuestTemplateTable.cs`; `NearbyQuestTemplateXmlExtractorTests.cs` | Low/Medium | Selected. Static-data-only and useful for the next guarded composition step. |
| B | Mail list packet splitting tests | Mail packet tests | Low | Safe later packet coverage, but less directly connected to current quest-finish guard path. |
| C | Additional date/time conversion vectors | custom reward runtime input tests | Low/Medium | Useful later; avoid named-zone assumptions without runtime comparison. |
| D | Opt-in system-mail DB hardening | system-mail DB integration tests | Medium | Valuable but touches DB behavior rather than current static-data prerequisite. |
| E | Full account aggregate read-only analysis | docs/runtime input audit | Low | Deferred; current blocker was static quest metadata. |

No sub-agents were spawned. The orchestrator owned the selected extractor/test/docs files.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VW-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NearbyQuestTemplateXmlExtractorTests" --nologo` | Passed: 5 tests after correcting the real-data `rewards` expectation to 7,935 direct child elements. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1,926 tests. |

## Migration Parity Table - UOW-1083

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor` | Static Quest Template DTO / Extractor | Partial | Unit Tested / Regression Tested | Partial Parity | Adds `can_report`, `reward_repeat_count`, and coarse direct-child presence for `rewards`, `extended_rewards`, `bonus`, and `quest_work_items`. It does not project actual reward values, reward items, class/race filters, selectable rewards, target NPC context, JAXB list defaults beyond presence, serialization behavior, or production static lookup. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `NearbyQuestTemplateSummary.HasRewards`; `NearbyQuestTemplateSummary.HasExtendedRewards`; `NearbyQuestTemplateSummary.HasBonus` | Reward Template Dependency | Partial | Unit Tested as presence only / Regression Tested | Needs Verification | Only direct child presence is projected. Numeric fields, reward item lists, selectable rewards, extended reward contents, bonus contents, precision/rounding, and reward-group selection remain unported. |
| `com.aionemu.gameserver.model.templates.quest.QuestWorkItems` | `NearbyQuestTemplateSummary.HasQuestWorkItems` | Work Item Dependency | Partial | Unit Tested as presence only / Regression Tested | Needs Verification | Only direct child presence is projected. Work item ids, counts, item removal behavior, inventory mutation, and failure ordering remain unported. |
| `com.aionemu.gameserver.dataholders.QuestsData.afterUnmarshal` | `Aion.GameServer.Dataholders.NearbyQuestTemplateTable` | Static Data Index | Partial | Regression Tested | Needs Verification | Existing table continues indexing 8,043 quest summaries by quest id and now carries extra summary metadata. Duplicate/null behavior, JAXB lifecycle behavior, and production loader wiring remain only partially represented. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | Future `QuestDialogAutoRewardGuardPlanService` input from `NearbyQuestTemplateSummary.CanReport` | Socket Guard Dependency | Partial | No Tests in this unit | Needs Verification | The real static `CanReport` source now exists, but production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner or operation planner. Threading and packet ordering remain untouched. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Fixture XML reads `can_report`, `reward_repeat_count`, and direct child presence for reward/work metadata. | Source-reviewed from Java `QuestTemplate` XML/JAXB shape; no Java runtime comparison. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Missing attributes/children default to `false`/`0` in the C# summary. | Conservative C# projection default, not runtime-verified against JAXB. |
| `NearbyQuestTemplateXmlExtractorTests.RealDataAudit_LoadsNearbyQuestTemplateSummariesWithoutProductionWiring` | Current real XML counts: 8,043 templates, 64 `can_report`, 241 reward-repeat, 7,935 `rewards`, 235 `extended_rewards`, 782 `bonus`, and 1,630 `quest_work_items`. | Real Java XML loaded through C# extractor; not Java runtime object comparison. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
- `QuestDialogAutoRewardGuardPlanService` still consumes explicit inputs and does not read `NearbyQuestTemplateTable`.
- Full `QuestFinishRewardTemplateProjection` construction from Java static quest data remains missing.
- Direct child presence flags are not sufficient for live reward/work-item execution.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Custom reward receipt/mail execution, packet ordering, and persistence remain gated and unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial static quest summary/extractor slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/partial categories: production socket integration, full static reward projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a narrow adapter from `NearbyQuestTemplateSummary` into dialog guard/planner prerequisite metadata so real `CanReport` and coarse reward/work availability can replace explicit mock inputs while full reward projection remains blocked.

Keep production quest-finish/custom reward execution disabled.

## Next Session Start Checklist

1. Read `docs/csharp-port.md`.
2. Read `docs/PHASE-6-PROGRESS.md`.
3. Read this handoff, `docs/Phase-6VW-Completion.md`.
4. Inspect `NearbyQuestTemplateSummary`, `NearbyQuestTemplateXmlExtractor`, and `QuestDialogAutoRewardGuardPlanService`.
5. Implement only a non-live adapter/composition slice unless a later handoff explicitly proves live reward mutation readiness.
