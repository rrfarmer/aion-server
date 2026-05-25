# Phase 6UL Completion - UOW-1046 Quest XP System Messages

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UK-Completion.md`.

## Last Completed Unit

- UOW-1046: `[Phase 6][UOW-1046] Add quest XP system messages`
- Recent commits:
  - `35b9c7a0d [Phase 6][UOW-1045] Compose quest XP reward metadata`
  - `6d78fef5b [Phase 6][UOW-1044] Add quest XP reward planner scaffold`
  - `212b9fda5 [Phase 6][UOW-1043] Gate offline GP execution`
  - `cdc294cac [Phase 6][UOW-1042] Add offline GP DAO boundary`
- UOW-1046 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1046 adds concrete `SmSystemMessage` helpers for the XP reward messages used by Java `PlayerCommonData.addExp`. The unit covers named and unnamed XP gain messages, repose bonus, salvation bonus, combined repose/salvation bonus, and the level-9 class-change/ascension-limit warning. These helpers are regression-tested for message id and parameter order, but are not yet bridged from `QuestXpRewardPlan` and are not sent live.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | XP system-message helpers | `SM_SYSTEM_MESSAGE` XP reward variants, `PlayerCommonData.addExp` message branches | `SmSystemMessage.cs`, `GamePacketTests.cs` | Packet Helper | No with other packet edits | Low-Medium | Isolated packet helper/test files; orchestrator-owned implementation. |
| B | Level-change side-effect map | `PlayerCommonData.setExp`, `PlayerController.onLevelChange`, quest/skill/nearby hooks | read-only | Java Analysis | Yes | Medium | Broad but read-only and independent from message helper implementation. |
| C | XP plan-to-packet bridge | `PlayerCommonData.addExp` message selection | `QuestRewardService.cs` or XP helper tests | Service Port | Not with A if packet helpers still changing | Medium | Depends on concrete message helpers being stable. |
| D | Offline GP opt-in adapter | `GloryPointsService.addGp`, `AbyssRankDAO.addGp` | GP service/repository files | Service Port | Yes only if docs serialized | Medium | Code-isolated from XP packet helpers, but shared docs remain orchestrator-owned. |

## Selected Parallel Batch

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Port XP system-message helpers and packet tests | Packet Helper | `SmSystemMessage.cs`, `GamePacketTests.cs`, progress/audit/handoff docs | level-change implementation files, GP files | Java message helper source review | Code/tests/docs/commit |
| Explorer A | Analyze Java XP level-change side effects | Java Analysis | read-only | all writes | none | Behavior report for future live XP work |

Explorer A completed read-only analysis and was closed. No sub-agent files changed.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UL-Completion.md`

## What Changed

- Added `SmSystemMessage.GetExp(string, long)`.
- Added `SmSystemMessage.GetExpVitalBonus(string, long, long)`.
- Added `SmSystemMessage.GetExpMakeupBonus(string, long, long)`.
- Added `SmSystemMessage.GetExpVitalMakeupBonus(string, long, long, long)`.
- Added `SmSystemMessage.GetExp2VitalBonus(long, long)`.
- Added `SmSystemMessage.GetExp2MakeupBonus(long, long)`.
- Added `SmSystemMessage.GetExp2VitalMakeupBonus(long, long, long)`.
- Added `SmSystemMessage.LevelLimitQuestNotFinished()`.
- Added XP message assertions to `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages`.
- Existing `SmSystemMessage.GetExp2(long)` now has explicit quest-XP regression coverage.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages" --nologo` | Passed: 1 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1827 |

## Migration Parity Table - UOW-1046

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExp(string,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1370000` and Java parameter order `(npcName, exp)` are regression-tested through C# packet serialization. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP2` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExp2(long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Existing helper now has quest-XP regression coverage for message id `1370002` and numeric string parameter. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP_VITAL_BONUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExpVitalBonus(string,long,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400342` and Java parameter order `(npcName, exp, repose)` are regression-tested. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP_MAKEUP_BONUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExpMakeupBonus(string,long,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400343` and Java parameter order `(npcName, exp, salvation)` are regression-tested. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP_VITAL_MAKEUP_BONUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExpVitalMakeupBonus(string,long,long,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400344` and Java parameter order `(npcName, exp, repose, salvation)` are regression-tested. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP2_VITAL_BONUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExp2VitalBonus(long,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400348` and Java parameter order `(exp, repose)` are regression-tested. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP2_MAKEUP_BONUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExp2MakeupBonus(long,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400349` and Java parameter order `(exp, salvation)` are regression-tested. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GET_EXP2_VITAL_MAKEUP_BONUS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.GetExp2VitalMakeupBonus(long,long,long)` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400350` and Java parameter order `(exp, repose, salvation)` are regression-tested. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_LEVEL_LIMIT_QUEST_NOT_FINISHED1` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LevelLimitQuestNotFinished` | Packet Helper | Complete | Regression Tested | Partial Parity | Message id `1400545` is regression-tested. It is not yet emitted from XP reward execution. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addExp` | `QuestXpRewardPlan.MessageKind`; `SmSystemMessage` XP helpers | Reward/Packet Dependency | Partial | Unit/Regression Tested | Partial Parity | C# now has message-kind metadata and concrete packet helpers, but no bridge from `QuestXpRewardPlan` to packet instances and no live send. Java no-exp/nightmare guards, rate, repose, salvation, `setExp`, threading, and persistence remain separate parity gaps. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setExp`; `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | Read-only level-change report only | Java Analysis | Not Started | Manual Only | Needs Verification | Explorer confirmed Java live XP level-up ordering and C# gaps. No code was ported for level mutation, visual stats, nearby refresh, quest callbacks, skills, NPC faction level-up, custom rewards, or persistence. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` | Regression | `SM_SYSTEM_MESSAGE` XP helper source review | XP gain and ascension-limit message ids and parameter serialization for named, unnamed, repose, salvation, and combined bonus variants. | Source-reviewed Java message helper ids and parameter order, serialized through C# packet writer. | No Java golden-byte runtime comparison and no live XP reward send. |

## Explorer Level-Change Findings

- Java XP rewards call `setExp` before the XP gain system message is sent.
- `setExp` mutates exp/level and calls `PlayerController.onLevelChange(oldLevel, newLevel)` before sending `SM_STATUPDATE_EXP`.
- `onLevelChange` order includes stats template update, max repose update, salvation reset, upgrade player, level-up animation, NPC faction level-up, quest level-change callbacks, nearby quest refresh, guide, skill auto-learn, bonus/faction packs, and starter kit.
- Packet order includes visual stat output, optional team/legion updates, level-up animation, faction/quest packets, guaranteed nearby quest packet, skill-list packets, `SM_STATUPDATE_EXP`, XP gain message, and optional ascension-limit warning.
- Java quest XP level-up side effects run before `QuestService.finishQuest` marks the quest complete.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- XP message helpers are not bridged from `QuestXpRewardPlan.MessageKind` and are not sent live.
- `SM_STATUPDATE_EXP` remains separate and is not emitted from a live XP execution path.
- Java level-change side effects remain unported: visual stats, level-up animation, NPC faction level-up, quest level-change callbacks, nearby refresh, guide/starter-kit, skill auto-learn, custom rewards, and deferred persistence.
- Java level-up side effects run before quest completion state mutation; future C# live integration must preserve this ordering.

## Summary Metrics

- Total Java artifacts discovered: 11 in this unit
- Total artifacts ported: 9 concrete XP system-message helper surfaces
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 11
- Total blocked artifacts: 4 blocked/partial categories: Java runtime comparison, XP plan-to-packet bridge, live XP execution, and level-change side effects
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds concrete XP packet helpers without enabling live XP reward mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Bridge `QuestXpRewardPlan.MessageKind` to concrete XP system-message packet metadata without live sends.
- Why: The XP planner has message-kind metadata and UOW-1046 added concrete packet helpers; a pure bridge would make future live execution less ambiguous while preserving the non-live boundary.
- Likely files: `QuestRewardService.cs` or a small XP packet plan helper, `QuestRewardServiceTests.cs` or a dedicated test file, `QuestXpReward-Audit.md`, progress/handoff docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | XP plan-to-packet metadata bridge | XP service/helper tests | Medium | Do not combine with live XP mutation. |
| B | Level-change executor design/audit | read-only Java/docs | Medium | Safe as explorer or docs-only unit. |
| C | Add `SmActionAnimation` level-up named constant/test | `SmActionAnimation.cs`, packet tests | Low-Medium | Isolated if no XP execution wiring. |
| D | Offline GP opt-in adapter | GP service/repository files | Medium | Code-isolated from XP files; docs serialized by orchestrator. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | XP plan-to-packet metadata bridge and docs | XP service/helper files, focused tests, progress/handoff docs | live XP executor, GP files |
| Explorer A | Level-change executor dependency map refinement | read-only | all writes |
| Worker B | Level-up animation packet constant/test | `SmActionAnimation.cs`, a dedicated packet test section | XP service files, docs |

## Do Not Parallelize

- `QuestRewardService.cs`: shared reward helper surface.
- `SmSystemMessage.cs`: shared packet helper surface if XP message work continues.
- `GamePacketTests.cs`: shared packet regression file if packet helpers are being added.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1046, `SmSystemMessage.cs`, `GamePacketTests.cs`, and the level-change findings above.
