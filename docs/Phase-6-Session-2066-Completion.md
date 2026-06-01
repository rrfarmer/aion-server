# Phase 6 Session 2066 Completion - Find Group Live Dispatch Prerequisites

Date: 2026-06-01
Unit of Work: UOW-2066
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP.runImpl`, current C# `GameServerConnection` dispatch, and the disabled find-group action composer.
- Added a focused readiness map for live `CM_FIND_GROUP` dispatch prerequisites.
- Did not enable live dispatch.

## What Changed

- Added `FindGroupClientActionDispatchPrerequisites`.
- Added `FindGroupClientActionDispatchPrerequisitePlan`.
- Added `FindGroupClientActionDispatchReadiness`.
- Added `FindGroupClientActionRuntimeRequirement`.
- Added focused tests for:
  - Action `10` mask-list/runtime config prerequisites.
  - Actions `11` and `12` world-player lookup and side-effect prerequisites.
  - Parsed-but-no-run actions `20` and `25`.
  - Unknown actions.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesFindGroupPackets" --no-restore`
  - Result: passed, 12 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit added a disabled readiness map and tests only.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupClientActionDispatchPrerequisites` | Composition / Readiness Utility | Partial | Unit Tested | Partial Parity | Java run switch reviewed. C# readiness map names runtime facts and side-effect dispatchers required before live dispatch. It does not execute actions. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` dependencies | `FindGroupClientActionRuntimeRequirement` | Enum | Partial | Unit Tested | Needs Verification | Requirements identify active player, state store, player lookup, config/data lookup, packet dispatch, broadcast dispatch, team/member snapshots, and invite dispatch. Live sourcing remains unimplemented. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupClientActionDispatchPrerequisitesTests.Inspect_ActionTenRequiresMaskListRuntimeSourcesBeforeLiveDispatch` | Unit | Java `CM_FIND_GROUP.runImpl` action `10` and `FindGroupService.showInstanceGroups` source review | Action `10` requires active player, state, timestamp, direct packet dispatch, config, target NPC, and auto-group data before live dispatch | C# unit assertions plus Java source review | Does not prove live config/DataManager sourcing |
| `FindGroupClientActionDispatchPrerequisitesTests.Inspect_InstanceApplicationActionsRequireWorldPlayerLookup` | Unit | Java actions `11` and `12` source review | World player lookup and send/invite side-effect prerequisites are explicit | C# unit assertions plus Java source review | Does not implement `World.getPlayer` or group/alliance invite dispatch |
| `FindGroupClientActionDispatchPrerequisitesTests.Inspect_ParsedActionsWithoutJavaRunImplHaveNoLiveRequirements` | Unit | Java actions `20` and `25` source review | Parsed-but-no-run actions have no live requirements | C# unit assertions and Java parser golden tests | Does not prove future live dispatch stays disabled |
| `FindGroupClientActionDispatchPrerequisitesTests.Inspect_UnknownActionHasNoRuntimeRequirements` | Unit | Java default/no run branch behavior | Unknown action maps to no requirements | C# unit assertions | Logging parity for unknown actions remains parser-level/deferred |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; readiness mapping has source-reviewed unit evidence.
- Total artifacts needing verification or partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Readiness requirements are conservative and may need refinement when real runtime services are wired.
- No live `FindGroupService` singleton, world player lookup, packet send/broadcast, config/data sourcing, team/member snapshot sourcing, or group/alliance invite dispatch is implemented by this unit.
- Real-client behavior and service concurrency remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionDispatchPrerequisites.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionDispatchPrerequisitesTests.cs`
- `docs/Phase-6-Session-2066-Completion.md`
- `docs/Phase-6-Session-2066-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect and model the smallest runtime fact source for live `CM_FIND_GROUP` dispatch, likely active-player plus deterministic current-time/state-store access, without enabling packet sends.

Safe alternative candidates:

- Add a disabled end-to-end action composition test that uses `FindGroupClientAction.FromPacket` plus runtime prerequisite inspection.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
