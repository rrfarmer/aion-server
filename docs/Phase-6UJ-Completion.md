# Phase 6UJ Completion - UOW-1044 Quest XP Reward Planner Scaffold

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UI-Completion.md`.

## Last Completed Unit

- UOW-1044: `[Phase 6][UOW-1044] Add quest XP reward planner scaffold`
- Commits in this continuation before this handoff:
  - `e5b71f470 [Phase 6][UOW-1040] Add quest GP reward helper`
  - `9c6aee9ef [Phase 6][UOW-1041] Compose quest GP reward metadata`
  - `cdc294cac [Phase 6][UOW-1042] Add offline GP DAO boundary`
  - `212b9fda5 [Phase 6][UOW-1043] Gate offline GP execution`
- UOW-1044 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1044 adds a non-live quest XP reward planner. It source-reviews Java `QuestService.giveReward`, `PlayerCommonData.addExp`, `PlayerCommonData.setExp`, and `Rates.XP_QUEST`, then ports the deterministic planning surface into C# without enabling live XP mutation. The helper records rate, repose, salvation, level-cap, ascension-limit, message-kind, stat-update, and level-change intent metadata. Quest finish still does not compose or execute XP mutation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | XP rate/config/planner scaffold | `QuestService.giveReward`, `PlayerCommonData.addExp`, `PlayerCommonData.setExp`, `Rates.XP_QUEST` | `QuestRewardService.cs`, `GameServerOptions.cs`, focused tests | Service Port | No with other reward-service edits | Medium | Shared reward helper and config surfaces; orchestrator-owned. |
| B | XP message/packet detail audit | `SM_SYSTEM_MESSAGE`, `SM_STATUPDATE_EXP` | read-only or audit doc | Java Analysis | Yes if read-only | Medium | Packet ids/order are independent from planner state if no writes occur. |
| C | Compose XP metadata into quest-finish operations | `QuestService.giveReward` reward order | quest operation planner files | Service Port | Not with A | Medium | Shared quest-finish ordering surface. |
| D | Offline GP opt-in adapter | `GloryPointsService.addGp`, `AbyssRankDAO.addGp` | GP service/repository tests | Service Port | Yes only if files isolated from XP | Medium | Separate GP files, but shared docs must remain orchestrator-owned. |

## Selected Work

No write sub-agent was used for UOW-1044. The previous read-only XP explorer findings from UOW-1043 were integrated by the orchestrator.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestRewardService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestRewardServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UJ-Completion.md`

## What Changed

- Added `GameServerRateOptions.XpQuestRates`.
- Added config load for `gameserver.rates.xp.quest`, defaulting to `1.0, 2.0`.
- Added `QuestRewardService.CreateXpRewardPlan`.
- Added `QuestRewardService.ApplyQuestXpRate`.
- Added `QuestXpRewardPlan`.
- Added `QuestXpRewardStatus`.
- Added `QuestXpRewardMessageKind`.
- Added `QuestXpRewardPacketIntent`.
- Created `docs/QuestXpReward-Audit.md`.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestRewardServiceTests|GameServerOptionsTests" --nologo` | Passed: 22 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1826 |

## Migration Parity Table - UOW-1044

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.giveReward` XP branch | `Aion.GameServer.Services.QuestRewardService.CreateXpRewardPlan` | Quest Reward Helper | Partial | Unit Tested | Partial Parity | Non-live helper models raw zero skip, no-exp guard, nightmare-circus world guard, NPC-name message selection, resulting XP, display level, repose/salvation metadata, ascension-limit intent, and packet/level-change intents. Quest finish does not compose or execute this plan yet. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addExp` | `Aion.GameServer.Services.QuestXpRewardPlan` | Model/Reward Plan | Partial | Unit Tested | Partial Parity | Source-reviewed repose and salvation calculations are represented without mutating player state. Java runtime comparison, negative/edge XP cases, threading, persistence, live packet send, and actual `setExp` mutation remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setExp` | `QuestXpRewardPlan.CurrentExp`; `QuestXpRewardPlan.CurrentLevel`; `QuestXpRewardPacketIntent.LevelChangeSideEffects` | Model Side-Effect Metadata | Partial | Unit Tested | Needs Verification | Plan clamps resulting XP to the C# experience table cap and records level-change intent. C# uses current `Player.Level` as previous level input; Java derives and updates level through `setExp`. Stat recalculation, nearby quest refresh, skill learn, guide/starter-kit, and NPC faction hooks are not ported. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.XP_QUEST` | `Aion.GameServer.Services.QuestRewardService.ApplyQuestXpRate`; `GameServerRateOptions.XpQuestRates` | Rate Utility / Config | Partial | Unit Tested | Partial Parity | Membership clamp, empty-rate fallback, Java `float` precision, boost stat, legion bonus, truncation, and overflow saturation are covered from source-reviewed Java behavior. Java runtime comparison and unusual config values remain unverified. |
| `com.aionemu.gameserver.configs.main.RatesConfig.XP_QUEST_RATES` | `Aion.GameServer.Configuration.GameServerRateOptions.XpQuestRates` | Config | Complete | Unit Tested | Partial Parity | Loads `gameserver.rates.xp.quest` with Java default `1.0, 2.0` and override behavior. No Java runtime config dump comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP` | `QuestXpRewardPacketIntent.StatUpdateExp`; existing `Aion.GameServer.Network.Aion.ServerPackets.SmStatUpdateExp` | Packet Dependency | Partial | Unit Tested | Needs Verification | XP plan records stat-update packet intent only. No live send, ordering verification, or Java golden-byte comparison was added. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` XP reward variants | `QuestXpRewardMessageKind` | Packet Message Metadata | Partial | Unit Tested | Needs Verification | Message-kind metadata distinguishes NPC-name, repose, salvation, and combined bonus cases. Concrete message ids/parameter ordering are not ported in this unit. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` and level-up dependents | `QuestXpRewardPacketIntent.LevelChangeSideEffects` | Controller/Side-Effect Dependency | Not Started | Unit Tested as intent only | Needs Verification | Dependency is newly surfaced as a coarse intent. Stat template updates, max repose mutation, salvation reset, quest callbacks, nearby quest refresh, skill learning, guide/starter-kit, animation broadcast, and persistence remain unported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestRewardServiceTests.CreateXpRewardPlan_AppliesJavaQuestRateReposeAndSalvationWithoutMutatingPlayer` | Unit | `QuestService.giveReward`; `PlayerCommonData.addExp`; `Rates.XP_QUEST` | Membership-rated XP, repose usage/bonus, salvation bonus, NPC-name message kind, stat-update intent, max repose metadata, and no player mutation. | Source-reviewed Java formulas and branch order. | No Java runtime comparison, live packet send, or level-change case. |
| `QuestRewardServiceTests.CreateXpRewardPlan_RecordsJavaGuardsAndNonDaevaLevelCap` | Unit | `PlayerCommonData.addExp`; `PlayerCommonData.setExp` | Raw zero, no-exp, nightmare-circus guards, non-Daeva level cap, and ascension-limit message intent. | Source-reviewed Java guards/cap behavior. | C# no-exp and Daeva status are explicit inputs rather than full player-state parity. |
| `QuestRewardServiceTests.ApplyQuestXpRate_MatchesJavaFloatRateBoostLegionFallbacksAndOverflow` | Unit | `Rates.XP_QUEST` | Membership fallback, boost stat, legion bonus, empty-rate fallback, Java float rounding, and saturation on overflow. | Source-reviewed Java arithmetic. | No Java runtime comparison or unusual negative-rate coverage. |
| `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit | `RatesConfig.XP_QUEST_RATES` | Default XP quest rates load as `[1.0, 2.0]`. | Java config default. | No Java runtime config dump comparison. |
| `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit | Java config override precedence | `mygs.properties` XP quest rate override. | Existing config precedence tests. | No Java runtime config dump comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- XP helper is non-live and not composed into quest-finish operation descriptors.
- XP packet/system-message ids and parameter order remain metadata only.
- `PlayerCommonData.setExp` side effects are only coarsely represented; stat updates, nearby quest refresh, quest callbacks, skill learning, animation broadcast, guide/starter-kit, salvation reset, and persistence are not ported.
- C# previous-level handling depends on `Player.Level`; Java derives displayed level through `setExp`.
- Repose/salvation and Java float arithmetic are source-reviewed but not runtime-compared against Java.
- Threading and persistence behavior remain unknown for future live XP execution.

## Summary Metrics

- Total Java artifacts discovered: 8 in this unit
- Total artifacts ported: 1 rate/config surface plus 1 non-live XP reward planner and result metadata surface
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8
- Total blocked artifacts: 5 blocked/partial categories: Java runtime comparison, live quest-finish XP execution, concrete XP message packets, level-change side effects, and persistence/threading behavior
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds a source-reviewed XP planning scaffold without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `QuestXpRewardPlan` into quest-finish operation descriptors after the XP non-item projection.
- Why: It moves the XP helper into the same non-live operation metadata path used by title/cube/warehouse/GP while preserving Java reward order and avoiding live mutation.
- Likely files: `QuestRewardSideEffectPlanService.cs`, `QuestFinishOperationPlanService.cs`, related operation descriptor models, `QuestFinishOperationPlanServiceTests.cs`, `QuestRewardSideEffectPlanServiceTests.cs`, progress/handoff docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | XP operation metadata composition | quest operation planner/test files | Medium | Do not combine with other edits to the same operation descriptors. |
| B | XP concrete system-message helper audit | Java read-only, packet tests if scoped | Medium | Can be read-only parallel work; write phase should be separate from operation composition. |
| C | Level-change side-effect audit | read-only docs | Low-Medium | Safe as an explorer task. |
| D | Offline GP opt-in integration adapter | GP service/repository files | Medium | Separate from XP files, but docs remain orchestrator-owned. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose XP plan into operation metadata and docs | quest operation planner files, focused tests, progress/handoff docs | GP service/repository files |
| Explorer A | XP system-message ids and Java parameter order | read-only | all writes |
| Explorer B | Level-change side-effect dependency map | read-only | all writes |

## Do Not Parallelize

- `QuestRewardService.cs`: shared reward helper surface.
- `QuestFinishOperationPlanService.cs`: shared quest-finish ordering.
- `QuestRewardSideEffectPlanService.cs`: shared side-effect metadata surface.
- `GameServerOptions.cs`: shared config surface.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1044, `QuestRewardServiceTests`, `GameServerOptionsTests`, and the XP helper in `QuestRewardService.cs`.
