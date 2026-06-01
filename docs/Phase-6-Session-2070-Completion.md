# Phase 6 Session 2070 Completion - Find Group World Player Resolver Fact

Date: 2026-06-01
Unit of Work: UOW-2070
Status: Completed

## Scope

- Added `World.getPlayer`-equivalent resolver sourcing to the disabled find-group connection adapter.
- Kept live `CM_FIND_GROUP` dispatch disabled.
- Preserved explicit resolver injection so tests and future runtime bridges can override the world snapshot.

## What Changed

- Extended `FindGroupConnectionClientActionCompositionPlanService` with an optional C# `World` dependency.
- Defaulted the adapter's `resolvePlayer` fact to a `World.TryGetObject(... Player ...)` lookup when no explicit resolver is supplied.
- Added focused tests proving:
  - Action `11` application composition resolves the recruiter from world state.
  - Action `12` application-result composition resolves the applicant from world state.
  - Explicit resolver delegates override world lookup.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionRuntimeFactsTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests" --no-restore`
  - Result: passed, 20 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit changed only a disabled find-group adapter and focused tests.
  - No shared packet primitives, serialization helpers, crypto, persistence, live connection dispatch case, packet sends, or live side effects were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java resolves the recipient with `World.getInstance().getPlayer(playerOrTeamId)`. C# disabled adapter now sources a `Player` from C# `World.TryGetObject` when no explicit resolver is supplied; no live packet send is enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java resolves the applicant with `World.getInstance().getPlayer(applicantId)`. C# disabled adapter now sources that runtime fact from C# world state for disabled planning only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Parsed C# actions `11` and `12` flow into the disabled world-backed adapter. Java parser golden tests cover payload layout. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesWorldResolverForInstanceApplication` | Unit | Java `FindGroupService.sendInstanceApplication` source review | Disabled adapter uses C# world state as the default resolver for action `11` recipient lookup | C# unit assertions plus Java parser golden | Does not send `SM_FIND_GROUP` live |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesWorldResolverForApplicationResult` | Unit | Java `FindGroupService.sendInstanceApplicationResult` source review | Disabled adapter uses C# world state as the default resolver for action `12` applicant lookup and composes invite intent | C# unit assertions plus Java parser golden | Does not call group/alliance invite services live |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_ExplicitResolverOverridesWorldResolver` | Unit | Java source review plus C# adapter design | Explicit resolver delegates remain authoritative over default world lookup | C# unit assertions | Override behavior is C# adapter plumbing, not a Java live behavior claim |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Adapter does not source group/team snapshots, config, target NPC masks, or `DataManager.AUTO_GROUP` facts from live runtime services.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2070-Completion.md`
- `docs/Phase-6-Session-2070-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add runtime-fact sourcing for current-team and current-member snapshots into the disabled find-group connection adapter, still without live sends.

Safe alternative candidates:

- Source `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` and target NPC mask facts for action `10` from existing C# config/static-data surfaces if available.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
