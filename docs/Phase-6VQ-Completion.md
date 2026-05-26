# Phase 6VQ Completion - UOW-1077 Account Creation Runtime State

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VP-Completion.md`.

## Last Completed Unit

UOW-1077: `[Phase 6][UOW-1077] Preserve account creation runtime state`

Recent commits before this unit:

- `ac3af9231 [Phase 6][UOW-1076] Add quest-finish custom reward runtime input assembler`
- `24d054f04 [Phase 6][UOW-1075] Audit quest-finish custom reward runtime inputs`
- `e6cce6cc3 [Phase 6][UOW-1074] Add quest-finish custom reward context adapter`

## Summary

UOW-1077 preserves the login-server account creation epoch milliseconds needed by Java `FactionPackService.sendRewards` without enabling production custom reward execution.

C# already parsed the login-server auth response field into `AccountAuthResult.CreationDate`; this unit carries positive values through `GameServerConnection` and applies them to the active `Player` after successful enter-world.

Missing or fake-auth creation time remains `null`, so the UOW-1076 assembler can continue to treat account creation as a required dependency before enabled custom reward execution.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAccountRuntimeStateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAccountRuntimeStateServiceTests.cs`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VQ-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerAccountRuntimeStateServiceTests\|QuestFinishCustomRewardRuntimeInputAssemblerServiceTests" --nologo` | Passed: 8 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1904 |

## Migration Parity Table - UOW-1077

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.account.Account` | `Aion.GameServer.Model.GameObjects.Player.AccountCreationEpochMillis`; `Aion.GameServer.Services.PlayerAccountRuntimeState` | Runtime Account State | Partial | Unit Tested | Partial Parity | C# does not yet have a full game-server `Account` aggregate like Java. This unit preserves only the login-server account creation epoch milliseconds needed by faction/custom reward consumers. Fake-auth or missing login-server creation remains `null`, an intentional C# guard difference from Java's primitive long default. |
| `com.aionemu.gameserver.services.AccountService.getAccount` | `Aion.GameServer.Services.PlayerAccountRuntimeStateService.ApplyLoginAccountState` | Account Auth State Applier | Partial | Unit Tested | Partial Parity | Java builds an `Account` and stores name, creation date, account time, access level, membership, toll, and allowed HDD serial. C# applies only access level, membership, and account creation milliseconds to the active player; account time, toll, allowed HDD serial, account warehouse, deleted-character pruning, and full account aggregate behavior remain outside this unit. |
| `com.aionemu.gameserver.network.loginserver.clientpackets.CM_ACOUNT_AUTH_RESPONSE` | `Aion.GameServer.Network.LoginServer.AccountAuthResult`; `Aion.GameServer.Network.Aion.GameServerConnection` | Login Bridge / Session State | Partial | Unit Tested at state boundary | Needs Verification | C# already parses creation milliseconds from the login-server bridge and now stores positive values on the connection before enter-world. Tests cover the state applier, not encrypted socket login, reconnect, auth failure reset ordering, or Java runtime packet comparison. |
| `com.aionemu.gameserver.services.FactionPackService.sendRewards` | `Player.AccountCreationEpochMillis` feeding future `QuestFinishCustomRewardRuntimeInputAssemblerService` calls | Custom Reward Runtime Input Dependency | Partial | Unit Tested as dependency carrier | Needs Verification | The active player can now carry the Java account-creation dependency, but no production quest-finish/custom reward path reads it yet. Date/time conversion, item-template filtering, receipt writes, and system mail remain guarded by the disabled assembler. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `PlayerAccountRuntimeStateServiceTests.ApplyLoginAccountState_CarriesLoginServerCreationMillisToActivePlayer` | Access level, membership, and login-server creation epoch milliseconds are copied to the active player state. | Source-reviewed from `AccountService.getAccount`; no socket or Java runtime comparison. |
| `PlayerAccountRuntimeStateServiceTests.ApplyLoginAccountState_PreservesMissingCreationMillisAsNull` | Missing creation milliseconds remain absent instead of becoming Unix epoch zero. | Intentional C# guard so enabled custom reward assembly still refuses missing Java dependency. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- End-to-end login-server auth/reconnect/enter-world socket ordering is not covered by this unit.
- C# still lacks a full Java-like game-server `Account` aggregate and account-time/toll/allowed-HDD/account-warehouse behavior on active player state.
- Production quest-finish and socket handlers do not invoke the custom reward assembler.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom reward level checks must stay disabled by default.
- Transaction/failure ordering between custom reward receipt writes and system-mail persistence/fanout is unresolved.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 partial runtime account-state carrier and 1 account-state applier
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 5 categories: full account aggregate, auth/reconnect integration, production quest-finish invocation, live XP mutation, and custom reward receipt/mail transaction ordering
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Add a disabled quest-finish/session assembly adapter that reads `Player.AccountCreationEpochMillis`, `IDFactory.NextId`, and static item templates into `QuestFinishCustomRewardRuntimeInputAssemblerService` without invoking production custom reward execution. Keep production quest-finish/custom reward execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Mail list packet splitting tests | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or new packet test file | Low/Medium | Independent from account/session state. |
| B | Additional date/time conversion vectors | `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.cs` | Low/Medium | Avoid named-zone assumptions unless cross-platform behavior is verified. |
| C | Live DB run/hardening of system-mail opt-in integration suite | no code unless failures are isolated | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION=1` and MySQL. |
| D | Read-only Java account/session model analysis for full account aggregate parity | read-only | Low | Useful before porting account time/toll/warehouse state. |

## Do Not Parallelize

- `GameServerConnection`, `Player`, login auth/session state, or production quest-finish wiring if the next unit reads account creation into assembly.
- `QuestFinishOperationPlanService`, `QuestXpExecutionPlanService`, and custom reward assembler files if runtime input assembly changes continue.
- Phase 6 progress and handoff docs: orchestrator-owned only.
