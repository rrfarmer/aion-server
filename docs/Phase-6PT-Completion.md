# Phase 6PT Completion Handoff - ItemPurification Workflow Planner

Date: May 25, 2026
Unit of Work: UOW-924
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-924] Add ItemPurification workflow planner`)

## Status

Phase 6 is still in progress. This unit composes the existing ItemPurification lookup, AP validation, material mutation, and target inheritance planner slices in Java order, without live packet, repository, or fanout wiring.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationWorkflowService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationWorkflowServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PT-Completion.md`

## What Changed

- Added `ItemPurificationWorkflowService`.
- Resolved base purification template and selected result item through `ItemPurificationTable`.
- Ran Java-style `isPurificationAllowed` validation before material mutation planning.
- Ran material/base/kinah mutation planning only after validation succeeds.
- Ran target item inheritance planning only after material planning succeeds.
- Kept live AP mutation out of the composed planner; AP remains a projected spend amount for later live work.
- Preserved target object id and optional random-bonus reroll injection as explicit future factory/RNG boundaries.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationWorkflowServiceTests
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1580 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION.runImpl` | `Aion.GameServer.Services.ItemPurificationWorkflowService.CreateWorkflowPlan` | Workflow Planner / Packet Boundary Projection | Partial | Regression Tested in C# | Partial Parity | Composes lookup, validation, material planning, and target inheritance in Java order. Live packet parsing, object lookup, persistence, messages, and fanout remain unported. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `ItemPurificationWorkflowService` + `ItemPurificationApService.ValidatePurificationAllowed` | Service Validation | Partial | Regression Tested in C# | Partial Parity | Called before material mutation planning. Java warn/audit/system-message side effects remain absent. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `ItemPurificationWorkflowService` + `ItemPurificationMaterialMutationService.CreateDecreaseMaterialsPlan` | Service Mutation Planner | Partial | Regression Tested in C# | Partial Parity | Material/base/kinah plan is only created after validation succeeds. Live mutation and persistence remain unported. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `ItemPurificationWorkflowService` + `ItemPurificationInheritanceService.CreateTargetItemPlan` | Service Target Projection | Partial | Regression Tested in C# | Partial Parity | Target inheritance is only planned after material planning succeeds. Live factory/add/persistence/fanout remain missing. |
| `com.aionemu.gameserver.dataholders.ItemPurificationData` | `Aion.GameServer.Dataholders.ItemPurificationTable` consumed by workflow planner | Static Data Lookup | Partial | Regression Tested in C# | Partial Parity | Base template/result lookup is composed into workflow entry. Java JAXB/null lifecycle is not runtime-compared. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate` | `Aion.GameServer.Dataholders.ItemTemplateTable` consumed by workflow planner | Static Data Lookup / DTO | Partial | Regression Tested in C# | Needs Verification | Source and target templates are required for inheritance planning. Full template/factory defaults remain outside this unit. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | Not invoked by workflow planner | Service Boundary | Not Started for this workflow | Regression Tested in C# planner | Needs Verification | Workflow intentionally avoids live AP mutation and carries spend projection through material plan. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateWorkflowPlan_ComposesValidationMaterialMutationAndInheritanceWithoutLiveApMutation` | Regression | Java `CM_ITEM_PURIFICATION` and `ItemPurificationService` source review | Lookup/validation/material/target composition, AP spend projection, material/base deletes, target projection, and no live AP mutation. | Deterministic C# regression grounded in Java source ordering. | No live packet, persistence, or packet fanout. |
| `CreateWorkflowPlan_StopsBeforeMaterialMutationWhenValidationFails` | Regression | Java `isPurificationAllowed` before `decreaseMaterials` source review | Enchant failure stops before material or target planning. | Deterministic C# regression. | Java messages/audit not modeled. |
| `CreateWorkflowPlan_StopsBeforeMaterialMutationWhenValidationFindsMissingMaterials` | Regression | Java material count validation before decrease source review | Missing materials stop at validation when using one inventory snapshot. | Deterministic C# regression. | Does not model concurrent inventory races. |
| `CreateWorkflowPlan_ReportsLookupAndTemplateFailures` | Regression | Java lookup and target-template source review | Missing purification template, invalid result, and missing target template statuses. | Deterministic C# regression. | Java live null/NPE behavior and logs remain unported. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Workflow planner is pure and does not parse packets, resolve world/inventory objects, allocate object ids, mutate repositories, send packets, or log audit events.
- Java concurrent inventory races between validation and material mutation are not modeled beyond the lower-level material planner's defensive failure path.
- Kinah behavior remains unresolved for live wiring: the composed workflow carries the lower-level documented Java negative-spend no-op.
- Random bonus selection and target item creation remain injected/projected rather than data-backed live runtime work.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 composed ItemPurification workflow planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live packet handler, object-id allocation/factory add, AP/rank side effects, repository persistence, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Composed workflow planner | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | new planner/test files | Workflow Planner | No within same files | Medium | Completed in UOW-924. |
| B | Shared inventory decrement helper | `Storage`, `Item` | shared helper/service files | Refactor / Utility | Maybe later | Medium | Deferred to avoid broad churn before live mutation application is designed. |
| C | Live `CM_ITEM_PURIFICATION` wiring | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | packet, service, repository, packet fanout files | Integration | No | High | Still blocked by object allocation, persistence, AP side effects, kinah decision, and packet order. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a non-persistent `CM_ITEM_PURIFICATION` parser/guard adapter or a persistence application plan for the composed workflow. Keep it read-only/planning unless live mutation scope is explicitly selected.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Inspect Java `CM_ITEM_PURIFICATION` packet fields and C# packet parser conventions | read-only Java/C# packet files | edits, docs | Exact field/order report. |
| Agent B | Inspect existing C# repository mutation planner/application patterns | read-only service/repository/test files | edits, docs | Candidate persistence plan shape. |
| Orchestrator | Choose one narrow adapter/planner slice | new service/test files plus docs | live mutation/fanout unless scoped | Code, tests, docs, commit. |

## Do Not Parallelize

- Live mutation/fanout with parser/guard work.
- Repository persistence with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue with a non-persistent packet parser/guard adapter, persistence application plan, or another narrow AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
