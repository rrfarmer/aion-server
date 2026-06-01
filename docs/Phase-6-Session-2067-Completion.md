# Phase 6 Session 2067 Completion - Find Group Runtime Facts Boundary

Date: 2026-06-01
Unit of Work: UOW-2067
Status: Completed

## Scope

- Added a typed runtime-facts boundary for disabled `CM_FIND_GROUP` composition.
- Kept live dispatch disabled.
- Proved parsed `CmFindGroup` packets can flow through runtime facts into the disabled planner.

## What Changed

- Added `FindGroupClientActionRuntimeFacts`.
- Added `ComposeDisabledPlan(...)` overloads for:
  - Parsed `CmFindGroup` packets.
  - Already-normalized `FindGroupClientAction` records.
- Added focused tests proving runtime facts supply:
  - Action `10` mask-list/config data.
  - Action `12` world-player resolver data.
  - Parsed packet action `2` data through `FindGroupClientAction.FromPacket`.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupClientActionRuntimeFactsTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Result: passed, 15 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit added a typed disabled runtime-facts boundary and tests only.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupClientActionRuntimeFacts` | Composition DTO | Partial | Unit Tested | Partial Parity | Java runImpl obtains active player from connection and runtime facts from service/world/config/data managers. C# facts package those values for disabled planning only; no live sends are enabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `FindGroupClientAction.FromPacket(CmFindGroup)` via runtime facts test | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | C# parsed packet action `2` flows into disabled planner. Java parser golden tests cover CM_FIND_GROUP payload layout. Full live handler remains deferred. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupClientActionRuntimeFactsTests.ComposeDisabledPlan_UsesRuntimeFactsForActionTenMaskList` | Unit | Java `CM_FIND_GROUP` action `10` and `FindGroupService.showInstanceGroups` source review | Runtime facts provide mask-list/config snapshots to disabled action `10` composition | C# unit assertions | Does not source live config/DataManager/target NPC |
| `FindGroupClientActionRuntimeFactsTests.ComposeDisabledPlan_UsesResolverFactForInstanceApplicationResult` | Unit | Java action `12` source review | Runtime facts provide a world-player resolver for disabled application-result planning | C# unit assertions | Does not implement live `World.getPlayer` or invite dispatch |
| `FindGroupClientActionRuntimeFactsTests.ComposeDisabledPlan_CanUseParsedCmFindGroupPacket` | Unit | Java `CM_FIND_GROUP.readImpl` and run action `2` source review | Parsed C# packet data flows through `FindGroupClientAction.FromPacket` into disabled composition | C# parser/factory path plus Java parser golden tests | Does not enable live connection dispatch |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 1.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; parser bridge has focused evidence.
- Total artifacts needing verification or partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- `FindGroupClientActionRuntimeFacts` packages facts but does not source them from live `GameServerConnection`, `World`, `GroupConfig`, target NPC, `DataManager.AUTO_GROUP`, or team services.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionRuntimeFacts.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionRuntimeFactsTests.cs`
- `docs/Phase-6-Session-2067-Completion.md`
- `docs/Phase-6-Session-2067-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add a disabled `GameServerConnection`-adjacent adapter plan for `CmFindGroup` that extracts `ActivePlayer` and produces a disabled composition result, but still does not send packets.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Add runtime-fact sourcing tests for current-team/member snapshots before live dispatch.
