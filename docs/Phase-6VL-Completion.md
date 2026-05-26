# Phase 6VL Completion - UOW-1072 System Mail Online Fanout Packet Order

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VK-Completion.md`.

## Last Completed Unit

UOW-1072: `[Phase 6][UOW-1072] Add system mail online fanout packet order tests`

Recent commits before this unit:

- `16023bb7b [Phase 6][UOW-1071] Add system mail store-letter failure integration gate`
- `b2f5db566 [Phase 6][UOW-1070] Add system mail failure-ordering integration gate`
- `80007a151 [Phase 6][UOW-1069] Add system mail repository integration gate`

## Summary

UOW-1072 adds regression coverage for the online recipient branch of `SystemMailService.updateRecipientMailbox` as represented by `GameClientSocketServer.NotifyMailReceivedAsync`.

The new tests use a captured `GameServerConnection` send observer and verify that online mail fanout follows Java order: mailbox state packet first, optional open-mailbox list refresh second, and express postman notification last. The express open-mailbox test also verifies unread express mail filtering and newest-first list order.

This improves online fanout confidence, but it does not enable automatic reward mail delivery and does not compare against a Java runtime packet capture.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| Online mailbox fanout packet-order tests | New isolated test file | Safe isolated test-only unit | Selected for UOW-1072 |
| Runtime custom reward input adapter | XP context factory, custom reward repository inputs, system-mail execution options | Needs explicit disabled gate and shared service design | Defer |
| Live DB run/hardening | Environment-dependent opt-in DB suite | Blocked by missing enabled DB gate in default environment | Defer |
| XP live wiring risk audit | Docs only | Safe parallel support task | Defer |

File ownership map:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Online system-mail fanout regression tests and docs | `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerMailFanoutTests.cs`; Phase 6 docs/handoff | Production code; existing DB integration test fixture | Tests, docs, parity table, commit |

No subagents were used. The selected unit was a focused test-only change around one shared registry method, and adding agents would not have reduced risk.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerMailFanoutTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VL-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameClientSocketServerMailFanoutTests" --nologo` | Passed: 3 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1889 |

## Migration Parity Table - UOW-1072

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.mail.SystemMailService.updateRecipientMailbox` | `Aion.GameServer.Network.Aion.GameClientSocketServer.NotifyMailReceivedAsync`; `GameClientSocketServerMailFanoutTests` | Online Mailbox Fanout | Partial | Regression Tested | Partial Parity | C# online fanout now has regression coverage for mailbox insertion, mailbox state packet, optional open-mailbox list refresh, express-only filtering, and express postman notification order. It is still not wired into default XP/custom reward gameplay and no Java runtime packet capture was compared. |
| `com.aionemu.gameserver.services.mail.MailService.sendMailList` | `Aion.GameServer.Network.Aion.ServerPackets.SmMailService.CreateListPackets`; `GameClientSocketServerMailFanoutTests` | Packet List Fanout | Partial | Regression Tested | Partial Parity | Test verifies open express mailbox sends list packet after state packet, filters unread express mail, and keeps newest-first ordering. Packet splitting beyond the small list, Java runtime bytes, and socket-level ordering under load remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE` | `Aion.GameServer.Network.Aion.ServerPackets.SmMailService` | Packet | Partial | Regression Tested | Partial Parity | Tests serialize C# mailbox state/list packets to inspect service ids and list ids in send order. Existing packet tests cover more byte fields, but this unit does not compare against Java golden bytes for this exact fanout path. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.PostmanNotify` | Packet | Partial | Regression Tested | Partial Parity | Test verifies message id `1300899` is sent after mailbox state/list fanout for express mail. Java runtime comparison and real socket capture remain missing. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `GameClientSocketServerMailFanoutTests.NotifyMailReceivedAsync_SendsMailboxStateListThenPostmanNotifyForOpenExpressMailbox` | Online express mail with open express mailbox sends mailbox state, express-only mail list, then postman notification; mailbox is updated before sends. | Source-reviewed `SystemMailService.updateRecipientMailbox`, `MailService.sendMailList`, and `SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY`. No Java runtime packet capture. |
| `GameClientSocketServerMailFanoutTests.NotifyMailReceivedAsync_SendsOnlyMailboxStateForClosedNormalMailbox` | Closed normal-mail recipient gets mailbox state only, with no list refresh or postman notify. | Source-reviewed Java branch. No Java runtime packet capture. |
| `GameClientSocketServerMailFanoutTests.NotifyMailReceivedAsync_ReturnsFalseWithoutSendingWhenRecipientOffline` | Missing online recipient returns false and sends no packets. | Source-reviewed Java online branch dependency. Offline DB counter behavior is covered separately. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Default XP/custom reward gameplay still does not invoke live system-mail execution.
- The opt-in DB integration suite has not been run against a live DB in this session.
- Packet splitting, socket ordering under load, thread affinity, and real-client mailbox UI behavior remain unverified for reward mail.
- Java `InventoryDAO.store` item-stone side effects and generated item defaults remain unverified for system-mail attachments.
- Date/time capture, serialization beyond inspected packet fields, reflection, and precision/rounding remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 online mailbox fanout regression test surface covering 4 Java artifacts/dependencies
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 6 categories: Java runtime comparison, default XP/custom reward integration, live DB gate not run, item-stone persistence, generated item defaults, and real socket/client validation
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Add a disabled-by-default runtime adapter that gathers custom reward execution inputs for XP level-change composition, or run the opt-in system-mail DB integration suite against a live MySQL schema when available. Keep automatic reward mail delivery disabled by default.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled-by-default runtime adapter that collects custom reward execution inputs and emits `QuestXpLevelChangeContextFactoryInput` with supplied custom reward execution results.
- Why: System-mail planning, persistence, DB gates, and online fanout now have staged evidence; the missing bridge is safe runtime input gathering without enabling automatic XP mutation.
- Files: likely `dotnetConversion/src/Aion.GameServer/Services/*CustomReward*` or a new service file plus focused tests.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Java-only analysis of custom reward runtime input dependencies | read-only Java/C# service files | Low | No writes; can map account/time/static-data dependencies. |
| B | Packet splitting coverage for `SmMailService.CreateListPackets` with larger mailboxes | new or existing packet test file only | Low/Medium | Avoid editing registry tests concurrently. |
| C | Live DB run/hardening of system-mail opt-in integration suite | no code unless failures are isolated | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION=1` and MySQL. |
| D | Documentation-only audit of XP live wiring blockers | docs only | Low | Orchestrator should own shared docs if code work is also active. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Agent A | Analyze custom reward runtime input dependencies | read-only | all writes |
| Agent B | Add packet splitting tests for mail list packets | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or a new packet test file | production code; docs |
| Orchestrator | Implement runtime adapter after analysis | new service/test files selected after analysis | docs until integration step |

### Do Not Parallelize

- `QuestXpLevelChangeContextFactoryService`, `QuestXpExecutionPlanService`, and custom reward execution services if a runtime adapter is being implemented: these are shared XP/custom reward contract surfaces.
- Phase 6 progress and handoff docs: orchestrator-owned only.
