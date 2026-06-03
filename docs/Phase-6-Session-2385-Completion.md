# Phase 6 Session 2385 Completion - Apply Autogroup Type Difficulty Metadata

## Scope

Ported Java `AutoGroupType` enum-derived difficulty and maximum-join metadata onto C# `AutoGroupSummary`, then used the difficulty id during live ready-match instance allocation.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/data/static_data/auto_group/auto_group.xml`

Java behavior used:

- `AutoGroupType` carries `maximumJoinTime` independently from `auto_group.xml`.
- Arena masks carry non-zero `difficultId` values; periodic PvP masks use difficulty `0`.
- `AutoGroupService.createNewInstance(...)` passes `agt.getDifficultId()` into `InstanceService.getNextAvailableInstance(...)`.
- `AutoInstance.isRegistrationDisabled(lfp)` uses `agt.getMaximumJoinTime()` for late quick-entry rejection after instance creation.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added Java `AutoGroupType.getMaximumJoinTime()` equivalent metadata as `AutoGroupSummary.MaximumJoinTimeMilliseconds`.
- Added Java `AutoGroupType.getDifficultId()` equivalent metadata as `AutoGroupSummary.DifficultyId`.
- Extended `AutoGroupInstanceRuntimeRegistration` with `DifficultyId`.
- `AutoGroupLookingPartyRegistrationService` now includes the auto-group difficulty id in ready-match runtime-registration intents.
- `GameServerConnection.MaterializeAutoGroupReadyMatchRuntimeInstance(...)` now passes the registration difficulty id into `InstanceRuntimeService.GetNextAvailableInstance(...)`.
- Added static-data parity assertions for representative Java auto-group masks:
  - arena masks with difficulty `1` and `4`;
  - periodic PvP masks with maximum join windows and difficulty `0`;
  - recruitable non-enum masks with default metadata.
- Added autogroup service and connection assertions that periodic PvP ready matches continue to allocate with Java difficulty `0`.

Known limitations:

- `MaximumJoinTimeMilliseconds` is modeled but not yet consumed by quick-entry/open-registration logic.
- Difficulty is now carried into the modeled world instance, but the bridge still does not spawn Java instance contents or construct Java-equivalent handler subclasses.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.

## Validation Decision

- Changed surface: auto-group static metadata, ready-match runtime registration, live connection allocation, and tests.
- Specific behavior/contract: C# auto-group summaries expose Java `AutoGroupType` maximum-join/difficulty metadata, and live ready-match allocation passes the Java-derived difficulty id into modeled world instance state.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

Result: passed 102, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this enum/static-data metadata bridge; Java source review supplied the authoritative mask-to-time/difficulty values.
- Broad-validation trigger: live connection dispatch and shared world/instance runtime allocation state are touched.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered static-data metadata, ready-match runtime registration, live ready-match allocation, and world-map runtime state.
- Why this scope is sufficient: the UOW only added derived metadata and passed difficulty through an existing allocation call; packet primitives, persistence, scheduler, spawn engine internals, and XML parsing shape were not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Enum Metadata/Dataholder | Partial | Unit Tested | Partial Parity | `maximumJoinTime` and `difficultId` are represented for known Java enum masks. Full enum behavior, factory selection, and PvP arena availability remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `GameServerConnection.MaterializeAutoGroupReadyMatchRuntimeInstance(...)` | Service/Connection | Partial | Unit Tested | Partial Parity | Ready-match allocation now passes Java-derived difficulty id. Spawn content, handler subclasses, and start-position porting remain missing. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(...)` | `InstanceRuntimeService.GetNextAvailableInstance(...)` / `WorldMapInstanceRuntimeState` | Service/Runtime | Partial | Unit Tested | Partial Parity | Modeled instance receives the requested difficulty id through the autogroup live bridge. Java spawn engine side effects remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeRegistration` | Runtime Model | Partial | Unit Tested | Partial Parity | Runtime registration carries difficulty id plus prior ready/start timestamps. Maximum-join quick-entry rejection remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StaticDataLoadingTests.AssertAutoGroupCorpusMatchesJavaStaticData` | Unit | Java source review plus Java static XML | Representative masks expose Java `maximumJoinTime` and `difficultId` metadata. | Focused C# static-data test. | Does not validate every enum mask individually. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java source review | Ready-match runtime-registration intent carries the Java-derived periodic PvP difficulty id. | Focused C# service test. | Periodic mask has difficulty `0`; non-zero difficulty is covered by static-data metadata tests. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupReadyMatchSendsWindowFourAndRemovesQueuesLikeJava` | Unit | Java source review | Live ready-match allocation stores the registration difficulty id on the modeled world instance while preserving window `4` and window `102` behavior. | Focused C# connection/world runtime test. | Spawn content and start-position teleport remain missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup ready-match allocation now carries Java difficulty metadata, but full auto-instance lifecycle remains partial.

## Remaining Gaps

- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- `MaximumJoinTimeMilliseconds` is not yet used by quick-entry/open-registration eligibility.
- Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains outside the autogroup allocation bridge.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` and quick-entry refill remain missing.
- Penalty scheduling, delayed open-registration refresh, member-cleanup queue recheck, and full destroy lifecycle remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2385] Apply autogroup type difficulty metadata
```
