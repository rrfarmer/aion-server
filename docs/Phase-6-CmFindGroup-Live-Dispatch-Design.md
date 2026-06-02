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
- Actions `2` and `6`: posted system message plus refreshed show-list direct packet intents.
- Actions `3` and `7`: state update with no packet intents.
- Action `8`: register instance group action 14 direct packet intent.
- Action `9`: remove instance group followed by action 10 updated show-list intent.
- Actions `10` and `13`: instance-group show-list direct packet intents; action `10` also supports optional action 26 mask-list intent when form-anywhere is enabled.
- Action `11`: instance-application action 11 direct packet intent to resolved recruiter.
- Action `12`: declined whisper direct packet intent and accepted group/alliance invite intent.
- Action `15`: instance-group member-info action 16 direct packet intent.
- Action `17`: update instance group followed by action 10 updated show-list intent.

The evidence is intentionally disabled and opt-in. `GameServerConnection` still contains only a deferred `case CmFindGroup`.

## Required Live Dispatch Shape

A safe live implementation should keep the existing planner/composition boundary and add one narrow adapter from `GameServerConnection`:

1. Resolve the active player from the connection.
2. Build a disabled composition plan from the parsed `CmFindGroup`.
3. Execute direct packet and world-broadcast intents through `FindGroupSideEffectDispatchExecutorService`.
4. Execute action 12 invite intents through `FindGroupInstanceApplicationInviteDispatchPlanService`.
5. Preserve parsed-only no-op behavior for actions `20` and `25`.
6. Keep `FindGroupRecruitmentPlanService` as a live singleton only after its state lifetime and concurrency behavior are reviewed.

The adapter must not silently ignore execution failures. Missing active player, missing world recipient, skipped invite player, and no-op Java branches should be visible through testable result objects or logs.

## Blockers Before Live Dispatch

- The C# `FindGroupRecruitmentPlanService` state is currently instantiated in tests and planners; live use needs an explicit singleton lifetime review.
- Java uses `ConcurrentHashMap`; C# currently uses `Dictionary`. Live singleton use needs thread-safety or serialized access evidence before multiple client packets can mutate it concurrently.
- Direct packet sends and world broadcasts need live connection-registry tests proving packet ordering relative to the triggering client packet.
- Action 12 invite dispatch mutates question/request state and may send invite packets indirectly; it needs live boundary tests before being triggered by `CM_FIND_GROUP`.
- Lifecycle hooks for logout and joined-team cleanup have observer evidence, but the same singleton instance must be wired across all live callers before live dispatch can claim parity.
- Real encrypted socket or real-client behavior remains unverified.

## Narrow Next Implementation Candidate

The safest next code unit is not full live dispatch. It is a non-live adapter result type and test coverage that composes one `CmFindGroup` packet into:

- direct packet intents,
- world broadcast intents,
- optional invite intent,
- explicit no-op status for actions `20` and `25`,
- explicit blocked status for missing runtime dependencies.

That adapter can then become the seam used by `GameServerConnection` after singleton lifetime and concurrency risks are closed.

## Validation Expectations

For a non-live adapter unit:

- Run focused C# tests for the adapter, boundary composition, side-effect executor, invite dispatcher, planner, readiness reports, and `SmFindGroup`.
- Skip broad .NET validation unless the implementation touches shared connection dispatch, live side effects, common state, or packet primitives.
- Skip Java/Maven only when the unit is limited to C# composition around already-reviewed Java source. If a Java fixture or golden target exists for packet bytes, prefer a targeted Maven command.

For any live dispatch unit:

- Start with focused C# tests, then run broader validation because live connection dispatch and side effects are broad-validation triggers.
- Document the trigger before running broad validation.
- Add runtime or socket-level evidence before claiming live parity.
