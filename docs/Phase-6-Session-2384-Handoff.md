# Phase 6 Session 2384 Handoff - Allocate Autogroup Ready Match Instance Ids

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2384-Completion.md`
- `docs/Phase-6-Session-2384-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2384, allocated modeled world-map instance ids for live ready autogroup matches before registering runtime state and sending ready-enter window `4`.

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
- Ready queue matches register auto-instance runtime entries for matched players before window `4` delivery.
- Live ready queue matches now allocate a modeled `WorldMapInstanceRuntimeState` id and notify instance creation before runtime registration.
- Ready queue match runtime registrations carry `ReadyEnterStartTime`, matching Java `LookingForParty.setStartEnterTime()`.
- Ready queue match runtime registrations carry `StartInstanceTime`, matching Java `AutoInstance.onInstanceCreate(instance)`.
- `CM_AUTO_GROUP` window `102` press-enter can resolve matched players from the ready-match runtime bridge and send window `5`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- Java `SpawnEngine.spawnInstance(...)` and event spawn behavior are not wired into autogroup ready-match allocation.
- Java `AutoGroupType.getDifficultId()` is not represented on the C# auto-group summary, so difficulty remains `0`.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position behavior remains missing.
- Java `LookingForParty.isOnStartEnterTask()` 120-second expiry is not enforced yet.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.

## Commits Made

- `[Phase 6][UOW-2384] Allocate autogroup ready match instance ids`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `docs/Phase-6-Session-2384-Completion.md`
- `docs/Phase-6-Session-2384-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

Result: passed 79, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this production `createNewInstance(...)` bridge. Java source review identified the allocation, `onInstanceCreate`, and ready-window ordering.

Broad-validation trigger: live connection dispatch and shared world/instance runtime state changed.

Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered ready-match service materialization, runtime snapshots, connection window `100`/`102` flow, and world-map allocation/lifecycle behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` / `GameServerConnection.MaterializeAutoGroupReadyMatchRuntimeInstance(...)` | Service/Connection | Partial | Unit Tested | Partial Parity | Live ready matches now allocate a modeled world instance id before runtime registration/window `4`; Java spawn content, difficulty id, and full auto-instance object state remain partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(...)` | `InstanceRuntimeService.GetNextAvailableInstance(...)` / `WorldMapRuntimeStateTable` | Service/Runtime | Partial | Unit Tested | Partial Parity | Autogroup live path now consumes the existing C# allocator and notifies instance creation. Java `SpawnEngine.spawnInstance`, handler factory selection, and event spawns remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeRegistration` / `AutoGroupInstanceRuntimeState` | Runtime Model | Partial | Unit Tested | Partial Parity | Runtime state now stores allocated instance id plus `StartInstanceTime`; Java instance reference and many handler callbacks remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `AutoGroupInstanceKind.PvpRaceInstance` / ready-match runtime registration | Runtime Model | Partial | Unit Tested | Partial Parity | Matched player count is used as the allocated max-player count for ready periodic PvP matches. Race team formation and port-to-start-position remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP` | `GameServerConnection.HandleAutoGroupAsync(...)` | Packet Handler | Partial | Unit Tested | Partial Parity | Window `100` ready matches now materialize world instance state before window `4`; window `102` still reaches press-enter window `5`. |

## Next Sequential UOW

Next sequential production slice: port the next direct Java auto-instance side effect now that ready-match runtime has a real modeled instance id.

Recommended first candidate: implement a narrow `AutoPvpInstance.onPressEnter(...)` destination/port-to-start-position plan if a C# instance-handler/start-position abstraction is available; otherwise implement the 120-second ready-enter expiry check using the existing `ReadyEnterStartTime`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- Java instance handlers for periodic PvP start-position behavior, if port-to-start-position is selected.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/World/*`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Expected Java behavior to model next:

- `AutoPvpInstance.onPressEnter(player)` calls base cooldown handling and then `instance.getInstanceHandler().portToStartPosition(player)`.
- `LookingForParty.isOnStartEnterTask()` uses the ready-enter timestamp as a 120-second window for quick-entry/open-registration decisions.
- `AutoInstance.isRegistrationDisabled(lfp)` uses `startInstanceTime` and `agt.getMaximumJoinTime()` to reject late quick entries.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

Specific behavior this command should prove: the next press-enter/expiry bridge preserves allocated instance id reachability, ready-enter/start-instance timestamps, and existing group cleanup/window `5` behavior.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch and shared world/instance runtime state if press-enter teleport or expiry mutates runtime state; start focused and add only directly related instance/teleport tests if needed.

## Safe Candidates

- Add a press-enter destination/port-to-start-position plan for `AutoPvpInstance.onPressEnter(...)`.
- Add 120-second ready-enter expiry checks for matched runtime registrations.
- Add `AutoGroupType.getDifficultId()` data to `AutoGroupSummary` if static-data parsing can supply it narrowly.
- Add open quick-entry planning around registered runtime instances using `ReadyEnterStartTime` and `StartInstanceTime`.

Avoid:

- Evidence/reporting-only units.
- Claiming full `createNewInstance(...)` parity before spawn content, difficulty, and instance handler behavior are represented.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
