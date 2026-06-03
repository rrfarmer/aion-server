# Phase 6 Session 2379 Handoff - Autogroup Queue Match Result Planning

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2379-Completion.md`
- `docs/Phase-6-Session-2379-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2379, exposed queue match planning on successful start-looking results.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- Non-live queue ordering and periodic PvP match readiness planning for `checkQueueForNewMatches`.
- Successful `StartLooking` results now expose the queue match plan after registration/announcement planning.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, non-Harmony member denial checks, Harmony fixed-size, and Harmony missing-ticket member/requester denials.

Still not proven or not implemented:

- The queue match plan is not consumed for live instance creation or ready-enter packets.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java `createNewInstance(...)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, and start time settings remain partial or missing.
- Broader PvP arena availability and non-Harmony item checks remain partial.

## Commits Made

- `[Phase 6][UOW-2379] Wire autogroup queue match result planning`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2379-Completion.md`
- `docs/Phase-6-Session-2379-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 36, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupService.startLooking` queue follow-up. Java source review identified branch order and deferred side effects.

Broad-validation trigger: none. This was result-data wiring only; live dispatch was unchanged.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and covered the edited service plus adjacent ingress behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` | Service | Partial | Unit Tested | Partial Parity | Successful registration now exposes queue follow-up planning after registration/announcement. Open quick-entry checks, instance creation, and window `4` fanout remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.checkQueueForNewMatches(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateQueueMatchPlan(...)` / `AutoGroupStartLookingResult.QueueMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Plan is now attached to successful start-looking results. Live mutation and instance creation remain deferred. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Queue entries preserve member ids, race, entry type, leader id, and registration time for result planning. Java `startEnterTime` and AGPlayer class/name data remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupQueueMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Periodic PvP readiness is exposed through start-looking results. Runtime callbacks and live registration remain missing. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` / `AutoGroupStartLookingResult` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Existing success fanout remains covered; window `4` ready-enter fanout remains missing. |

## Next Sequential UOW

Next sequential production slice: port the ready-match queue mutation and ready-enter window plan without allocating a real world instance, or add the first live-ready fanout only if queue mutation is fully modeled.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model:

- For a ready match, Java `createNewInstance(...)` allocates an instance, calls `autoInstance.onInstanceCreate`, stores it in `autoInstances`, removes matched queue entries, calls `lfp.setStartEnterTime()`, calls `searchAndRemoveAdditionalRegistrations(id)`, and sends `SM_AUTO_GROUP(maskId, 4)` to matched online players.
- `searchAndRemoveAdditionalRegistrations` has different leader/member removal behavior and penalties; avoid live fanout until that cleanup is understood.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: ready-match queue mutation/plan behavior preserves existing registration fanout and only emits window `4` or removes registrations when the Java cleanup branch is explicitly modeled.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch if `GameServerConnection` sends new packets or mutates shared instance runtime; otherwise keep validation focused.

## Safe Candidates

- Add a non-live `CreateReadyMatchPlan` that lists matched queue entries, window `4` recipients, and additional-registration cleanup intents.
- Add queue mutation for matched entries while still deferring real world instance allocation.
- Add open quick-entry planning around existing `AutoGroupInstanceLeaveRuntimeService` facts, if a narrow seam exists.
- Add C# config option binding for remaining autogroup schedule/period/start defaults.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
