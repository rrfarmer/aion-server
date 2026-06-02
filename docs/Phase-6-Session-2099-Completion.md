# Phase 6 Session 2099 Completion - CM_FIND_GROUP Live Dispatch Design Notes

Date: 2026-06-01
Unit of Work: UOW-2099
Status: Completed

## Scope

- Reviewed the latest `CM_FIND_GROUP` controlled boundary evidence and the current deferred C# `GameServerConnection` boundary.
- Added a conservative design note for live `CM_FIND_GROUP` dispatch readiness.
- Did not enable live dispatch or change runtime behavior.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Reviewed the complete Java `runImpl` action set and confirmed actions `20` and `25` remain parsed-only in Java.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Used previous branch reviews for recruitment, application, instance-group, instance-application, lifecycle, direct packet, and world-broadcast behavior.

## What Changed

- Added `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Documented:
  - Java source-of-truth action mapping.
  - Current controlled C# evidence by action.
  - Required live dispatch shape.
  - Blockers before live dispatch.
  - Safer next implementation candidate: a non-live adapter result type before `GameServerConnection` wiring.
  - Focused validation expectations and when broad validation becomes required.

## Validation

- Documentation hygiene:
  - `git diff --check`
  - Result: passed.
- Focused C#:
  - Not run.
  - Rationale: documentation-only UOW; no generated artifacts, scripts, product code, or tests changed.
- Focused Java/Maven:
  - Not run.
  - Rationale: documentation-only UOW based on reviewed Java source; no Java source or Java-executable test target changed.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: documentation-only UOW; no runtime or shared-surface code changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; proposed non-live adapter | Client Packet / Boundary Design | Partial | Unit Tested / Manual Only | Partial Parity | Documentation confirms controlled parsed-boundary evidence exists for Java `runImpl` actions 0/1/2/3/4/5/6/7/8/9/10/11/12/13/15/17 and no-op behavior for parsed-only actions 20/25. Live dispatch remains intentionally deferred pending singleton, concurrency, lifecycle, invite, ordering, and runtime validation. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService`; dispatch/audit/executor services | Service / Boundary Design | Partial | Unit Tested / Manual Only | Partial Parity | Design note identifies the remaining live-service blockers: C# state lifetime, Java `ConcurrentHashMap` equivalence, shared lifecycle wiring, live registry sends, invite side effects, and real-client/socket validation. |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 1 documentation artifact.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in only and are not invoked by the packet boundary.
- `FindGroupRecruitmentPlanService` live singleton lifetime and concurrency are not verified.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.

## Files Changed

- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2099-Completion.md`
- `docs/Phase-6-Session-2099-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add a non-live `CM_FIND_GROUP` dispatch adapter/result surface that composes parsed packets into direct packet intents, world-broadcast intents, optional invite intents, parsed-only no-op status, and explicit blocked/missing-runtime status without invoking live `GameServerConnection` sends.

Safe alternative candidates:

- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review lifecycle singleton wiring requirements for logout and joined-team cleanup before any live dispatch work.
