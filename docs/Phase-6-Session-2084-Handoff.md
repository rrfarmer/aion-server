# Phase 6 Session 2084 Handoff - Find Group Side-Effect Dispatch Audit Contract

Date: 2026-06-01
Unit of Work: UOW-2084
Status: Completed

## Startup Context Rule

Future Phase 6 sessions should not read `PHASE-6-PROGRESS.md` during normal startup.

Read these instead:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Completion.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`

`docs/PHASE-6-PROGRESS.md` is a historical archive. Open it only for targeted archaeology when the latest completion/handoff docs do not contain enough context.

## Test Selection Rule

Focused validation is the default.

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Full .NET validation is reserved for documented broad-validation triggers.

Choose the narrowest command that still proves the scoped change:

- Documentation-only units: run repository hygiene such as `git diff --check`; runtime tests are not applicable unless generated artifacts, scripts, or executable docs changed.
- Test-only units: run the edited test class or smallest directly affected filter; do not broaden unless product-code risk is revealed.
- Production-code units: run edited service/packet/parser tests plus directly adjacent adapter/composition tests.
- Shared-surface units: start focused, then escalate only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common model/state changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

Prefer C# filters such as `--filter "FullyQualifiedName~SpecificTestClass|FullyQualifiedName~RelatedTestClass"` and Java Maven filters such as `-Dtest=SpecificJavaTest`. Avoid unfiltered `dotnet test dotnetConversion/AionServer.slnx`, unfiltered project-wide tests, and full solution builds unless the broad trigger is documented.

When broad validation is skipped, document the focused commands and why they were sufficient. When Java/Maven is skipped, document why a narrower Java parity command was unavailable or irrelevant.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, instance-group join-threshold removal evidence, readiness evidence alignment, action 12 disabled invite executor evidence, and side-effect dispatch audit evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupLiveDispatchReadinessReportService` separates observer/executor/audit evidence from global live-dispatch blockers.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupInstanceApplicationInviteDispatchPlanService` can consume action 12 invite intents and compose group/alliance invite request-service results without sending packets.
- `FindGroupSideEffectDispatchAuditService` can audit direct packet and world-broadcast intents without calling the live connection registry.
- `PlayerEnterWorldService.LeaveWorldAsync` can record observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2080: observer-only find-group joined-team plan in group/alliance invite accept composition.
- UOW-2081: find-group joined-team instance-group threshold removal in disabled planner.
- UOW-2082: find-group readiness report records lifecycle observer evidence while staying blocked for live dispatch.
- UOW-2083: action 12 disabled group/alliance invite executor evidence.
- UOW-2084: side-effect dispatch audit contract for direct packet and world-broadcast intents.

## Recent Commits

- `188e80fb8 [Phase 6][UOW-2083] Add find group action 12 invite executor evidence`
- `26a5f4579 [Phase 6][UOW-2082] Align find group readiness evidence`
- `dcbd85521 [Phase 6][UOW-2081] Remove joined instance group registrations`
- `17b52546b [Phase 6][UOW-2080] Record find group joined-team invites`

## Validation In UOW-2084

- Focused C# side-effect audit/readiness tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Result: 43 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - C# audit-only reporting around reviewed Java send/broadcast call sites; no Java packet parser or runtime behavior changed.
- Broad .NET validation was skipped:
  - Audit-only service and readiness evidence update; no live dispatch, connection registry send, packet primitive, persistence, crypto, scheduling, world-state, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` direct `PacketSendUtility.sendPacket` boundaries | `Aion.GameServer.Services.FindGroupSideEffectDispatchAuditService`; `FindGroupDirectPacketDispatchAudit` | Side-Effect Boundary / Audit Service | Partial | Unit Tested | Partial Parity | C# can audit planned direct packet intents by recipient and packet type without sending. Live send execution remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` `PacketSendUtility.broadcastToWorld` race-filter boundaries | `Aion.GameServer.Services.FindGroupSideEffectDispatchAuditService`; `FindGroupWorldBroadcastDispatchAudit` | Side-Effect Boundary / Audit Service | Partial | Unit Tested | Partial Parity | C# can audit planned world-broadcast intents by race and packet type without broadcasting. Live race-filter fanout remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` side-effect readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService` | Readiness Report / Service | Partial | Unit Tested | Partial Parity | Readiness report records side-effect audit evidence while preserving live-dispatch blockers. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect audit service does not send packets or broadcast to the world.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- A future opt-in executor must still prove connection-registry send/broadcast behavior before live dispatch can be considered.

## Next Recommended Unit of Work

- Next sequential task: inspect `GameServerConnection`'s deferred `CmFindGroup` branch and create a final disabled boundary aggregation report that lists planner, invite executor, side-effect audit, lifecycle observer, and remaining live blockers in one place.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Design opt-in connection-registry direct-send/world-broadcast executor tests without wiring them into `CM_FIND_GROUP`.

## Files Changed In UOW-2084

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupSideEffectDispatchAuditService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSideEffectDispatchAuditServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2084-Completion.md`
- `docs/Phase-6-Session-2084-Handoff.md`
