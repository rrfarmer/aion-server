# Phase 6GV Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GU and covers Session 692.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter PlayerRecallInstantRequestServiceTests`
  - Result: Passed, 5 tests.
- Latest packet/service validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerRecallInstantRequestServiceTests|GamePacketTests"`
  - Result: Passed, 87 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1243 tests.

## Recent Work Completed

### Session 692 - Recall Instant Question Slice

- Added `SmQuestionWindow.SummonPartyAcceptRequest = 901721`.
- Added recall denial system-message factories:
  - `SmSystemMessage.RecallRejectEffect` for Java id `1400099`,
  - `SmSystemMessage.RecallRejectedEffect` for Java id `1400100`.
- Added `PendingRecallInstantRequest` and `QuestionResponseRequestKind.RecallInstant`.
- Added `PlayerRecallInstantRequestService`:
  - registers the effected player's pending recall request,
  - sends the summon question intent,
  - preserves duplicate-question put-if-absent semantics,
  - denies with the Java-shaped packet fanout to both players,
  - accepts by teleporting the effected player to the captured destination.
- Routed `CM_QUESTION_RESPONSE` for summon/recall through `GameServerConnection`.
- Added enter-world/logout cleanup and deny side effect for pending recall requests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.RecallInstantEffect` | `Aion.GameServer.Services.PlayerRecallInstantRequestService` / `GameServerConnection.HandleRecallInstantQuestionResponseAsync` | Skill Effect / Request Handler | Partial | Regression Tested | Needs Verification | Models question registration, deny fanout, and accept teleport destination. Full Java `calculate` gating, effect runtime, combat rejection, enemy checks, and SkillEngine caller are not wired. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest/respond/denyAll` | `QuestionResponseRegistry` with `QuestionResponseRequestKind.RecallInstant` | Request Registry | Partial | Regression Tested | Needs Verification | Uses put-if-absent, response removal, and logout deny side effect. Java anonymous callback identity/reflection behavior and concurrent-map stress remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_SUMMON_PARTY_DO_YOU_ACCEPT_REQUEST` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow.SummonPartyAcceptRequest` | Server Packet / Question Id | Partial | Regression Tested | Needs Verification | Question id `901721` and three params are asserted in C# packet tests. Golden bytes/encrypted frames not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_Recall_Reject_EFFECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.RecallRejectEffect` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Java id `1400099` represented for the effected player's deny message. Packet-byte comparison not run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_Recall_Rejected_EFFECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.RecallRejectedEffect` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Java id `1400100` represented for effector deny notification and logout-deny cleanup. Packet-byte comparison not run. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `Aion.GameServer.Services.PlayerTeleportService.TeleportWithinSameInstance` | Teleport Service Dependency | Partial | Regression Tested | Needs Verification | C# moves represented player and resets movement. Java revive-if-dead, duel-loss side effect, `sendLoc` task/animation behavior, map-change fanout, and live socket ordering are not represented. |
| `com.aionemu.gameserver.skillengine.model.Effect` | `PendingRecallInstantRequest` destination snapshot | Runtime Model Dependency | Partial | Regression Tested | Needs Verification | C# stores captured participant/destination data only. Java `Effect`, `Skill` target position, JAXB instantiation, and effect lifecycle are outside this unit. |

## Tests Added Or Updated

- `PlayerRecallInstantRequestServiceTests.SendRecallRequest_RegistersQuestionAndSendsSummonWindow`
- `PlayerRecallInstantRequestServiceTests.SendRecallRequest_DuplicateQuestionKeepsOriginalPendingRequest`
- `PlayerRecallInstantRequestServiceTests.HandleResponse_DenyClearsPendingAndNotifiesBothPlayers`
- `PlayerRecallInstantRequestServiceTests.HandleResponse_AcceptTeleportsEffectedPlayerToCapturedDestination`
- `PlayerRecallInstantRequestServiceTests.HandleResponse_EffectorMissingConsumesRequestWithoutTeleport`
- `GamePacketTests` system-message and question-window assertions for ids `1400099`, `1400100`, and `901721`.

These tests are source-derived from Java. They do not compare against Java runtime execution, golden bytes, encrypted frames, full SkillEngine effect execution, `Effect.calculate` gating, full teleport side effects, reflection behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 recall instant request/response slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: SkillEngine caller integration, `Effect.calculate` gating, full teleport side effects, map/instance fanout, RequestResponseHandler reflection semantics, socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Recall is not invoked by the C# SkillEngine/effect runtime yet.
- Java `RecallInstantEffect.calculate` gates are not modeled in this slice.
- Java teleport side effects remain partial, especially dead/duel handling, instance/map transition behavior, and production packet fanout.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Continue compact `ResponseRequester` parity with craft skill rank-up confirmation or cube/warehouse expansion warning if their dependencies stay small. Avoid deepening recall until the C# effect runtime can represent Java `RecallInstantEffect.calculate` gates and destination capture.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GU-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
