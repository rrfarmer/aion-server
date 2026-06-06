# Phase 6 Session 2776 Completion

## Unit of Work

[Phase 6][UOW-2776] Guard full legion invite acceptance

## Runtime Progress Gate

- Deferred/live behavior advanced: live legion invite acceptance persisted new members before checking Java's legion max-member guard.
- Java source of truth: `LegionService.addToLegion`, `Legion.addLegionMember`, `Legion.canAddMember`, `LegionConfig.LEGION_LEVEL{1..8}_MAX_MEMBERS`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE`.
- C# runtime artifact wired/fixed: `GameServerConnection.AcceptLegionInviteAsync`, `GameServerOptions.Legion.LevelMaxMembers`, Java config loading for `gameserver.legion.level{1..8}maxmembers`, and `SmSystemMessage.GuildInviteCanNotAddMemberAnyMore`.
- Client-visible/state/persistence effect changed: accepting an invite into a full legion now sends the inviter Java message `1300257`, does not persist a new `legion_members` row, does not mutate the responder's legion state, and does not broadcast join packets.
- Why this is not preview-only/test-only/documentation-only: it changes live invite-acceptance control flow, reads runtime persisted member count, prevents invalid persistence/state mutation, and sends a real server packet from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Added Java max-member legion options for levels 1 through 8, including Java config loader keys and defaults.
- Added system message helper for `STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE` with message id `1300257`.
- Added a live invite-acceptance guard that counts current persisted legion members before `SaveNewLegionMemberAsync`.
- Returned without persistence, responder mutation, history insert, direct responder packets, or broadcasts when the legion is full.

## Validation Decision

- Changed surface: live question-response invite acceptance, Java config option loading, and a server system message helper.
- Specific behavior/contract: Java allows add only when `currentMembers < maxMembers` for the inviter legion level and sends `1300257` to the inviter when full.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptions" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for the live `Legion.canAddMember` invite path.
- Broad-validation trigger: none. The change is isolated to a live legion invite branch and config values, and the filtered tests compile the touched project plus assert the live side effects.

## Validation Result

- Focused C# result: Passed, 93 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_LegionInviteAcceptFullLegionNotifiesInviterAndDoesNotPersistLikeJava` | Unit | `LegionService.addToLegion` and `Legion.canAddMember` | Full legion invite acceptance sends `1300257` to inviter and avoids persistence, state mutation, history, responder packets, and broadcasts. | Live handler side-effect assertions from Java source review. | C# uses DB count rather than Java's in-memory legion member set. |
| `LoadFromJavaConfig_UsesJavaPropertiesWhenPresent` | Unit | `LegionConfig.LEGION_LEVEL{1..8}_MAX_MEMBERS` | Java max-member config keys flow into `GameServerOptions.Legion.LevelMaxMembers`. | Config loader assertion with Java property override. | No external Java config golden fixture. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.legion.Legion.canAddMember` | `GameServerConnection.CanAddLegionInviteMemberAsync` | Runtime State Guard | Partial | Unit Tested | Partial Parity | Enforces Java max-members by level before invite persistence. Uses persisted DB count instead of Java in-memory `memberIds`. |
| `com.aionemu.gameserver.configs.main.LegionConfig.LEGION_LEVEL{1..8}_MAX_MEMBERS` | `GameServerLegionOptions.LevelMaxMembers` | Runtime Config | Ported | Unit Tested | Partial Parity | Defaults and Java property keys are modeled; full Java config file coverage is broader than this UOW. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE` | `SmSystemMessage.GuildInviteCanNotAddMemberAnyMore` | Server Packet Helper | Ported | Unit Tested through handler | Partial Parity | Message id `1300257` is sent from live invite failure path. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 3 runtime/config/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked/not-started artifacts: 2 invite/roster gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# member list still includes only online same-legion players visible through the connection registry; Java uses the full legion member cache including offline members.
- Java splits member lists into chunks of 80; the C# invite path currently sends a single online chunk.
- Member-list house address and door-state ids are currently zero because the C# invite path does not query active houses for roster rows.
- C# still does not model Java's full in-memory `Legion` aggregate and `memberIds`; this UOW uses the existing DB count hook for live enforcement.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- No Java golden fixture was generated for the full-legion invite failure path.
