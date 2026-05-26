# Phase 6VE Completion - UOW-1065 System Mail Persistence Metadata

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VD-Completion.md`.

## Last Completed Unit

UOW-1065: `[Phase 6][UOW-1065] Stage system mail persistence metadata`

Recent commits before this unit:

- `db72654bd [Phase 6][UOW-1064] Compose custom reward execution metadata`
- `335e248b6 [Phase 6][UOW-1063] Add custom reward execution boundary`
- `0debc3e62 [Phase 6][UOW-1062] Add system mail reward planning`

## Summary

UOW-1065 adds a non-live operation planner for system-mail persistence and mailbox fanout.

`SystemMailRewardPersistencePlanService` converts a planned `SystemMailRewardPlan` into Java-order metadata for `SystemMailService.sendMail`: `MailDAO.storeLetter`, optional `InventoryDAO.store`, then either offline mailbox-counter update or online mailbox/packet fanout. It records Java SQL and parameter order for the mail, inventory, and offline counter DAO work, plus metadata for `SM_MAIL_SERVICE`, open-mailbox `MailService.sendMailList`, and express `STR_POSTMAN_NOTIFY`.

This does not execute SQL, mutate mailboxes, send packets, or enable default live starter/custom reward delivery.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| Stage system-mail persistence/fanout metadata | New service/test, mail audits/docs | Shared mail reward boundary; single owner preferred | Selected for UOW-1065 |
| Live mail persistence adapter | `IMailRepository`, inventory persistence, packet fanout | Wider runtime and failure-ordering scope | Defer |
| Runtime custom reward input adapter | XP context factory, account/DAO/time inputs | Needs explicit disabled gate | Recommended next |
| Live XP execution | Player mutation, packets, persistence | Unsafe until prerequisites are verified | Defer |

No subagents were used. File ownership stayed with the main orchestrator because this unit created a shared mail reward prerequisite and updated shared progress/handoff docs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/SystemMailRewardPersistencePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SystemMailRewardPersistencePlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VE-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SystemMailRewardPersistencePlanServiceTests" --nologo` | Passed: 3 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1872 |

## Migration Parity Table - UOW-1065

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | `Aion.GameServer.Services.SystemMailRewardPersistencePlanService`; `SystemMailRewardPersistencePlan` | Mail Service / Persistence Metadata | Partial | Unit Tested | Partial Parity | C# now stages Java DAO and mailbox-fanout ordering after a planned system-mail payload: store letter, optionally store attached item, then offline counter or online mailbox/packet fanout. It does not execute SQL, mutate mailboxes, send packets, allocate ids, or verify Java runtime output. |
| `com.aionemu.gameserver.dao.MailDAO.storeLetter`; `saveLetter` | `SystemMailRewardPersistenceOperation.StoreLetter`; `SystemMailRewardPersistencePlanService.JavaMailInsertSql` | DAO / SQL Metadata | Partial | Unit Tested | Partial Parity | Java insert SQL, misspelled `recieved_time` column, parameter order, mail id, recipient id, and attached-item object id are recorded. No live DB integration or transaction behavior is executed. |
| `com.aionemu.gameserver.dao.InventoryDAO.store`; `insertItems` | `SystemMailRewardPersistenceOperation.StoreAttachedItem`; `SystemMailRewardPersistencePlanService.JavaInventoryInsertSql` | DAO / SQL Metadata | Partial | Unit Tested | Partial Parity | Attached mailbox item persistence intent records Java insert SQL and 28-column parameter order. C# does not execute the insert, persist item stones, release ids, or verify transaction/rollback behavior. |
| `com.aionemu.gameserver.dao.MailDAO.updateOfflineMailCounter` | `SystemMailRewardPersistenceOperation.UpdateOfflineMailboxCounter` | DAO / SQL Metadata | Partial | Unit Tested | Partial Parity | Offline recipient branch records Java `UPDATE players SET mailbox_letters=? WHERE name=?` SQL and increments from the planned recipient mailbox count. No live DB update or Java runtime comparison. |
| `com.aionemu.gameserver.services.mail.SystemMailService.updateRecipientMailbox` | `SystemMailRewardPersistenceOperation` online mailbox/fanout operations | Mailbox / Packet Fanout Metadata | Partial | Unit Tested | Partial Parity | Online recipient branch records mailbox insertion, `SM_MAIL_SERVICE`, open-mailbox `MailService.sendMailList`, express-only flag derivation, and `STR_POSTMAN_NOTIFY` intent. It does not mutate C# live player state or serialize/send packets in this flow. |
| `com.aionemu.gameserver.services.mail.MailService.sendMailList` | `SystemMailRewardPersistenceOperation.SendMailListPackets` | Packet Fanout Dependency | Not Started | Unit Tested as metadata | Needs Verification | Newly discovered dependency for open mailbox refresh. C# only records future packet-list intent; list splitting, packet serialization, and socket ordering are covered elsewhere only partially and are not exercised by this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE` | `Aion.GameServer.Network.Aion.ServerPackets.SmMailService` via metadata reference | Packet Dependency | Partial | Unit Tested elsewhere / Metadata Only in this unit | Needs Verification | Metadata points at the existing C# packet class but does not instantiate or serialize it. Packet byte parity is not verified for this reward-delivery flow. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.PostmanNotify` via metadata reference | Packet Dependency | Partial | Unit Tested elsewhere / Metadata Only in this unit | Needs Verification | Metadata records express notification intent after online mailbox/list fanout. This unit does not send or serialize the packet. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `SystemMailRewardPersistencePlanServiceTests.CreatePlan_StagesOfflineItemMailPersistenceInJavaFailureOrder` | Offline item-mail persistence metadata stages store-letter, store-attached-item, then offline mailbox counter update with Java SQL and parameter order. | Source-reviewed `SystemMailService.sendMail`, `MailDAO.saveLetter`, `InventoryDAO.insertItems`, and `MailDAO.updateOfflineMailCounter`. |
| `SystemMailRewardPersistencePlanServiceTests.CreatePlan_StagesOnlineExpressMailboxFanoutAfterPersistence` | Online express recipient metadata stages mailbox insertion, mailbox state packet, open-mailbox refresh, express-only flag, and postman notify after persistence intents. | Source-reviewed `SystemMailService.updateRecipientMailbox`, `MailService.sendMailList`, and `SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY`. |
| `SystemMailRewardPersistencePlanServiceTests.CreatePlan_SkipsUnplannedMailAndDoesNotStageDaoWork` | Guarded/unplanned mail payloads do not stage DAO, item, counter, or packet operations. | Uses existing system-mail plan guard behavior before Java DAO calls. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- This unit records SQL/packet intent only; no `IMailRepository`, `InventoryDAO`, mailbox counter, or socket fanout execution is enabled.
- Java `MailDAO.storeLetter` and `InventoryDAO.store` are not transactional together in `SystemMailService`; C# live adapter policy still needs explicit failure-ordering and rollback decisions before execution.
- Attached-item persistence omits item-stone side effects and any generated-item default fields beyond those already carried by `InventoryItem`.
- Online mailbox fanout is metadata-only; live `GameClientSocketServer.NotifyMailReceivedAsync` is not called from custom/starter rewards.
- Packet serialization, packet splitting, socket ordering, thread affinity, and recipient-online lookup remain unverified for this flow.
- Date/time handling remains caller-supplied from `SystemMailRewardPlan`; Java uses `new Timestamp(System.currentTimeMillis())`.
- Reflection, serialization, precision/rounding, and broader persistence parity remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 in this unit
- Total artifacts ported: 1 non-live system-mail persistence/fanout operation planner
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 7 categories: Java runtime comparison, live mail repository execution, attached-item DB integration, item-stone persistence, online packet fanout, live XP/custom reward integration, and failure-ordering policy
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Bridge `SystemMailRewardPersistencePlanService` into an explicit disabled-by-default system-mail execution adapter, or add a disabled-by-default runtime adapter that gathers custom reward execution inputs for XP level-change composition.
