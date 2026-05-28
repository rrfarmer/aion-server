# Phase 6AOX Completion - Assembly Partial-Consumption Parity

Date: 2026-05-27
Unit of Work: UOW-1578
Status: Complete after validation.

## Scope

Continue the scheduled item-use ordering slice by fixing the Java assembly partial-consumption behavior when a later assembly part disappears before delayed completion.

## Completed Work

- Spawned one read-only Explorer sub-agent for Java scheduled item-use ordering review.
- Closed the Explorer after completion; it made no file changes.
- Reviewed Java scheduled item-use behavior for decompose, assembly, XP extraction, composition, extraction, and AP extraction.
- Implemented assembly partial-consumption parity:
  - Java validates all assembly parts before scheduling;
  - delayed completion decreases parts sequentially by item id;
  - if a later part disappears, earlier part decreases are not rolled back;
  - Java returns silently without failure animation, success animation, success system message, or reward add.
- Updated C# assembly completion to persist, packet, and apply already-consumed part mutations before returning silently on partial failure.
- Added a focused regression test for the later-part-disappears branch.
- Added a test-helper save counter to `EmptyPlayerEnterWorldRepository`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "Assembly"`.
- Result: passed 43 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 78 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj`.
- Result: passed 3350 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Read-only scheduled item-use ordering review | `CM_USE_ITEM`, `DecomposeAction`, `AssemblyItemAction`, `ExpExtractAction`, `CM_COMPOSITE_STONES`, `CompositionAction`, `ExtractAction`, `ApExtractAction` | none/read-only | Java Analysis | Yes | Low | Safe Explorer sidecar to map multiple scheduled item-use paths without writes. |
| B | Assembly partial-consumption parity | `AssemblyItemAction` and dependencies | assembly service/connection/tests | Integration Fix / Regression Test | No with other assembly writer | Medium | Smallest concrete parity bug from Explorer findings. |
| C | Extraction target-before-tool non-atomic parity | `ExtractAction`, `EnchantService` | extraction service/connection/tests | Integration Fix / Regression Test | Later | Medium | Similar race behavior but separate mutation path. |
| D | AP extraction target-before-tool non-atomic parity | `ApExtractAction`, `AbyssPointsService` | AP extraction service/tests | Integration Fix / Regression Test | Later | Medium | No-delay path; separate persistence and AP side effects. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Read-only scheduled item-use ordering survey | Java Analysis | read-only Java source under `game-server/src` | all writes, docs, C# edits | none | Concise ordering and non-atomic behavior report. |
| Orchestrator | Implement assembly partial-consumption parity fix and docs | Integration/Test/Documentation | assembly service/connection/repository test helper, inventory item-use tests, progress/handoff docs | Java source writes, unrelated item-use paths, solution-wide refactors | Explorer report and local code review | Focused regression and game-server tests pass. |

Parallelism was used for read-only Java analysis only. No sub-agent wrote files.

## Migration Parity Table - UOW-1578

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleUseItemAsync` | Packet Handler | Partial | Regression Tested | Needs Verification | Explorer reviewed common item-use ordering. This unit changed only the delayed assembly completion branch. No Java runtime comparison was executed. |
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteAssemblyUseItemAsync`; `Aion.GameServer.Services.AssemblyItemService` | Item Action / Service | Partial | Regression Tested | Partial Parity | C# now preserves Java's non-atomic delayed completion when a later part disappears. Source-reviewed only; no Java runtime golden. |
| `com.aionemu.gameserver.model.templates.item.AssemblyItem` | `Aion.GameServer.Dataholders.AssemblyItemSummary` | DTO / Static Data | Partial | Regression Tested | Partial Parity | Used as the assembly part/reward descriptor. No schema change; behavior depends on ordered parts processing. |
| `com.aionemu.gameserver.dataholders.AssemblyItemsData` | `Aion.GameServer.Dataholders.AssemblyItemTable` | Data Holder | Partial | Regression Tested | Partial Parity | Completion path now fetches the descriptor directly instead of re-running full part validation. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Services.AssemblyItemService` | Storage / Mutation Dependency | Partial | Regression Tested | Needs Verification | Java `decreaseByItemId` mutates and packets parts sequentially. C# covers the selected partial branch only. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.InventoryAddService`; assembly reward packet path in `GameServerConnection` | Service Dependency | Partial | Regression Tested | Needs Verification | Reward add still occurs only after all part decreases succeed. Missing later part prevents reward add. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | decompose services/tests | Item Action / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer documented delayed and selectable decompose order. No C# decompose code changed. |
| `com.aionemu.gameserver.model.templates.item.actions.ExpExtractAction` | exp-extract services/tests | Item Action / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer documented EXP update ordering. No C# exp-extract code changed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | composition packet handler/tests | Packet Handler / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer confirmed composition bypasses `CM_USE_ITEM`. No C# composition code changed. |
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | composition packet handler/tests | Item Action / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer documented non-atomic decreases and unconditional success animation. No C# composition code changed. |
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | extraction services/tests | Item Action / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer documented target-before-tool behavior through `EnchantService.breakItem`. No C# extraction code changed. |
| `com.aionemu.gameserver.services.EnchantService` | extraction services/tests | Service / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Discovered as extraction's target-delete/tool-decrease/reward source. |
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService`; `GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer documented no-delay AP extraction and target-before-tool/AP order. No C# AP extraction code changed. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Discovered AP mutation/packet order dependency. |
| `com.aionemu.gameserver.controllers.observer.ItemUseObserver` | pending item-use cancellation model in `GameServerConnection` | Observer / Discovered Dependency | Partial | Existing Regression Tests | Needs Verification | Explorer noted broad observer abort surface. No cancellation code changed. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Added `HandleUseItemAsync_AssemblyKeepsEarlierPartConsumeWhenLaterPartDisappearsBeforeCompletion` | Regression | Java source review of `AssemblyItemAction.act` delayed loop and `Storage.decreaseByItemId` behavior | Later missing part preserves earlier consumed part mutation and packets, then silently stops without reward or finish animation. | Source-review backed C# regression; `Assembly` slice, inventory item-use class, and full game-server tests pass. | No Java runtime packet golden or DB mutation capture. |

## Remaining Risks

- No Java runtime packet capture or golden artifact was generated for assembly partial consumption.
- Assembly duplicate-stack selection still depends on Java-like first item-by-id behavior and has no live Java comparison.
- C# persistence groups partial part mutations transactionally, while Java storage/DAO side effects may occur per decrease; this is a C# persistence-boundary difference that still needs runtime/DAO-level comparison.
- Explorer-discovered decompose, extraction, AP extraction, and composition non-atomic branches remain future work unless already covered by existing tests.
- Java 25 JDK/Maven blocker still prevents new generated Java runtime artifacts in this environment.

## Summary Metrics

- Total Java artifacts discovered: 15 grouped rows in this unit
- Total artifacts ported: 2 production C# artifacts adjusted plus 1 test-helper counter and 1 regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 15 grouped rows
- Total blocked artifacts: Java runtime packet capture/golden generation for scheduled item-use paths, Java 25 JDK, Java compiler, Maven, Maven wrapper, and DAO-level Java mutation ordering comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue scheduled item-use ordering with another isolated non-atomic branch.
- Preferred target: extraction target-before-tool behavior through `ExtractAction` and `EnchantService.breakItem`, or AP extraction target-before-tool/AP ordering.
- Why: Explorer already mapped the relevant Java behavior, and these branches are independent from assembly.
- Scope:
  - choose exactly one branch;
  - source-review the precise Java mutation and packet order;
  - add one focused C# regression or small runtime fix;
  - keep persistence-boundary differences explicit;
  - avoid unrelated item-use refactors.

## Suggested Acceptance Criteria

- One isolated non-atomic branch is covered.
- Focused and affected slice tests pass.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction target-before-tool branch | extraction service/connection/tests | Medium | Similar to assembly but uses `EnchantService.breakItem` behavior. |
| B | AP extraction target-before-tool/AP branch | AP extraction service/connection/tests | Medium | No-delay path; keep AP rank side effects narrow. |
| C | Decompose success-message-before-reward audit | decompose tests/services | Medium | Existing coverage may already cover much of this. |
| D | Hook detail readiness surfacing | protection readiness/export files | Medium | Independent from item-use files. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement one focused extraction or AP extraction parity regression/fix | selected service/connection/test files plus progress/handoff docs | Java source writes, unrelated item-use paths |
| Explorer | Optional read-only deep dive for the selected branch only | read-only Java source inspection | all writes, shared docs, C# edits |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Broad item-use runtime refactors.
- Java generator implementation without Java 25 JDK and Maven.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1578] Preserve assembly partial consumes`.
- Files changed in UOW-1578:
  - `dotnetConversion/src/Aion.GameServer/Services/AssemblyItemService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AOX-Completion.md`
- Latest prior commits:
  - `91c11d286 [Phase 6][UOW-1577] Validate full dotnet solution`
  - `10afdc8f6 [Phase 6][UOW-1576] Triage inventory cleanup-seal regressions`
  - `bf0957db0 [Phase 6][UOW-1575] Integrate protection hook detail export`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
