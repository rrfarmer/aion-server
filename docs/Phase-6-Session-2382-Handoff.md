# Phase 6 Session 2382 Handoff - Register Autogroup Ready Match Runtime

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2382-Completion.md`
- `docs/Phase-6-Session-2382-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2382, registered synthetic auto-instance runtime state for ready matches so matched players can press enter after window `4`.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- Non-live queue ordering and periodic PvP match readiness planning for `checkQueueForNewMatches`.
- Successful `StartLooking` results expose the queue match plan after registration/announcement planning.
- Ready queue matches expose `CreateReadyMatchPlan(...)` with matched parties, window `4` recipients, and additional-registration cleanup intents.
- Ready queue matches apply live matched queue removal, additional-registration queue cleanup, cleanup window `2`, and ready-enter window `4`.
- Ready queue matches now register a synthetic auto-instance runtime entry for matched players before window `4` delivery.
- `CM_AUTO_GROUP` window `102` press-enter can resolve matched players from the ready-match runtime bridge and send window `5`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- The ready-match runtime registration uses a synthetic instance id and does not allocate a real world instance.
- Java `InstanceService.getNextAvailableInstance(...)` and `AutoInstance.onInstanceCreate(instance)` remain missing.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position behavior remains missing.
- Java `LookingForParty.startEnterTime` remains missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.

## Commits Made

- `[Phase 6][UOW-2382] Register autogroup ready match runtime`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2382-Completion.md`
- `docs/Phase-6-Session-2382-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 52, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this `createNewInstance(...)` runtime-registration bridge. Java source review identified the auto-instance storage and press-enter lookup behavior.

Broad-validation trigger: live connection dispatch and shared auto-instance runtime state changed.

Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered ready-match service output, existing runtime press/cancel behavior, and the edited connection path through window `100` then window `102`.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` / `AutoGroupInstanceLeaveRuntimeService.RegisterInstance(...)` | Service/Runtime | Partial | Unit Tested | Partial Parity | Ready matches now register matched players in a synthetic runtime instance before window `4` delivery. Real world allocation and `AutoInstance.onInstanceCreate` remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.pressEnter(...)` | `GameServerConnection.HandleAutoGroupAsync(...)` window `102` / `AutoGroupInstanceLeaveRuntimeService.PressEnter(...)` | Packet Handler/Runtime | Partial | Unit Tested | Partial Parity | Press-enter can resolve players registered by the ready-match bridge and sends window `5`. Real port-to-start-position and world instance state remain missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeRegistration` / `AutoGroupInstanceRuntimeState` | Runtime Model | Partial | Unit Tested | Partial Parity | Registered player tracking is represented. Instance object, start time, max-player runtime state, and handler callbacks remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `AutoGroupInstanceKind.PvpRaceInstance` | Runtime Model | Partial | Unit Tested | Partial Parity | Periodic PvP ready matches map to PvpRace runtime kind for leave/press-enter behavior. Race team formation on enter instance remains missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Handler | Partial | Unit Tested | Partial Parity | Window `100` now registers a runtime entry for ready matches; window `102` can consume it. Full instance allocation and quick-entry attachment remain missing. |

## Next Sequential UOW

Next sequential production slice: replace the synthetic ready-match runtime id with a narrow real allocated instance runtime seam if available, or add explicit start-enter timestamp/expiry tracking for ready matches if allocation remains too broad.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/Instance*.cs`
- `dotnetConversion/src/Aion.GameServer/World/*`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model next:

- `createNewInstance(...)` obtains a real available instance id and map runtime from `InstanceService`.
- `autoInstance.onInstanceCreate(instance)` records start time and instance reference before ready-window delivery.
- `LookingForParty.setStartEnterTime()` starts the 120-second ready-enter window for matched entries.
- If real allocation is too large, add ready-enter timestamp tracking as a smaller, explicit partial step.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: any allocation or timestamp bridge preserves ready-match queue cleanup, runtime press-enter reachability, and existing cancel/leave runtime behavior.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: shared world/instance runtime state if real allocation is touched; start with focused tests and add only directly related instance-runtime tests.

## Safe Candidates

- Add ready-enter timestamp data and 120-second expiry checks for matched runtime registrations.
- Add a narrow instance id allocator seam to replace synthetic instance id `1` without full world transfer behavior.
- Add penalty scheduling as explicit delayed runtime state once ready-entry state is stable.
- Add open quick-entry planning around registered runtime instances.

Avoid:

- Evidence/reporting-only units.
- Claiming full `createNewInstance(...)` parity before real world allocation and `AutoInstance.onInstanceCreate` are represented.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
