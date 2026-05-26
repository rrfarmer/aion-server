# Phase 6VT Completion - UOW-1080 Quest-Finish Production Call-Site Audit

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VS-Completion.md`.

## Last Completed Unit

UOW-1080: `[Phase 6][UOW-1080] Audit quest-finish production call sites`

Recent commits before this unit:

- `f5ca7b9c7 [Phase 6][UOW-1079] Add disabled custom reward composition regression`
- `dc13d9dd9 [Phase 6][UOW-1078] Add quest-finish custom reward session runtime adapter`
- `39be9fd0f [Phase 6][UOW-1077] Preserve account creation runtime state`

## Summary

UOW-1080 adds a documentation-only production call-site audit for future quest-finish custom reward wiring.

The audit maps Java's call chain from `CM_DIALOG_SELECT.runImpl` self/reportable quest auto-reward handling into `QuestService.finishQuest`, `QuestService.giveReward`, `PlayerCommonData.addExp/setExp`, `PlayerController.onLevelChange`, bonus/faction custom reward services, and `SystemMailService.sendMail`.

It identifies `GameServerConnection.HandleDialogSelectAsync` as the future C# socket entry point, documents the context assembly order, and explicitly keeps live quest finish, XP mutation, custom reward DAO writes, system-mail persistence/fanout, object-id allocation, and packet sends disabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A | Production call-site audit | `CM_DIALOG_SELECT`; `QuestService`; `PlayerCommonData`; `PlayerController`; custom reward/mail services | docs only | Java Analysis / Documentation Update | No for selected unit | Medium | Shared production quest-finish path analysis and progress/handoff docs are orchestrator-owned. |
| B | Mail list packet splitting tests | Mail list packet artifacts | packet test file only | Test Creation | Yes later | Low/Medium | Independent from reward runtime input assembly. |
| C | Additional timezone vectors | `ServerTime.ofEpochMilli` | assembler test file | Test Creation | Yes later | Low/Medium | Independent if assembler service is not edited. |
| D | Opt-in system-mail DB hardening | `MailDAO`; system-mail repository | DB integration tests | Parity Verification | Yes later | Medium | Environment-dependent. |
| E | Full account aggregate analysis | `Account`; `AccountService` | read-only | Java Analysis | Yes later | Low | Read-only support task before full account aggregate porting. |

File ownership map:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Production call-site analysis and docs | `docs/QuestFinishProductionCallSite-Audit.md`; `docs/QuestFinishRuntimeInput-Audit.md`; `docs/QuestXpReward-Audit.md`; `docs/QuestRewardSideEffects-Audit.md`; `docs/PHASE-6-PROGRESS.md`; `docs/Phase-6VT-Completion.md` | production code; `GameServerConnection`; `Player`; custom reward adapter code; mail executor code | Source-reviewed audit, progress update, parity table, handoff |

No sub-agents were used.

## Files Changed

- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VT-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| Documentation-only unit | No code tests run |
| Latest full code validation from UOW-1079: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1910 |

## Migration Parity Table - UOW-1080

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync`; future self auto-reward guard planner | Socket Packet Handler | Partial | Manual Only | Needs Verification | Java self/reportable auto-reward branch calls `QuestService.finishQuest` for selected quest reward dialog actions. C# currently handles other dialog branches but not this quest-finish branch. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService`; future production planning intent | Quest Finish Service | Partial | Manual Only | Needs Verification | Source-reviewed ordering remains rewards, work-item removal, quest-state completion, update packet, completion callback. C# has non-live metadata planning only; live reward mutation, work-item removal, quest packet send, persistence, and callback dispatch remain disabled. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `QuestFinishRewardSideEffectContext`; `QuestFinishOperationPlanService` side-effect descriptors | Reward Side-Effect Service | Partial | Manual Only | Partial Parity | Audit confirms future C# context sources for XP/custom reward metadata. Kinah/title/AP/DP/GP/cube/warehouse/XP live mutation remains unexecuted; serialization/packet ordering and persistence are not verified. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addExp` | `QuestRewardService.CreateXpRewardPlan`; future live XP mutation boundary | XP Calculation / Mutation | Partial | Unit Tested previously; Manual Only in this unit | Partial Parity | Existing C# XP planner covers source-derived rate/repose/salvation metadata, but this unit only audits the production call site. Live XP mutation, `SM_STATUPDATE_EXP`, threading/order, and persistence remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setExp` | Future C# live XP/level mutation boundary | XP/Level Mutation | Not Started | No Tests | Unknown | Java mutates XP/level before calling `PlayerController.onLevelChange`. C# has no live equivalent; custom rewards must remain disabled to avoid stale level checks. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpExecutionPlanService`; `QuestFinishCustomRewardRuntimeSideEffectAdapterService` | Level-Change Service | Partial | Unit Tested previously; Manual Only in this unit | Needs Verification | Audit confirms Java custom reward hooks run after skill auto-learn and before starter kit/XP stat packet flow. C# level-change side effects remain non-live metadata. |
| `com.aionemu.gameserver.services.BonusPackService.addPlayerCustomReward` | `CustomLevelRewardExecutionService`; `QuestFinishCustomRewardSessionRuntimeInputAdapterService` | Custom Reward Service | Partial | Unit Tested previously; Manual Only in this unit | Partial Parity | Future production context must keep disabled options until receipt DAO and mail execution policy are ready. No repository call or object-id allocation occurs in this docs-only unit. |
| `com.aionemu.gameserver.services.FactionPackService.sendRewards` | `CustomLevelRewardExecutionService`; `QuestFinishCustomRewardRuntimeInputAssemblerService` | Faction Custom Reward Service | Partial | Unit Tested previously; Manual Only in this unit | Partial Parity | Account creation time, item-template filtering, and id allocation sources are mapped. Date/time DST/named-zone runtime comparison, opposite-race filtering runtime behavior, receipt persistence, and mail fanout remain unverified. |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | `SystemMailRewardPersistenceExecutionService`; `SystemMailRewardPersistenceOperationExecutor` | System Mail Service | Partial | Integration Tested previously where opt-in enabled; Manual Only in this unit | Needs Verification | Audit keeps mail persistence/fanout disabled from quest finish. Previous opt-in repository/fanout tests exist, but no production quest-finish path invokes them and failure ordering with quest completion is unresolved. |
| `com.aionemu.gameserver.utils.idfactory.IDFactory.nextId` | `Aion.GameServer.Utils.IdFactory.IDFactory.NextId`; disabled session adapter input | Object ID Allocation Dependency | Partial | Unit Tested previously; Manual Only in this unit | Needs Verification | Future production wiring must pass the id factory but disabled options must not allocate ids. No allocation path was touched in this unit. |

## Tests Added

None. This was a documentation-only source review. It does not provide new executable parity evidence.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Production `CM_DIALOG_SELECT` still does not implement Java self/reportable auto-reward quest finish.
- Live XP mutation and `PlayerCommonData.setExp` equivalent are absent.
- Quest reward template projection from live static quest data is not wired into a socket path.
- Custom reward receipt store-before-mail ordering and system-mail persistence/fanout remain gated.
- Per-player ordering, live packet ordering, date/time runtime comparison, serialization/packet bytes, and persistence behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 10 in this unit
- Total artifacts ported: 0 new production artifacts; 1 production call-site audit document
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked artifacts: 6 categories: production quest-finish socket branch, live XP mutation, reward template projection, custom reward receipt/mail execution, Java runtime comparison, and per-player packet/persistence ordering
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Add a non-live `CM_DIALOG_SELECT` self auto-reward guard planner or test helper that recognizes Java's reportable auto-reward branch and returns a disabled quest-finish planning intent. Keep production quest-finish/custom reward execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Mail list packet splitting tests | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or new packet test file | Low/Medium | Independent from reward runtime input assembly. |
| B | Additional date/time conversion vectors | `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.cs` | Low/Medium | Avoid named-zone assumptions unless cross-platform behavior is verified. |
| C | Live DB run/hardening of system-mail opt-in integration suite | no code unless failures are isolated | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION=1` and MySQL. |
| D | Read-only Java account/session model analysis for full account aggregate parity | read-only | Low | Useful before porting account time/toll/warehouse state. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Agent A | Mail list packet splitting tests | packet test file only | production code; docs |
| Agent B | Read-only full account aggregate analysis | read-only | all writes |
| Orchestrator | Non-live `CM_DIALOG_SELECT` self auto-reward guard planner/test helper | exact new service/test files selected after discovery plus docs | live quest finish; custom reward execution; mail executor; shared adapter rewrites |

## Do Not Parallelize

- `GameServerConnection`, `Player`, login auth/session state, or production quest-finish wiring if the next unit moves beyond a separate planner/helper.
- `QuestFinishCustomRewardRuntimeInputAssemblerService`, `QuestFinishCustomRewardSessionRuntimeInputAdapterService`, and `QuestFinishCustomRewardRuntimeSideEffectAdapterService` if runtime input composition changes continue.
- Phase 6 progress and handoff docs: orchestrator-owned only.

## Context For Next Session

- `docs/commit-conventions.md` is still missing; use the orchestration commit format.
- Keep Java as source of truth and leave live custom reward execution disabled.
- The newest audit is `docs/QuestFinishProductionCallSite-Audit.md`.
- A safe next implementation is a small non-live planner/test helper for Java's `CM_DIALOG_SELECT` self auto-reward branch, not a live `GameServerConnection` mutation.
