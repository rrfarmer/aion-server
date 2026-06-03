# Phase 6 Session 2385 Handoff - Apply Autogroup Type Difficulty Metadata

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2385-Completion.md`
- `docs/Phase-6-Session-2385-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2385, ported Java `AutoGroupType` maximum-join/difficulty metadata and used difficulty id during live ready-match instance allocation.

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
- Ready queue match allocation now passes Java-derived `AutoGroupType.getDifficultId()` into modeled world instance state.
- C# auto-group summaries expose Java-derived `AutoGroupType.getMaximumJoinTime()` as `MaximumJoinTimeMilliseconds`.
- `CM_AUTO_GROUP` window `102` press-enter can resolve matched players from the ready-match runtime bridge and send window `5`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position behavior remains missing.
- Java `AutoInstance.isRegistrationDisabled(lfp)` maximum-join quick-entry rejection remains missing.
- Java `SpawnEngine.spawnInstance(...)` and event spawn behavior are not wired into autogroup ready-match allocation.
- Java `LookingForParty.isOnStartEnterTask()` 120-second use in quick-entry/open-registration decisions remains incomplete.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.

## Commits Made

- `[Phase 6][UOW-2385] Apply autogroup type difficulty metadata`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2385-Completion.md`
- `docs/Phase-6-Session-2385-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

Result: passed 102, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this enum/static-data metadata bridge. Java source review identified authoritative mask-to-time/difficulty values.

Broad-validation trigger: live connection dispatch and shared world/instance runtime allocation state are touched.

Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered static-data metadata, ready-match runtime registration, live ready-match allocation, and world-map runtime state.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Enum Metadata/Dataholder | Partial | Unit Tested | Partial Parity | `maximumJoinTime` and `difficultId` are represented for known Java enum masks. Full enum behavior, factory selection, and PvP arena availability remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `GameServerConnection.MaterializeAutoGroupReadyMatchRuntimeInstance(...)` | Service/Connection | Partial | Unit Tested | Partial Parity | Ready-match allocation now passes Java-derived difficulty id. Spawn content, handler subclasses, and start-position porting remain missing. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(...)` | `InstanceRuntimeService.GetNextAvailableInstance(...)` / `WorldMapInstanceRuntimeState` | Service/Runtime | Partial | Unit Tested | Partial Parity | Modeled instance receives the requested difficulty id through the autogroup live bridge. Java spawn engine side effects remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeRegistration` | Runtime Model | Partial | Unit Tested | Partial Parity | Runtime registration carries difficulty id plus prior ready/start timestamps. Maximum-join quick-entry rejection remains missing. |

## Next Sequential UOW

Next sequential production slice: consume the newly modeled `MaximumJoinTimeMilliseconds` for Java `AutoInstance.isRegistrationDisabled(lfp)` quick-entry/open-registration behavior, or implement a press-enter destination plan once C# instance handler start-position support is introduced.

Recommended first candidate: add open quick-entry planning around registered runtime instances using `StartInstanceTime`, `MaximumJoinTimeMilliseconds`, quick-registration allowance, mask id, and player race/member-count facts.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model next:

- `AutoInstance.isRegistrationDisabled(lfp)` returns false before instance creation, rejects ended instance scores, accepts quick entries only until `System.currentTimeMillis() - startInstanceTime <= agt.getMaximumJoinTime()`, and rejects non-quick entries after creation.
- `AutoGroupService.checkInstancesForOpenQuickEntries(lfp, maskId)` tries to attach a quick-entry `LookingForParty` to an existing matching auto-instance before queue matching.
- `AutoGroupService.checkQueueForQuickEntries(autoInstance)` refills open quick-entry slots after cancel/leave.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: the next quick-entry/open-registration bridge preserves ready-match allocation reachability, Java maximum-join metadata use, ready/start timestamps, and existing cancel/leave/press-enter behavior.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: shared autogroup runtime state or live connection dispatch if the next UOW mutates runtime state; start focused and add only directly related tests if needed.

## Safe Candidates

- Add open quick-entry planning around registered runtime instances using `MaximumJoinTimeMilliseconds`.
- Add 120-second ready-enter/open-registration checks using `ReadyEnterStartTime`.
- Add a press-enter destination/port-to-start-position plan for `AutoPvpInstance.onPressEnter(...)` after a C# handler abstraction exists.
- Add penalty scheduling as explicit delayed runtime state once quick-entry refill planning is stable.

Avoid:

- Evidence/reporting-only units.
- Claiming full `AutoGroupType` parity before factory selection, PvP arena availability, and full enum behavior are represented.
- Claiming full `createNewInstance(...)` parity before spawn content, handler subclasses, and start-position behavior are represented.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
