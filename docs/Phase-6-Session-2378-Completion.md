# Phase 6 Session 2378 Completion - Port Autogroup Queue Match Planning

## Scope

Ported a non-mutating planning slice of `AutoGroupService.checkQueueForNewMatches` for periodic PvP autogroups.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/EntryRequestType.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AGQuestion.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`

Java behavior used:

- `checkQueueForNewMatches(maskId)` returns when the queue is missing/empty or the autogroup type is missing.
- Java sorts queued `LookingForParty` entries with `Comparable`: higher `EntryRequestType.ordinal()` first, larger party size first, then older registration time first.
- For periodic PvP instances, `AutoPvpInstance.addLookingForParty` rejects parties when the race-specific count would exceed `InstanceCooltimeData.getMaxMemberCount(worldId, race)`.
- Before instance creation, `AutoPvpInstance.getMaxPlayers()` returns Asmodian capacity plus Elyos capacity.
- Java probes each sorted starting party, then later sorted parties, and reaches `AGQuestion.READY` only when the tentatively registered player count equals the total capacity.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `EntryRequestType` and `RegistrationTime` to `AutoGroupLookingPartyRegistration`.
- Extended `RegisterLookingParty` test helper path to seed race, request type, and registration time.
- Added `AutoGroupQueueMatchPlan` and `AutoGroupQueueMatchPlanStatus`.
- Added `CreateQueueMatchPlan(...)` to order queued parties and plan periodic PvP ready/not-ready matching without mutating queues or creating world instances.
- Added focused tests for Java queue ordering, ready periodic PvP matching, and race over-capacity rejection.

Known limitations:

- This UOW does not call the planner from live `StartLooking`; it only ports the deterministic planning behavior needed by the next wiring slice.
- This UOW models `AutoPvpInstance` periodic capacity rules. Harmony, FFA/solo/glory, open quick-entry matching, instance creation, queue mutation, packet fanout, and additional-registration removal remain deferred.

## Validation Decision

- Changed surface: non-live autogroup queue service/planner and focused service tests.
- Specific behavior/contract: Java `LookingForParty.compareTo` ordering and periodic `AutoPvpInstance.addLookingForParty` capacity readiness should be represented by a non-mutating C# queue match plan.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore
```

Result: passed 27, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for `AutoGroupService.checkQueueForNewMatches`; Java source review identified ordering, capacity rules, and readiness behavior.
- Broad-validation trigger: none. This UOW added a non-live planner and tests without changing live connection dispatch, packet primitives, shared runtime state, persistence, or scheduling.
- Broad .NET decision: skipped. The focused filtered command compiled the affected project and validated the edited service behavior.
- Why this scope is sufficient: the edited behavior is local to queue planning and has no side effects; the focused service tests assert the Java-derived ordering and periodic PvP capacity outcomes directly.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.checkQueueForNewMatches(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateQueueMatchPlan(...)` | Service/Planner | Partial | Unit Tested | Partial Parity | Queue ordering and periodic PvP readiness planning are covered. Live queue mutation, instance creation, and other auto-instance types remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Member ids, race, entry type, leader id, and registration time are represented. Java `startEnterTime`, `AGPlayer` class/name data, leader reassignment, and member unregister behavior remain partial. |
| `com.aionemu.gameserver.model.autogroup.EntryRequestType` | `Aion.GameServer.Services.AutoGroupEntryRequestType` | Enum | Complete | Unit Tested | Partial Parity | Ids and ordering are used in parser and queue sort tests. Full lifecycle behavior remains partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateQueueMatchPlan(...)` | Service/Planner | Partial | Unit Tested | Partial Parity | Periodic PvP add/readiness capacity logic is modeled for planning. `onEnterInstance`, `onPressEnter`, team creation, instance registration, and live `AutoInstance` state remain missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `Aion.GameServer.Services.AutoGroupQueueMatchPlan` | Abstract Runtime/Planner | Partial | Unit Tested | Partial Parity | Only pre-instance max-player and add-party readiness concepts are represented. Registration-disabled checks for existing instances and runtime callbacks remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.CreateQueueMatchPlan_OrdersQueuedPartiesLikeJavaLookingForPartyCompareTo` | Unit | Java source review | Entry type descending, party size descending, then registration time ascending queue order. | Focused C# service test. | Does not compare against a Java runtime fixture. |
| `AutoGroupLookingPartyRegistrationServiceTests.CreateQueueMatchPlan_PeriodicPvpMatchReachesReadyLikeJavaAutoPvpInstance` | Unit | Java source review | Periodic PvP plan reaches ready when accepted same-race counts fill Elyos plus Asmodian capacity. | Focused C# service test. | Does not create a live world instance. |
| `AutoGroupLookingPartyRegistrationServiceTests.CreateQueueMatchPlan_PeriodicPvpSkipsRaceOverCapacityPartiesLikeJavaAutoPvpInstance` | Unit | Java source review | Race over-capacity parties are rejected and do not produce a ready match. | Focused C# service test. | Other auto-instance subclasses remain deferred. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup queue matching planning advanced, but live lifecycle parity remains partial.

## Remaining Gaps

- Live `StartLooking` still does not execute queue matching after successful registration.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java `createNewInstance(...)` remains missing, including instance allocation, auto-instance registry update, queue removals, `startEnterTime`, additional-registration cleanup, and window `4` fanout.
- Harmony, FFA/solo/glory, and recruitable auto-instance matching remain partial or missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2378] Port autogroup queue match planning
```
