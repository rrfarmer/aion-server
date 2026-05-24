# Phase 6GI Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GH and covers Session 679.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter PlayerTeleportToNpcRequestServiceTests`
  - Result: Passed, 7 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1182 tests.

## Recent Work Completed

### Session 679 - Teleport-To-NPC ResponseRequester Slice

- Source-read Java `TeleportService.sendTeleportRequest`, `TeleportService.teleportToNpc`, `SpawnsData.getFirstSpawnByNpcId`, and `PositionUtil.convertHeadingToAngle`.
- Added `SmQuestionWindow.TeleportToNpcConfirm` for Java question id `905097`.
- Added `QuestionResponseRequestKind.TeleportToNpc`.
- Added `NpcSpawnTable.GetFirstSpawnByNpcId`, preferring the player's current world before fallback to another world with the NPC.
- Added `PlayerTeleportToNpcRequestService`:
  - registers the pending question,
  - resolves a Java-derived destination on accept using NPC bound radius, heading angle, fallback z, and look-at-NPC heading,
  - consumes deny/accept through `QuestionResponseRegistry`,
  - applies the existing `TeleportAnimation.NONE` same-instance teleport completion path on accept.
- Wired `GameServerConnection.HandleQuestionResponseAsync` to dispatch question `905097`.
- WebReward remains deferred, so this unit adds the request/response surface but not the Java reward caller.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.TeleportService.sendTeleportRequest` | `Aion.GameServer.Services.PlayerTeleportToNpcRequestService.SendTeleportRequest` | Service / Request Handler | Partial | Unit Tested | Needs Verification | Registers question id `905097`; C# returns the packet for future callers because WebReward is not ported yet. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportToNpc` | `PlayerTeleportToNpcRequestService.CreateDestination` / `HandleResponse` | Teleport Service | Partial | Unit Tested | Needs Verification | Accept-time destination math and heading are source-derived. GeoService z lookup and full instance routing are not ported in this slice. |
| `com.aionemu.gameserver.dataholders.SpawnsData.getFirstSpawnByNpcId` | `Aion.GameServer.Dataholders.NpcSpawnTable.GetFirstSpawnByNpcId` | Dataholder Lookup | Partial | Unit Tested | Needs Verification | Current-world-first lookup is represented; C# flattened spawn order does not prove Java spawn-group parity. |
| `com.aionemu.gameserver.utils.PositionUtil.convertHeadingToAngle` | `PlayerTeleportToNpcRequestService.ConvertHeadingToAngle` | Utility Math | Partial | Unit Tested | Needs Verification | Uses source-derived `heading * 3f` normalization, without Java runtime float comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry` | Request Registry | Partial | Unit Tested / Regression Tested | Needs Verification | Adds teleport-to-NPC metadata dispatch. Java anonymous handler object execution remains unported. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_QUESTION_RESPONSE` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleQuestionResponseAsync` | Client Packet Dispatch | Partial | Regression Tested by full suite | Needs Verification | Dispatches `905097`; focused tests exercise the service directly, not socket parsing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Server Packet | Partial | Regression Tested by packet suite / Unit Tested by service | Needs Verification | Constant added. No Java golden-byte validation for this prompt. |
| `com.aionemu.gameserver.services.reward.WebRewardService` | Not yet ported | Service Dependency | Not Started | No Tests | Unknown | Java caller dependency discovered; C# still defers WebReward from `CM_PLAYER_LISTENER`. |
| `com.aionemu.gameserver.geoEngine.GeoService` | Not ported for this flow | Geo Dependency | Not Started | No Tests | Unknown | Java collision z lookup falls back to `spot.Z + 0.5`; C# only implements the fallback. |
| `com.aionemu.gameserver.instance.InstanceService` / `WorldMapInstance` selection | `WorldPosition.InstanceId` plus `PlayerTeleportService.TeleportWithinSameInstance` | Instance Dependency | Partial | Unit Tested | Needs Verification | Same-world instance id is preserved; cross-world instance/open-world selection needs future work. |

## Tests Added Or Updated

- `PlayerTeleportToNpcRequestServiceTests.GetFirstSpawnByNpcId_SearchesPlayerWorldBeforeOtherWorlds`
- `PlayerTeleportToNpcRequestServiceTests.SendTeleportRequest_RegistersQuestionWindowAndAcceptComputesJavaDestination`
- `PlayerTeleportToNpcRequestServiceTests.SendTeleportRequest_DuplicateQuestionLeavesOriginalRequest`
- `PlayerTeleportToNpcRequestServiceTests.HandleResponse_DenyConsumesRequestAndDoesNotMovePlayer`
- `PlayerTeleportToNpcRequestServiceTests.HandleResponse_AcceptConsumesRequestAndTeleportsWithNoneArrival`
- `PlayerTeleportToNpcRequestServiceTests.HandleResponse_WrongQuestionLeavesRegisteredRequest`
- `PlayerTeleportToNpcRequestServiceTests.HandleResponse_NoSpawnConsumesRequestAndDoesNotMovePlayer`

These tests are source-derived from Java; they do not compare against Java runtime execution, golden bytes, encrypted frames, WebReward runtime behavior, GeoService z lookup, reflection callback behavior, precision/rounding across runtimes, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 teleport-to-NPC request/response slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: WebReward caller integration, Java runtime comparison, GeoService z lookup, full instance-service routing, Java spawn-group/event-spawn parity, generic anonymous handler callback execution, real socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- C# resolves the destination on accept like Java, but Java spawn-group/event-spawn mutation behavior is not runtime-compared.
- C# always uses Java's fallback z `spot.Z + 0.5` because `GeoService.getZ` is not represented.
- Cross-world and instance-map selection is simplified.
- WebReward is not ported, so no production C# caller yet invokes this request service.
- C# flattened spawn order does not prove Java spawn-group/event-spawn parity.
- Generic Java `RequestResponseHandler` callback execution remains partial.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Continue with another narrow `ResponseRequester` user:

1. Best next candidate: warehouse/cube expansion if static expander data and persistence can be scoped safely.
2. Alternative: source-read duel/group/legion invite handlers and pick the smallest one whose runtime state already has C# coverage.
3. Keep teleport-to-NPC marked `Needs Verification` until Java runtime/golden packet or live behavior comparison exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GH-Completion.md`
   - this handoff
3. Inspect the selected Java `ResponseRequester` user and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
