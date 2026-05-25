# Phase 6UW Completion - UOW-1057 Custom Bonus/Faction Reward Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6UV-Completion.md`.

## Last Completed Unit

UOW-1057: `[Phase 6][UOW-1057] Stage custom level reward plan`

Recent commits before this unit:

- `4cebb36ee [Phase 6][UOW-1056] Stage starter-kit level-change plan`
- `e899c34a7 [Phase 6][UOW-1055] Stage skill auto-learn level-change plan`
- `a748be9c0 [Phase 6][UOW-1054] Stage guide HTML level-change plan`

## Summary

UOW-1057 staged a non-live C# side-effect plan for Java `BonusPackService.addPlayerCustomReward` and `FactionPackService.addPlayerCustomReward`.

The new planner captures level-65 gating, mailbox capacity checks, one-per-account DAO load/store outcomes, Java reward item constants, faction account creation windows, opposite-race item-template skips, and express system-mail intent. It does not write `bonus_packs` / `faction_packs`, send live mail, persist attachments, increment mailbox counts, or compose into XP/enter-world execution.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| Bonus custom reward plan | New custom reward planner/tests, XP docs | Shares docs and mail/DAO concepts with faction | Selected with faction |
| Faction custom reward plan | New custom reward planner/tests, XP docs | Shares docs and mail/DAO concepts with bonus | Selected with bonus |
| Compose sub-plans into XP metadata | `QuestXpExecutionPlanService`, existing sub-plan APIs | Shared orchestrator surface; needs context design | Defer |
| Mail/DAO live prerequisites | Mail repository/service, bonus/faction repositories | Broader persistence/failure-ordering scope | Defer |

No subagents were used. File ownership stayed with the main orchestrator because the selected unit touched shared migration docs and a combined planner/test pair.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CustomLevelRewardPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CustomLevelRewardPlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UW-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CustomLevelRewardPlanServiceTests" --nologo` | Passed: 4 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1850 |

## Migration Parity Table - UOW-1057

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange`; `com.aionemu.gameserver.services.player.PlayerEnterWorldService` | `Aion.GameServer.Services.QuestXpExecutionPlanService`; `Aion.GameServer.Services.CustomLevelRewardPlanService` | Controller / Enter-World Dependency | Partial | Unit Tested as metadata | Needs Verification | Java invokes bonus and faction reward services from level-change and enter-world paths. C# now has standalone non-live sub-plans, but they are not composed into XP execution or enter-world execution. |
| `com.aionemu.gameserver.services.BonusPackService.addPlayerCustomReward` | `Aion.GameServer.Services.CustomLevelRewardPlanService.CreateBonusPackPlan`; `CustomLevelRewardPlan` | Service Side-Effect Plan | Partial | Unit Tested | Partial Parity | Non-live plan records level-65 guard, mailbox capacity, one-per-account DAO load/store outcomes, Java reward constants, and express system-mail intent. It does not write `bonus_packs`, create live letters, persist items, increment mailbox counts, or send packets. Java `HashMap` iteration order is not claimed as verified runtime parity. |
| `com.aionemu.gameserver.services.FactionPackService.addPlayerCustomReward`; `sendRewards` | `Aion.GameServer.Services.CustomLevelRewardPlanService.CreateFactionPackPlan`; `CustomLevelRewardDescriptor` | Service Side-Effect Plan | Partial | Unit Tested | Partial Parity | Non-live plan records level-65 guard, mailbox capacity, Elyos/Asmodian creation windows, one-per-account DAO gates, faction reward constants, opposite-race item-template skips, and express system-mail intent. It does not write `faction_packs`, send mail, or perform live `ServerTime.ofEpochMilli` conversion. |
| `com.aionemu.gameserver.dao.BonusPackDAO` | `CustomLevelRewardPlan.ReceivedPlayerId`; `CustomLevelRewardPlan.StoreReceivingPlayerSucceeded` | Repository Dependency | Not Started | Unit Tested as planner inputs | Needs Verification | C# has no bonus-pack repository. Planner models Java `loadReceivingPlayer(accountId) > 0` and failed `storeReceivingPlayer` outcomes as explicit inputs. SQL, exception fallback to `Integer.MAX_VALUE`, transaction behavior, and `REPLACE INTO bonus_packs` are unported. |
| `com.aionemu.gameserver.dao.FactionPackDAO` | `CustomLevelRewardPlan.ReceivedPlayerId`; `CustomLevelRewardPlan.StoreReceivingPlayerSucceeded` | Repository Dependency | Not Started | Unit Tested as planner inputs | Needs Verification | C# has no faction-pack repository. Planner models Java `loadReceivingPlayer(accountId) > 0` and failed `storeReceivingPlayer` outcomes as explicit inputs. SQL, exception fallback to `Integer.MAX_VALUE`, transaction behavior, and `REPLACE INTO faction_packs` are unported. |
| `com.aionemu.gameserver.model.templates.rewards.RewardItem` | `Aion.GameServer.Services.CustomLevelRewardItem` | DTO / Reward Item | Partial | Unit Tested | Partial Parity | C# DTO records item id/count for bonus and faction rewards. It is not a full Java JAXB reward-template equivalent and does not validate all item templates. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA`; `com.aionemu.gameserver.model.templates.item.ItemTemplate.getRace` | `Aion.GameServer.Dataholders.ItemTemplateTable`; `ItemTemplateSummary.Race` | Static Data Dependency | Partial | Unit Tested with supplied templates | Needs Verification | Faction planner can skip opposite-race reward items when a caller supplies item templates. Full static XML load parity and Java null-template behavior across real data need verification. |
| `com.aionemu.gameserver.utils.time.ServerTime.ofEpochMilli` | `CustomLevelRewardPlan.AccountCreationLocalTime` | Date/Time Dependency | Partial | Unit Tested with local DateTime inputs | Needs Verification | Planner accepts already-converted local account creation time to preserve Java window comparisons. Epoch-millis conversion, configured server timezone, DST behavior, and account model plumbing remain unverified. |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | `CustomLevelRewardDescriptor` metadata | Mail Service Dependency | Not Started | Unit Tested as metadata | Needs Verification | Planner records sender/title/body/item/count/express intent only. It does not create `Letter`, attach items, store mail, update online/offline mailbox counts, or verify Java mail SQL/fanout behavior. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `CustomLevelRewardPlanServiceTests.CreateBonusPackPlan_StagesJavaBonusPackMailRewards` | Level-65 success path, Java bonus reward constants, express mail metadata, and non-live descriptors. | Static Java source comparison against `BonusPackService`. |
| `CustomLevelRewardPlanServiceTests.CreateBonusPackPlan_RecordsJavaGuardBranches` | Wrong level, mailbox capacity, already-received DAO result, store failure, and missing-player guard statuses. | Source-reviewed Java guards plus explicit C# planner inputs. |
| `CustomLevelRewardPlanServiceTests.CreateFactionPackPlan_StagesWindowAndOppositeRaceTemplateFiltering` | Asmodian creation-window success, Java faction reward constants, opposite-race item-template skip, and faction mail metadata. | Static Java source comparison against `FactionPackService`. |
| `CustomLevelRewardPlanServiceTests.CreateFactionPackPlan_RecordsCreationWindowDaoAndCapacityBranches` | Elyos/asmodian creation-window skips, mailbox capacity, already-received DAO result, and store failure. | Source-reviewed Java guards plus explicit C# local DateTime inputs. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The custom reward plans are not composed into `QuestXpExecutionPlan` and are not connected to enter-world execution.
- Live `BonusPackDAO` / `FactionPackDAO` SQL, exception fallback behavior, and transaction boundaries are not ported.
- Live `SystemMailService.sendMail`, letter persistence, attached item persistence, mailbox-count updates, online recipient packet fanout, and failure handling remain unported for custom rewards.
- Faction pack date/time parity needs a real epoch-millis to server-local conversion bridge; the current planner accepts already-converted local times.
- Java `BonusPackService` uses `HashMap`; C# exposes deterministic reward order for tests but does not claim Java runtime mail order parity.
- Faction item-template race filtering depends on caller-supplied C# templates; full XML/static-data loading and real reward item races remain unverified.
- Threading, serialization, reflection, precision/rounding, and persistence parity remain unverified for this side effect.

## Summary Metrics

- Total Java artifacts discovered: 9 in this unit
- Total artifacts ported: 1 non-live custom level reward side-effect planner covering bonus and faction reward branches plus reward descriptor metadata
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 7 categories: Java runtime comparison, live XP/enter-world composition, bonus/faction DAO repositories, live system-mail execution, mail/item persistence, item-template/static-data validation, and server-time conversion
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Compose one existing non-live level-change sub-plan into XP execution metadata only after choosing a stable context shape for player/account/DAO/static-data inputs, or add concrete mail/DAO prerequisites for starter/custom reward delivery. Keep live XP execution disabled.
