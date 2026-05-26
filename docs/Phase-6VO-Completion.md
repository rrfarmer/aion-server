# Phase 6VO Completion - UOW-1075 Quest-Finish Runtime Input Audit

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VN-Completion.md`.

## Last Completed Unit

UOW-1075: `[Phase 6][UOW-1075] Audit quest-finish custom reward runtime inputs`

Recent commits before this unit:

- `e6cce6cc3 [Phase 6][UOW-1074] Add quest-finish custom reward context adapter`
- `53258ae5c [Phase 6][UOW-1073] Add custom reward runtime input adapter`
- `6bb2db39d [Phase 6][UOW-1072] Add system mail online fanout packet order tests`

## Summary

UOW-1075 adds `docs/QuestFinishRuntimeInput-Audit.md`, a read-only/source-analysis checkpoint for future production wiring of `QuestFinishCustomRewardRuntimeSideEffectAdapterService`.

The audit maps Java runtime inputs to current C# homes: player/account identity, account creation time, item templates, object-id allocation, received mail time, XP level mutation timing, quest reward template context, and the explicit C# opt-in policy.

No code behavior changed in this unit.

## Key Findings

- `IDFactory.NextId()` is the right future source for `QuestXpCustomRewardRuntimeInputAdapterOptions.NextObjectId`, but it must stay guarded because tests and optional constructors may run without an id factory.
- `runtimeContext.DataManager.StaticData.ItemTemplates` is the established C# item-template source and is needed before enabled faction custom reward execution because Java filters opposite-race items by item template.
- `AccountAuthResult.CreationDate` is read from the login server, but `GameServerConnection` does not currently preserve it and `Player` does not expose account creation time.
- Java `FactionPackService` uses account creation time through `player.getAccount().getCreationDate()` and `ServerTime.ofEpochMilli(...).toLocalDateTime()`. Do not substitute `players.creation_date`.
- Java custom rewards run after `PlayerCommonData.setExp` mutates the player's level. C# still lacks live XP mutation, so production custom reward execution must not run from a stale pre-mutation level snapshot.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| Runtime input analysis | Docs/read-only source inspection | Safe, orchestrator-owned docs | Selected for UOW-1075 |
| Disabled runtime input assembler | New service/test | Shared XP/account-time boundary; should follow audit | Defer |
| Mail list packet splitting tests | Packet test file only | Safe isolated test task | Defer |
| Live DB run/hardening | System-mail opt-in DB tests | Environment-dependent | Defer |

File ownership map:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Quest-finish runtime input audit | `docs/QuestFinishRuntimeInput-Audit.md`; `docs/QuestXpReward-Audit.md`; `docs/QuestRewardSideEffects-Audit.md`; `docs/PHASE-6-PROGRESS.md`; handoff docs | Production code; test files | Audit doc, progress updates, handoff |

No subagents were used. This was documentation/read-only source analysis, and Phase 6 progress/handoff docs remain orchestrator-owned.

## Files Changed

- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VO-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| Not run | Documentation-only/read-only analysis unit. Latest code validation remains UOW-1074: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 1896 tests. |

## Migration Parity Table - UOW-1075

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.QuestService.finishQuest`; `giveReward` | `docs/QuestFinishRuntimeInput-Audit.md`; `QuestFinishOperationPlanService` dependencies | Runtime Input Analysis | Partial | Manual Only | Needs Verification | Audit maps where production quest-finish custom reward composition would need inputs. No code path was changed and default quest finish remains non-live metadata. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setExp` | `QuestXpRewardPlan`; future live XP mutation boundary | XP Mutation Dependency | Not Started for live mutation | Manual Only | Needs Verification | Audit confirms Java mutates level before `onLevelChange`; C# still uses explicit snapshots and must not execute custom reward level checks from stale pre-mutation player state. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestFinishCustomRewardRuntimeSideEffectAdapterService` future caller | Level-Change Runtime Wiring Dependency | Partial | Manual Only | Needs Verification | Audit identifies required runtime inputs before calling the disabled context adapter. Ratio/stat/animation/live callback/skill/starter-kit side effects remain non-live metadata or unported. |
| `com.aionemu.gameserver.services.FactionPackService.sendRewards` | `AccountAuthResult.CreationDate`; missing active player/session account creation field | Account Creation Time Dependency | Blocked | Manual Only | Needs Verification | Java uses `player.getAccount().getCreationDate()` converted by `ServerTime.ofEpochMilli`. C# reads login auth creation millis but does not retain it on connection/player state, and no equivalent conversion helper is tested. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` | `runtimeContext.DataManager.StaticData.ItemTemplates` | Static Data Dependency | Partial | Manual Only | Needs Verification | Audit finds item templates are available through established runtime context access. Future assembler should require them before enabled faction custom reward execution because opposite-race filtering depends on templates. |
| `com.aionemu.gameserver.utils.idfactory.IDFactory.nextId` | `Aion.GameServer.Utils.IdFactory.IDFactory.NextId` | Object ID Allocation Dependency | Partial | Existing Unit Coverage | Needs Verification | Audit identifies `IDFactory.NextId` as the future `NextObjectId` source. It must remain guarded because tests and optional constructors may run without an id factory. |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | Future runtime input assembler options: `ReceivedTime`, `NextObjectId`, `ItemTemplates` | Mail Runtime Dependency | Partial | Manual Only | Needs Verification | Audit documents mail time/id/template dependencies. No mail persistence, item storage, mailbox counter update, online fanout, or socket send was enabled. |

## Tests Added

None. This was a documentation-only source audit.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Account creation time is a real blocker for faction pack parity because active C# player/session state does not currently retain it.
- Server-time conversion from login auth epoch milliseconds to Java-like local `LocalDateTime` is unimplemented and untested.
- Live XP mutation remains absent, so Java's post-`setExp` level timing is not reproduced.
- Transaction/failure ordering for custom reward receipt rows plus system-mail persistence remains unresolved.
- No code tests were run in this documentation-only unit; latest code validation remains UOW-1074 full suite.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 0 code artifacts; 1 runtime input audit document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 4 categories: account creation time retention, server-time conversion, live XP mutation, and custom reward receipt/mail transaction ordering
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Add a disabled-by-default quest-finish runtime input assembler that refuses enabled custom reward execution when account creation time, item templates, or object-id allocation are missing, and add source-derived tests for Java `ServerTime.ofEpochMilli` account-creation conversion around faction pack boundary timestamps.

## Next Work Options

### Recommended Sequential Task

- Task: Implement a disabled-by-default runtime input assembler for quest-finish custom reward context options.
- Why: The audit identifies the missing prerequisites; a small assembler can make the guard behavior testable without calling repositories or mutating player state.
- Files: likely a new service/test file plus docs. Avoid production socket/quest handler wiring until XP mutation and transaction policies are ready.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Runtime input assembler tests | new test file | Low/Medium | Must not touch production socket handler. |
| B | Java `ServerTime.ofEpochMilli` conversion analysis/tests | new helper/test if implemented | Medium | Date/time parity needs careful timezone handling. |
| C | Mail list packet splitting tests | packet test file only | Low/Medium | Independent from runtime input assembler. |
| D | Live DB run/hardening of system-mail opt-in integration suite | no code unless failures are isolated | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION=1` and MySQL. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Agent A | Add mail list packet splitting tests | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or a new packet test file | production code; docs |
| Orchestrator | Implement runtime input assembler after date/time decision | exact new service/test files plus docs | `GameServerConnection`, `QuestFinishOperationPlanService`, live mail executor unless explicitly scoped |

### Do Not Parallelize

- Account/session state changes, `GameServerConnection`, `Player`, and production quest-finish wiring: shared runtime surfaces.
- `QuestFinishOperationPlanService`, `QuestXpExecutionPlanService`, `QuestXpLevelChangeContextFactoryService`, and `QuestFinishCustomRewardRuntimeSideEffectAdapterService` if runtime input assembly is being implemented.
- Phase 6 progress and handoff docs: orchestrator-owned only.
