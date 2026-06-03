# Phase 6 Session 2378 Handoff - Autogroup Queue Match Planning

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2378-Completion.md`
- `docs/Phase-6-Session-2378-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2378, ported a non-live periodic PvP queue match planner.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- Non-live queue ordering and periodic PvP match readiness planning for `checkQueueForNewMatches`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, non-Harmony member denial checks, Harmony fixed-size, and Harmony missing-ticket member/requester denials.

Still not proven or not implemented:

- Live `StartLooking` still does not call queue matching or create auto instances.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java `createNewInstance(...)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, and start time settings remain partial or missing.
- Broader PvP arena availability and non-Harmony item checks remain partial.

## Commits Made

- `[Phase 6][UOW-2378] Port autogroup queue match planning`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2378-Completion.md`
- `docs/Phase-6-Session-2378-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore
```

Result: passed 27, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupService.checkQueueForNewMatches`; Java source review identified ordering, capacity rules, and readiness behavior.

Broad-validation trigger: none. This was a non-live planner UOW.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and validated the edited service behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.checkQueueForNewMatches(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateQueueMatchPlan(...)` | Service/Planner | Partial | Unit Tested | Partial Parity | Queue ordering and periodic PvP readiness planning are covered. Live queue mutation, instance creation, and other auto-instance types remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Member ids, race, entry type, leader id, and registration time are represented. Java `startEnterTime`, `AGPlayer` class/name data, leader reassignment, and member unregister behavior remain partial. |
| `com.aionemu.gameserver.model.autogroup.EntryRequestType` | `Aion.GameServer.Services.AutoGroupEntryRequestType` | Enum | Complete | Unit Tested | Partial Parity | Ids and ordering are used in parser and queue sort tests. Full lifecycle behavior remains partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateQueueMatchPlan(...)` | Service/Planner | Partial | Unit Tested | Partial Parity | Periodic PvP add/readiness capacity logic is modeled for planning. `onEnterInstance`, `onPressEnter`, team creation, instance registration, and live `AutoInstance` state remain missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `Aion.GameServer.Services.AutoGroupQueueMatchPlan` | Abstract Runtime/Planner | Partial | Unit Tested | Partial Parity | Only pre-instance max-player and add-party readiness concepts are represented. Registration-disabled checks for existing instances and runtime callbacks remain missing. |

## Next Sequential UOW

Next sequential production slice: wire the queue match plan into the post-registration path in a still-safe way, then continue toward instance creation.

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
- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model:

- After successful registration and optional announcement, Java calls `checkInstancesForOpenQuickEntries(lfp, maskId)`.
- If no open quick-entry instance accepts the party, Java calls `checkQueueForNewMatches(maskId)`.
- For a ready queue match, Java `createNewInstance(...)` allocates an instance, registers an `AutoInstance`, removes matched queue entries, sets start-enter time, removes additional registrations for each matched member, and sends `SM_AUTO_GROUP(maskId, 4)` to matched online players.
- A safe next step is to expose the queue match plan on `AutoGroupStartLookingResult` or a post-registration service result without mutating live instance state yet.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: post-registration result/dispatch wiring preserves existing success fanout while exposing or consuming queue match planning only where Java would call `checkQueueForNewMatches`.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch if `GameServerConnection` sends new packets or mutates shared instance runtime; otherwise keep validation focused.

## Safe Candidates

- Add post-registration queue follow-up result data without creating live instances.
- Add open quick-entry planning around existing `AutoGroupInstanceLeaveRuntimeService` facts, if a narrow seam exists.
- Add the first live window `4` fanout only after a ready plan and queue mutation behavior are fully modeled.
- Add C# config option binding for remaining autogroup schedule/period/start defaults.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
