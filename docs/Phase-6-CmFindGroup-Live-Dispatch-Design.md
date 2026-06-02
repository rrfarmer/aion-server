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
- Action `12`: declined whisper direct packet intent, accepted group/alliance invite intent, and inner branch status for accepted group invite, accepted alliance invite, declined, missing applicant, and missing responder instance group; the declined `SM_MESSAGE` whisper now has focused payload evidence for the Java `SM_MESSAGE.writeImpl` field order and `ChatUtil.l10n(1400217)` encoded string, and accepted group invites have disabled-boundary trace evidence that boundary acceptance is recorded before the opt-in group invite request.
- Action `12` failure-result evidence now records declined-whisper missing direct recipients and accepted-invite missing inviter/responder cases without live dispatch or request mutation.
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
- `FindGroupLiveDispatchGoNoGoChecklistService` now aggregates the boundary, lifecycle, direct-packet, world-broadcast, action `12` invite, parsed-only no-op, and runtime-comparison gates into a blocked go/no-go checklist before any live wiring.
- `FindGroupLiveDispatchActionGateMatrixService` now maps each parsed Java `CM_FIND_GROUP` action to its remaining live evidence gate. It covers runImpl actions `0`/`1`/`2`/`3`/`4`/`5`/`6`/`7`/`8`/`9`/`10`/`11`/`12`/`13`/`15`/`17`, preserves parsed-only no-op actions `20`/`25`, and explicitly excludes server-packet-only action codes `14` and `16`.
- `FindGroupDirectPacketTriggerOrderingReadinessService` now records that Java `AionClientPacket.run` invokes `CM_FIND_GROUP.runImpl` synchronously, C# opt-in executor ordering evidence exists, and live direct-packet ordering relative to the triggering `CM_FIND_GROUP` client packet remains blocked.
- `FindGroupDirectPacketBoundaryTraceReadinessService` now records action `0` disabled-boundary acceptance before opt-in registry execution of the direct `SmFindGroup` packet to the active player, while keeping live `ProcessPacketAsync` ordering blocked.
- `FindGroupWorldBroadcastFanoutReadinessService` now records Java `PacketSendUtility.broadcastToWorld` predicate fanout, FindGroup action `1`/`5` race filters, C# registry fanout evidence, disabled action `1`/`5` boundary fanout trace evidence, and the missing live boundary proof for actions `1`/`5`.
- `FindGroupConcurrentMutationOrderingReadinessService` now records Java independent `ConcurrentHashMap` state stores, Java `onJoinedTeam` method order, C# `ConcurrentDictionary` store evidence, C# sequential `onJoinedTeam` evidence, C# basic concurrent store tests, and deterministic shared-singleton interleaving tests while keeping live singleton caller interleaving blocked.
- `PlayerGroupRuntime` and `PlayerAllianceRuntime` can now expose non-live find-group recruitment-removal plans for Java group/alliance disband paths when supplied with a `FindGroupRecruitmentPlanService`.
- `GameServerConnection` and `GameClientSocketServer` can now consume injected group/alliance invite request services, allowing joined-team cleanup to use a shared `FindGroupJoinedTeamLifecycleRecorder` and `FindGroupRecruitmentPlanService` in focused connection tests.
- `FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph` registers a production singleton graph for `FindGroupRecruitmentPlanService`, `FindGroupJoinedTeamLifecycleRecorder`, group/alliance runtimes, group/alliance invite services, and non-live boundary adapter services.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can consume injected composition and dispatch adapter services to produce a non-live `CmFindGroup` boundary plan without executing packet sends; `GameClientSocketServer` passes those services into new connections when DI supplies them.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` has focused action `12` evidence that the connection resolver can find the applicant through `IGameClientConnectionRegistry.ForEachOnlinePlayer`, compose disabled group/alliance invite requests for accept replies, record disabled action `12` boundary acceptance before the opt-in group invite request, expose the declined-whisper direct packet intent, and preserve missing-applicant/missing-instance-group no-side-effect outcomes without socket sends.
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
- Basic Java map-shape parity is now covered by `ConcurrentDictionary`, show-list materialization has focused snapshot evidence, group/alliance joined-team recorder ordering has focused evidence, disabled client-action-to-logout singleton cleanup has focused evidence, disabled client-action-to-group/alliance-disband singleton cleanup has focused evidence, and deterministic shared-singleton interleaving evidence covers logout-before-joined-team and joined-team-before-logout-before-disband caller orders; live singleton use still needs boundary/runtime evidence for concurrent callers.
- `FindGroupLifecycleSingletonWiringReadinessService` records the Java singleton call-site inventory; current C# status is production graph lifecycle evidence plus deferred boundary evidence.
- `FindGroupLiveDispatchGoNoGoChecklistService` records a concise blocked go/no-go checklist; only parsed-only actions `20`/`25` are ready as no-op behavior, while boundary wiring, shared singleton lifecycle, live packet/fanout ordering, action `12` live invite dispatch, and runtime/socket comparison remain not ready.
- `FindGroupLiveDispatchActionGateMatrixService` records the action-by-action blocked surface. Actions `1` and `5` still require world-broadcast live evidence; actions `3` and `7` still require shared singleton lifecycle evidence; actions `0`/`2`/`4`/`6`/`8`/`9`/`10`/`11`/`12`/`13`/`15`/`17` still require direct-packet live evidence; action `12` additionally requires invite-dispatch evidence.
- `FindGroupDirectPacketTriggerOrderingReadinessService` records that opt-in executor ordering is not enough to claim live parity; C# still needs a connection-boundary ordered trace proving direct sends occur after the triggering `CM_FIND_GROUP` packet is accepted and before later boundary work.
- `FindGroupDirectPacketBoundaryTraceReadinessService` records one disabled-boundary-plus-opt-in action `0` ordered trace. This is still not live parity because `ProcessPacketAsync` does not invoke the boundary helper or executor for `CmFindGroup`.
- `FindGroupWorldBroadcastFanoutReadinessService` records that opt-in race-filter fanout plus disabled action `1`/`5` boundary fanout trace evidence is not enough to claim live parity; C# still needs a live boundary trace proving actions `1` and `5` emit to same-race recipients, exclude opposite-race recipients, and preserve ordering from the triggering client packet.
- `FindGroupConcurrentMutationOrderingReadinessService` records that `ConcurrentDictionary` storage shape, sequential method-order tests, and deterministic shared-service interleaving fixtures are not enough to claim live singleton concurrency parity; C# still needs live boundary tests or runtime traces for `CM_FIND_GROUP`, logout cleanup, joined-team cleanup, and group/alliance disband cleanup sharing one singleton.
- Direct packet sends and world broadcasts have opt-in executor ordering evidence, including disabled-boundary action `2`/`6` and action `10` multi-direct ordering, but still need live connection-registry tests proving packet ordering relative to the triggering client packet.
- Action 12 accepted invite, declined whisper, missing-applicant, and missing-instance-group branches have disabled connection-helper evidence, including the inner planner status surfaced at the connection-boundary intent plan. The accepted group invite has disabled action `12` boundary-acceptance-before-opt-in-request trace evidence, the declined whisper has focused `SM_MESSAGE` payload evidence, and disabled failure-result tests surface missing direct recipients plus missing invite players without live dispatch or request mutation, but action 12 still needs live boundary tests before being triggered by `CM_FIND_GROUP`.
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

The safest next code unit is still not full live dispatch. It is a narrow focused test or readiness slice for one remaining live-blocked boundary:

- additional direct-action boundary traces beyond action `0`,
- live connection-boundary world-broadcast fanout for actions `1` or `5`,
- action `12` live invite dispatch failure/result handling, or
- live boundary or runtime traces for `CM_FIND_GROUP`, logout, joined-team, and disband callers sharing one singleton.

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
