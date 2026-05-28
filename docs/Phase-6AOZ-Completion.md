# Phase 6AOZ Completion - Extraction Live Mutation Boundary Plan

Date: 2026-05-27
Unit of Work: UOW-1580
Status: Complete after validation.

## Scope

Continue scheduled item-use ordering by source-reviewing Java extraction target-before-tool behavior through `ExtractAction` and `EnchantService.breakItem`, then documenting the live storage boundary C# needs before a safe runtime parity fix.

## Completed Work

- Performed Parallel Work Discovery across extraction, AP extraction, decompose, and hook readiness candidates.
- Spawned one read-only Explorer sub-agent to verify Java extraction mutation and packet ordering.
- Added `ItemExtractionLiveMutationBoundaryPlanService` as a tested planning artifact with Java breadcrumbs.
- Added `ItemExtractionLiveMutationBoundaryPlanServiceTests`.
- Confirmed Java behavior:
  - `ExtractAction` uses a delayed 5000ms completion and final animation from `breakItem`'s boolean result;
  - `EnchantService.breakItem` does initial live inventory guards before mutation;
  - target deletion is attempted before extraction-tool decrease;
  - reward add happens only when tool decrease succeeds;
  - after the delete branch Java returns `true`, so delete failure or tool-decrease failure can still produce final success animation.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter ItemExtractionLiveMutationBoundaryPlanServiceTests`.
- Result: passed 1 test.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 78 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj`.
- Result: passed 3351 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Extraction live mutation boundary | `ExtractAction`, `EnchantService`, `Storage`, `ItemService`, `SM_ITEM_USAGE_ANIMATION` | new extraction boundary plan service/test | Planning / Regression Test | Yes with read-only Java check | Medium | Documents the exact Java mutation/packet edge without forcing a risky runtime refactor. |
| B | Runtime extraction partial mutation implementation | same as A plus repository/connection paths | `EnchantService.cs`, `GameServerConnection.cs`, repository mutation paths, inventory tests | Integration Fix | No with A | High | Needs a live storage mutation result boundary and packet ordering decision. |
| C | AP extraction target-before-tool/AP partial failure | `ApExtractAction`, `AbyssPointsService` | AP extraction service/connection/tests | Integration Fix / Planning | Later | Medium | Similar no-delay path, but separate AP side effects and just-touched service area. |
| D | Decompose success-message-before-reward audit | `DecomposeAction`, item service dependencies | decompose tests/services | Regression / Audit | Later | Medium | Existing tests may already cover much of this; independent from extraction. |
| E | Hook detail readiness surfacing | protection readiness/export artifacts | protection readiness/export files | Integration/Test | Yes separately | Medium | Independent from item-use ordering. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Verify Java extraction live mutation and final animation ordering | Java/C# Analysis | read-only Java/C# source inspection | all writes, docs, C# edits | none | Concise confirmation of mutation order and C# snapshot limitation. |
| Orchestrator | Add extraction live mutation boundary planning artifact and docs | Planning/Test/Documentation | new service/test plus progress/handoff docs | Java source writes, runtime extraction refactor, AP extraction files | Explorer/local source review | A tested, handoff-friendly boundary artifact and conservative parity table. |

Parallelism was used for read-only Java/C# analysis only. No sub-agent wrote files.

## Migration Parity Table - UOW-1580

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `Aion.GameServer.Services.ItemExtractionLiveMutationBoundaryPlanService`; `Aion.GameServer.Network.Aion.GameServerConnection.CompleteExtractUseItemAsync` | Item Action / Planning Artifact | Partial | Unit Tested | Needs Verification | Planning artifact records Java scheduled start/final animation behavior and the `breakItem` result dependency. Runtime C# still uses snapshot planning and does not model delete-failed success animation or target-deleted/tool-decrease-failed success animation. |
| `com.aionemu.gameserver.services.EnchantService` | `Aion.GameServer.Services.EnchantService`; `Aion.GameServer.Services.ItemExtractionLiveMutationBoundaryPlanService` | Service / Planning Artifact | Partial | Unit Tested | Needs Verification | Java `breakItem` live order is documented: live guards, compatibility guard, reward selection, target delete, tool decrease, reward add if tool decrease succeeds, then `true` after delete branch. C# `CreateBreakItemPlan` computes one in-memory plan and cannot represent those partial live mutation outcomes yet. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Services.ItemExtractionLiveMutationBoundaryPlanService`; repository mutation paths remain future work | Storage Dependency | Not Started | Unit Tested planning metadata only | Needs Verification | Newly documented dependency: `Storage.delete` and `Storage.decreaseByObjectId` return values are behaviorally visible in Java. C# persistence is currently transaction-oriented and snapshot planned, so live partial storage results are not exposed. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.InventoryAddService`; `Aion.GameServer.Services.ItemExtractionLiveMutationBoundaryPlanService` | Service Dependency | Partial | Unit Tested planning metadata only | Needs Verification | Java reward add only happens after tool decrease succeeds. Existing C# happy-path reward planning exists, but no runtime comparison covers the partial failure path where no reward is added and final success animation is still sent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation`; `ItemExtractionLiveMutationBoundaryPlanService` | Packet / Planning Artifact | Partial | Unit Tested planning metadata only | Needs Verification | Final animation result depends on Java `breakItem` boolean. C# sends failure on planner failure and success on completed plan; it does not yet model Java's delete-failed success-animation branch. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleUseItemAsync` | Packet Handler / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Extraction scheduling enters through item-use handling. This unit did not change handler behavior; it records the runtime boundary needed before a safe parity fix. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Added `CreatePlan_RecordsJavaTargetDeleteBeforeToolDecreaseBoundary` | Unit / Planning Regression | Java source review plus Explorer confirmation of `ExtractAction.act` and `EnchantService.breakItem` | The plan records Java breadcrumbs, exact mutation order, missing live `Storage.delete`/`Storage.decreaseByObjectId` boundaries, delete-failure success-animation risk, C# snapshot limitation, and missing Java golden trace. | Source-review backed C# metadata regression; focused, affected item-use, and full game-server tests passed. | No runtime behavior changed; no Java runtime packet trace; no C# live partial mutation implementation. |

## Remaining Risks

- This unit documents a blocker and adds a tested planning artifact; it does not implement runtime partial mutation parity.
- Java may send final success animation even when `Storage.delete(targetItem)` returns null after initial guards; C# currently cannot represent that path.
- Java may delete the target, fail to decrease the extraction tool, add no reward, and still send final success animation; C# currently cannot represent that path.
- C# extraction persistence is transaction-oriented and planner-based, so it intentionally avoids Java's live partial mutation behavior for now.
- No Java runtime packet capture or generated golden artifact was produced because Java 25 JDK/Maven tooling remains blocked.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows in this unit
- Total artifacts ported: 1 planning service plus 1 focused regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: runtime extraction live storage mutation boundary, Java runtime packet capture/golden generation, Java 25 JDK, Java compiler, Maven, Maven wrapper
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue scheduled item-use ordering without touching runtime extraction partial mutation yet.
- Preferred targets:
  - AP extraction live mutation boundary artifact for target delete/tool decrease/AP add ordering; or
  - decompose success-message-before-reward audit if existing runtime surfaces can be covered cleanly.
- Why: extraction now has a documented blocker; another narrow source-reviewed branch can advance parity while avoiding risky runtime storage changes.
- Scope:
  - choose exactly one branch;
  - source-review Java mutation and packet order;
  - add a focused regression or planning artifact;
  - keep live storage/persistence differences explicit;
  - keep Java runtime parity claims conservative until generated artifacts exist.

## Suggested Acceptance Criteria

- One isolated branch is source-reviewed and either covered or explicitly documented as needing a new mutation boundary.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AP extraction live mutation boundary | AP extraction service/connection/tests or new boundary artifact/test | Medium | Avoid editing `ApExtractService.cs` unless implementing a concrete non-overfit test. |
| B | Decompose success-message-before-reward audit | decompose tests/services or new audit artifact/test | Medium | Existing tests may already cover core ordering. |
| C | Hook detail readiness surfacing | protection readiness/export files | Medium | Independent from item-use files. |
| D | Runtime extraction live mutation design | extraction service/connection/repository/test files | High | Do only after an explicit live storage boundary design is selected. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Optional read-only deep dive for selected AP extraction or decompose branch | read-only Java/C# source inspection | all writes, shared docs, C# edits |
| Orchestrator | Implement or document one focused parity branch | selected service/test files plus progress/handoff docs | Java source writes, unrelated item-use paths |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple writers in `ApExtractService.cs`.
- Runtime extraction refactors while another worker touches extraction tests.
- Java generator implementation without Java 25 JDK and Maven.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1580] Add extraction mutation boundary plan`.
- Files changed in UOW-1580:
  - `dotnetConversion/src/Aion.GameServer/Services/ItemExtractionLiveMutationBoundaryPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ItemExtractionLiveMutationBoundaryPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AOZ-Completion.md`
- Latest prior commits:
  - `a58009192 [Phase 6][UOW-1579] Reject AP extraction wing targets`
  - `2158444f7 [Phase 6][UOW-1578] Preserve assembly partial consumes`
  - `91c11d286 [Phase 6][UOW-1577] Validate full dotnet solution`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
