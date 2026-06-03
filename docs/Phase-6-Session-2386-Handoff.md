# Phase 6 Session 2386 Handoff - Add Autogroup Open Quick-Entry Runtime Gate

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2386-Completion.md`
- `docs/Phase-6-Session-2386-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2386, added an autogroup runtime quick-entry gate using Java maximum-join timing, 120-second start-enter guarding, total capacity, and race capacity.

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
- Live ready queue matches allocate a modeled `WorldMapInstanceRuntimeState` id and notify instance creation before runtime registration.
- Ready queue match runtime registrations carry `ReadyEnterStartTime`, matching Java `LookingForParty.setStartEnterTime()`.
- Ready queue match runtime registrations carry `StartInstanceTime`, matching Java `AutoInstance.onInstanceCreate(instance)`.
- Ready queue match allocation passes Java-derived `AutoGroupType.getDifficultId()` into modeled world instance state.
- C# auto-group summaries expose Java-derived `AutoGroupType.getMaximumJoinTime()` as `MaximumJoinTimeMilliseconds`.
- Runtime registrations now carry `MaximumJoinTimeMilliseconds`, total max players, and member race facts.
- Runtime open quick-entry gating can accept/reject quick-entry requests like Java `checkInstancesForOpenQuickEntries(...)` and `AutoPvpInstance.addLookingForParty(...)` for the modeled facts.
- `CM_AUTO_GROUP` window `102` press-enter can resolve matched players from the ready-match runtime bridge and send window `5`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- `StartLooking(...)` does not yet call open quick-entry runtime attachment before queue matching.
- Accepted open quick entries are not yet removed from the looking-party queue, do not send window `4`, and do not trigger additional-registration cleanup.
- Java `AutoGroupService.checkQueueForQuickEntries(autoInstance)` refill after cancel/leave remains missing.
- Java `AutoInstance.isRegistrationDisabled(lfp)` score-ended branch remains missing.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position behavior remains missing.
- Java `SpawnEngine.spawnInstance(...)` and event spawn behavior are not wired into autogroup ready-match allocation.
- Java penalties, delayed cancel removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.

## Commits Made

- `[Phase 6][UOW-2386] Add autogroup open quick-entry runtime gate`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2386-Completion.md`
- `docs/Phase-6-Session-2386-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 55, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this open quick-entry runtime gate. Java source review identified the authoritative branch behavior.

Broad-validation trigger: shared autogroup runtime state changed.

Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered the runtime registry, ready-match bridge, and adjacent live autogroup connection behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.checkInstancesForOpenQuickEntries(...)` | `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` | Service/Runtime | Partial | Unit Tested | Partial Parity | Runtime acceptance/rejection gates are modeled. Start-looking live queue removal, window `4`, and additional-registration cleanup are not wired yet. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance.isRegistrationDisabled(...)` | `AutoGroupInstanceRuntimeState.TryAddOpenQuickEntry(...)` | Runtime Model | Partial | Unit Tested | Partial Parity | Quick-entry maximum-join rejection is covered for modeled runtime instances. Instance score-ended behavior remains missing. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.addLookingForParty(...)` | `AutoGroupInstanceRuntimeState.TryAddOpenQuickEntry(...)` | Runtime Model | Partial | Unit Tested | Partial Parity | Total and race-capacity rejection plus member registration are covered for open quick entries. Full lifecycle remains partial. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.isOnStartEnterTask()` | `AutoGroupOpenQuickEntryRequest.IsOnStartEnterTask(...)` | Model/Runtime Helper | Partial | Unit Tested | Partial Parity | 120-second guard is represented for runtime quick-entry requests. Looking-party registrations do not yet persist start-enter state for queue refill. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType` | `AutoGroupSummary` / `AutoGroupInstanceRuntimeRegistration` | Enum Metadata/Dataholder | Partial | Unit Tested | Partial Parity | `MaximumJoinTimeMilliseconds` now flows into runtime registration and is consumed by the quick-entry gate. Full enum behavior remains partial. |

## Next Sequential UOW

Next sequential production slice: wire `AutoGroupLookingPartyRegistrationService.StartLooking(...)` to try open quick-entry runtime attachment before queue matching, matching Java `AutoGroupService.startLooking(...)`:

```java
if (!checkInstancesForOpenQuickEntries(lfp, maskId))
    checkQueueForNewMatches(maskId);
```

Recommended scope:

- Add a start-looking callback/dependency that can call `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` after successful quick-entry registration.
- When the runtime gate returns `Added`, remove the accepted search entry, set/record ready-enter start time for the request, plan/send window `4` to the leader, and clean additional registrations for that leader.
- Preserve the current queue-match path when runtime attachment returns any non-added status.
- Keep refill after cancel/leave as a later UOW unless the same helper becomes trivial to reuse.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model next:

- New quick-entry `LookingForParty` can attach to an existing matching auto-instance before queue matching.
- Accepted quick-entry attachment removes the just-added search entry.
- Accepted quick-entry attachment sets `LookingForParty.startEnterTime` and sends `SM_AUTO_GROUP(maskId, 4)` to the leader.
- `searchAndRemoveAdditionalRegistrations(lfp.getLeaderObjId())` removes the leader from any other queues.
- Non-added runtime gate results fall back to `checkQueueForNewMatches(maskId)`.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: start-looking quick entries can attach to existing runtime auto-instances before queue matching while preserving the max-join gate, ready/start timestamps, queue cleanup, window `4`, and existing cancel/leave/press-enter behavior.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch or shared autogroup runtime state if the next UOW wires the runtime gate into packet handling; start focused and add only directly related tests.

## Safe Candidates

- Wire start-looking quick-entry runtime attachment before queue matching.
- Add open quick-entry window `4` delivery and additional-registration cleanup.
- Add queued quick-entry refill after cancel/leave using the same runtime gate.
- Add instance score-ended registration rejection when score runtime state exists.
- Add a press-enter destination/port-to-start-position plan for `AutoPvpInstance.onPressEnter(...)` after a C# handler abstraction exists.

Avoid:

- Evidence/reporting-only units.
- Claiming full `checkInstancesForOpenQuickEntries(...)` parity before start-looking removal, window `4`, and additional-registration cleanup are wired.
- Claiming full `AutoInstance.isRegistrationDisabled(...)` parity before score-ended rejection is modeled.
- Claiming full `createNewInstance(...)` parity before spawn content, handler subclasses, and start-position behavior are represented.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
