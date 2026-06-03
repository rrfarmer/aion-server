# Phase 6 Session 2380 Handoff - Autogroup Ready Match Side-Effect Planning

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2380-Completion.md`
- `docs/Phase-6-Session-2380-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2380, added non-live ready-match side-effect planning for autogroup queue matches.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- Non-live queue ordering and periodic PvP match readiness planning for `checkQueueForNewMatches`.
- Successful `StartLooking` results expose the queue match plan after registration/announcement planning.
- Ready queue matches now expose a non-live `CreateReadyMatchPlan(...)` with matched parties, window `4` recipients, and additional-registration cleanup intents.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, non-Harmony member denial checks, Harmony fixed-size, and Harmony missing-ticket member/requester denials.

Still not proven or not implemented:

- The ready-match plan is not consumed for live instance creation or ready-enter packets.
- Java `createNewInstance(...)` live mutation remains missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, and start time settings remain partial or missing.
- Broader PvP arena availability and non-Harmony item checks remain partial.

## Commits Made

- `[Phase 6][UOW-2380] Port autogroup ready match side-effect planning`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2380-Completion.md`
- `docs/Phase-6-Session-2380-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 39, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this `AutoGroupService.createNewInstance(...)` planning slice. Java source review identified branch order and deferred side effects.

Broad-validation trigger: none. This was non-live service planning only; live dispatch was unchanged.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and covered the edited service plus adjacent ingress behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateReadyMatchPlan(...)` / `AutoGroupReadyMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Ready-enter recipients and additional-registration cleanup side effects are planned. Instance allocation, live queue mutation, start-enter time, and packet dispatch remain deferred. |
| `com.aionemu.gameserver.services.AutoGroupService.searchAndRemoveAdditionalRegistrations(int)` | `AutoGroupAdditionalRegistrationCleanupIntent` / planner simulation | Service/Planner | Partial | Unit Tested | Partial Parity | Leader whole-party removal and member-only removal intents are modeled with window `2`, penalty flags, and queue-recheck flag. Live mutation and penalty application remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.checkQueueForNewMatches(...)` | `AutoGroupQueueMatchPlan` / `AutoGroupReadyMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Ready queue matches can now feed a non-live create-new-instance side-effect plan. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Queue entries support member/leader cleanup planning. Java `startEnterTime`, AGPlayer class/name data, and live runtime state remain partial. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendWindowToPlayerIfOnline(...)` | `AutoGroupReadyMatchPlan.ReadyWindowRecipientObjectIds` / `AutoGroupAdditionalRegistrationCleanupIntent.NotifiedMemberObjectIds` | Utility/Dispatch Planner | Partial | Unit Tested | Partial Parity | Window `4` and window `2` recipients are planned, not sent. |

## Next Sequential UOW

Next sequential production slice: consume `AutoGroupReadyMatchPlan` for live queue mutation and ready-enter window `4` fanout, while still avoiding real world instance allocation if no narrow instance seam exists.

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

Expected Java behavior to model next:

- For a ready match, remove matched queue entries from the live queue and mark them in the start-enter window state.
- For each matched member, perform the planned additional-registration cleanup:
  - leader: remove whole additional party, plan/apply party penalty, notify all members with window `2`;
  - member: remove only that member, notify with window `2`, plan/apply player penalty, and re-check queue.
- Send `SM_AUTO_GROUP(maskId, 4)` to matched online players only after cleanup modeling is safe.
- If a full instance seam is available, Java also allocates the instance, calls `autoInstance.onInstanceCreate(instance)`, and stores the `autoInstance`.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: ready-match live mutation removes matched entries, preserves existing registration fanout, sends window `4` only to matched online players, and performs additional-registration cleanup without double-removing parties.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch if `GameServerConnection` sends new packets or mutates shared instance runtime; otherwise keep validation focused.

## Safe Candidates

- Add a live `ApplyReadyMatchPlanAsync(...)` or similar service method that mutates queues and returns packet/cleanup delivery intents.
- Wire ready-match window `4` fanout through `GameServerConnection` only after live queue mutation is modeled.
- Add start-enter timestamp/runtime state on queued registrations or a companion ready-entry record.
- Add open quick-entry planning around existing `AutoGroupInstanceLeaveRuntimeService` facts, if a narrow seam exists.

Avoid:

- Evidence/reporting-only units.
- Real world instance allocation until `InstanceService`/runtime ownership is understood.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
