# Phase 6UV Completion - UOW-1056 Starter Kit Level-Change Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. The Java implementation is still the source of truth, and this handoff continues from `docs/Phase-6UU-Completion.md`.

## Last Completed Unit

UOW-1056: `[Phase 6][UOW-1056] Stage starter-kit level-change plan`

Recent commits before this unit:

- `e899c34a7 [Phase 6][UOW-1055] Stage skill auto-learn level-change plan`
- `a748be9c0 [Phase 6][UOW-1054] Stage guide HTML level-change plan`
- `54ceef72f [Phase 6][UOW-1053] Align nearby quest empty packet intent`

## Summary

UOW-1056 staged a non-live C# side-effect plan for Java `StarterKitService.onLevelUp`.

The new C# planner captures the Java starter-kit config gate, inclusive level traversal, static reward buckets, and the system-mail intent metadata used by `SystemMailService.sendMail`. It does not yet send live letters, persist mailbox state, emit packets, or compose the starter-kit plan into the active XP level-change execution flow.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| Starter kit level-change plan | New planner, focused tests, Phase 6 docs | Safe as a single-lane task; touches shared docs | Selected for UOW-1056 |
| Bonus custom reward audit | Java `BonusPackService`, C# reward planning/services | Can follow after starter-kit; may touch shared XP docs | Recommended next |
| Faction custom reward audit | Java `FactionPackService`, C# reward planning/services | Can follow after starter-kit; may touch shared XP docs | Recommended next |
| Compose staged sub-plans into XP metadata | `QuestXpExecutionPlanService` and tests | Shared orchestrator surface; do separately | Defer |

No subagents were used. File ownership stayed with the main orchestrator because the selected unit touched shared migration docs and a focused planner/test pair.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/StarterKitLevelChangePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StarterKitLevelChangePlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UV-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "StarterKitLevelChangePlanServiceTests" --nologo` | Passed: 3 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1846 |

## Migration Parity Table - UOW-1056

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `Aion.GameServer.Services.QuestXpExecutionPlanService`; `Aion.GameServer.Services.StarterKitLevelChangePlanService` | Controller / Orchestrator | Partial | Unit Tested metadata only | Needs Verification | Java invokes starter-kit rewards during the XP level-change path when `CustomConfig.ENABLE_STARTER_KIT` is true. C# now has a standalone non-live starter-kit sub-plan, but it is not yet composed into the live XP executor. |
| `com.aionemu.gameserver.services.reward.StarterKitService.onLevelUp` | `Aion.GameServer.Services.StarterKitLevelChangePlanService.CreatePlan`; `Aion.GameServer.Services.StarterKitLevelChangePlan` | Service Side-Effect Plan | Partial | Unit Tested | Partial Parity | C# records the Java config gate, inclusive `fromLevel..toLevel` traversal, static reward buckets, sender/title/body metadata, `EXPRESS` letter type, and `SystemMailService.sendMail` intent. Live mail delivery, persistence, packets, and item-template validation are not implemented. |
| `com.aionemu.gameserver.configs.main.CustomConfig.ENABLE_STARTER_KIT` | `Aion.GameServer.Configuration.GameServerCustomOptions.EnableStarterKit`; `Aion.GameServer.Services.StarterKitLevelChangePlan.StarterKitEnabled` | Config Gate | Partial | Unit Tested as planner input | Needs Verification | Config option exists in C# and the planner accepts an explicit gate value. Live configuration binding into player level-change execution remains unwired for starter-kit behavior. |
| `com.aionemu.gameserver.model.templates.rewards.RewardItem` | `Aion.GameServer.Services.StarterKitRewardItem` | DTO | Partial | Unit Tested | Partial Parity | C# DTO records item id and count needed by the Java starter-kit buckets. This is not a full JAXB/template port of Java `RewardItem`. |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | `Aion.GameServer.Services.StarterKitLevelChangeDescriptor` metadata | Mail Service Dependency | Not Started | Unit Tested as metadata only | Needs Verification | C# captures intended mail sends only. Missing live `SystemMailService` equivalent, mailbox persistence, express-mail packet behavior, attachment handling, and failure behavior. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `StarterKitLevelChangePlanServiceTests.CreatePlan_StagesJavaStarterKitLevelBucketsInInclusiveOrder` | Enabled planner emits Java level 20 and 25 reward buckets in inclusive level order, with Java mail metadata and source breadcrumb. | Static Java source comparison against `StarterKitService.onLevelUp` buckets and metadata. |
| `StarterKitLevelChangePlanServiceTests.CreatePlan_RecordsDisabledEmptyNoMatchAndMissingPlayerBranches` | Disabled config, empty level range, no matching starter-kit levels, and missing player branches stay explicit and non-live. | Mirrors Java config guard plus documents C# planner-only safety branches. Missing-player handling is a C# planner guard, not a Java behavior claim. |
| `StarterKitLevelChangePlanServiceTests.RewardBuckets_MatchJavaStarterKitStaticItems` | Static C# starter-kit buckets expose the Java level keys and representative level 1 / level 60 item-count pairs. | Static Java source comparison against `StarterKitService` constructor buckets. |

## Remaining Risks

- Starter-kit mail sends are not live. The C# port still lacks a verified `SystemMailService.sendMail` equivalent for starter-kit attachments.
- The starter-kit sub-plan is not composed into `QuestXpExecutionPlanService` or a live player level-change executor.
- No Java runtime comparison was executed; parity is based on deterministic source inspection and unit tests over copied constants.
- Item template validity, inventory/mailbox capacity side effects, database persistence, packet emission, and express-letter timing remain unverified.
- The Java level loop includes `fromLevel` through `toLevel`; C# planner mirrors that inclusive behavior, but runtime caller semantics still need verification when wired.
- Date/time behavior is not involved in this starter-kit planner. Threading behavior is not live yet because no async/persistence dispatch is implemented.
- Serialization/reflection differences are not applicable to the current DTO-only planner, but full Java `RewardItem` template serialization remains unported.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 non-live starter-kit level-change side-effect sub-plan plus reward-item metadata
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 categories: Java runtime comparison, live XP composition, live system-mail execution, mail/item persistence, item-template validation
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Audit and stage the remaining Java `PlayerController.onLevelChange` custom reward branches:

1. `BonusPackService.addPlayerCustomReward`
2. `FactionPackService.addPlayerCustomReward`

Recommended approach:

- Read the Java services and their DAO/config dependencies first.
- Create non-live C# planners for reward intent if live inventory/mail delivery dependencies are still missing.
- Add focused tests that compare Java level/faction/config gates and reward descriptors.
- Update `docs/PHASE-6-PROGRESS.md`, `docs/QuestXpReward-Audit.md`, and `docs/QuestRewardSideEffects-Audit.md`.
- Generate the next handoff document after the unit.

Do not parallelize shared edits to `QuestXpExecutionPlanService`, migration docs, or any live mail/reward service wiring with reward-branch planner work.
