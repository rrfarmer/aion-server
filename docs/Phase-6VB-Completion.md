# Phase 6VB Completion - UOW-1062 System Mail Reward Planning

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VA-Completion.md`.

## Last Completed Unit

UOW-1062: `[Phase 6][UOW-1062] Add system mail reward planning`

Recent commits before this unit:

- `69574ff45 [Phase 6][UOW-1061] Add custom reward receipt repository`
- `876915eea [Phase 6][UOW-1060] Compose quest-finish XP execution metadata`
- `096a57b9b [Phase 6][UOW-1059] Stage XP level-change context factory`

## Summary

UOW-1062 added a non-live system-mail reward planner for starter/custom reward descriptors.

`SystemMailRewardPlanService` applies Java `SystemMailService.sendMail` guard and truncation rules, then shapes successful descriptor payloads into non-live `PlayerMail` metadata plus optional mailbox-location `InventoryItem` attachment metadata. It does not allocate ids, persist mail/items, update mailbox counters, send packets, or mutate recipient mailbox state.

## Parallel Work Discovery

| Candidate | Files / Areas | Parallel Safety | Decision |
| --- | --- | --- | --- |
| System-mail reward payload planning | New mail planner/tests, XP/reward docs | Shared starter/custom mail surface; single owner needed | Selected for UOW-1062 |
| Opt-in custom reward executor | Receipt repo, mail planner, operation boundaries | Depends on UOW-1061/UOW-1062 prerequisites | Recommended next |
| Live mail persistence/fanout | `IMailRepository`, connection registry, packets | Wider failure-ordering and runtime scope | Defer |
| Live XP execution | Player mutation, packets, persistence | Unsafe until prerequisites are verified | Defer |

No subagents were used. File ownership stayed with the main orchestrator because this unit introduced a shared mail-planning service and docs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/SystemMailRewardPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SystemMailRewardPlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VB-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SystemMailRewardPlanServiceTests" --nologo` | Passed: 3 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1863 |

## Migration Parity Table - UOW-1062

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.mail.SystemMailService.sendMail` | `Aion.GameServer.Services.SystemMailRewardPlanService`; `SystemMailRewardPlan`; `SystemMailRewardRequest` | Service / Mail Plan | Partial | Unit Tested | Partial Parity | C# now plans Java-style system-mail payloads for starter/custom reward descriptors, including guard/truncation rules and attachment metadata. It does not persist letters/items, allocate ids, update mailbox counters, send `SM_MAIL_SERVICE`, send postman notifications, or mutate player mailboxes. |
| `com.aionemu.gameserver.model.gameobjects.Letter` | `Aion.GameServer.Model.GameObjects.PlayerMail` via `SystemMailRewardPlan.Mail` | DTO / Mail Payload | Partial | Unit Tested | Needs Verification | Planner creates unread express `PlayerMail` metadata with sender/title/body/item/kinah/time. Java `Letter` runtime fields, DB serialization, packet serialization in this flow, and online/offline update behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemFactory.newItem`; `com.aionemu.gameserver.model.items.storage.StorageType.MAILBOX` | `Aion.GameServer.Model.GameObjects.InventoryItem` via `SystemMailRewardPlan.Mail.AttachedItem` | Item Attachment Metadata | Partial | Unit Tested | Needs Verification | Planner creates unequipped mailbox-location attached item metadata using caller-supplied object ids. Java item factory defaults, object-id allocation, inventory DB writes, item stones/sockets, expiration, and template-derived defaults remain unverified. |
| `com.aionemu.gameserver.services.StarterKitService.onLevelUp`; `BonusPackService.addPlayerCustomReward`; `FactionPackService.addPlayerCustomReward` | `StarterKitLevelChangeDescriptor`; `CustomLevelRewardDescriptor`; `SystemMailRewardPlanService` | Reward Mail Dependency | Partial | Unit Tested as metadata | Needs Verification | Starter/custom reward descriptors can now be shaped into non-live mail payloads, but no opt-in executor coordinates receipt DAO writes, mail persistence, item persistence, mailbox update, or failure ordering. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `SystemMailRewardPlanServiceTests.CreatePlan_ShapesCustomRewardExpressMailWithoutSending` | Custom reward descriptor becomes unread express `PlayerMail` plus mailbox-location attached item metadata without mutating the recipient. | Source-reviewed `SystemMailService.sendMail` and `BonusPackService.addPlayerCustomReward`. |
| `SystemMailRewardPlanServiceTests.CreatePlan_ShapesStarterKitMailAndTruncatesJavaTitleAndMessageLimits` | Java title/message truncation limits and express letter type mapping. | Source-reviewed `SystemMailService.sendMail`. |
| `SystemMailRewardPlanServiceTests.CreatePlan_RecordsJavaSystemMailGuardBranches` | Missing recipient, invalid item count, missing item template, name length, `$$` sender exception, and mailbox-full guards. | Source-reviewed `SystemMailService.sendMail` guard order. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The planner is not wired into live starter/custom reward execution.
- The planner uses caller-supplied mail/item object ids and received time; Java allocates ids and timestamps inside `SystemMailService`.
- No `MailDAO.storeLetter`, `InventoryDAO.store`, `MailDAO.updateOfflineMailCounter`, online mailbox update, `SM_MAIL_SERVICE`, mail-list refresh, or postman notification is executed.
- Java `ItemFactory.newItem` template-derived defaults are only partially represented by C# `InventoryItem` metadata.
- Threading, serialization, date/time, precision/rounding, reflection, packet fanout, and persistence parity remain unverified for system-mail reward delivery.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 non-live system-mail reward payload planner
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 7 categories: Java runtime comparison, live reward execution, id allocation, live mail persistence, attached item persistence, online/offline mailbox fanout, and XP level-change execution
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Bridge `ICustomLevelRewardRepository` and `SystemMailRewardPlanService` into an opt-in custom reward execution boundary. Keep live XP execution disabled by default.
