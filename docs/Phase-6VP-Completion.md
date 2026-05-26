# Phase 6VP Completion - UOW-1076 Quest-Finish Custom Reward Runtime Input Assembler

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VO-Completion.md`.

## Last Completed Unit

UOW-1076: `[Phase 6][UOW-1076] Add quest-finish custom reward runtime input assembler`

Recent commits before this unit:

- `24d054f04 [Phase 6][UOW-1075] Audit quest-finish custom reward runtime inputs`
- `e6cce6cc3 [Phase 6][UOW-1074] Add quest-finish custom reward context adapter`
- `53258ae5c [Phase 6][UOW-1073] Add custom reward runtime input adapter`

## Summary

UOW-1076 adds `QuestFinishCustomRewardRuntimeInputAssemblerService`, a disabled-by-default factory for `QuestXpCustomRewardRuntimeInputAdapterOptions`.

The assembler does not call repositories, mutate player state, invoke quest finish, or execute mail persistence/fanout. It only creates inert or fully guarded options for future callers of `QuestFinishCustomRewardRuntimeSideEffectAdapterService`.

Enabled options require account creation epoch milliseconds, object-id allocation, and item templates before any executable custom reward adapter options are produced. Account creation epoch milliseconds are converted through `GameServerOptions.Core.GetTimeZone()` in the shape of Java `ServerTime.ofEpochMilli(...).toLocalDateTime()`.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| Runtime input assembler | New service/test, docs | Shared date/time and custom reward options boundary; single owner needed | Selected for UOW-1076 |
| Mail list packet splitting tests | Packet test file only | Safe isolated test task | Defer |
| Live DB run/hardening | System-mail opt-in DB tests | Environment-dependent | Defer |
| Java `ServerTime` read-only analysis | read-only | Safe support task | Done directly as part of selected unit |

File ownership map:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Disabled quest-finish custom reward runtime input assembler | `dotnetConversion/src/Aion.GameServer/Services/QuestFinishCustomRewardRuntimeInputAssemblerService.cs`; `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.cs`; Phase 6 docs/handoff | `GameServerConnection`; `Player`; production quest handlers; live mail executor; shared XP planners unless tests prove required | Service, tests, docs, commit |

No subagents were used. This unit owns a shared date/time/custom reward options boundary.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishCustomRewardRuntimeInputAssemblerService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.cs`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VP-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishCustomRewardRuntimeInputAssemblerServiceTests\|QuestFinishCustomRewardRuntimeSideEffectAdapterServiceTests\|QuestXpCustomRewardRuntimeInputAdapterServiceTests" --nologo` | Passed: 13 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1902 |

## Migration Parity Table - UOW-1076

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.utils.time.ServerTime.ofEpochMilli` | `Aion.GameServer.Services.QuestFinishCustomRewardRuntimeInputAssemblerService.ConvertEpochMillisToServerLocalTime` | Date/Time Utility Boundary | Partial | Unit Tested | Partial Parity | C# converts epoch milliseconds through a supplied `TimeZoneInfo` and returns a local `DateTime` for Java `LocalDateTime` window comparison. Tests cover UTC and a fixed-offset zone. DST/ambiguous local times, IANA/Windows id mapping differences, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.services.FactionPackService.sendRewards` | `QuestFinishCustomRewardRuntimeInputAssemblerService.CreateOptions` | Runtime Input Assembler | Partial | Unit Tested | Partial Parity | Enabled options require account creation epoch milliseconds and item templates before adapter execution, preserving faction-pack window and opposite-race filtering prerequisites. Active session/player account creation storage remains missing. |
| `com.aionemu.gameserver.utils.idfactory.IDFactory.nextId` | `QuestFinishCustomRewardRuntimeInputAssemblerInput.NextObjectId`; `Aion.GameServer.Utils.IdFactory.IDFactory.NextId` future source | Object ID Allocation Dependency | Partial | Unit Tested as guard | Needs Verification | Assembler refuses enabled options without an object-id function and does not allocate ids itself. Production wiring to `IDFactory.NextId` remains future work. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` | `QuestFinishCustomRewardRuntimeInputAssemblerInput.ItemTemplates`; `ItemTemplateTable` | Static Data Dependency | Partial | Unit Tested as guard | Needs Verification | Assembler requires item templates before enabled custom reward execution. It does not verify full Java XML item-template parity or live runtime availability. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpCustomRewardRuntimeInputAdapterOptions` produced by assembler | Level-Change Adapter Options | Partial | Unit Tested | Needs Verification | Assembler produces disabled-by-default options for the existing runtime input adapter. It does not call `PlayerController` equivalents, execute custom rewards, or solve Java post-`setExp` level timing. |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | `QuestXpCustomRewardRuntimeInputAdapterOptions.ReceivedTime`; `NextObjectId`; `ItemTemplates` | Mail Runtime Dependency | Partial | Unit Tested as metadata | Needs Verification | Assembler carries received time/id/template dependencies forward only. No mail persistence, item storage, mailbox counter update, online fanout, or socket send is enabled. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.CreateOptions_DisabledGateReturnsDisabledAdapterOptionsWithoutDependencies` | Disabled input returns inert adapter options without account time, id factory, or item templates. | C# safety gate around Java custom reward side effects; no Java runtime behavior claimed. |
| `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.CreateOptions_EnabledGateRequiresRuntimeInputsBeforeAdapterExecution` | Enabled input reports missing account creation time, object-id function, or item templates before producing executable adapter options. | Source-reviewed Java dependency order; no repository or Java runtime execution. |
| `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.CreateOptions_CreatesAdapterOptionsWithJavaServerTimeEpochMillisConversion` | UTC epoch milliseconds at the ASMODIANS creation-window start produce local `2022-06-18T00:00:00` and carry id/time/template options. | Source-derived from `ServerTime.ofEpochMilli` and `FactionPackService`; no Java runtime comparison. |
| `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.ConvertEpochMillisToServerLocalTime_MatchesJavaServerTimeOfEpochMilliAcrossOffsets` | Fixed-offset server timezone converts the same UTC instant to local `2022-06-17T20:00:00`, matching Java `ZonedDateTime` local time semantics. | Deterministic source-derived conversion; DST and named timezone mapping remain unverified. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Active C# account/session state still does not retain login-server account creation time after auth.
- Production quest-finish and socket handlers do not invoke the assembler.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom reward level checks must stay disabled by default.
- Transaction/failure ordering between custom reward receipt writes and system-mail persistence/fanout is unresolved.
- Date/time parity is source-derived only; DST, IANA/Windows zone id mapping, and Java runtime comparison remain open.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 disabled runtime input assembler and 1 source-derived date/time helper slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 categories: account creation time retention, production quest-finish invocation, live XP mutation, custom reward receipt/mail transaction ordering, and Java runtime date/time comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Preserve login-server account creation time on active C# account/session state without enabling custom reward execution, or add a session-shaped guard adapter around `QuestFinishCustomRewardRuntimeInputAssemblerService`. Keep production quest-finish/custom reward execution disabled.

## Next Work Options

### Recommended Sequential Task

- Task: Add account creation time retention to active C# connection/session state and tests proving it can feed the disabled runtime input assembler.
- Why: The assembler exists, but the Java faction-pack account creation input is still not retained after login.
- Files: likely `GameServerConnection` and a focused test file. Avoid production quest-finish/custom reward invocation.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Mail list packet splitting tests | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or new packet test file | Low/Medium | Independent from account/session state. |
| B | Additional date/time conversion vectors | `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.cs` | Low/Medium | Avoid named-zone assumptions unless cross-platform behavior is verified. |
| C | Live DB run/hardening of system-mail opt-in integration suite | no code unless failures are isolated | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION=1` and MySQL. |
| D | Read-only Java account/session model analysis | read-only | Low | Can map `PlayerAccount` creation time and login bridge propagation before state changes. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Agent A | Add mail list packet splitting tests | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or a new packet test file | production code; docs |
| Orchestrator | Account creation retention or session-shaped guard adapter | exact files selected after analysis | live quest finish; custom reward execution; mail executor |

### Do Not Parallelize

- `GameServerConnection`, `Player`, login auth/session state, or production quest-finish wiring if account creation retention is being implemented.
- `QuestFinishOperationPlanService`, `QuestXpExecutionPlanService`, `QuestXpLevelChangeContextFactoryService`, and `QuestFinishCustomRewardRuntimeInputAssemblerService` if runtime input assembly changes continue.
- Phase 6 progress and handoff docs: orchestrator-owned only.
