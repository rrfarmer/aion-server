# Phase 6 Session 2072 Completion - Find Group Alliance Snapshot Evidence

Date: 2026-06-01
Unit of Work: UOW-2072
Status: Completed

## Scope

- Added focused alliance-backed evidence for the disabled find-group connection adapter.
- Kept production code unchanged in this unit.
- Confirmed the existing team/member fact sourcing handles `PlayerAllianceRuntime` snapshots.

## What Changed

- Added tests for alliance-backed:
  - Action `2` recruitment subject composition.
  - Action `8` instance group member snapshot composition.
- Confirmed the adapter remains disabled and does not dispatch packets or live side effects.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionRuntimeFactsTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests" --no-restore`
  - Result: passed, 25 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit added focused adapter tests only.
  - No production code, shared packet primitives, serialization helpers, crypto, persistence, live connection dispatch case, packet sends, or live side effects were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java uses `player.getCurrentTeam()` when present before building `GroupRecruitment`. C# disabled adapter now has focused evidence for alliance runtime facts as well as group facts; no live send is enabled. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentSubject` via connection adapter | DTO / Adapter Fact | Partial | Unit Tested | Partial Parity | Alliance test covers team id, leader name/class, member count, race, and member level range from `PlayerAllianceRuntime`. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup.getMembers` | `Aion.GameServer.Services.FindGroupInstanceGroupMemberState` via connection adapter | DTO / Adapter Fact | Partial | Unit Tested | Partial Parity | Alliance test covers current alliance members projected to disabled instance group member states. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Parsed C# actions `2` and `8` flow into the disabled alliance-backed adapter. Java parser golden tests cover payload layout. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesAllianceRuntimeForRecruitmentSubject` | Unit | Java `FindGroupService.addRecruitment` and `GroupRecruitment` source review | Disabled adapter derives alliance id, leader metadata, size, and level range from C# alliance runtime | C# unit assertions plus Java parser golden | Does not broadcast recruitment live |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesAllianceRuntimeMembersForInstanceGroupRegistration` | Unit | Java `ServerWideGroup.getMembers` source review | Disabled adapter supplies alliance member states for instance-group registration composition | C# unit assertions plus Java parser golden | Does not send action `14` packet live |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- C# does not yet have a sourced `AutoGroupData`/recruitable-instance-mask table for action `10` config/data facts.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2072-Completion.md`
- `docs/Phase-6-Session-2072-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect C# static-data loading for a minimal `AutoGroupData`/recruitable-instance-mask table representation, then source action `10` mask facts only if that representation can be added safely.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Add disabled adapter diagnostics documenting missing action `10` static-data source if static-data representation is still absent.
