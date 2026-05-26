# Phase 6YD Completion - UOW-1142 House Object Talk Range

Date: May 26, 2026

## Unit Of Work

UOW-1142: `[Phase 6][UOW-1142] Align house object talk range`

## Summary

UOW-1142 audits Java house-object talk range and tightens the C# `CM_USE_HOUSE_OBJECT` target range calculation. The C# handler now uses the shared Java-shaped `PositionUtilService` helper instead of an inclusive boundary and visible-distance fallback.

This remains a focused range-gate correction. It does not perform Java runtime comparison, production socket-order validation, or complete the broader house-object use/storage/postbox side-effect surface.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PositionUtilServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YD-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PositionUtilServiceTests\|GamePacketTests\|HousingWorldServiceTests" --nologo` | Passed: 104 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,145 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,352 tests. |

## Migration Parity Table - UOW-1142

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_HOUSE_OBJECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleUseHouseObjectAsync` | Packet / Handler | Partial | Unit Tested indirectly | Partial Parity | Existing C# handler already gates too-far use with the Java housing-too-far system message. This unit tightens target range calculation before dispatch. Packet route remains broad production code; no live socket-order comparison. |
| `com.aionemu.gameserver.controllers.PlaceableObjectController.onDialogRequest` | `GameServerConnection.FindHouseObjectUseTargetAsync` / `IsInHouseObjectTalkRange` | Controller / Range Gate | Partial | Unit Tested through helper | Partial Parity | C# now applies the same strict talk-range geometry before use/storage/postbox handling. Java controller side effects beyond range rejection remain partial. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange(Creature, HouseObject<?>)` | `Aion.GameServer.Services.PositionUtilService.IsInObjectTalkRange` | Utility Method | Partial | Unit Tested | Partial Parity | House-object range uses `talkingDistance + 1`, player radius, target radius `0`, world/instance equality, and strict `< range²`. Java runtime comparison was not executed. |
| `com.aionemu.gameserver.model.templates.housing.AbstractHouseObject.getTalkingDistance` | `Aion.GameServer.Dataholders.HousingObjectTemplateSummary.TalkingDistance` | Static Data Fact | Partial | Existing Unit Coverage | Partial Parity | Existing static-data loader carries housing `talking_distance`; this unit consumes it directly with no visible-distance fallback. XML validation/runtime Java dataholder comparison remains partial. |
| `com.aionemu.gameserver.model.templates.VisibleObjectTemplate.getBoundRadius` | `PositionUtilService.IsInObjectTalkRange` target radius input `0` for house objects | Template Default | Partial | Unit Tested | Partial Parity | Java house-object templates currently use default zero bound radius. C# models this as an explicit `0` at the house-object caller. If future C# house-object templates add radii, this caller must be revisited. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `PositionUtilServiceTests.IsInObjectTalkRange_ModelsHouseObjectTalkingDistance` | House-object talking distance plus Java player radius admits just-inside distances and rejects exactly-on-boundary distances. | Source-reviewed Java `PositionUtil.isInTalkRange(Creature, HouseObject<?>)` and default house-object bound radius. |

## Remaining Risks

- House-object range is covered at the shared helper level, not by a direct integration test through `GameServerConnection.HandleUseHouseObjectAsync`.
- Full house-object behavior, including occupant release on not-known, use/storage/postbox branch side effects, persistence, packet ordering, and deletion/expiration side effects, remains partial.
- Java runtime comparison, broader `PositionUtil` methods, non-player creature radius sourcing, threading, and live `DataManager` integration remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial production range-gate correction plus 1 helper test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: direct handler integration tests, full house-object side effects, non-player creature radius sourcing, Java runtime comparison, and production socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit removes the visible-distance/inclusive-boundary mismatch from house-object talk range.

## Next Recommended Unit Of Work

Continue house-object use parity with a small `GameServerConnection`-level too-far integration test if the required harness is practical, or move to the trade-list runtime-readiness audit before production NPC dialog routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | House-object handler integration test feasibility | Read-only first across existing GameServerConnection tests/fixtures | Medium | Could become sequential if it needs shared socket/connection harness changes. |
| B | Trade-list runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Independent if it avoids house-object and dialog range files. |
| C | Non-player creature radius audit | Java pet/summon/static-object templates and C# models; read-only first | Medium | Needed for future non-player callers, not current player-to-NPC or player-to-house-object paths. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only survey of existing `GameServerConnection` test harnesses for house-object too-far coverage | Read-only | All writes |
| Agent B | Read-only trade-list runtime-readiness audit | Read-only | All writes |

Keep implementation sequential if changing `GameServerConnection.cs`, shared test fixtures, or Phase 6 docs.

## Do Not Parallelize

- `GameServerConnection.cs`: broad production packet handler.
- `PositionUtilService.cs`: shared geometry utility.
- Shared socket/test harness files unless one owner is assigned.
- Phase 6 progress/handoff docs: orchestrator-owned.
