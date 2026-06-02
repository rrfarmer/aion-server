# Phase 6 Session 2097 Completion - Find Group Instance Application Boundary Evidence

Date: 2026-06-01
Unit of Work: UOW-2097
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` actions 11 and 12 plus the corresponding `FindGroupService` instance-application methods.
- Added controlled C# evidence that parsed action 11 and action 12 `CmFindGroup` payloads can flow through disabled boundary composition.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 11 reads `playerOrTeamId` and `instanceMaskId`, then dispatches to `sendInstanceApplication(player, playerOrTeamId)`.
  - Action 12 reads `playerOrTeamId` and `instanceApplicationReply`, then dispatches to `sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplication` resolves the target player through `World.getInstance().getPlayer(playerOrTeamId)` and sends `new SM_FIND_GROUP(applicant)` only when the target exists.
  - `sendInstanceApplicationResult` resolves the applicant through `World.getInstance().getPlayer(applicantId)`.
  - Reply `1` invites the applicant to a group when the responder's registered instance group has `minMembers <= 6`; otherwise it invites to an alliance.
  - Non-`1` replies send the applicant `new SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)`.

## What Changed

- Added parsed-packet boundary evidence tests:
  - Action 11 composes and opt-in executes the direct `SM_FIND_GROUP(applicant)` packet intent to the recruiter.
  - Declined action 12 composes and opt-in executes the direct `SM_MESSAGE` whisper packet intent to the applicant.
  - Accepted action 12 with `minMembers <= 6` composes a group invite intent and disabled request-service result.
  - Accepted action 12 with `minMembers > 6` composes an alliance invite intent and disabled request-service result.
- Updated readiness/aggregate evidence text to include action 11/12 parsed instance-application direct/invite intent evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 102 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# boundary evidence around reviewed Java source behavior. It did not change Java source, Java packet parsing, or a Java-executable test target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: controlled evidence tests/readiness text only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 4, action 5, action 8, action 9, action 10, action 11, action 12, action 13, action 15, and action 17 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor or invite-dispatch evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplication`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 11 can resolve the target player and produce the Java-shaped direct `SM_FIND_GROUP(applicant)` intent. It does not prove live socket order or real-client parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Planner / Opt-In Direct Packet and Invite Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed declined action 12 can produce the Java-shaped whisper packet intent, and accepted action 12 can compose group/alliance invite intents based on `minMembers`. Live question-window packet order and real-client invite behavior remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionElevenInstanceApplicationAsDirectPacket` | Unit | Java `CM_FIND_GROUP` action 11 and `FindGroupService.sendInstanceApplication` source review | Parsed action 11 payload composes and opt-in executes the direct applicant packet to the resolved recruiter | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTwelveDeclineAsWhisperDirectPacket` | Unit | Java `CM_FIND_GROUP` action 12 and declined `FindGroupService.sendInstanceApplicationResult` source review | Parsed declined action 12 payload composes and opt-in executes the direct whisper packet to the resolved applicant | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.CreateIntentPlan_ComposesParsedActionTwelveAcceptAsGroupInviteIntent` | Unit | Java accepted action 12 `minMembers <= 6` branch source review | Parsed accepted action 12 composes the group invite intent and disabled request-service result | Focused C# unit test using real packet parsing and reviewed Java source | Does not dispatch live invite packets; no real-client question-window comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.CreateIntentPlan_ComposesParsedActionTwelveAcceptAsAllianceInviteIntent` | Unit | Java accepted action 12 `minMembers > 6` branch source review | Parsed accepted action 12 composes the alliance invite intent and disabled request-service result | Focused C# unit test using real packet parsing and reviewed Java source | Does not dispatch live invite packets; no real-client question-window comparison |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite-dispatch evidence are opt-in only and are not invoked by the packet boundary.
- Parsed action 2/3/6/7 add/update recruitment/application branches do not yet have boundary evidence beyond planner tests.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2097-Completion.md`
- `docs/Phase-6-Session-2097-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add controlled parsed-boundary evidence for actions 2, 3, 6, and 7 recruitment/application add/update branches, using existing planner output and Java `FindGroupService` guard/side-effect order.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Review action 20/25 parsed-only behavior for documentation clarity before any live dispatch work.
