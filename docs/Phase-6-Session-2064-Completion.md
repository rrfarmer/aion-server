# Phase 6 Session 2064 Completion - CM_FIND_GROUP Action 25 No-Run Evidence

Date: 2026-06-01
Unit of Work: UOW-2064
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP.readImpl` and `runImpl` for action `25`.
- Confirmed Java parses action `25` as "ban from instance group" payload data but has no `runImpl` branch for it.
- Strengthened C# disabled composition evidence so action `25` cannot accidentally plan live side effects.

## What Changed

- Added a Java parity breadcrumb in `FindGroupClientActionPlanService` explaining why actions `20` and `25` return `ParsedButNoRunImpl`.
- Strengthened `FindGroupClientActionPlanServiceTests` to assert parsed-but-no-run plans have no recruitment, application, instance-group, member-info, or instance-application plan payloads.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesFindGroupPackets" --no-restore`
  - Result: passed, 6 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit changed only disabled composition evidence and focused tests.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested / Golden File Tested | Partial Parity | Action `25` payload parsing is covered by C# parser tests and Java parser golden tests. Java has no `runImpl` branch for action `25`; C# composition intentionally plans no live side effect. Full live handler parity remains deferred. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action absence | `Aion.GameServer.Services.FindGroupClientActionPlanService` | Composition Service | Partial | Unit Tested | Partial Parity | Actions `20` and `25` are represented as parsed-but-no-run. Tests assert all planner payload slots remain null and live dispatch stays disabled. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupClientActionPlanServiceTests.Plan_DocumentsParsedActionsWithoutJavaRunImplAndUnknownActionsAsNonDispatching` | Unit | Java `CM_FIND_GROUP.runImpl` switch review | Actions `20` and `25` produce no disabled planner payloads or live side effects | Static Java source review plus C# focused assertions | Does not prove future live handler wiring; it prevents this disabled composer from inventing behavior |
| `GamePacketTests.ClientPacketFactory_ParsesFindGroupPackets` | Unit | Java `CM_FIND_GROUP.readImpl`; Java parser golden test | Action `25` reads `playerOrTeamId`, `instanceMaskId`, and `bannedPlayerId` | C# parser assertions and Java `CM_FIND_GROUP_ReadPayloadGoldenTest` | Does not imply action `25` is executed |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 1.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; parser slices have focused evidence.
- Total artifacts needing verification or partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- No live Java action `25` behavior exists in `CM_FIND_GROUP.runImpl`; C# therefore intentionally does not implement a find-group ban side effect for this packet.
- Java group/alliance ban services exist elsewhere, but no `CM_FIND_GROUP` action `25` call site was found in this unit.
- Live `CM_FIND_GROUP` dispatch remains deferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionPlanServiceTests.cs`
- `docs/Phase-6-Session-2064-Completion.md`
- `docs/Phase-6-Session-2064-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect logout cleanup parity for recruitment/application/instance-group maps.

Safe alternative candidates:

- Continue refining disabled live-handler composition only after runtime dependency sourcing is explicitly planned.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
