# Phase 6ASK Completion - Position Heading Helper

Date: 2026-05-28
Unit of Work: UOW-1669
Status: Complete after focused utility tests

## Scope

This unit added Java-derived heading helpers to the existing C# `PositionUtilService`.

The helper methods model Java `PositionUtil` angle normalization, angle-to-heading conversion, signed-byte heading-to-angle conversion, and coordinate heading calculation. This reduces the NPC target-change heading snapshot gap from UOW-1668, but does not yet wire the helper into live NPC controller behavior.

## Completed Work

- Added `PositionUtilService.CalculateAngleFrom`.
- Added `PositionUtilService.NormalizeAngle`.
- Added `PositionUtilService.ConvertHeadingToAngle`.
- Added `PositionUtilService.ConvertAngleToHeading`.
- Added `PositionUtilService.GetHeadingTowards`.
- Preserved Java signed-byte behavior for `convertHeadingToAngle(byte)`.
- Added focused utility tests for axis/diagonal headings, normalization, signed-byte heading conversion, and truncating angle conversion.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/utils/PositionUtil.java`
  - `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`
- Java `calculateAngleFrom` uses `Math.atan2`, converts to degrees, casts to float, and normalizes.
- Java `convertHeadingToAngle(byte)` multiplies signed Java byte values by `3f`, then normalizes.
- Java `convertAngleToHeading(float)` truncates `angle / 3` through a byte cast.
- Java `getHeadingTowards(float, float, float, float)` composes `calculateAngleFrom` and `convertAngleToHeading`.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PositionUtilServiceTests|FullyQualifiedName~NpcTargetChangePacketPlanServiceTests|FullyQualifiedName~SmLookAtObjectPacketTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 273 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| `PositionUtil` heading helper | position utility/tests | Low | Yes | Directly supports NPC target-change heading calculation without live NPC state mutation. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after heading helper is documented. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside heading utility scope. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Position heading utility, tests, docs, commit | `PositionUtilService.cs`, `PositionUtilServiceTests.cs`, progress/handoff docs | Java source writes, live NPC controller/broadcast integration, unrelated services/tests | Implemented and documented UOW-1669. |
| Sub-agents | None | None | All files | Not spawned because selected work touched one existing utility/test pair plus shared docs. |

No sub-agent was spawned for UOW-1669 because the selected utility change was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `GetHeadingTowards_UsesJavaAtan2AngleAndTruncatedHeading` | Added | Axis and diagonal coordinate cases produce Java-shaped angles/headings. | Java `calculateAngleFrom` and `getHeadingTowards`. |
| `NormalizeAngle_MatchesJavaModuloBranches` | Added | Positive, negative, and beyond-360 angles follow Java branch behavior. | Java `normalizeAngle`. |
| `ConvertHeadingToAngle_NormalizesJavaByteHeading` | Added | C# handles normal headings and Java signed-byte values above 127. | Java `convertHeadingToAngle(byte)`. |
| `ConvertAngleToHeading_TruncatesLikeJavaByteCast` | Added | Angle-to-heading conversion truncates fractional heading values. | Java `convertAngleToHeading(float)`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.utils.PositionUtil.calculateAngleFrom` | `Aion.GameServer.Services.PositionUtilService.CalculateAngleFrom` | Utility | Complete | Unit Tested | Partial Parity | C# uses `Math.Atan2`, degrees conversion, float cast, and Java-shaped normalization. Axis/diagonal cases are covered. No Java runtime vector/golden comparison was produced. |
| `com.aionemu.gameserver.utils.PositionUtil.normalizeAngle` | `Aion.GameServer.Services.PositionUtilService.NormalizeAngle` | Utility | Complete | Unit Tested | Partial Parity | C# mirrors Java modulo branches for positive and negative angles. Float modulo behavior is covered for representative values; NaN/infinity behavior is not tested. |
| `com.aionemu.gameserver.utils.PositionUtil.convertHeadingToAngle` | `Aion.GameServer.Services.PositionUtilService.ConvertHeadingToAngle` | Utility | Complete | Unit Tested | Partial Parity | C# preserves Java signed-byte multiplication by casting C# `byte` to `sbyte` before multiplying by `3f`. Values above 127 are covered. No runtime Java vector comparison. |
| `com.aionemu.gameserver.utils.PositionUtil.convertAngleToHeading` | `Aion.GameServer.Services.PositionUtilService.ConvertAngleToHeading` | Utility | Complete | Unit Tested | Partial Parity | C# truncates `angle / 3f` into a byte like Java's byte cast for normal heading range. Out-of-range cast wrapping beyond Java's normal normalized-angle caller path is not exhaustively tested. |
| `com.aionemu.gameserver.utils.PositionUtil.getHeadingTowards` | `Aion.GameServer.Services.PositionUtilService.GetHeadingTowards` | Utility | Complete | Unit Tested | Partial Parity | C# composes angle calculation and heading conversion for coordinates. It is not yet wired into `NpcTargetChangePacketPlanService` or live NPC heading mutation. |
| `com.aionemu.gameserver.controllers.NpcController.onTargetChanged` | `NpcTargetChangePacketPlanService`; `PositionUtilService.GetHeadingTowards` | Controller Boundary | Partial | Regression Tested | Partial Parity | The heading dependency now exists as a C# helper, but live NPC target-change integration still supplies snapshot headings and does not mutate heading or broadcast. |

## Remaining Risks

- The heading helper is not yet wired into `NpcTargetChangePacketPlanService`; current NPC target-change tests still pass precomputed heading snapshots.
- Live NPC heading mutation, `PositionUtil.getHeadingTowards(VisibleObject, VisibleObject)`, object reference access, and broadcast remain unported.
- Java float precision was source-derived but not runtime-compared with Java vector output.
- NaN/infinity and unusual out-of-range angle cast behavior are not covered.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 5 utility methods plus 4 focused utility regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented runtime/golden gaps.
- Total blocked artifacts: live NPC controller integration, live object-reference heading calculation, broadcast utility integration, Java runtime vector comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Wire heading helper into NPC plan | NPC target-change plan/tests | Add a coordinate-input overload so NPC target-change planning can calculate heading rather than accepting a precomputed snapshot. |
| Another isolated packet parity unit | packet class/tests | Continue packet-body parity while live dispatch remains incomplete. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise wire `PositionUtilService.GetHeadingTowards` into a non-live NPC target-change coordinate-input overload.
- Why: the Java heading helper now exists, but NPC target-change planning still receives a precomputed heading snapshot.
- Files: likely no file changes for DB execution; otherwise `NpcTargetChangePacketPlanService.cs`, its tests, and docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | NPC heading overload design | read-only NPC plan and position utility | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, heading overload, or next packet unit | exact selected files | Java writes, live NPC broadcast, unrelated shared files |
| Read-only Agent | Audit heading overload or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live NPC target broadcast, live scheduler mutation, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1669] Add position heading helper
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/PositionUtilService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PositionUtilServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASK-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
