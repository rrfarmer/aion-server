# Phase 6 Session 2099 Handoff - CM_FIND_GROUP Live Dispatch Design Notes

Date: 2026-06-01
Unit of Work: UOW-2099
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

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Filtered `dotnet test` commands already build the affected project and dependencies, so a full solution build needs its own documented broad-validation trigger.

Use the narrowest command that proves the scoped change:

- Documentation-only units: run `git diff --check`; runtime tests are not applicable unless generated artifacts, scripts, or executable docs changed.
- Test-only units: run the edited test class or smallest directly affected filter; do not broaden unless product-code risk is revealed.
- Production-code units: run edited service/packet/parser tests plus directly adjacent adapter/composition tests.
- Shared-surface units: start focused, then escalate only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common model/state changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

Avoid broad .NET commands unless a broad-validation trigger is documented first.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Controlled parsed-boundary evidence now exists for Java `runImpl` actions `0,1,2,3,4,5,6,7,8,9,10,11,12,13,15,17`.
- Parsed actions `20` and `25` remain no-op because Java parses them but has no `runImpl` branch.
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` is the current design note for the next live-dispatch preparation slice.
- The design note explicitly does not approve live dispatch yet.

## Latest Completed Work

- UOW-2096: parsed action 8/9/17 instance-group mutation boundary evidence added.
- UOW-2097: parsed action 11/12 instance-application direct/invite boundary evidence added.
- UOW-2098: parsed action 2/3/6/7 recruitment/application mutation boundary evidence added.
- UOW-2099: conservative live-dispatch design note added.

## Recent Commits

- Current UOW commit message: `[Phase 6][UOW-2099] Document find group live dispatch design`
- `8f1415c77 [Phase 6][UOW-2098] Add find group recruitment mutation evidence`
- `17eb85462 [Phase 6][UOW-2097] Add find group instance application evidence`
- `b9f5696b5 [Phase 6][UOW-2096] Add find group instance mutation evidence`
- `2ca0d1328 [Phase 6][UOW-2095] Add find group instance show direct evidence`

## Validation In UOW-2099

- Documentation hygiene passed:
  - `git diff --check`
- Focused C# was not run:
  - Documentation-only UOW; no generated artifacts, scripts, product code, or tests changed.
- Focused Java/Maven was not run:
  - Documentation-only UOW based on reviewed Java source; no Java source or Java-executable test target changed.
- Broad .NET validation was skipped:
  - Documentation-only UOW; no runtime or shared-surface code changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; proposed non-live adapter | Client Packet / Boundary Design | Partial | Unit Tested / Manual Only | Partial Parity | Controlled parsed-boundary evidence exists for all Java `runImpl` actions, and parsed-only action 20/25 no-op behavior is documented. Live dispatch remains intentionally deferred pending singleton, concurrency, lifecycle, invite, ordering, and runtime validation. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService`; dispatch/audit/executor services | Service / Boundary Design | Partial | Unit Tested / Manual Only | Partial Parity | Design note identifies remaining live-service blockers: C# state lifetime, Java `ConcurrentHashMap` equivalence, shared lifecycle wiring, live registry sends, invite side effects, and real-client/socket validation. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in only and are not invoked by the packet boundary.
- `FindGroupRecruitmentPlanService` live singleton lifetime and concurrency are not verified.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.

## Next Recommended Unit of Work

- Next sequential task: add a non-live `CM_FIND_GROUP` dispatch adapter/result surface that composes parsed packets into direct packet intents, world-broadcast intents, optional invite intents, parsed-only no-op status, and explicit blocked/missing-runtime status without invoking live `GameServerConnection` sends.

Safe alternative candidates:

- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review lifecycle singleton wiring requirements for logout and joined-team cleanup before any live dispatch work.

## Files Changed In UOW-2099

- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2099-Completion.md`
- `docs/Phase-6-Session-2099-Handoff.md`
