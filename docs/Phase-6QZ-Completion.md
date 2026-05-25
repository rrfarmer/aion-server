# Phase 6QZ Completion Handoff - ItemPurification Opt-in Persistent Execution

Date: May 25, 2026
Unit of Work: UOW-956
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-956] Add item purification persistent execution seam`)

## Status

Phase 6 is still in progress. This unit adds an explicit opt-in service seam that composes ItemPurification live execution with persistence payload generation and repository saving.

Normal `CM_ITEM_PURIFICATION` dispatch remains plan-only. This unit does not wire production packet handling to live mutation or persistence.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistentLiveExecutionService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPersistentLiveExecutionServiceTests.cs`
- `docs/ItemPurification-Persistence-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QZ-Completion.md`

## What Changed

- Added `ItemPurificationPersistentLiveExecutionService`.
- The service:
  - calls `ItemPurificationLiveExecutionService.ExecuteAsync`
  - derives `ItemPurificationPersistencePlan` from ready live mutation output
  - calls `IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync`
  - reports persistence save failure instead of claiming success
- Added `ItemPurificationPersistentLiveExecutionResult` and status enum.
- Added `EmptyPlayerEnterWorldRepository.SaveItemPurificationMutationResult` for failure-path tests.
- Added focused tests for ready persistence, no persistence when live execution is not ready, and persistence failure after ready execution.
- Updated persistence plan/progress docs.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Opt-in persistent execution seam | `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `InventoryDAO`, `AbyssRankDAO` | new service/tests, repository test stub | Service / Integration Fix | No | Medium | Completed in this unit; composed shared live execution and persistence contracts. |
| B | Handler-level opt-in helper | `CM_ITEM_PURIFICATION` | `GameServerConnection.cs`, handler tests | Integration Fix | No | Medium | Possible next, but large shared handler file needs exclusive ownership. |
| C | Persistence failure-ordering hardening/docs | same ItemPurification path | docs/tests | Test/Docs | Yes if docs-only | Medium | Needed before automatic dispatch. |
| D | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | isolated test file | Test Creation | Yes | Medium | Separate safe alternative. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-956 service, tests, docs, commit | new persistent execution service, focused tests, `PlayerEnterWorldRepository.cs`, progress/handoff/persistence docs | unrelated runtime files | Code, focused tests, parity docs, commit |

No sub-agents were spawned because the selected unit composed shared service and repository contracts.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationPersistentLiveExecutionServiceTests|ItemPurificationLiveExecutionServiceTests|ItemPurificationPersistencePlanServiceTests|ItemPurificationLiveMutationServiceTests|PlayerEnterWorldRepositoryItemStonePersistenceTests"
```

Result: passed, 12 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1651 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Services.ItemPurificationPersistentLiveExecutionService` | Client Handler / Opt-in Persistent Execution | Partial | Unit Tested | Needs Verification | C# now has an explicit seam that composes live send/mutation with repository persistence, but automatic packet dispatch remains plan-only. Java runtime packet/DB ordering is still unverified. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationPersistentLiveExecutionService` via `ItemPurificationLiveExecutionService` | Service / Success Message Ordering | Partial | Unit Tested + Regression Tested | Partial Parity | Success message still sends before mutation/persistence in the opt-in seam. Packet byte parity and send failure behavior remain fake-registry tested only. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPersistentLiveExecutionService` plus `ItemPurificationPersistencePlanService` | Service / Material/Base/AP Persistence Caller | Partial | Unit Tested | Partial Parity | Ready live mutation now feeds repository persistence payloads in opt-in execution. Java storage persistent-state flags, deleted queue, quest callbacks, partial failure behavior, and automatic handler invocation remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationPersistentLiveExecutionService` plus repository target add payload | Service / Target Add Persistence Caller | Partial | Unit Tested | Partial Parity | Ready target add payloads are passed to the repository after live execution. Java `ItemFactory`/storage add quest callbacks and live DB/runtime comparison remain missing. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` called by persistent seam | Repository / Inventory Persistence Caller | Partial | Unit Tested | Needs Verification | Fake repository tests verify the payload call. Real DB transaction behavior, rollback, and Java category-commit differences remain unverified. |
| `com.aionemu.gameserver.dao.AbyssRankDAO` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` called with AP rank payload | Repository / AP Rank Persistence Caller | Partial | Unit Tested | Needs Verification | Updated AP rank is passed through opt-in persistence. Java rank persistent-state behavior, `last_update`, side effects, and runtime comparison remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.ItemPurificationPacketSendAdapter` through persistent seam | Packet Send Boundary | Partial | Regression Tested in C# | Needs Verification | Packets are sent before persistence in this opt-in C# seam. Java storage sends mutation packets during mutation and persistence happens later; real socket ordering and failure recovery remain unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_PersistsPayloadAfterReadyLiveExecution` | Unit | Java `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `InventoryDAO`, and `AbyssRankDAO` source review | Validates ready opt-in live execution sends packets, mutates inventory/AP, builds persistence payload, and calls repository with material update, base delete, target add, and AP rank. | Deterministic C# unit coverage for the opt-in persistent execution seam. | Does not execute real DB writes, Java runtime sockets, quest callbacks, or AP side effects. |
| `ExecuteAsync_DoesNotPersistWhenLiveExecutionIsNotReady` | Unit | Java validation/storage mutation guard source review | Validates stale current inventory blocks live execution and prevents repository persistence call. | Deterministic C# guard coverage. | Does not model Java mid-loop partial material decrement behavior. |
| `ExecuteAsync_ReportsPersistenceSaveFailureAfterReadyExecution` | Unit | Java persistence occurs after storage mutation; C# repository can fail | Validates persistence failure is surfaced after send/mutation and does not claim success. | Deterministic C# failure-path coverage. | No rollback exists; send/mutation already happened. Automatic handler dispatch remains disabled because this ordering needs runtime policy. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and does not invoke live execution or persistence.
- The new persistent seam sends/mutates before repository save; persistence failure is reported but not rolled back.
- Live DB integration for `SaveItemPurificationMutationAsync`, including inserted `item_stones`, is not tested.
- Quest item get/remove callbacks and AP rank side-effect execution remain missing.
- Java `Storage` persistent-state/deleted queue behavior and `ItemStoneListDAO` load cleanup are not modeled.
- C# repository method still uses one transaction for the whole write set, an intentional safety difference from Java category-level commits.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 opt-in persistent live execution seam
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, automatic live handler invocation, live DB integration, quest callbacks, and AP side-effect execution
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a guarded handler-level opt-in helper that invokes `ItemPurificationPersistentLiveExecutionService` only when explicitly requested by tests/callers, or document/test send-mutation-persistence failure ordering before any handler-level seam.

Suggested shape:
- If adding the handler helper, keep `HandleInfrastructurePacketAsync` unchanged.
- Reuse the existing plan-only handler composition, then call the new persistent execution seam with explicit repository and registry overrides.
- Add a handler test proving repository save is called only through the explicit helper and normal packet dispatch remains plan-only.
- Keep persistence failure visible in the result; do not silently treat it as success.

Do not combine with:
- automatic live packet sending from `HandleInfrastructurePacketAsync`
- quest item get/remove callbacks
- AP rank side-effect execution
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Handler-level opt-in persistent helper | `GameServerConnection.cs`, handler tests | Medium | Shared handler file; sequential. |
| B | Persistence failure-ordering design/test | docs or focused service tests | Low/Medium | Useful before automatic dispatch. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| D | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

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
6. If still tooling-blocked, choose handler-level opt-in persistent helper, persistence failure-ordering design/test, or isolated Kinah charge-all regression.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.

