# Phase 6AOV Completion - Inventory Cleanup-Seal Regression Triage

Date: 2026-05-27
Unit of Work: UOW-1576
Status: Complete after validation.

## Scope

Triage and resolve the known inventory cleanup-seal test blockers while preserving Java source-of-truth behavior.

## Completed Work

- Used one read-only Explorer sub-agent for Java behavior review.
- Closed the Explorer after completion; it made no file changes.
- Reviewed Java item-use, composition, XP extraction, item-service, storage, packet serialization, and cleanup-table behavior.
- Updated `GameServerConnectionInventoryExpansionUseItemTests` only.
- Corrected `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag` to wait for the six packets it actually asserts. Java merged stack update sends `SM_INVENTORY_UPDATE_ITEM` and no cube update.
- Corrected `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` to expect cleanup-seal flag `0` for composition enchantment-stone reward ids because real Java `item_restriction_cleanups.xml` does not list those reward ids.
- Added an inline Java parity breadcrumb for the corrected composite cleanup-seal expectation.
- No production C# code or Java source was changed.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate|HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag"`.
- Result: passed 2 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 77 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj`.
- Result: passed 3349 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Inventory cleanup-seal failure triage | Item-use, composition, XP extraction, storage, item packet cleanup artifacts | inventory use-item tests/services | Integration Fix / Regression Test | Yes with read-only Java review | Medium | Known full-suite blocker, isolated from protection metadata. |
| B | Read-only Java item-use behavior review | Same Java item-use/storage artifacts | none/read-only | Java Analysis | Yes | Low | Safe Explorer sidecar with no writes. |
| C | Hook detail readiness surfacing | Protection stop-trigger readiness/export artifacts | readiness service/tests or new companion report | Integration/Test | No with inventory docs | Medium | Safe later, but protection metadata has recent coverage. |
| D | Nearby-refresh prerequisite | Quest/reward nearby-refresh artifacts | separate quest/reward files | Service/Test Creation | Maybe | Medium | Larger surface than this blocker fix. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Read-only Java cleanup-seal behavior review | Java Analysis | read-only Java source under `game-server/src` and `game-server/data` | all writes, docs, C# edits | none | Behavior report for item-use merge, packet ordering, and cleanup-seal source. |
| Orchestrator | Correct isolated inventory cleanup-seal regression expectations and docs | Integration/Test/Documentation | `GameServerConnectionInventoryExpansionUseItemTests.cs`, progress/handoff docs | Java source writes, production C# code unless Java evidence required it, protection metadata files | Explorer report plus local Java review | Previously failing inventory tests and full game-server test project pass. |

Parallelism was used for read-only Java analysis only. No sub-agent wrote files.

## Migration Parity Table - UOW-1576

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Packet Handler / Regression Test | Partial | Regression Tested | Partial Parity | Test now reflects Java merged reward ordering: start animation, consumed delete/update packets, merged reward update without cube update, finish animation. No Java runtime artifact comparison was executed. |
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Item Action / Regression Test | Partial | Regression Tested | Partial Parity | Java source reviewed: delayed task decreases tool, first stone, second stone by item id, then adds reward only if all decreases succeed. Non-atomic failure behavior remains documented but not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Packet Handler / Regression Test | Partial | Regression Tested | Partial Parity | Java source reviewed for exp-extract dispatch path. Broader item-use action selection, cooldown, observer, and quest callbacks remain only partially covered. |
| `com.aionemu.gameserver.model.templates.item.actions.ExpExtractAction` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Item Action / Regression Test | Partial | Regression Tested | Partial Parity | Test wait now matches Java merged reward packet count: source decrease update, EXP update, merged reward inventory update, system message, finish animation after start. Java full-cube precheck behavior remains a known oddity preserved by C# validation. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Service / Regression Test Dependency | Partial | Regression Tested | Partial Parity | Java `addStackableItem` merge path reviewed: existing stack update sends `SM_INVENTORY_UPDATE_ITEM`, no cube update. Power-shard equipped merge branch and non-stackable paths remain outside this unit. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Storage / Regression Test Dependency | Partial | Regression Tested | Partial Parity | Java storage increase/decrease packet ordering reviewed. C# tests cover selected merge/delete/update branches only; concurrency, persistence state, and quest callbacks remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Packet Utility / Regression Test Dependency | Partial | Regression Tested | Needs Verification | Reviewed as packet send dependency via Explorer. Exact byte-level Java output was not generated in this unit; packet tests validate C# shape only. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Serialization / Regression Test Dependency | Partial | Regression Tested | Partial Parity | Java source reviewed: cleanup-seal flag is computed from `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(itemId) ? 3 : 0`; remaining unseal time is `0`. No Java byte golden was generated. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Data Holder / Regression Test Dependency | Partial | Regression Tested | Partial Parity | Java source reviewed: item id must be present and account or legion warehouse result must be `0`. C# fixture assertion now distinguishes listed reward `188053996` from unlisted composition enchantment rewards. |
| `game-server/data/static_data/items/item_restriction_cleanups.xml` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests` | Static Data / Regression Test Dependency | Partial | Manual Source Reviewed | Needs Verification | Java XML search showed `188053996` is listed, but composition enchantment reward ids `166000015` through `166000035` are not listed. Full XML load/golden comparison was not rerun for this unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` | Regression | Java source review of `CompositionAction`, `ItemService`, `Storage`, `GeneralInfoBlobEntry`, and cleanup XML | Merged composition reward emits inventory update without reward cube update and writes cleanup flag `0` for unlisted enchantment reward ids. | Source-review backed C# regression; full game-server tests pass. | No Java runtime packet golden. |
| Updated `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag` | Regression | Java source review of `ExpExtractAction`, `ItemService`, `Storage`, and `GeneralInfoBlobEntry` | Merged restricted reward emits six asserted packets and keeps cleanup flag `3` for listed reward id `188053996`. | Source-review backed C# regression; full game-server tests pass. | No Java runtime packet golden. |

## Remaining Risks

- No Java runtime packet capture or golden artifact was generated for these inventory item-use scenarios.
- Composite action remains intentionally Java-like and non-atomic: earlier decreases are not rolled back if a later decrease fails.
- Composite delayed execution consumes by item id after initial object-id validation; duplicate-stack edge cases may consume a different stack than initially selected.
- XP extraction preserves Java's inventory-full precheck before reward merge feasibility; this behavior can reject a merge-capable reward when the cube is full.
- Cleanup-seal flags are still source-review verified rather than byte-for-byte compared against Java runtime output.
- Java 25 JDK/Maven blocker still prevents new generated Java runtime artifacts in this environment.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: no production Java artifacts; 1 C# regression test file corrected
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: Java runtime packet capture/golden generation for these item-use paths, Java 25 JDK, Java compiler, Maven, Maven wrapper, and broader item-use runtime comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: run broader solution-level validation now that known game-server test blockers are cleared, or continue with the next isolated Phase 6 item-use runtime prerequisite.
- Why: `Aion.GameServer.Tests` now passes fully. A broader validation can reveal cross-project regressions, while item-use scheduled ordering evidence is the next nearby parity seam.
- Scope:
  - validation path: run the full .NET solution or selected broader test projects and document any failures;
  - item-use path: inspect scheduled decompose/assembly/XP/composition/extraction/AP extraction ordering and add a narrow metadata/report or regression test;
  - keep Java runtime parity claims conservative unless Java artifacts can be generated.

## Suggested Acceptance Criteria

- Broader validation path: command, result, and any failures are documented.
- Item-use path: one isolated behavior is source-reviewed and covered by focused C# regression.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Full solution validation | no files unless docs update | Low-Medium | Good next sanity check after game-server suite is green. |
| B | Read-only Java item-use scheduled ordering review | read-only Java source | Low | Safe Explorer sidecar. |
| C | Decompose/assembly/XP/composition ordering regression | specific inventory tests/services | Medium | One writer only. |
| D | Hook detail readiness surfacing | protection readiness/export files | Medium | Separate from inventory files. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Full solution validation or one isolated item-use ordering regression | selected test/service files plus progress/handoff docs | Java source writes, unrelated production runtime files |
| Explorer | Optional read-only Java scheduled item-use ordering review | read-only Java source inspection | all writes, shared docs, C# edits |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Java generator implementation without Java 25 JDK and Maven.
- Production item-use runtime refactors without Java evidence.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1576] Triage inventory cleanup-seal regressions`.
- Files changed in UOW-1576:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AOV-Completion.md`
- Latest prior commits:
  - `bf0957db0 [Phase 6][UOW-1575] Integrate protection hook detail export`
  - `c0033fc8a [Phase 6][UOW-1574] Add protection Java hook detail map`
  - `994e0a06c [Phase 6][UOW-1573] Add protection dashboard summary export`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
