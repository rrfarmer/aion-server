# Phase 6 CM_FIND_GROUP Live Dispatch Design Notes

Date: 2026-06-01
Status: Draft design, not implementation approval

## Purpose

This note captures the current evidence and remaining work before enabling live C# `CM_FIND_GROUP` dispatch in `GameServerConnection`.

Do not treat this document as proof that live dispatch is ready. It is a conservative checklist for the next implementation slice.

## Java Source Of Truth

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java `runImpl` dispatches actions:

- `0`: `showRecruitments(player)`
- `1`: `removeRecruitment(player, serverId, unk1, unk2, unk3)`
- `2`: `addRecruitment(player, message, groupType)`
- `3`: `updateRecruitment(player, message, groupType)`
- `4`: `showApplications(player)`
- `5`: `removeApplication(player)`
- `6`: `addApplication(player, message, groupType, classId, level)`
- `7`: `updateApplication(player, message, groupType, classId, level)`
- `8`: `registerInstanceGroup(player, instanceMaskId, message, minMembers)`
- `9`: `removeInstanceGroup(player)`
- `10`: `showInstanceGroups(player, false)`
- `11`: `sendInstanceApplication(player, playerOrTeamId)`
- `12`: `sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`
- `13`: `showInstanceGroups(player, true)`
- `15`: `showInstanceGroupMembersInfo(player, playerOrTeamId)`
- `17`: `updateInstanceGroup(player, message)`

Java parses actions `20` and `25`, but `runImpl` has no branches for them.

## Current C# Evidence

Controlled parsed-boundary evidence exists for every Java `runImpl` action listed above:

- Actions `0` and `4`: show-list direct packet intents.
- Actions `1` and `5`: race-filtered world-broadcast intents.
- Actions `2` and `6`: posted system message plus refreshed show-list direct packet intents, with focused disabled-boundary execution-order evidence that the posted message is recorded before the refreshed list.
- Actions `3` and `7`: state update with no packet intents.
- Action `8`: register instance group action 14 direct packet intent.
- Action `9`: remove instance group followed by action 10 updated show-list intent, with focused disabled-boundary evidence that missing removal records missing status while still sending the updated show-list packet like Java.
- Actions `10` and `13`: instance-group show-list direct packet intents; action `10` also supports optional action 26 mask-list intent when form-anywhere is enabled, with focused disabled-boundary evidence that action 26 is recorded before the action 10 show-list packet; action `13` has focused disabled-boundary evidence that update requests do not emit action 26.
- Action `11`: instance-application action 11 direct packet intent to resolved recruiter.
- Action `12`: declined whisper direct packet intent, accepted group/alliance invite intent, and inner branch status for accepted group invite, accepted alliance invite, declined, missing applicant, and missing responder instance group.
- Action `15`: instance-group member-info action 16 direct packet intent, with focused disabled-boundary evidence that missing member-info target records missing status and no packet side effects.
- Action `17`: update instance group followed by action 10 updated show-list intent, with focused disabled-boundary evidence that a missing instance-group update records missing status and no packet side effects.
- Actions `20` and `25`: parsed-only no-run-impl actions preserve Java's absence of a `runImpl` branch, with focused disabled-boundary evidence that neither action emits direct packets, world broadcasts, executor order entries, or registry side effects.
- `FindGroupRecruitmentPlanService` now uses `ConcurrentDictionary` for recruitment, application, and instance-group state stores to mirror Java `FindGroupService` `ConcurrentHashMap` declarations.
- `FindGroupRecruitmentPlanService` show-list plans now have focused evidence that recruitment, application, and instance-group results are materialized snapshots, matching Java `values().stream().filter(...).toList()` terminal list behavior after later mutations.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` has focused evidence for the Java mutation priority where a removed solo leader recruitment is re-added as the team recruitment before the full-team removal branch can run.
- `FindGroupJoinedTeamLifecycleRecorder.RecordGroupJoin` has focused evidence that it reads the group runtime after membership mutation, matching Java `PlayerGroupService.addPlayerToGroup` ordering before `FindGroupService.onJoinedTeam(invited)`.
- `FindGroupJoinedTeamLifecycleRecorder.RecordAllianceJoin` has focused evidence that it reads the alliance runtime after membership mutation, matching Java `PlayerAllianceService.addPlayerToAlliance` ordering before `FindGroupService.onJoinedTeam(invited)`.
- `PlayerEnterWorldService.LeaveWorldAsync` has focused cross-caller evidence that FindGroup state created through the disabled `CM_FIND_GROUP` client-action planner is removed through the same injected `FindGroupRecruitmentPlanService`, matching Java singleton `CM_FIND_GROUP.runImpl` plus `PlayerLeaveWorldService.leaveWorld` cleanup shape.
- `PlayerGroupRuntime.RemoveMemberWithLeavePlan` has focused cross-caller evidence that group disband cleanup removes FindGroup team recruitment created through the disabled `CM_FIND_GROUP` client-action planner, matching Java singleton `CM_FIND_GROUP.runImpl` plus `PlayerGroupService.disband` cleanup shape.
- `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` has focused cross-caller evidence that alliance disband cleanup removes FindGroup team recruitment created through the disabled `CM_FIND_GROUP` client-action planner, matching Java singleton `CM_FIND_GROUP.runImpl` plus `PlayerAllianceService.disband` cleanup shape.
- `FindGroupLifecycleSingletonWiringReadinessService` now enumerates Java `FindGroupService.getInstance` lifecycle call sites and keeps live singleton wiring blocked.
- `PlayerGroupRuntime` and `PlayerAllianceRuntime` can now expose non-live find-group recruitment-removal plans for Java group/alliance disband paths when supplied with a `FindGroupRecruitmentPlanService`.
- `GameServerConnection` and `GameClientSocketServer` can now consume injected group/alliance invite request services, allowing joined-team cleanup to use a shared `FindGroupJoinedTeamLifecycleRecorder` and `FindGroupRecruitmentPlanService` in focused connection tests.
- `FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph` registers a production singleton graph for `FindGroupRecruitmentPlanService`, `FindGroupJoinedTeamLifecycleRecorder`, group/alliance runtimes, group/alliance invite services, and non-live boundary adapter services.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can consume injected composition and dispatch adapter services to produce a non-live `CmFindGroup` boundary plan without executing packet sends; `GameClientSocketServer` passes those services into new connections when DI supplies them.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` has focused action `12` evidence that the connection resolver can find the applicant through `IGameClientConnectionRegistry.ForEachOnlinePlayer`, compose disabled group/alliance invite requests for accept replies, expose the declined-whisper direct packet intent, and preserve missing-applicant/missing-instance-group no-side-effect outcomes without socket sends.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast ordering, disabled-boundary action `2`/`6` posted-message-before-refresh ordering, and disabled-boundary action `10` action-26-before-action-10 ordering, without wiring `ProcessPacketAsync`.

The evidence is intentionally disabled and opt-in. `GameServerConnection` still keeps live `case CmFindGroup` deferred.

## Required Singleton Lifecycle Shape

Java uses one `FindGroupService.SingletonHolder` instance across these call sites:

- `CM_FIND_GROUP.runImpl`: all live find-group packet actions.
- `PlayerLeaveWorldService.leaveWorld`: `onLogout(player)` before `ResponseRequester.denyAll()`.
- `PlayerGroupService.addPlayerToGroup`: `onJoinedTeam(invited)` after group membership mutation.
- `PlayerAllianceService.addPlayerToAlliance`: `onJoinedTeam(invited)` after alliance membership mutation.
- `PlayerGroupService.disband`: `removeRecruitment(group)` before removing the group.
- `PlayerAllianceService.disband`: `removeRecruitment(alliance)` before alliance disband events.

Current C# evidence is intentionally not live singleton proof. Logout cleanup, joined-team cleanup, and disband cleanup now have production singleton graph evidence, and the connection can compose non-live boundary plans from injected services, but live `CM_FIND_GROUP` still is not wired to execute the shared service side effects.

## Required Live Dispatch Shape

A safe live implementation should keep the existing planner/composition boundary and add one narrow adapter from `GameServerConnection`:

1. Resolve the active player from the connection.
2. Build a disabled composition plan from the parsed `CmFindGroup`.
3. Execute direct packet and world-broadcast intents through `FindGroupSideEffectDispatchExecutorService`.
4. Execute action 12 invite intents through `FindGroupInstanceApplicationInviteDispatchPlanService`.
5. Preserve parsed-only no-op behavior for actions `20` and `25`.
6. Keep `FindGroupRecruitmentPlanService` as a live singleton only after its state lifetime, caller wiring, and multi-step mutation ordering are reviewed.

The adapter must not silently ignore execution failures. Missing active player, missing world recipient, skipped invite player, and no-op Java branches should be visible through testable result objects or logs.

## Blockers Before Live Dispatch

- The C# `FindGroupRecruitmentPlanService` now has production singleton graph evidence for logout, joined-team, and disband callers; live use still needs `CM_FIND_GROUP` execution proof against the same singleton.
- Basic Java map-shape parity is now covered by `ConcurrentDictionary`, show-list materialization has focused snapshot evidence, group/alliance joined-team recorder ordering has focused evidence, disabled client-action-to-logout singleton cleanup has focused evidence, and disabled client-action-to-group/alliance-disband singleton cleanup has focused evidence; live singleton use still needs evidence for multi-step mutation ordering under concurrent callers and additional cross-caller lifecycle cleanup.
- `FindGroupLifecycleSingletonWiringReadinessService` records the Java singleton call-site inventory; current C# status is production graph lifecycle evidence plus deferred boundary evidence.
- Direct packet sends and world broadcasts have opt-in executor ordering evidence, including disabled-boundary action `2`/`6` and action `10` multi-direct ordering, but still need live connection-registry tests proving packet ordering relative to the triggering client packet.
- Action 12 accepted invite, declined whisper, missing-applicant, and missing-instance-group branches have disabled connection-helper evidence, including the inner planner status surfaced at the connection-boundary intent plan, but still need live boundary tests before being triggered by `CM_FIND_GROUP`.
- Action 17 existing and missing instance-group update branches have disabled boundary evidence, including surfaced instance-group mutation status; live boundary tests are still needed before being triggered by `CM_FIND_GROUP`.
- Action 9 existing and missing instance-group removal branches have disabled boundary evidence, including surfaced instance-group mutation status and Java's always-refresh-list behavior; live boundary tests are still needed before being triggered by `CM_FIND_GROUP`.
- Action 15 existing and missing instance-group member-info branches have disabled boundary evidence, including surfaced member-info status; live boundary tests are still needed before being triggered by `CM_FIND_GROUP`.
- Lifecycle hooks for logout/joined-team/disband cleanup now have production singleton graph evidence, and the connection can consume the non-live adapter services. The same singleton instance must still be executed through the live `CM_FIND_GROUP` boundary before live dispatch can claim parity.
- Real encrypted socket or real-client behavior remains unverified.

## Non-Live Adapter Evidence

`FindGroupConnectionBoundaryDispatchAdapterService` now provides the non-live result surface recommended by this note.

It composes:

- direct packet intents,
- world broadcast intents,
- optional action 12 invite plans,
- no-side-effect Java branch status,
- parsed-only no-op status for actions `20` and `25`,
- missing-active-player status,
- missing invite-runtime status.

It does not execute live `GameServerConnection` sends and does not mark `CM_FIND_GROUP` live dispatch ready.

`GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can now call the composition service and adapter to create the same disabled plan shape from a parsed `CmFindGroup` packet and the connection's active player. This helper is not called by `ProcessPacketAsync`.

## Narrow Next Implementation Candidate

The safest next code unit is still not full live dispatch. It is a narrow review of multi-step mutation ordering and connection-registry side-effect ordering before any call from `ProcessPacketAsync`.

The adapter can become the seam used by `GameServerConnection` after singleton lifetime and concurrency risks are closed.

## Validation Expectations

For a non-live adapter unit:

- Run focused C# tests for the adapter, boundary composition, side-effect executor, invite dispatcher, planner, readiness reports, and `SmFindGroup`.
- Skip broad .NET validation unless the implementation touches shared connection dispatch, live side effects, common state, or packet primitives.
- Skip Java/Maven only when the unit is limited to C# composition around already-reviewed Java source. If a Java fixture or golden target exists for packet bytes, prefer a targeted Maven command.

For any live dispatch unit:

- Start with focused C# tests, then run broader validation because live connection dispatch and side effects are broad-validation triggers.
- Document the trigger before running broad validation.
- Add runtime or socket-level evidence before claiming live parity.
