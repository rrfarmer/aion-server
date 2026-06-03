# Phase 6 Session 2381 Handoff - Apply Autogroup Ready Match Windows

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2381-Completion.md`
- `docs/Phase-6-Session-2381-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2381, applied autogroup ready-match queue/window behavior live.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- Non-live queue ordering and periodic PvP match readiness planning for `checkQueueForNewMatches`.
- Successful `StartLooking` results expose the queue match plan after registration/announcement planning.
- Ready queue matches expose `CreateReadyMatchPlan(...)` with matched parties, window `4` recipients, and additional-registration cleanup intents.
- Ready queue matches now apply live matched queue removal, additional-registration queue cleanup, cleanup window `2`, and ready-enter window `4`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling for already registered auto-instance runtime state.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, non-Harmony member denial checks, Harmony fixed-size, and Harmony missing-ticket member/requester denials.

Still not proven or not implemented:

- The ready-match live path does not allocate/register an auto-instance, so pressing enter after the live ready window is not yet end-to-end.
- Java `createNewInstance(...)` instance allocation and `autoInstances` map update remain missing.
- Java `LookingForParty.startEnterTime` remains missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, and start time settings remain partial or missing.
- Broader PvP arena availability and non-Harmony item checks remain partial.

## Commits Made

- `[Phase 6][UOW-2381] Apply autogroup ready match windows`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2381-Completion.md`
- `docs/Phase-6-Session-2381-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 43, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this `AutoGroupService.createNewInstance(...)` live-window slice. Java source review identified branch order and side effects.

Broad-validation trigger: live connection dispatch changed.

Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered the edited service mutation path plus the edited connection dispatch branch. Packet primitives, shared serialization, persistence, scheduler, and shared world state were not changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Matched queue removal and window `4` fanout are live. Instance allocation, auto-instance registration, start-enter time, and penalty scheduling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.searchAndRemoveAdditionalRegistrations(int)` | `ApplyAdditionalRegistrationCleanup(...)` / `AutoGroupAdditionalRegistrationCleanupIntent` | Service | Partial | Unit Tested | Partial Parity | Leader whole-party cleanup and member-only cleanup now mutate queues and send window `2`; penalty application and queue recheck remain deferred. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendWindowToPlayerIfOnline(...)` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync(...)` with `SmAutoGroup` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Ready and cleanup windows are sent only through the online-player registry. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Handler | Partial | Unit Tested | Partial Parity | Window `100` now consumes ready-match plans after successful registration. Real instance creation and quick-entry attachment remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Queue entries support live removal and member cleanup. Java `startEnterTime` and AGPlayer class/name data remain partial. |

## Next Sequential UOW

Next sequential production slice: register a minimal auto-instance runtime entry for ready matches so `CM_AUTO_GROUP` window `102` press-enter can find the matched players after window `4`, while still deferring real `InstanceService.getNextAvailableInstance(...)` world allocation if no safe seam exists.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model next:

- `createNewInstance(...)` allocates a world instance and stores an `AutoInstance`.
- Later `pressEnter(player, maskId)` resolves that stored auto-instance for each matched player and sends `SM_AUTO_GROUP(maskId, 5)`.
- If full world allocation remains too broad, introduce an explicitly documented runtime registration seam that lets matched players reach the existing press-enter runtime tests without claiming full Java instance parity.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: ready-match runtime registration lets matched players use the existing press-enter/cancel-enter runtime path after window `4`, while preserving current live registration fanout and queue cleanup behavior.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch/shared auto-instance runtime state if the next UOW wires runtime registration or press-enter behavior; start with the focused command above.

## Safe Candidates

- Add a minimal ready-match runtime registration result to `ApplyReadyMatchPlanAsync(...)` and pass it into `AutoGroupInstanceLeaveRuntimeService.RegisterInstance(...)`.
- Add start-enter timestamp data to ready-match application results without changing press-enter behavior.
- Add penalty scheduling as explicit delayed runtime state once queue/window behavior is stable.
- Add open quick-entry planning around registered runtime instances if a narrow seam exists.

Avoid:

- Evidence/reporting-only units.
- Claiming full `createNewInstance(...)` parity before real world instance allocation and `AutoInstance.onInstanceCreate` are represented.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
