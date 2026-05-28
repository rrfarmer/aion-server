# Phase 6APC Completion - Normal Decompose Persistence Failure Regression

Date: 2026-05-27
Unit of Work: UOW-1583
Status: Complete after validation.

## Scope

Close the normal decompose persistence-failure test gap identified by UOW-1582, while documenting that this is a C# persistence-boundary safeguard rather than Java-live verified parity.

## Completed Work

- Performed Parallel Work Discovery for normal decompose persistence failure, hook readiness, decompose invalid reward boundary, and deferred runtime extraction/AP extraction design.
- Kept the unit single-writer because the implementation target was the shared `GameServerConnectionInventoryExpansionUseItemTests.cs` file.
- Reviewed existing selectable decompose persistence-failure regression.
- Added `HandleUseItemAsync_DecomposePersistenceFailureDoesNotMutateRuntimeInventory`.
- Confirmed the C# persistence-boundary behavior:
  - normal decompose start animation is sent before delayed completion;
  - delayed completion reaches persistence;
  - if `SaveDecomposeActionMutationAsync` returns false, source and reward runtime inventory are not mutated;
  - no source consume, success message, reward packet, cube update, or final animation is sent after persistence failure.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "HandleUseItemAsync_DecomposePersistenceFailureDoesNotMutateRuntimeInventory|HandleSelectDecomposableAsync_PersistenceFailureDoesNotMutateRuntimeInventory"`.
- Result: passed 2 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- First result: failed 1 unrelated composition cleanup-seal test (`ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs`) with a remaining-count mismatch.
- Reran the isolated failing composition test.
- Result: passed 1 test.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 80 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj` with a longer timeout.
- Result: passed 3355 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Normal decompose persistence-failure regression | `DecomposeAction` as behavior context; C# persistence boundary has no direct Java transaction equivalent | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Regression Test | No writer parallelism | Medium | Single focused test in a shared item-use test file; multiple writers would be unsafe. |
| B | Hook detail readiness surfacing | protection readiness/export artifacts | protection readiness/export files | Integration/Test | Yes later | Medium | Independent from item-use files, but not selected because UOW-1582 identified a concrete test gap. |
| C | Decompose invalid reward add boundary | `DecomposeAction`, `ItemService.addItem` | possible new boundary artifact/test | Planning / Regression | Later | Medium | Needs static-data-invalid semantics; avoid mixing with persistence regression. |
| D | Runtime extraction/AP extraction live mutation design | extraction/AP extraction live storage artifacts | extraction/AP extraction service/connection/repository/test files | Design / Integration | No | High | Requires explicit live storage design; not safe to mix. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Add normal decompose persistence-failure regression and docs | Regression/Documentation | `GameServerConnectionInventoryExpansionUseItemTests.cs`, progress/handoff docs | Java source writes, extraction/AP extraction runtime files, concurrent item-use test writers | UOW-1582 decompose ordering audit | Focused regression and conservative persistence-boundary documentation. |

No sub-agent was spawned for UOW-1583. The selected implementation touched a shared item-use test file and the prior UOW already supplied the needed read-only Java/C# decompose analysis.

## Migration Parity Table - UOW-1583

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteDecomposeUseItemAsync` | Item Action / Handler | Partial | Regression Tested | Partial Parity | Normal decompose delayed completion remains covered for success, full-inventory failure, and now C# persistence failure. The new test is not Java-live parity because Java does not use the same transaction boundary; it protects C# from mutating runtime state after failed persistence. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplySourceItemMutationAsync`; `PlayerEnterWorldService.SaveDecomposeActionMutationAsync` | Storage / Persistence Dependency | Partial | Regression Tested | Needs Verification | Java storage mutation is live and packeted. C# persists first, then sends source/reward packets. The new regression confirms failed C# persistence leaves source and reward runtime state unchanged and sends no completion packets beyond the start animation. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.InventoryAddService`; decompose reward persistence path | Service Dependency | Partial | Existing Regression Tests | Needs Verification | Reward add behavior itself was not changed. The new regression ensures planned rewards are not applied when C# persistence fails. Java reward add failure semantics remain unverified. |
| N/A - C# persistence boundary for decompose action mutation | `Aion.GameServer.Data.PlayerEnterWorldRepository.SaveDecomposeActionMutationAsync`; `Aion.GameServer.Services.PlayerEnterWorldService.SaveDecomposeActionMutationAsync` | Repository / Persistence Boundary | Refactored | Regression Tested | Intentional Difference | C# uses an all-or-nothing persistence gate before runtime inventory packet/mutation application. This differs from Java live `Storage`/`ItemService` side effects and is documented as a C# persistence safeguard, not Java verified parity. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Added `HandleUseItemAsync_DecomposePersistenceFailureDoesNotMutateRuntimeInventory` | Regression | C# persistence boundary review, with Java `DecomposeAction` live-side-effect behavior documented from UOW-1582 | If normal decompose planning reaches persistence and `SaveDecomposeActionMutationAsync` returns false, C# leaves source count unchanged, adds no reward, and sends only the already-started usage animation. | C# transaction-boundary regression; focused, affected item-use, and full game-server tests passed after rerun. | Not Java runtime parity; Java does not share the same repository transaction boundary. |

## Remaining Risks

- Java live storage/item-service side effects differ from C# transaction-first persistence for decompose.
- Java reward add return values after success message remain unpinned by runtime artifacts.
- Two affected item-use suite runs across UOW-1582/UOW-1583 hit unrelated composition tests that passed in isolation and on rerun; possible timing/shared-state flakiness should be watched.
- Java 25 JDK/Maven blocker still prevents generated Java decompose packet/golden artifacts.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit
- Total artifacts ported: 1 focused regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped Java/C# behavior rows plus 1 intentional C# persistence-boundary difference
- Total blocked artifacts: Java runtime decompose packet/golden generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, Java live storage/repository transaction comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: move away from the shared item-use test file unless a new concrete gap appears.
- Preferred target: hook detail readiness surfacing.
- Alternative target: read-only composition flake audit if the intermittent composition tests recur.
- Why: the last several units touched or audited scheduled item-use paths; extraction/AP extraction/decompose have documented live-boundary risks. Hook readiness is independent and safer for the next parallelizable slice.
- Scope:
  - inspect latest protection/hook readiness docs/artifacts;
  - choose one isolated readiness/export gap;
  - avoid item-use runtime refactors unless fixing a concrete failure;
  - keep parity status conservative.

## Suggested Acceptance Criteria

- One isolated branch is source-reviewed and either covered or explicitly documented.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Hook detail readiness surfacing | protection readiness/export files | Medium | Independent from item-use files and likely safest next implementation target. |
| B | Composition flake audit | read-only item-use/composition tests and service inspection | Low-Medium | Use only if composition intermittent failures recur; avoid edits unless root cause is concrete. |
| C | Decompose invalid reward add boundary artifact | new decompose boundary artifact/test | Medium | Avoid shared item-use test edits unless Java invalid static-data behavior is pinned. |
| D | Runtime extraction/AP extraction live mutation design | extraction/AP extraction service/connection/repository/test files | High | Do only after explicit design selection. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Read-only hook readiness or composition flake analysis | read-only Java/C# source inspection | all writes, shared docs, C# edits |
| Orchestrator | Implement/document one focused branch | selected service/test files plus progress/handoff docs | Java source writes, unrelated item-use paths |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple writers in progress/handoff docs.
- Runtime extraction/AP extraction refactors while another worker touches related tests.
- Java generator implementation without Java 25 JDK and Maven.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1583] Cover decompose persistence failure`.
- Files changed in UOW-1583:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APC-Completion.md`
- Latest prior commits:
  - `f7ef38c54 [Phase 6][UOW-1582] Audit decompose reward ordering`
  - `d620dced3 [Phase 6][UOW-1581] Add AP extraction mutation boundary plan`
  - `7a65330d0 [Phase 6][UOW-1580] Add extraction mutation boundary plan`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
