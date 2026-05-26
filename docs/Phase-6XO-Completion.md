# Phase 6XO Completion - UOW-1127 NPC Dialog Interaction Planner

Date: May 26, 2026

## Unit Of Work

UOW-1127: `[Phase 6][UOW-1127] Model NPC dialog interaction restrictions`

## Summary

UOW-1127 adds a non-live planner for Java `DialogService.isInteractionAllowed`, plus static-data support for `TalkInfo.subdialog_type` and `subdialog_value`. The planner models summon-owner restrictions before sub-dialog restrictions and keeps all live dependencies as explicit input facts.

This is staged only. It does not call production `GameServerConnection`, live group/alliance/legion resolvers, zone or siege services, skill/inventory/ranking lookups, audit logging, or packet sends.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/NpcTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogInteractionAllowedPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogInteractionAllowedPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XO-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogInteractionAllowedPlanServiceTests\|StaticDataLoadingTests" --nologo` | Passed: 39 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,265 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1127

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `Aion.GameServer.Services.NpcDialogInteractionAllowedPlanService` | Service Guard Planner | Partial | Unit Tested | Partial Parity | Guard order and decision branches are modeled from explicit input facts. No live player/NPC/world lookup, logging, packet side effects, or Java runtime comparison. |
| `com.aionemu.gameserver.services.DialogService.isSummonOwner` | `NpcDialogInteractionAllowedPlanService.IsSummonOwner` | Service Guard Dependency | Partial | Unit Tested | Needs Verification | `PRIVATE`, `GROUP`, `ALLIANCE`, and `LEGION` owner checks are represented from explicit facts. C# does not yet resolve actual group/alliance/legion membership from live player state. |
| `com.aionemu.gameserver.services.DialogService.isSubDialogRestricted` | `NpcDialogInteractionAllowedPlanService.CreatePlan` | Service Guard Dependency | Partial | Unit Tested | Partial Parity | Java switch branches are represented as non-live planner decisions. Live fort-zone discovery, SiegeService, skill list, inventory, abyss ranking cache, legion dominion service, and warning logs remain disabled. |
| `com.aionemu.gameserver.model.templates.npc.TalkInfo` | `NpcTemplateSummary.SubDialogType`; `NpcTemplateSummary.SubDialogValue` | Static DTO | Partial | Static Data Tested | Needs Verification | XML loader now captures `subdialog_type` and `subdialog_value`. Unknown enum behavior is lenient in C# and not Java JAXB-verified. |
| `com.aionemu.gameserver.model.templates.npc.SubDialogType` | `Aion.GameServer.Dataholders.NpcSubDialogType` | Enum | Partial | Unit Tested + Static Data Tested | Needs Verification | All Java enum names are represented with C# names. Serialization/XML string mapping is one-way loader-only and not golden-file compared. |
| `com.aionemu.gameserver.skillengine.effect.SummonOwner` | `Aion.GameServer.Services.NpcDialogSummonOwnerType` | Enum / Service Input | Partial | Unit Tested | Needs Verification | All Java owner enum values are represented as planner inputs. No live NPC `getSummonOwner` or creator lookup is wired. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcDialogInteractionAllowedPlanServiceTests.CreatePlan_AllowsNpcWithoutSummonOwnerOrSubDialog` | No summon owner and no sub-dialog type allows interaction. | Source-reviewed Java null checks. |
| `NpcDialogInteractionAllowedPlanServiceTests.CreatePlan_AppliesSummonOwnerRulesBeforeSubDialog` | PRIVATE/GROUP/ALLIANCE/LEGION owner rules and guard ordering. | Source-reviewed Java switch. |
| `NpcDialogInteractionAllowedPlanServiceTests.CreatePlan_RejectsSubDialogWhenFortCaptureFactsDoNotMatch` | FORT_CAPTURE rejects when fort ownership facts do not match. | Source-reviewed Java branch. |
| `NpcDialogInteractionAllowedPlanServiceTests.CreatePlan_RejectsMissingOrUnhandledSubDialogs` | Missing skill/item/return item and unhandled subdialog types reject. | Source-reviewed Java switch/default. |
| `NpcDialogInteractionAllowedPlanServiceTests.CreatePlan_AppliesRankRankingAndLevelRestrictions` | Abyss rank, ranking, and level comparisons match Java inequality directions. | Source-reviewed Java comparisons. |
| `NpcDialogInteractionAllowedPlanServiceTests.CreatePlan_AppliesLegionDominionRestrictions` | Legion dominion restrictions use current/occupied dominion facts and calculation-time blocker. | Source-reviewed Java branches. |
| `StaticDataLoadingTests.LoadsStaticDataFromXmlDirectory` | Real static-data NPC `832011` loads `LEVEL_LOW` and value `39`. | Deterministic C# XML load coverage; no Java runtime comparison. |

## Remaining Risks

- `NpcDialogInteractionAllowedPlanService` is not wired into `QuestDialogNpcTargetBranchInputAssemblyPlanService` or production `GameServerConnection`.
- The planner consumes explicit facts instead of live group/alliance/legion, fort-zone, SiegeService, skill, inventory, abyss ranking, legion dominion, and player-level state.
- C# `NpcSubDialogType` parsing is lenient for unknown values; Java JAXB enum behavior and load-time failure/warning behavior remain unverified.
- Java logging for missing fort zones and unhandled sub-dialog types is represented only through status/java-source metadata.
- Threading/player-ordering, packet side effects, and Java runtime comparison remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live interaction planner plus 1 partial static DTO/enum extension
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: live membership resolvers, live zone/SiegeService lookup, live skill/inventory/ranking/legion-dominion lookups, production socket routing, Java XML enum behavior, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds the staged interaction guard needed by the NPC-target dialog branch.

## Next Recommended Unit Of Work

Compose `NpcDialogInteractionAllowedPlanService` into `QuestDialogNpcTargetBranchInputAssemblyPlanService` as an optional non-live interaction-plan dependency, so the NPC-target branch can consume the same explicit interaction facts while production routing remains disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose interaction planner into NPC dialog input adapter | `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs` and its tests | Medium | Recommended next; keep non-live. |
| B | NpcController dispatch audit | Read-only audit doc | Medium | Map talk-range, AI, and `DialogService` fallback dependencies. |
| C | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java unknown-action warning behavior before adding loader warnings. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Static-data loader and NPC template metadata paths: avoid concurrent edits with dialog action or TalkInfo work.
- Phase 6 progress/handoff docs: orchestrator-owned.
