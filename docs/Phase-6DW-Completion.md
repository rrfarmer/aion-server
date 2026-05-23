# Phase 6DW Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6DV and covers Sessions 602-604.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~GamePacketTests"`
  - Result: Passed, 152 tests.
- Latest full validation:
  - `dotnet test dotnetConversion\AionServer.slnx`
  - Result: Passed, 1250 tests.

## Recent Work Completed

### Session 602 - Alliance Leave Fanout Plan

- Source-read Java `PlayerLeavedEvent` base behavior and leave-related `SM_SYSTEM_MESSAGE` factories.
- Added `PlayerAllianceLeaveReason`, `PlayerAllianceLeavedPlan`, and `PlayerAllianceLeavedPlanner`.
- Added Java leave system-message factories for disband, offline timeout, leave, ban-me, and ban-him ids.
- Modeled `PlayerAllianceLeavedEvent` output after remove-member:
  - remove leaved id from vice-captain snapshot;
  - reason-specific message to remaining members;
  - non-disband `SM_ALLIANCE_MEMBER_INFO(LEAVE)` and `SM_ALLIANCE_INFO`;
  - ban-to-leaved-player message;
  - disband message behavior without member/alliance info.
- Commit: `39ab8ade2 Add alliance leave fanout plan`

### Session 603 - Base Leave Side-Effect Plan

- Source-read `SM_LEAVE_GROUP_MEMBER`, opcode registration, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY`.
- Added `SmLeaveGroupMember` with opcode `247`.
- Added `PlayerBaseLeaveSideEffectPlan`, `PlayerBaseLeavePacketIntent`, `PlayerBaseLeavePacketIntentKind`, and `PlayerBaseLeavePlanner`.
- Added `SmSystemMessage.LeaveInstanceNotParty()` (`1400042`).
- Modeled online/offline base `PlayerLeavedEvent` side effects:
  - online leaver gets `SM_LEAVE_GROUP_MEMBER`;
  - registered-team-instance path gets instance warning and 30-second kick metadata;
  - all paths record `EventService.onLeftTeam` metadata.
- Commit: `0d750a5de Add base leave side effect plan`

### Session 604 - Alliance Leave Workflow Composition

- Added `PlayerAllianceLeaveWorkflowPlan`, `PlayerAllianceLeaveWorkflowStep`, `PlayerAllianceLeaveWorkflowStepKind`, and `PlayerAllianceLeaveWorkflowPlanner`.
- Composed alliance leave fanout before base leave side effects, matching Java `PlayerAllianceLeavedEvent.handleEvent` calling `super.handleEvent()` at the end.
- Added regression coverage for a ban workflow preserving alliance fanout first, then base leave packets/instance-warning metadata.
- Commit: `99c726578 Add alliance leave workflow plan`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceLeavedPlanner` / `PlayerAllianceLeaveWorkflowPlanner` | Event / Workflow Planning Bridge | Partial | Regression Tested | Needs Verification | Leave/ban/timeout/disband output ordering and base-call composition are modeled. Live remove-member, leader-change composition, disband/league execution, and sockets remain missing. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` / `PlayerBaseLeaveSideEffectPlan` | Base Event Planning Bridge | Partial | Regression Tested | Needs Verification | Base leave packet, instance warning, 30-second kick metadata, and event-service metadata are modeled. Live scheduler, instance movement, event-service invocation, and sockets remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` | Server Packet | Partial | Unit Tested | Needs Verification | Fixed Java packet body and opcode `247` are serialized. Java golden bytes, encoded frames, and client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` leave/base methods | `SmSystemMessage` leave/base factories | Server Packet Factory | Partial | Unit Tested | Needs Verification | Java ids `1300201`, `1300203`, `1300978`, `1300979`, `1300980`, and `1400042` are modeled. Runtime frame comparison remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `SmAllianceMemberInfo` through leave intents | Server Packet | Partial | Regression Tested | Needs Verification | Prefix-only `LEAVE` event-id-0 branch is tested. Live wrapper metadata, Java golden bytes, and encoded frames remain missing. |
| `com.aionemu.gameserver.model.TaskId.INSTANCE_KICK` | `PlayerBaseLeaveSideEffectPlan.InstanceKickDelay` metadata | Scheduler Dependency | Not Started | Unit Tested as Metadata | Unknown | Java schedules 30 seconds later. C# records the delay only; no execution/cancellation semantics are ported. |
| `com.aionemu.gameserver.services.instance.InstanceService.moveToExitPoint` | `PlayerBaseLeaveSideEffectPlan.WouldScheduleInstanceKick` metadata | Service Dependency | Not Started | No Tests | Unknown | Actual instance movement is deferred. |
| `com.aionemu.gameserver.services.event.EventService.onLeftTeam` | `PlayerBaseLeaveSideEffectPlan.WouldNotifyEventServiceOnLeftTeam` metadata | Service Dependency | Not Started | Unit Tested as Metadata | Unknown | Event-service invocation is deferred. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 12
- Total artifacts ported or partially modeled in this handoff window: 8
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live alliance mutation, live vice-captain mutation, leader-change composition, disband/league execution, live scheduler execution, instance movement, event-service invocation, Java runtime ordering comparison, encoded opcode/frame validation, and live client validation
- Estimated overall migration completion: 63%

The percentage stays conservative. Alliance leave output and base leave side-effect ordering are now modeled, but live runtime execution remains incomplete.

## Remaining Risks

- Live `PlayerAlliance` add/remove/member wrapper state is still not implemented.
- Leader-leave composition with `ChangeAllianceLeaderEvent` is still metadata/deferred.
- Disband and league broadcast execution remain deferred.
- Base leave scheduler execution, instance movement, and event-service invocation are metadata only.
- Java runtime ordering is source-derived but not compared against Java execution.
- Java golden byte vectors and encoded-frame validation are unavailable.
- Real-client handling is unverified for these leave packets.

## Next Recommended Unit of Work

Continue alliance lifecycle parity:

1. Prefer adding a minimal live `PlayerAlliance` runtime/snapshot bridge for add/remove/member lookup around the existing alliance planners, if it can stay narrow and non-invasive.
2. If runtime remains too broad, source-read alliance ready-check and brand events and port the next isolated packet fanout slice.
3. Keep Java breadcrumbs and update `PHASE-6-PROGRESS.md` with a Migration Parity Table after the unit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6DV-Completion.md`
   - this handoff
3. Decide whether to start the narrow live alliance runtime bridge or the next isolated packet fanout.
4. Implement one narrow unit.
5. Add tests.
6. Run focused tests, then full `dotnet test dotnetConversion\AionServer.slnx`.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
