# Phase 6YE Completion - UOW-1143 House Object Range Regression

Date: May 26, 2026

## Unit Of Work

UOW-1143: `[Phase 6][UOW-1143] Cover house object range target`

## Summary

UOW-1143 adds handler-level regression coverage for the C# house-object use target builder. The test exercises `GameServerConnection.TryCreateHouseObjectUseTarget` through reflection and verifies Java-style strict range metadata for just-inside and exact-boundary house-object use.

This is test-only parity work. It does not change production code, invoke a live socket, send packets, persist house-object state, or run Java runtime comparison.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionHouseObjectTalkRangeTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YE-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionHouseObjectTalkRangeTests\|PositionUtilServiceTests" --nologo` | Passed: 8 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,147 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,354 tests. |

## Migration Parity Table - UOW-1143

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_HOUSE_OBJECT` | `Aion.GameServer.Network.Aion.GameServerConnection` house-object target builder | Packet / Handler | Partial | Unit Tested | Partial Parity | Production target builder now has direct regression coverage for Java strict house-object talk range. Full packet handler invocation, socket order, sends, persistence, and side effects remain partial. |
| `com.aionemu.gameserver.controllers.PlaceableObjectController.onDialogRequest` | `GameServerConnection.TryCreateHouseObjectUseTarget` range metadata | Controller / Range Gate | Partial | Unit Tested | Partial Parity | Test validates the C# range gate metadata that drives the too-far branch before object use. Java controller side effects beyond range rejection remain unverified. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange(Creature, HouseObject<?>)` | `PositionUtilService.IsInObjectTalkRange` as consumed by `GameServerConnection` | Utility Method | Partial | Unit Tested | Partial Parity | Handler-level test covers just-inside and exact-boundary decisions through the production target builder. No Java runtime comparison. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionHouseObjectTalkRangeTests.TryCreateHouseObjectUseTarget_UsesJavaHouseObjectTalkRange` | Production target builder marks just-inside house-object use as in range and exact-boundary use as out of range. | Source-reviewed Java `CM_USE_HOUSE_OBJECT`, `PlaceableObjectController.onDialogRequest`, and `PositionUtil.isInTalkRange(Creature, HouseObject<?>)`. |

## Remaining Risks

- Test exercises target construction metadata, not full `HandleUseHouseObjectAsync` send behavior.
- Full house-object behavior, including occupant release on not-known, use/storage/postbox branch side effects, persistence, packet ordering, and deletion/expiration side effects, remains partial.
- Java runtime comparison, broader `PositionUtil` methods, non-player creature radius sourcing, threading, and live `DataManager` integration remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts; 1 handler-level regression test added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: full handler send behavior, full house-object side effects, Java runtime comparison, and production socket-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit strengthens production-target coverage for the house-object talk-range gate.

## Next Recommended Unit Of Work

Move to the trade-list runtime-readiness audit before production NPC dialog routing, or continue house-object parity with a full `HandleUseHouseObjectAsync` too-far send test if the socket harness can remain tightly scoped.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Trade-list runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Independent if it avoids house-object and dialog range files. |
| B | Full house-object too-far send harness audit | Existing GameServerConnection socket/test fixtures; read-only first | Medium/High | Only implement if setup stays tightly scoped and avoids broad connection refactors. |
| C | Non-player creature radius audit | Java pet/summon/static-object templates and C# models; read-only first | Medium | Needed for future non-player callers, not current player-to-NPC or player-to-house-object paths. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only trade-list runtime-readiness audit | Read-only | All writes |
| Agent B | Read-only full house-object send-harness feasibility audit | Read-only | All writes |

Keep implementation sequential if changing `GameServerConnection.cs`, shared test fixtures, or Phase 6 docs.

## Do Not Parallelize

- `GameServerConnection.cs`: broad production packet handler.
- Shared socket/test harness files unless one owner is assigned.
- `PositionUtilService.cs`: shared geometry utility.
- Phase 6 progress/handoff docs: orchestrator-owned.
