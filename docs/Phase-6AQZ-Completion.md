# Phase 6AQZ Completion - Region Id Utility

Date: 2026-05-28
Unit of Work: UOW-1632
Status: Complete after focused unit tests

## Scope

This unit returned to nearby-region prerequisites by porting Java `RegionUtil` region-id math into a non-live C# helper. It also ran a read-only sidecar audit for charge-all rollback coverage; no sidecar files were changed.

Live map-region storage, neighbour arrays, zone filtering, and nearby dispatch remain disabled.

## Completed Work

- Added `WorldRegionIdService`.
- Added `WorldRegionIdServiceTests`.
- Ported Java 2D and 3D region-id formulas.
- Ported reverse region-id component extraction.
- Covered Java default region size `128`.
- Covered Java overload behavior for custom region sizes.
- Integrated sidecar charge-all rollback audit findings into progress and next-work notes.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Region Findings

- `RegionUtil.get2DRegionId(regionSize, x, y)` computes `(int)x / regionSize * 1000 + (int)y / regionSize`.
- `RegionUtil.get3DRegionId(regionSize, x, y, z)` computes `(int)x / regionSize * 1000000 + (int)y / regionSize * 1000 + (int)z / regionSize`.
- `WorldConfig.WORLD_REGION_SIZE` defaults to `128`.
- `WorldMap2DInstance.getRegion` ignores Z and uses 2D ids.
- `WorldMap3DInstance.getRegion` includes Z and uses 3D ids.
- 2D init creates regions for `x <= worldSize` and `y <= worldSize`.
- 3D init creates regions for `x <= worldSize`, `y <= worldSize`, and `z < maxZ`, where `maxZ = round(worldSize / regionSize) * regionSize`.

## Sidecar Audit

Explorer `019e6d39-1876-7931-8c03-6b294d4f4608` audited charge-all rollback coverage read-only and was closed.

Findings:

- Existing fake-repository tests cover no runtime mutation and no packets when AP or Kinah charge-all save fails.
- Existing tests do not prove actual MySQL rollback if `SaveItemChargeAllMutationAsync` fails after one or more item charge updates.
- A gated DB integration rollback regression remains worthwhile as a future ItemCharge unit.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldRegionIdServiceTests|FullyQualifiedName~NearbyQuestRegionSnapshotServiceTests|FullyQualifiedName~PlayerKnownListRegionSnapshotServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests"
```

Result: passed 40 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Nearby region-id calculation | `WorldRegionIdService.cs`, `WorldRegionIdServiceTests.cs` | Low | Yes | Ports Java `RegionUtil` math before live region storage. |
| Charge-all rollback coverage audit | read-only charge-all tests/repository files | Low | Yes, sidecar | Non-overlapping read-only audit. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Live nearby refresh dispatch | world/connection services | High | No | Still blocked by live region storage. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Region-id utility, tests, docs | `dotnetConversion/src/Aion.GameServer/World/WorldRegionIdService.cs`, `dotnetConversion/tests/Aion.GameServer.Tests/WorldRegionIdServiceTests.cs`, progress/handoff docs | Java source writes, live nearby dispatch, repository rewrites | Tested utility and docs. |
| Explorer | Charge-all rollback audit | read-only inspection only | all writes, docs, production/test edits | Coverage/gap report. |

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `Get2DRegionId_UsesJavaIntegerCastAndRegionOffsets` | Added | 2D region ids use Java integer casts, default region size, and X offset 1000. | Static source review of Java `RegionUtil.get2DRegionId`; no Java runtime comparison. |
| `Get3DRegionId_UsesJavaIntegerCastAndRegionOffsets` | Added | 3D region ids use Java integer casts, default region size, X offset 1000000, and Y offset 1000. | Static source review of Java `RegionUtil.get3DRegionId`; no Java runtime comparison. |
| `GetRegionStartCoordinates_ReversesJavaRegionIdComponents` | Added | Reverse extraction returns 2D/3D region start coordinates. | Static source review of Java reverse helpers. |
| `GetRegionIds_SupportCustomRegionSizeLikeJavaOverloads` | Added | Custom region sizes affect forward and reverse formulas. | Static source review of Java overloads accepting `regionSize`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.RegionUtil` | `Aion.GameServer.World.WorldRegionIdService` | Utility | Complete | Unit Tested | Partial Parity | Java 2D/3D formulas, offsets, default region size, and reverse component extraction are ported and tested. Negative coordinate behavior is not separately tested; no Java runtime comparison was performed. |
| `com.aionemu.gameserver.configs.main.WorldConfig` | `WorldRegionIdService.DefaultRegionSize` | Config Dependency | Partial | Unit Tested | Needs Verification | C# helper uses Java default `128`, but it is not yet wired to live `.properties` config. Runtime config override behavior remains unported for this helper. |
| `com.aionemu.gameserver.world.WorldMap2DInstance` | `WorldRegionIdService.Get2DRegionId`; future nearby region-key planning | World Region Instance | Partial | Unit Tested Utility | Needs Verification | 2D region-id derivation is covered, including ignoring Z. Region precreation loops, neighbour linking, zone filtering, owner/personal instance behavior, and live `getRegion` lookup are not ported. |
| `com.aionemu.gameserver.world.WorldMap3DInstance` | `WorldRegionIdService.Get3DRegionId`; future nearby region-key planning | World Region Instance | Partial | Unit Tested Utility | Needs Verification | 3D region-id derivation and reverse starts are covered. Region precreation loops, `parallelStream` creation, 3D neighbour linking, zone filtering, and live lookup remain unported. |
| `com.aionemu.gameserver.world.MapRegion` | `NearbyQuestRegionKey`; `PlayerKnownListRegionKey`; future C# live region storage | Region Storage / Boundary | Partial | Existing Unit Tested + Manual Analysis | Needs Verification | Region ids can now be derived by Java-equivalent helper, but live object maps, neighbour arrays, activation/deactivation, synchronized player counts, zone revalidation, and parent instance storage remain snapshot-only or unported. |

## Remaining Risks

- `WorldRegionIdService` is non-live and not yet connected to nearby or known-list snapshot planners.
- Java region precreation boundaries, neighbour linking, zone filtering, and live `regions` map behavior remain unported.
- Negative-coordinate behavior is likely equivalent because both Java and C# casts/integer division truncate toward zero, but it was not separately tested.
- Region size config override behavior is not wired into the helper.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 region-id utility plus 4 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 4 grouped rows explicitly marked Needs Verification or Partial Parity with known gaps.
- Total blocked artifacts: live C# MapRegion storage, region precreation/neighbour model, zone filtering/revalidation, config-bound region size, Java runtime comparison, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Region-key derivation adapter coverage | nearby/known-list region snapshot tests or a small helper | Derive `NearbyQuestRegionKey` or `PlayerKnownListRegionKey` from `WorldPosition` using `WorldRegionIdService`. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add non-live adapter coverage that derives region keys from `WorldPosition` through `WorldRegionIdService`.
- Why: region ids are now ported, but existing nearby/known-list planners still use manually supplied region ids.
- Files: likely nearby or known-list region snapshot tests, possibly a small helper.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback research/test | DB integration tests/repository fixtures | Medium | Independent from nearby files; gated integration likely needed. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Region precreation analysis | read-only Java `WorldMap2DInstance`/`WorldMap3DInstance` and C# world files | Low | Useful before live storage. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Region-key derivation adapter coverage | nearby/known-list region snapshot tests or helper, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.
- Shared region abstraction changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1632] Add Java region-id helper
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/World/WorldRegionIdService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldRegionIdServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQZ-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
