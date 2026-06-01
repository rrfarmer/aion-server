# Phase 6 Session 2071 Completion - Find Group Team Snapshot Facts

Date: 2026-06-01
Unit of Work: UOW-2071
Status: Completed

## Scope

- Added current-team and current-member snapshot sourcing to the disabled find-group connection adapter.
- Kept live `CM_FIND_GROUP` dispatch disabled.
- Preserved explicit `currentTeam`/`currentMembers` override inputs.

## What Changed

- Extended `FindGroupConnectionClientActionCompositionPlanService` with optional `PlayerGroupRuntime` and `PlayerAllianceRuntime` dependencies.
- Added disabled runtime fact assembly for:
  - `player.getCurrentTeam()` equivalent recruitment subject data.
  - `ServerWideGroup.getMembers()` equivalent member snapshots.
- Added focused tests for:
  - Group-backed action `2` recruitment subject composition.
  - Group-backed action `8` instance group member snapshots.
  - Explicit `currentTeam` override precedence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionRuntimeFactsTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests" --no-restore`
  - Result: passed, 23 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit changed only a disabled find-group adapter and focused tests.
  - No shared packet primitives, serialization helpers, crypto, persistence, live connection dispatch case, packet sends, or live side effects were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java uses `player.getCurrentTeam()` when present before building `GroupRecruitment`. C# disabled adapter now sources group/alliance runtime facts into `FindGroupRecruitmentSubject`; no live send is enabled. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentSubject` via connection adapter | DTO / Adapter Fact | Partial | Unit Tested | Partial Parity | Java team recruitment exposes team id, leader name/class, member count, race, and member level range. C# adapter composes equivalent disabled facts from runtime snapshots where available. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup.getMembers` | `Aion.GameServer.Services.FindGroupInstanceGroupMemberState` via connection adapter | DTO / Adapter Fact | Partial | Unit Tested | Partial Parity | Java returns current team members when recruiter is in a team, otherwise the recruiter list. C# adapter now supplies current team member states for disabled instance-group registration planning. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Parsed C# actions `2` and `8` flow into the disabled team-backed adapter. Java parser golden tests cover payload layout. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesGroupRuntimeForRecruitmentSubject` | Unit | Java `FindGroupService.addRecruitment` and `GroupRecruitment` source review | Disabled adapter derives team id, leader metadata, size, and level range from C# group runtime | C# unit assertions plus Java parser golden | Does not broadcast recruitment live |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesGroupRuntimeMembersForInstanceGroupRegistration` | Unit | Java `ServerWideGroup.getMembers` source review | Disabled adapter supplies team member states for instance-group registration composition | C# unit assertions plus Java parser golden | Does not send action `14` packet live |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_ExplicitCurrentTeamOverridesRuntimeTeamFact` | Unit | C# adapter design | Explicit current-team runtime fact remains authoritative over default runtime sourcing | C# unit assertions | Override behavior is adapter plumbing, not a Java live behavior claim |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Adapter does not source `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`, target NPC masks, or `DataManager.AUTO_GROUP` facts from live runtime services.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.
- Alliance-backed current-team tests are not yet added.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2071-Completion.md`
- `docs/Phase-6-Session-2071-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: source `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` and target NPC mask facts for action `10` from existing C# config/static-data surfaces if available, still without live sends.

Safe alternative candidates:

- Add alliance-backed current-team/member snapshot tests for the disabled adapter.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
