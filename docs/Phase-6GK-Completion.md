# Phase 6GK Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GJ and covers Session 681.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionGroupInviteTests|PlayerGroupInviteRequestServiceTests|ClientPacketFactory_ParsesInviteToGroupPacket|DeniesGroupRequests"`
  - Result: Passed, 13 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1195 tests.

## Recent Work Completed

### Session 681 - Production Group Invite Packet Wiring

- Source-read Java `CM_INVITE_TO_GROUP`, `AionClientPacketFactory`, `DeniedStatus`, and the existing group invite handler.
- Added `CmInviteToGroup` for Java opcode `97`.
- Registered opcode `97` in `GameClientPacketFactory`.
- Added `PlayerSettings.DenyGroupRequests` for Java `DeniedStatus.GROUP`.
- Added `SmSystemMessage.PartyCantInviteWhenDead` and `SmSystemMessage.RejectedInviteParty`.
- Routed invite type `0` through `GameServerConnection.HandleInviteToGroupAsync`.
- Routed question id `60000` through `HandleGroupInviteQuestionResponseAsync`.
- Added group-entered plan packet fanout through the connection registry or active connection fallback.
- Invite types `12` (alliance) and `28` (league) remain deferred.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_INVITE_TO_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmInviteToGroup` / `GameServerConnection.HandleInviteToGroupAsync` | Client Packet / Handler | Partial | Unit Tested / Regression Tested | Needs Verification | Opcode `97` and invite type `0` group path are routed. Invite types `12` and `28` remain deferred. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `Aion.GameServer.Network.Aion.GameClientPacketFactory` | Packet Factory | Partial | Unit Tested | Needs Verification | Registers opcode `97` for in-game state. |
| `com.aionemu.gameserver.model.gameobjects.player.DeniedStatus` | `Aion.GameServer.Model.GameObjects.PlayerSettings.DenyGroupRequests` | Enum / Bitmask | Partial | Unit Tested | Needs Verification | Adds Java `GROUP(4)` deny mask. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerSettings.isInDeniedStatus` | `PlayerSettings.DeniesGroupRequests` | Player Settings | Partial | Unit Tested | Needs Verification | Group invite rejection checks the represented deny bit. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.inviteToGroup` | `GameServerConnection.HandleInviteToGroupAsync` + `PlayerGroupInviteRequestService.SendInvite` | Service / Request Starter | Partial | Unit Tested / Regression Tested | Needs Verification | Sends inviter message, registers pending request, and sends question id `60000`. Full `PlayerRestrictions` remains missing. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupInvite` | `GameServerConnection.HandleGroupInviteQuestionResponseAsync` + `PlayerGroupInviteRequestService.HandleResponse` | Request Handler | Partial | Unit Tested / Regression Tested | Needs Verification | Deny and accept response path is wired; Java anonymous handler execution remains unported. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `QuestionResponseRegistry.Respond` via group invite connection path | Request Registry Method | Partial | Unit Tested / Regression Tested | Needs Verification | Registry consumption is now reachable from packet response handling. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_PARTY_DO_YOU_ACCEPT_INVITATION` | `SmQuestionWindow.PartyInvite` | Server Packet / Question Id | Partial | Unit Tested / Regression Tested | Needs Verification | Prompt code `60000` is routed to the invited player. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_PARTY_CANT_INVITE_WHEN_DEAD` | `SmSystemMessage.PartyCantInviteWhenDead` | Server Packet / System Message | Partial | No dedicated packet test in this unit | Needs Verification | Dead inviter path is source-derived but not separately asserted in focused tests. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_PARTY` | `SmSystemMessage.RejectedInviteParty` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Group-deny path asserts id `1390116`. |
| `com.aionemu.gameserver.world.World.getPlayer` | `IGameClientConnectionRegistry.TryGetOnlinePlayerByName` | Runtime Lookup | Partial | Regression Tested | Needs Verification | Uses online connection registry rather than Java singleton world lookup. |
| `com.aionemu.gameserver.utils.ChatUtil.getRealCharName` | Case-insensitive registry lookup | Utility Dependency | Partial | Regression Tested | Needs Verification | Full Java name canonicalization is not ported here. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.inviteToAlliance` | Deferred from `CmInviteToGroup.InviteType == 12` | Service Dependency | Not Started | No Tests | Unknown | Java dispatches alliance invite for type `12`. |
| `com.aionemu.gameserver.model.team.league.LeagueService.inviteToLeague` | Deferred from `CmInviteToGroup.InviteType == 28` | Service Dependency | Partial elsewhere / Not wired here | No Tests in this unit | Needs Verification | League invite planner exists elsewhere but packet-level invite type is not wired. |
| `com.aionemu.gameserver.restrictions.PlayerRestrictions.canInviteToGroup` | Not fully ported for group invite | Restriction Dependency | Not Started | No Tests | Unknown | Only dead, missing target, and deny-list guards are modeled in this packet unit. |

## Tests Added Or Updated

- `GamePacketTests.ClientPacketFactory_ParsesInviteToGroupPacket`
- `PlayerSettingsTests.DeniesGroupRequests_FollowsJavaDeniedStatusMask`
- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_GroupInviteSendsInviterMessageAndQuestion`
- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_NoSuchUserSendsJavaFailure`
- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_DeniedGroupRequestsSendsRejectedInvite`
- `GameServerConnectionGroupInviteTests.HandleQuestionResponseAsync_GroupInviteDenyClearsRequestAndRejectsInviter`
- `GameServerConnectionGroupInviteTests.HandleQuestionResponseAsync_GroupInviteAcceptCreatesGroupAndFansOutEnteredPackets`

These tests are source-derived from Java; they do not compare against Java runtime execution, golden bytes, encrypted frames, full `PlayerRestrictions`, alliance/league invite types, `FindGroupService`, reflection callback behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 16
- Total artifacts ported or partially modeled in this handoff window: 1 production group-invite packet/request/response wiring slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 16
- Total blocked/not-started artifacts: invite type `12` alliance wiring, invite type `28` league wiring, full `PlayerRestrictions` parity, full `ChatUtil` name normalization, `FindGroupService` parity, Java group id behavior, generic anonymous handler callback execution, real socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Invite types `12` and `28` are still deferred from `CM_INVITE_TO_GROUP`.
- Full `PlayerRestrictions.canInviteToGroup` remains missing.
- Java `ChatUtil.getRealCharName` canonicalization is not fully modeled.
- Java `FindGroupService.onJoinedTeam`, offline group checker startup, and complete group event fanout remain outside this unit.
- Group id semantics differ because C# runtime requires a positive group id.
- Generic Java `RequestResponseHandler` callback execution remains partial.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Continue `CM_INVITE_TO_GROUP` parity by wiring invite type `28` to the existing league invite planner or invite type `12` to an alliance invite service if the C# alliance runtime is ready. If those fanouts are too broad, pick the next narrow `ResponseRequester` handler with production packet reachability, such as duel request, craft-skill learn confirmation, or experience recovery dialog.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GJ-Completion.md`
   - this handoff
3. Inspect the selected Java `ResponseRequester` user and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
