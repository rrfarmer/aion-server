# Phase 6APB Completion - Decompose Reward Ordering Audit

Date: 2026-05-27
Unit of Work: UOW-1582
Status: Complete after validation.

## Scope

Continue scheduled item-use ordering by source-reviewing Java normal decompose success-message/reward/final-animation order, documenting the ordering boundary, and closing one concrete delayed revalidation regression.

## Completed Work

- Performed Parallel Work Discovery across decompose ordering, hook readiness, and deferred extraction/AP extraction live-boundary design.
- Spawned one read-only Explorer sub-agent to verify Java/C# decompose ordering and coverage gaps.
- Added `DecomposeRewardOrderingPlanService` as a tested planning artifact with Java breadcrumbs.
- Added `DecomposeRewardOrderingPlanServiceTests`.
- Added `HandleUseItemAsync_DecomposeInventoryFullBeforeCompletionFailsWithoutMutation`.
- Confirmed Java behavior:
  - normal decompose broadcasts start animation and schedules a 3000ms delayed task;
  - delayed completion re-runs `canAct` and consumes the source item in `postValidate`;
  - source consume storage packets happen before success message;
  - success message is sent before reward add calls;
  - reward add packets happen before final animation;
  - if the cube becomes full before delayed completion, Java sends inventory-full and final end state `2` without source/reward mutation.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "DecomposeRewardOrderingPlanServiceTests|DecomposeServiceTests|HandleUseItemAsync_DecomposeInventoryFullBeforeCompletionFailsWithoutMutation"`.
- Result: passed 5 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- First result: failed 1 unrelated composition mixed-delete test (`ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes`) with expected remaining count `1`, actual `2`.
- Reran the isolated failing composition test.
- Result: passed 1 test.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 79 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj` with the default timeout.
- Result: command timeout before test result, no pass/fail evidence.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj` with a longer timeout.
- Result: passed 3354 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Decompose success-message-before-reward audit | `DecomposeAction`, `Storage`, `ItemPacketService`, `ItemService`, `SM_ITEM_USAGE_ANIMATION`, `SM_SYSTEM_MESSAGE` | decompose ordering plan service/test; one focused item-use regression | Regression / Planning | Yes with read-only Java check | Medium | Direct next handoff target; can improve evidence without runtime storage-boundary refactors. |
| B | Hook detail readiness surfacing | protection readiness/export artifacts | protection readiness/export files | Integration/Test | Yes separately | Medium | Independent from item-use files, but not selected because decompose had a concrete gap. |
| C | Runtime extraction live mutation design | `ExtractAction`, `EnchantService`, `Storage` | extraction service/connection/repository/test files | Design / Integration | No | High | Requires explicit live storage mutation design; not safe to mix. |
| D | Runtime AP extraction live mutation design | `ApExtractAction`, `Storage`, `AbyssPointsService` | AP extraction service/connection/repository/test files | Design / Integration | No | High | Requires explicit live storage/AP side-effect design; not safe to mix. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Verify Java/C# decompose ordering and current coverage gaps | Java/C# Analysis | read-only Java/C# source inspection | all writes, shared docs, C# edits | none | Confirm success message/reward/final animation order and identify concrete test gaps. |
| Orchestrator | Add decompose ordering artifact, delayed full-inventory regression, and docs | Planning/Test/Documentation | decompose ordering service/test, item-use test file, progress/handoff docs | Java source writes, extraction/AP extraction runtime files | Explorer/local source review | Tested ordering artifact, focused regression, conservative parity table. |

Parallelism was used for read-only Java/C# analysis only. No sub-agent wrote files.

## Migration Parity Table - UOW-1582

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `Aion.GameServer.Services.DecomposeService`; `Aion.GameServer.Services.DecomposeRewardOrderingPlanService`; `Aion.GameServer.Network.Aion.GameServerConnection.CompleteDecomposeUseItemAsync` | Item Action / Service / Planning Artifact | Partial | Unit Tested / Regression Tested | Partial Parity | Java normal delayed order is now documented and additionally regression-tested for delayed inventory-full revalidation. Existing C# success path already sends source consume, success message, reward packets, final success animation. Java reward-add failure after success message remains unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplySourceItemMutationAsync`; `DecomposeRewardOrderingPlanService` | Storage Dependency | Partial | Regression Tested | Needs Verification | Java source decrease/delete emits live storage packets before success message. C# sends equivalent source update/delete packets before success message in covered normal decompose tests, but persistence is transaction-oriented and not Java-live compared. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`; `SmDeleteItem`; `SmCubeUpdate`; `DecomposeRewardOrderingPlanService` | Packet Service Dependency | Partial | Regression Tested | Needs Verification | Decompose source and reward packet ordering/flags are covered by existing and new C# tests. No Java-generated packet golden artifact for the new delayed full-inventory branch. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.InventoryAddService`; `GameServerConnection.SendDecomposeRewardItemsAsync`; `DecomposeRewardOrderingPlanService` | Service Dependency | Partial | Regression Tested / Unit Tested planning metadata only | Needs Verification | Java calls `ItemService.addItem` after success message and ignores add return values. C# plans reward adds before persistence and sends reward packets after success message; invalid reward/static-data failure remains a known semantic risk. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation`; item-use regression tests | Packet | Partial | Regression Tested | Needs Verification | New regression confirms delayed inventory-full branch sends final end state `2`. Success branch final end state `1` is covered by existing tests. No Java runtime golden. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`; item-use regression tests | Packet | Partial | Regression Tested | Needs Verification | New regression confirms delayed inventory-full revalidation emits message id `1300447`. Success message before reward packets is documented and covered by existing packet-order assertions, though those tests do not use Java-generated bytes. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleSelectDecomposableAsync` | Packet Handler / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer confirmed selectable decompose follows separate immediate packet order and lacks normal canAct full-inventory guard. This unit did not change selectable behavior. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Added `CreatePlan_RecordsJavaSuccessMessageBeforeRewardPackets` | Unit / Planning Regression | Java source review plus Explorer confirmation of `DecomposeAction`, `Storage`, `ItemPacketService`, and `ItemService` ordering | The plan records Java start animation, delayed post-validate/source consume, success message before reward packets, reward packets before final animation, existing C# coverage, and remaining risks. | Source-review backed C# metadata regression; focused and full game-server tests passed after rerun. | Does not execute Java; Java runtime packet artifacts still blocked. |
| Added `HandleUseItemAsync_DecomposeInventoryFullBeforeCompletionFailsWithoutMutation` | Regression | Java `DecomposeAction.postValidate` re-runs `canAct`, sends inventory-full, and final animation end state `2` when inventory becomes full before delayed completion | Normal decompose scheduled use fails at delayed completion if the cube is filled after start animation, does not persist/mutate source or reward, and sends start animation, inventory-full message `1300447`, final failure animation. | Source-review backed C# regression; focused decompose, affected item-use, and full game-server tests passed after rerun. | No Java runtime packet golden. Does not cover Java invalid reward add after success message. |

## Remaining Risks

- Java reward add return values are ignored after success message; C# reward add planning can fail before source mutation and before success/final animation.
- C# persists the whole decompose mutation before sending packets, while Java storage and item-service packet side effects are live and inline.
- Random reward type coverage is source-reviewed but not exhaustive against Java runtime artifacts.
- First affected item-use suite run hit a composition mixed-delete failure that passed in isolation and passed on rerun; keep an eye on possible timing flakiness outside this UOW.
- Java 25 JDK/Maven blocker still prevents new generated Java runtime packet artifacts.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows in this unit
- Total artifacts ported: 1 planning service, 1 focused planning test, plus 1 item-use regression
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime decompose packet/golden generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, invalid reward add runtime comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a normal decompose persistence-failure regression, or switch to hook detail readiness surfacing if avoiding more item-use tests.
- Why: Explorer found selectable has a persistence-failure regression but normal decompose does not. This is a C# transaction-boundary safeguard rather than Java-live behavior, so document it as a C# persistence boundary difference rather than verified Java parity.
- Scope:
  - source-review current C# normal decompose persistence failure behavior;
  - add one focused regression if the behavior is not already covered;
  - update docs with conservative parity language;
  - avoid runtime extraction/AP extraction partial mutation changes.

## Suggested Acceptance Criteria

- One isolated branch is source-reviewed and either covered or explicitly documented.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Normal decompose persistence-failure regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` only, plus docs | Medium | Single-writer only; document C# transaction-boundary difference from Java. |
| B | Hook detail readiness surfacing | protection readiness/export files | Medium | Independent from item-use files. |
| C | Decompose invalid reward add boundary artifact | new decompose boundary artifact/test | Medium | Avoid runtime mutation unless Java static-data-invalid behavior is captured. |
| D | Runtime extraction/AP extraction live mutation design | extraction/AP extraction service/connection/repository/test files | High | Do only after explicit design selection. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Optional read-only check for normal decompose persistence failure or hook readiness | read-only Java/C# source inspection | all writes, shared docs, C# edits |
| Orchestrator | Implement or document one focused branch | selected service/test files plus progress/handoff docs | Java source writes, unrelated item-use paths |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple writers in progress/handoff docs.
- Runtime extraction/AP extraction refactors while another worker touches related tests.
- Java generator implementation without Java 25 JDK and Maven.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1582] Audit decompose reward ordering`.
- Files changed in UOW-1582:
  - `dotnetConversion/src/Aion.GameServer/Services/DecomposeRewardOrderingPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/DecomposeRewardOrderingPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APB-Completion.md`
- Latest prior commits:
  - `d620dced3 [Phase 6][UOW-1581] Add AP extraction mutation boundary plan`
  - `7a65330d0 [Phase 6][UOW-1580] Add extraction mutation boundary plan`
  - `a58009192 [Phase 6][UOW-1579] Reject AP extraction wing targets`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
