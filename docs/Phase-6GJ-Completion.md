# Phase 6GJ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GI and covers Session 680.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerGroupInviteRequestServiceTests|SmSystemMessage_WritesDialogTooFarMessages"`
  - Result: Passed, 7 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1188 tests.

## Recent Work Completed

### Session 680 - Group Invite ResponseRequester Slice

- Source-read Java `PlayerGroupService.inviteToGroup` and `PlayerGroupInvite`.
- Added `SmQuestionWindow.PartyInvite` for Java question id `60000`.
- Added `QuestionResponseRequestKind.GroupInvite`.
- Added `SmSystemMessage.PartyInvitedHim` and `SmSystemMessage.PartyHeRejectInvitation`.
- Added `PlayerGroupInviteRequestService`:
  - registers the pending invite,
  - returns inviter notification and invited-player question packet,
  - consumes deny/accept through `QuestionResponseRegistry`,
  - returns the Java rejection message on deny,
  - creates a new C# group or adds the invited player to the inviter's existing group on accept.
- Reviewed NPC warehouse/cube expansion and deferred it because C# lacks the static NPC expander-data import needed for faithful parity.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.inviteToGroup` | `Aion.GameServer.Services.PlayerGroupInviteRequestService.SendInvite` | Service / Request Starter | Partial | Unit Tested | Needs Verification | Registers Java question id `60000`; full invite restrictions are not ported here. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupInvite` | `PlayerGroupInviteRequestService.HandleResponse` | Request Handler | Partial | Unit Tested | Needs Verification | Deny and accept branches are represented; Java anonymous handler execution remains unported. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.createGroup` | `Aion.GameServer.Services.PlayerGroupRuntime.CreateOrUpdateGroup` | Runtime Service | Partial | Unit Tested | Needs Verification | C# requires a supplied positive group id; production wiring must allocate it. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.addPlayer` | `PlayerGroupRuntime.AddMember` | Runtime Service | Partial | Unit Tested | Needs Verification | Existing-group accept is represented; full event fanout remains partial. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry` | Request Registry | Partial | Unit Tested / Regression Tested | Needs Verification | Adds group-invite metadata dispatch and duplicate behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_PARTY_DO_YOU_ACCEPT_INVITATION` | `SmQuestionWindow.PartyInvite` | Server Packet / Question Id | Partial | Unit Tested | Needs Verification | Constant `60000` added; no golden-byte validation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_PARTY_INVITED_HIM` | `SmSystemMessage.PartyInvitedHim` | Server Packet / System Message | Partial | Unit Tested / Regression Tested | Needs Verification | Packet test asserts message id `1300173` and parameter serialization. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_PARTY_HE_REJECT_INVITATION` | `SmSystemMessage.PartyHeRejectInvitation` | Server Packet / System Message | Partial | Unit Tested / Regression Tested | Needs Verification | Packet test asserts message id `1300161` and parameter serialization. |
| `com.aionemu.gameserver.restrictions.PlayerRestrictions` | Not fully ported for group invite | Restriction Dependency | Not Started | No Tests | Unknown | Java invite eligibility is broader than this unit. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | Not ported for group invite | Side-effect Dependency | Not Started | No Tests | Unknown | Java `onJoinedTeam` side effect remains missing. |

## Tests Added Or Updated

- `PlayerGroupInviteRequestServiceTests.SendInvite_RegistersPartyQuestionAndReturnsInviterMessage`
- `PlayerGroupInviteRequestServiceTests.SendInvite_DuplicateLeavesOriginalRequestLikeJavaPutIfAbsent`
- `PlayerGroupInviteRequestServiceTests.HandleResponse_DenyConsumesRequestAndReturnsRejectMessage`
- `PlayerGroupInviteRequestServiceTests.HandleResponse_AcceptCreatesGroupWhenInviterHasNoGroup`
- `PlayerGroupInviteRequestServiceTests.HandleResponse_AcceptAddsInvitedToExistingInviterGroup`
- `PlayerGroupInviteRequestServiceTests.HandleResponse_WrongQuestionLeavesRequest`
- `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` for party invite/reject message ids and parameters.

These tests are source-derived from Java; they do not compare against Java runtime execution, golden bytes, encrypted frames, full invite restrictions, `FindGroupService`, reflection callback behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 group-invite request/response slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: production group-invite command wiring, full `PlayerRestrictions` parity, `FindGroupService` parity, Java group id allocation behavior, generic anonymous handler callback execution, Java concurrent map stress parity, real socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Group invite is not wired to a production C# command/connection path yet.
- `PlayerRestrictions.canInviteToGroup` is not fully ported.
- `FindGroupService.onJoinedTeam`, offline group checker startup, and complete group event fanout are not represented in this unit.
- Java create-group id behavior differs from the C# positive-id runtime requirement.
- Generic Java `RequestResponseHandler` callback execution remains partial.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Either wire the new group-invite service into the production C# invite command path if that command surface is identifiable, or continue with another narrow `ResponseRequester` handler with existing runtime support. Good candidates are alliance invite, duel request, craft-skill learn confirmation, or experience recovery dialog. Defer NPC warehouse/cube expansion until static expander data is imported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GI-Completion.md`
   - this handoff
3. Inspect the selected Java `ResponseRequester` user and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
