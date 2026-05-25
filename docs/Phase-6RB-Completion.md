# Phase 6RB Completion Handoff - ItemPurification Handler Failure Ordering

Date: May 25, 2026
Unit of Work: UOW-958
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-958] Cover item purification handler save failure ordering`)

## Status

Phase 6 is still in progress. This unit adds handler-level coverage for the explicit ItemPurification persistent helper when repository persistence fails after live mutation and packet sending.

Normal `CM_ITEM_PURIFICATION` dispatch remains plan-only. `HandleInfrastructurePacketAsync` was intentionally not wired to the persistent helper.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
- `docs/ItemPurification-Persistence-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RB-Completion.md`

## What Changed

- Added `HandleItemPurificationPersistentLiveExecutionAsync_ReportsPersistenceFailureAfterLiveExecution`.
- The test passes `EmptyPlayerEnterWorldRepository { SaveItemPurificationMutationResult = false }` through the handler helper.
- It asserts:
  - result status is `PersistenceSaveFailed`
  - `PersistenceSaved` is false
  - live execution succeeded
  - inventory/AP were already mutated
  - packet records were emitted
  - repository payload was attempted once
- Updated persistence-plan and progress docs with failure-ordering notes, parity table, risks, metrics, and next work.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Handler-level persistence failure-ordering test | `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `InventoryDAO`, `AbyssRankDAO` | `GameServerConnectionItemPurificationTests.cs` | Test Creation | No | Medium | Completed in this unit; shared handler test file required exclusive ownership. |
| B | Failure-ordering design note | same ItemPurification path | docs | Docs | Yes if isolated | Low | Partially addressed in persistence-plan docs. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | isolated test file | Test Creation | Yes | Medium | Recommended next safe slice. |
| D | Java ItemPurification runtime observer design | Java observer docs/scripts | docs only until tooling exists | Analysis | Yes | Low | Do not claim runtime parity while Java tooling is blocked. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-958 handler failure-ordering test, docs, commit | `GameServerConnectionItemPurificationTests.cs`, ItemPurification/progress/handoff docs | unrelated runtime files | Focused test, parity docs, commit |

No sub-agents were spawned because the selected unit touched shared ItemPurification test and doc files.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationPersistentLiveExecutionServiceTests|ItemPurificationLiveExecutionServiceTests|ItemPurificationPersistencePlanServiceTests|PlayerEnterWorldRepositoryItemStonePersistenceTests|AbyssPointsServiceTests"
```

Result: passed, 31 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1653 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationPersistentLiveExecutionAsync` | Client Handler / Failure Ordering | Partial | Unit Tested | Needs Verification | C# handler-level opt-in helper now has explicit repository-save failure coverage. Automatic dispatch remains plan-only, and Java runtime ordering is not verified. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPersistentLiveExecutionService` through handler helper | Service / Material/Base/AP Failure Ordering | Partial | Unit Tested | Partial Parity | Test proves C# live material/AP/base/target mutation occurs before repository save failure is surfaced. Java storage persistent-state, deleted queues, quest callbacks, and mid-loop failure behavior remain unmodeled. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationPersistentLiveExecutionService` through handler helper | Service / Target Add Failure Ordering | Partial | Unit Tested | Partial Parity | Test proves the target item is present in memory even when persistence save returns false. Java target factory/storage callbacks and DB/runtime comparison remain missing. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` failure result via `EmptyPlayerEnterWorldRepository` | Repository / Failure Result | Partial | Unit Tested | Needs Verification | Fake repository failure is covered at the handler boundary. Real MySQL exceptions/false returns, transaction rollback, inserted `item_stones`, and Java category-commit differences remain unverified. |
| `com.aionemu.gameserver.dao.AbyssRankDAO` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` AP rank payload on failed save | Repository / AP Failure Result | Partial | Unit Tested | Needs Verification | Test proves updated AP rank payload is attempted before failure is reported. Java rank persistent-state, `last_update`, AP side effects, and runtime comparison remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.ItemPurificationPacketSendAdapter` through handler helper | Packet Send Boundary | Partial | Regression Tested in C# | Needs Verification | Test proves six packet records are emitted before save failure is reported. Real socket send failure, byte ordering, and Java storage-send timing remain unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleItemPurificationPersistentLiveExecutionAsync_ReportsPersistenceFailureAfterLiveExecution` | Unit | Java `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `InventoryDAO`, `AbyssRankDAO`, and `PacketSendUtility` source review | Validates repository save failure is surfaced as `PersistenceSaveFailed` after inventory/AP mutation, packet send records, and repository payload attempt. | Deterministic C# handler-level failure-ordering coverage. | Does not execute real DB rollback/commit behavior, Java runtime sockets, quest callbacks, AP side effects, or Java byte/DB comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and does not invoke live execution or persistence.
- The persistent opt-in path sends/mutates before repository save; the failure ordering is now tested, but rollback is still absent.
- Live DB integration for `SaveItemPurificationMutationAsync`, including inserted `item_stones`, is not tested.
- Quest item get/remove callbacks and AP rank side-effect execution remain missing.
- Java `Storage` persistent-state/deleted queue behavior and `ItemStoneListDAO` load cleanup are not modeled.
- C# repository method still uses one transaction for the whole write set, an intentional safety difference from Java category-level commits.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 new production artifacts; 1 handler-level failure-ordering regression added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, automatic live handler invocation, live DB integration, quest callbacks, and AP side-effect execution
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add the narrow Kinah charge-all partial-drift regression in `GameServerConnectionInventoryExpansionUseItemTests.cs`, or continue ItemPurification with a docs-only automatic-dispatch readiness policy note.

Suggested ItemCharge shape:
- Re-read Java `ItemChargeService` charge-all Kinah behavior before writing the test.
- Focus on stale/current item drift around charge-all payment and revalidation.
- Keep changes in tests unless a real bug is revealed.

Suggested ItemPurification policy-doc shape:
- Keep `HandleInfrastructurePacketAsync` unchanged.
- Document prerequisites for enabling automatic dispatch: DB integration, rollback/failure policy, quest callbacks, AP side effects, and runtime packet/DB comparison.

Do not combine with:
- automatic live packet sending from `HandleInfrastructurePacketAsync`
- quest item get/remove callbacks
- AP rank side-effect execution
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; recommended next. |
| B | ItemPurification automatic-dispatch readiness policy | docs | Low | Useful before any production dispatch change. |
| C | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing ItemPurification handler/service tests.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Read `docs/ItemPurification-Persistence-Plan.md`.
3. Confirm branch status and latest commit.
4. Run Parallel Work Discovery before selecting the next write unit.
5. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
6. If still tooling-blocked, choose Kinah charge-all partial-drift regression, ItemPurification automatic-dispatch policy docs, or Java observer design notes.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
