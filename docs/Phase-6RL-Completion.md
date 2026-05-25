# Phase 6RL Completion Handoff - ItemPurification AP Rank-Drop Metadata

Date: May 25, 2026
Unit of Work: UOW-968
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-968] Add item purification AP rank-drop coverage`)

## Status

Phase 6 is still in progress. This unit adds regression coverage for ItemPurification AP spend when the spend drops abyss rank.

No production code changed. Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. The new test verifies AP packet and side-effect metadata are produced but also documents that ItemPurification live execution still skips AP metadata sends.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationLiveExecutionServiceTests.cs`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RL-Completion.md`

## What Changed

- Added `ExecuteAsync_RankDropKeepsApSpendPacketsModeledButNotSent`.
- The regression sets a player at AP 1,300 and rank 2, spends 1,200 AP through ItemPurification live execution, and verifies:
  - AP becomes 100.
  - Rank drops to 1.
  - `AbyssPointsAddPlan.PlayerPackets` contains `SmSystemMessage` and `SmAbyssRank`.
  - `AbyssPointsAddPlan.RankUpdatePacket` is populated.
  - `ShouldCheckRankLimitItems` is true.
  - `ShouldUpdateAbyssSkills` is true.
  - Live execution still sends only success/inventory/cube packets and records `AbyssPointsUpdate` as skipped metadata.
- Updated audit/readiness/progress docs with the new AP evidence.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | AP spend rank-drop metadata test | `AbyssPointsService`, `AbyssRank` | `ItemPurificationLiveExecutionServiceTests.cs` | Test | No for selected write | Low | Completed sequentially because live-execution test file is a shared fixture. |
| B | AP packet-order wiring | `AbyssPointsService`, packet send utility | live execution service/tests | Implementation/Test | No with A | Medium | Deferred; should be explicit opt-in only and preserve production dispatch disabled. |
| C | Quest notifier seam | `Storage`, `QuestEngine` | new interface/service plus opt-in tests | Implementation/Test | Yes if disjoint from AP files | Medium | Deferred. |
| D | Java observer artifact generation | `CM_ITEM_PURIFICATION`, `PacketSendUtility` | Java/tooling files | Implementation | Yes if tooling exists | Medium | Blocked locally by Java 8 and missing Maven. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-968 AP rank-drop regression, docs, commit | live-execution test file, AP/quest readiness docs, progress/handoff docs | production live execution service, `GameServerConnection.cs`, repository files | Test-only AP metadata coverage and parity docs |

No sub-agents were spawned for this write unit.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationLiveExecutionServiceTests
```

Result: passed, 3 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationLiveExecutionServiceTests|ItemPurificationLiveMutationServiceTests|ItemPurificationPersistentLiveExecutionServiceTests|GameServerConnectionItemPurificationTests|AbyssPointsServiceTests|ItemPurificationApplicationPlanServiceTests"
```

Result: passed, 37 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` / `AbyssPointsAddPlan` through `ItemPurificationLiveExecutionServiceTests` | Service / AP Spend Metadata | Partial | Unit Tested + Regression Tested | Partial Parity | Added live-execution coverage that a purification AP spend dropping rank produces modeled spend packet, rank packet, rank-update broadcast packet, rank-limit flag, and abyss-skill flag. ItemPurification still does not send AP packets or execute rank side effects. Java runtime comparison is missing. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Unit Tested + Regression Tested | Needs Verification | Test covers deterministic AP drop from 1,300/rank 2 to 100/rank 1 through ItemPurification live execution. Broader rank thresholds, GP-gated rank behavior, precision/rounding, and Java runtime comparison remain unverified. |
| Java equipment rank-limit check via `player.getEquipment().checkRankLimitItems()` | `Aion.GameServer.Services.EquipmentService.CheckRankLimitItems` flag in `AbyssPointsAddPlan` | Service / AP Rank Side Effect | Partial | Regression Tested as metadata | Needs Verification | Test proves the rank-limit side-effect flag is set on rank drop, but no unequip execution, persistence, or packet fanout is wired in ItemPurification. |
| `com.aionemu.gameserver.services.abyss.AbyssSkillService` | `Aion.GameServer.Services.AbyssSkillService` flag in `AbyssPointsAddPlan` | Service / AP Rank Side Effect | Partial | Regression Tested as metadata | Needs Verification | Test proves the abyss-skill refresh flag is set on rank drop, but no skill update execution or Java runtime comparison exists for ItemPurification. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Services.ItemPurificationLiveExecutionService` / plan-only production handler | Client Handler / Dispatch Gate | Partial | Regression Tested in C# | Needs Verification | No production handler change. The regression explicitly documents that live execution skips AP metadata sends, so the AP gate remains open before automatic dispatch. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_RankDropKeepsApSpendPacketsModeledButNotSent` | Regression | Java `AbyssPointsService.addAp(Player, int)`, `AbyssRank.addAp`, and `ItemPurificationService.decreaseMaterials` source review | Verifies a rank-dropping ItemPurification AP spend updates AP/rank and produces modeled AP packet/rank-side-effect metadata, while live execution still skips the AP metadata operation. | Deterministic C# regression over Java-source-reviewed AP spend behavior. | Does not execute Java, send AP packets from ItemPurification, broadcast rank updates, invoke equipment rank-limit unequip, invoke abyss skill refresh, or enable automatic production dispatch. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.
- AP spend packets are modeled and now regression-tested for rank drop, but still not emitted by ItemPurification live execution.
- Rank-change side effects for equipment rank limits and abyss skill refresh are flags only in this path.
- Quest projection is metadata only; no `IQuestItemMutationNotifier`, handler map, `questUpdateItems` projection, nearby-quest refresh, or live dispatcher exists.
- Java storage dirty-state, deleted queue, and synchronization behavior remain unmodeled in the C# snapshot path.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 new production artifacts; 1 AP rank-drop live-execution regression added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, AP packet emission, AP rank-side-effect execution, and automatic production dispatch
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add AP spend packet emission behind explicit opt-in live execution, preserving production `CM_ITEM_PURIFICATION` plan-only dispatch, then update live-execution packet-order tests to expect success, inventory update/delete, AP spend/rank packets at the Java-reviewed operation point, base/target packets, and skipped Kinah metadata.

Alternative safe task:
- Add a no-op `IQuestItemMutationNotifier` seam behind explicit opt-in live execution only, preserving projection-only behavior by default.
- Add Java observer artifact generation only if Java 25/Maven tooling is available.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Opt-in AP packet emission design | live execution service/tests | Medium | Keep production handler dispatch disabled. |
| B | Quest notifier no-op seam | new interface/service plus focused tests | Medium | Do not invoke real quest handlers yet. |
| C | Java observer artifact generation feasibility | Java observer/test tooling files or read-only docs | Medium | Only if Java 25/Maven is available. |
| D | Static-data quest update item projection audit | read-only quest/static-data files | Low | Useful before live quest callback execution. |

## Do Not Parallelize

- Multiple agents editing `ItemPurificationLiveExecutionService.cs` or `ItemPurificationLiveExecutionServiceTests.cs`.
- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing progress and handoff docs.
- Any automatic `CM_ITEM_PURIFICATION` production dispatch work with DB integration or quest/AP side-effect work.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Read `docs/ItemPurification-Automatic-Dispatch-Readiness.md`, `docs/ItemPurification-AP-Quest-Readiness-Audit.md`, and `docs/ItemPurification-Java-Observer-Design.md`.
3. Confirm branch status and latest commit.
4. Run Parallel Work Discovery before selecting the next write unit.
5. Keep production `CM_ITEM_PURIFICATION` automatic dispatch disabled.
6. Prefer explicit opt-in AP packet emission tests/wiring or a no-op quest notifier seam.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.

