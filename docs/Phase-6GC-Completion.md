# Phase 6GC Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GB and covers Session 673.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "RiftPortalInteractionServiceTests|QuestionResponseRegistryTests"`
  - Result: Passed, 12 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1171 tests.

## Recent Work Completed

### Session 673 - Rift Portal Registry Adapter

- Migrated rift portal question registration to `Player.ResponseRequester`.
- `RiftPortalInteractionService.RequestDialog` registers direct and vortex question ids through `ResponseRequester.PutRequest`.
- Duplicate portal question registration returns `RiftPortalDialogStatus.PendingRequest`, mirroring Java's no-question-on-duplicate behavior as an explicit C# result.
- `PendingRiftPortalRequest` remains as typed payload metadata and a narrow adapter slot.
- `RiftPortalInteractionService.RespondAsync` now consumes `ResponseRequester.Respond` for direct/vortex accept and decline before existing teleport/update behavior.
- Registry removal-before-handle semantics are now live for rift portal accept and decline.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.RVController` | `Aion.GameServer.Services.RiftPortalInteractionService` | Controller / Service | Partial | Unit Tested | Needs Verification | Direct and vortex portal requests now register through `Player.ResponseRequester`. |
| `com.aionemu.gameserver.controllers.RVController.onRequest` | `RiftPortalInteractionService.RequestDialog` | Dialog Request / Runtime Routing | Partial | Unit Tested | Needs Verification | Registers direct and vortex question ids before returning `SM_QUESTION_WINDOW`. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `QuestionResponseRegistry.PutRequest` via rift portal request registration | Request Registry Method | Partial | Unit Tested | Needs Verification | Duplicate direct/vortex question ids reject by registry state. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `QuestionResponseRegistry.Respond` via `RiftPortalInteractionService.RespondAsync` | Request Registry Method | Partial | Unit Tested | Needs Verification | Rift portal responses remove the registry entry before accept/deny behavior. |
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler` | `QuestionResponseRequest` / `QuestionResponseDispatch` carrying `PendingRiftPortalRequest` payload | Request Handler Metadata | Partial | Unit Tested | Needs Verification | Typed metadata replaces Java anonymous handler subclass callbacks. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_ASK_PASS_BY_DIRECT_PORTAL` | `SmQuestionWindow.DirectPortalPassConfirm` | Packet Constant / Question Id | Complete | Unit Tested | Needs Verification | Used as the registry key for ordinary direct portal requests. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` literal `904304` | `SmQuestionWindow.VortexPortalPassConfirm` | Packet Constant / Question Id | Complete | Unit Tested | Needs Verification | Used as the registry key for vortex portal requests. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `RiftPortalUseService.AcceptPortal` through `RiftPortalInteractionService.RespondAsync` | Teleport Service Boundary | Partial | Unit Tested | Needs Verification | Accept branch delegates to existing C# teleport/update behavior. |
| `com.aionemu.gameserver.services.VortexService` | `VortexLocationService` / `RiftPortalInteractionService.ResolveVortexDestination` | Vortex Service Boundary | Partial | Unit Tested | Needs Verification | Existing vortex destination/team-removal/notice behavior is preserved. |
| `com.aionemu.gameserver.utils.audit.AuditLogger` | Not ported for rift level-restriction rejection | Audit Dependency | Not Started | No Tests | Unknown | Java logs out-of-level rift use attempts in `onAccept`; C# currently rejects without audit logging. |

## Tests Added Or Updated

- `RiftPortalInteractionServiceTests.RequestDialog_ForShowDialogTarget_SetsPendingPortalQuestion`
- `RiftPortalInteractionServiceTests.RequestDialog_DuplicatePortalQuestionIsRejectedThroughResponseRequester`
- `RiftPortalInteractionServiceTests.RespondAsync_ForAcceptedPortalQuestion_TeleportsAndRefreshesEntryUpdates`
- `RiftPortalInteractionServiceTests.RespondAsync_ForAcceptedVortexQuestion_UsesVortexLocationStartPoint`
- `RiftPortalInteractionServiceTests.RespondAsync_ForDeclinedPortalQuestion_ClearsPendingWithoutTeleport`
- `RiftPortalInteractionServiceTests.RespondAsync_WrongQuestionLeavesPortalRequestRegistered`

These tests are source-derived. They do not compare against Java runtime execution, Java golden vectors, live anonymous handler objects, real teleport tasks, audit logs, real socket order, encrypted frames, packet captures, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 rift portal registry-adapter migration slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: Java audit logging, deeper teleport/map-instance parity, Java polymorphic callback parity, Java concurrent map stress parity, Java rift schedule/despawn runtime comparison, real socket-order validation, and runtime/client validation.
- Estimated overall migration completion: 64%

## Remaining Risks

- League invite, friend invite, kisk bind, and rift portal use `Player.ResponseRequester`; charge and soulbind still use specialized pending slots.
- C# still keeps typed adapter slots alongside registry payload metadata.
- Registry dispatch metadata does not execute Java-style polymorphic callbacks directly.
- Java `AuditLogger` out-of-level rift logging is missing.
- Java teleport task, map-instance side effects, rift schedule/despawn state, and live socket order remain unverified.
- Java `ConcurrentHashMap` semantics remain approximated by a C# lock without stress tests.
- Packet sends and live client behavior remain unverified against Java golden bytes, encrypted frames, packet captures, or real-client validation.

## Next Recommended Unit of Work

Migrate charge-all question handling onto `Player.ResponseRequester`:

1. Source-read Java `ItemChargeService.startChargingEquippedItems` and current C# charge-all request setup.
2. Keep `PendingChargeAllRequest` as typed payload metadata if useful.
3. Register `STR_ITEM_CHARGE_ALL_CONFIRM` and `STR_ITEM_CHARGE2_ALL_CONFIRM` through `ResponseRequester.PutRequest`.
4. Consume `ResponseRequester.Respond` before existing payment/item mutation behavior.
5. Preserve removal-before-handle, duplicate-question semantics, and wrong-question behavior.
6. Keep existing charge-all tests green and add registry-count assertions for request/response cleanup.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GB-Completion.md`
   - this handoff
3. Inspect Java `ItemChargeService`, `ResponseRequester`, and current C# charge-all tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
