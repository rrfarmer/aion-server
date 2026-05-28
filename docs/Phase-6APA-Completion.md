# Phase 6APA Completion - AP Extraction Live Mutation Boundary Plan

Date: 2026-05-27
Unit of Work: UOW-1581
Status: Complete after validation.

## Scope

Continue scheduled item-use ordering by source-reviewing Java AP extraction target-delete/tool-decrease/AP-add behavior through `ApExtractAction`, `Storage`, and `AbyssPointsService.addAp`, then documenting the live storage/AP side-effect boundary C# needs before a safe runtime parity fix.

## Completed Work

- Performed Parallel Work Discovery across AP extraction, decompose, and hook readiness candidates.
- Spawned one read-only Explorer sub-agent to verify Java AP extraction mutation and packet ordering.
- Added `ApExtractionLiveMutationBoundaryPlanService` as a tested planning artifact with Java breadcrumbs.
- Added `ApExtractionLiveMutationBoundaryPlanServiceTests`.
- Confirmed Java behavior:
  - `ApExtractAction.canAct` performs target, AP eligibility, level, quality, and target-type guards;
  - `act` silently returns when acquisition metadata is missing or required AP is zero;
  - AP is calculated before mutation;
  - target delete happens before extraction-tool decrease;
  - AP add happens only after tool decrease succeeds;
  - no final animation packet exists for AP extraction; Java side effects are inline storage and AP/rank packets.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "ApExtractionLiveMutationBoundaryPlanServiceTests|ApExtractServiceTests"`.
- Result: passed 3 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 78 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj`.
- Result: passed 3352 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | AP extraction live mutation/AP side-effect boundary | `ApExtractAction`, `Storage`, `AbyssPointsService` | new AP extraction boundary plan service/test | Planning / Regression Test | Yes with read-only Java check | Medium | Documents Java target delete, tool decrease, AP add, and inline packet ordering without risky runtime mutation changes. |
| B | Runtime AP extraction partial mutation implementation | same as A plus connection/repository paths | `ApExtractService.cs`, `GameServerConnection.cs`, repository mutation paths, inventory tests | Integration Fix | No with A | High | Requires non-atomic live storage and AP side-effect design. |
| C | Decompose success-message-before-reward audit | `DecomposeAction`, item service dependencies | decompose tests/services | Regression / Audit | Later | Medium | Independent item-use branch and likely next safest ordering audit. |
| D | Hook detail readiness surfacing | protection readiness/export artifacts | protection readiness/export files | Integration/Test | Yes separately | Medium | Independent from item-use ordering. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Verify Java AP extraction live storage/AP packet ordering | Java/C# Analysis | read-only Java/C# source inspection | all writes, docs, C# edits | none | Concise confirmation of mutation order, packet side effects, and C# snapshot limitation. |
| Orchestrator | Add AP extraction live mutation boundary planning artifact and docs | Planning/Test/Documentation | new service/test plus progress/handoff docs | Java source writes, runtime AP extraction refactor, extraction files | Explorer/local source review | A tested, handoff-friendly AP boundary artifact and conservative parity table. |

Parallelism was used for read-only Java/C# analysis only. No sub-agent wrote files.

## Migration Parity Table - UOW-1581

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService`; `Aion.GameServer.Services.ApExtractionLiveMutationBoundaryPlanService` | Item Action / Planning Artifact | Partial | Unit Tested / Regression Tested | Needs Verification | Boundary artifact records Java guard order, silent acquisition return, target delete before tool decrease, AP add after tool decrease, and no final animation packet. Runtime C# still uses atomic/snapshot planning. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Services.ApExtractionLiveMutationBoundaryPlanService`; repository mutation paths remain future work | Storage Dependency | Not Started | Unit Tested planning metadata only | Needs Verification | Java `Storage.delete` and `Storage.decreaseByObjectId` are live packeted boundaries. C# currently plans and persists AP extraction as one complete operation and cannot expose target-deleted/tool-decrease-failed state. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService`; `Aion.GameServer.Services.ApExtractionLiveMutationBoundaryPlanService` | Service Dependency | Partial | Unit Tested planning metadata only | Needs Verification | Java AP/rank mutation and packets occur only after target delete and tool decrease. C# creates the AP plan before item mutation planning and sends AP packets after persistence succeeds. No Java runtime packet golden. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleApExtractUseItemAsync` | Packet Handler / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | AP extraction enters through item-use handling. This unit did not change handler behavior; it records the live boundary needed before a safe runtime parity fix. |
| `com.aionemu.gameserver.model.templates.item.actions.UseTarget` | `Aion.GameServer.Dataholders.ItemApExtractActionInfo.Target` | Enum-like Action Metadata | Partial | Existing Unit Tests | Needs Verification | Existing AP extraction tests cover target type rejection, including prior WING fix. This unit did not broaden `OTHER`/`ALL` semantics. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Added `CreatePlan_RecordsJavaTargetDeleteBeforeToolDecreaseAndApAddBoundary` | Unit / Planning Regression | Java source review plus Explorer confirmation of `ApExtractAction.act`, `Storage`, and `AbyssPointsService.addAp` ordering | The plan records Java breadcrumbs, guard/acquisition/AP/mutation order, missing `Storage.delete`/`Storage.decreaseByObjectId` live boundaries, AP side-effect ordering, inline packet risk, and C# atomic planning limitations. | Source-review backed C# metadata regression; focused AP extraction, affected item-use, and full game-server tests passed. | No runtime behavior changed; no Java runtime packet trace; no C# live partial mutation/AP side-effect implementation. |

## Remaining Risks

- This unit documents a blocker and adds a tested planning artifact; it does not implement runtime AP extraction partial mutation parity.
- Java may delete the target, fail to decrease the extraction tool, add no AP, and still have already sent target deletion side effects; C# currently cannot represent that path.
- C# creates AP plans before inventory mutation planning, while Java calls `AbyssPointsService.addAp` only after storage mutations succeed.
- C# repository persistence is transaction-oriented, while Java storage and AP packet side effects are live and inline.
- No Java runtime packet capture or generated golden artifact was produced because Java 25 JDK/Maven tooling remains blocked.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit
- Total artifacts ported: 1 planning service plus 1 focused regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: runtime AP extraction live storage/AP side-effect boundary, Java runtime packet capture/golden generation, Java 25 JDK, Java compiler, Maven, Maven wrapper
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue scheduled item-use ordering without touching runtime extraction/AP extraction partial mutation yet.
- Preferred target: decompose success-message-before-reward audit.
- Alternative target: hook detail readiness surfacing if avoiding item-use files.
- Why: extraction and AP extraction now both have explicit live-boundary blockers; decompose is the next narrow scheduled item-use branch that may be auditable without introducing storage-boundary refactors.
- Scope:
  - source-review Java decompose mutation and packet order;
  - compare current C# decompose tests/services;
  - add a focused regression or planning artifact only if a concrete gap is found;
  - keep Java runtime parity claims conservative until generated artifacts exist.

## Suggested Acceptance Criteria

- One isolated branch is source-reviewed and either covered or explicitly documented as needing a new boundary.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Decompose success-message-before-reward audit | decompose tests/services or new audit artifact/test | Medium | Avoid broad `GameServerConnectionInventoryExpansionUseItemTests.cs` changes if a metadata artifact is enough. |
| B | Hook detail readiness surfacing | protection readiness/export files | Medium | Independent from item-use files. |
| C | Runtime extraction live mutation design | extraction service/connection/repository/test files | High | Do only after explicit design selection. |
| D | Runtime AP extraction live mutation design | AP extraction service/connection/repository/test files | High | Do only after explicit design selection. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Optional read-only deep dive for selected decompose or hook branch | read-only Java/C# source inspection | all writes, shared docs, C# edits |
| Orchestrator | Implement or document one focused parity branch | selected service/test files plus progress/handoff docs | Java source writes, unrelated item-use paths |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple writers in `ApExtractService.cs`.
- Runtime extraction/AP extraction refactors while another worker touches related tests.
- Java generator implementation without Java 25 JDK and Maven.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1581] Add AP extraction mutation boundary plan`.
- Files changed in UOW-1581:
  - `dotnetConversion/src/Aion.GameServer/Services/ApExtractionLiveMutationBoundaryPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ApExtractionLiveMutationBoundaryPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APA-Completion.md`
- Latest prior commits:
  - `7a65330d0 [Phase 6][UOW-1580] Add extraction mutation boundary plan`
  - `a58009192 [Phase 6][UOW-1579] Reject AP extraction wing targets`
  - `2158444f7 [Phase 6][UOW-1578] Preserve assembly partial consumes`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
