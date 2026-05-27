# Phase 6AJN Completion - AP Extraction Atomicity Audit

Date: 2026-05-27
Unit of Work: UOW-1438
Status: Complete after documentation validation.

## Scope

Document the known Java/C# AP extraction transaction-boundary difference after UOW-1436 aligned AP extraction success packets and UOW-1437 characterized the partial-failure edge.

## Completed Work

- Created `docs/ApExtract-Atomicity-Audit.md`.
- Documented Java's packet-visible AP extraction ordering: target delete/cube, tool consume, then AP gain.
- Documented the theoretical Java edge where target delete succeeds but tool consume fails before AP gain.
- Documented the reachability assessment: normal same-client Java packet processing should not interleave a duplicate packet into that tiny synchronous window.
- Documented the C# decision to keep AP extraction atomic across target delete, tool mutation, and AP rank update unless future Java runtime evidence shows the partial edge matters.
- No production or test code changed in this unit.

## Validation

- Ran `git diff --check`.
- Result: passed with line-ending warnings only.

## Migration Parity Table - UOW-1438

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` / `GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Service / Connection Packet Caller | Refactored | Regression Tested in prior unit | Intentional Difference | Packet-visible success path was aligned in UOW-1436. The theoretical Java target-delete-success/tool-consume-failure edge remains intentionally atomic in C# unless runtime evidence shows it matters. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `PlayerEnterWorldRepository.SaveApExtractActionMutationAsync` | Storage / Persistence Boundary | Refactored | Manual Only | Intentional Difference | Java sends during storage mutation and persists dirty state later; C# persists target/tool/AP mutation atomically before sending success packets. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `ApExtractService.CreateMutationPlan` / repository transaction | Storage Count Mutation | Refactored | Manual Only | Intentional Difference | Java could theoretically fail tool consume after target delete; C# does not expose a partial plan shape. Normal same-client Java packet processing makes the edge unlikely. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` plus AP extraction save/send path | Service | Partial | Regression Tested in prior units | Partial Parity | Local AP gain packets are covered in existing AP extraction tests. Legion contribution, siege callback, and deeper rank-change side effects remain broader AP parity gaps. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Not applicable | Documentation-only validation | `ApExtractAction.act`, `Storage.delete`, `Storage.decreaseByObjectId` | No code behavior changed. | Prior UOW-1436 AP extraction tests cover the success packet path; this unit documents the partial-edge decision. | No Java runtime bytes; no fault-injection seam for the theoretical partial edge. |

## Remaining Risks

- No Java runtime capture proves the AP extraction partial edge is impossible.
- C# has no fault-injection seam for target-deleted/tool-consume-failed AP extraction.
- Broader AP side effects remain tracked separately: legion contribution, siege callbacks, rank-limited equipment persistence, and abyss skill refresh details.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed; 1 audit document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 1 grouped row remains partial parity; 3 rows are intentional documented differences
- Total blocked artifacts: Java runtime artifact generation, AP partial-edge runtime proof/fault-injection seam, broader AP side effects
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: toy-pet source consume readiness analysis.
- Why: recent item-use work has covered extraction, AP extraction, and composition packet ordering. Toy-pet/kisk source consumption remains a nearby scheduled/world-spawn packet family from prior handoffs.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - C# toy-pet/kisk/player connection sources found via `rg "ToyPet|Kisk|PetSpawn" dotnetConversion/src dotnetConversion/tests`

## Alternative Sequential Task

- Task: design a deterministic composition reward-template connection variant for same-object double consume.
- Why: UOW-1437 unit-tested reward creation for same-object double consume and connection-tested consumed packet order without a generated reward template. A connection reward variant needs a deterministic reward seam that does not reintroduce random reward merges.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Best next read-only discovery before packet/test changes. |
| B | Composition deterministic reward seam design | composition tests/static data, read-only | Medium | Avoid shared fixture edits until design is clear. |
| C | AP side-effect backlog audit | AP extraction/AP service docs and source, read-only | Medium | Legion/siege/rank side effects remain broader Phase 6 AP risks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | all writes, docs, commits |
| Explorer B | Design deterministic composition reward connection seam | Java/C# composition tests/static data, read-only | all writes, docs, commits |
| Orchestrator | Select one implementation/test slice after analysis | selected files only | shared docs until validation; unrelated files |

## Do Not Parallelize

- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1438] Document AP extraction atomicity`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# / docs files changed in UOW-1438:
  - `docs/ApExtract-Atomicity-Audit.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJN-Completion.md`
