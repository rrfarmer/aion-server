# Phase 6PO Completion Handoff - ItemPurification AP Planner

Date: May 25, 2026
Unit of Work: UOW-919
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-919] Add ItemPurification AP planner`)

## Status

Phase 6 is still in progress. This unit adds a pure AP validation/spend planner for Java `ItemPurificationService.isPurificationAllowed` and `decreaseMaterials`.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationApService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationApServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PO-Completion.md`

## What Changed

- Added `ItemPurificationApService`.
- Added projected DTOs for purification result and required material inputs.
- Modeled Java validation checks for identified state, enchant, AP, kinah, and material counts.
- Modeled Java AP spend ordering: spend AP only after the caller confirms required materials were decreased.
- Modeled Java zero-AP guard: no `AbyssPointsService.AddAp` call when `necessaryAbyssPoints <= 0`.
- Left static data parsing, packet handling, system messages, audit logging, material deletion, kinah mutation, base item deletion, target item creation/state copy, persistence, and packet fanout out of scope.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationApServiceTests
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1565 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationApService.ValidatePurificationAllowed` | Service / Validation Planner | Partial | Regression Tested in C# | Partial Parity | Models identified, enchant, AP, kinah, and required-material prechecks. Template/result lookup, log/audit branches, messages, l10n lookup, base-item null NPE behavior, and live packet integration remain unported. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `ItemPurificationApService.CreatePurificationApPlan` | Service / AP Spend Planner | Partial | Regression Tested in C# | Partial Parity | Models AP spend only after material-decrease confirmation and zero-AP skip. Material deletion, kinah mutation, base deletion, persistence, and fanout remain missing. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | Not ported in this unit | Service / Item Mutation | Not Started | Manual Analysis | Unknown | Broad target-item state copy/reroll behavior remains unported. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | Not ported in this unit | Client Packet Handler | Not Started | Manual Analysis | Needs Verification | Java ignores player/material object ids and calls validation/decrease/upgrade. No C# packet handler exists. |
| `com.aionemu.gameserver.dataholders.ItemPurificationData` | Not ported in this unit | Static Data Holder | Not Started | Manual Analysis | Unknown | C# has no `item_purifications` holder/parser. |
| `com.aionemu.gameserver.model.templates.item.purification.ItemPurificationTemplate` | Not ported in this unit | Static Data DTO | Not Started | Manual Analysis | Unknown | Java maps base item id to result maps. No C# DTO/static table exists yet. |
| `com.aionemu.gameserver.model.templates.item.purification.PurificationResult` | `Aion.GameServer.Services.ItemPurificationResultProjection` | DTO / Input Projection | Partial | Regression Tested in C# | Needs Verification | Projection includes result id, min enchant, AP, kinah, and materials. JAXB defaults/null behavior and XML parsing are not ported. |
| `com.aionemu.gameserver.model.templates.item.purification.RequiredMaterial` | `Aion.GameServer.Services.ItemPurificationMaterialRequirement` | DTO / Input Projection | Partial | Regression Tested in C# | Needs Verification | Projection models item id/count. Java zero-material/null-list behavior remains uncertain. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service / AP Mutation | Partial | Regression Tested in C# | Partial Parity | Negative AP mutation is routed through existing AP planner. Persistence, ranking cache, Legion contribution fanout, and exact packet bytes remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage.getItemCountByItemId` | `ItemPurificationApService` inventory count projection | Inventory / Input Projection | Partial | Regression Tested in C# | Needs Verification | Sums projected inventory by item id. Java storage location/stack semantics and decrement behavior are not fully ported. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` | Not ported in this unit | Inventory / Kinah Mutation | Not Started | Manual Analysis | Needs Verification | Java prechecks kinah but calls `decreaseKinah(-necessaryKinah)`, which may not actually spend kinah. Future live parity decision required. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePurificationApPlan_SpendsAbyssPointsOnlyAfterMaterialsDecrease` | Regression | Java `isPurificationAllowed` and `decreaseMaterials` source review | Validates no AP spend before material-decrease confirmation, then negative AP mutation. | Deterministic C# regression grounded in Java source ordering. | No material deletion or Java runtime packet comparison. |
| `ValidatePurificationAllowed_RejectsInsufficientAbyssPointsBeforeSpend` | Regression | Java AP precheck source review | Validates insufficient AP rejects before AP mutation. | Deterministic C# regression. | Message packet bytes not covered. |
| `ValidatePurificationAllowed_ChecksIdentifiedEnchantKinahAndMaterials` | Regression | Java validation branch source review | Validates identified, min enchant, kinah, and material-count rejection statuses. | Deterministic C# regression for projected inputs. | Template lookup, audit/logging, messages, and zero-material JAXB behavior not covered. |
| `CreatePurificationApPlan_SkipsAbyssPointSpendWhenJavaCostIsZero` | Regression | Java `if (necessaryAbyssPoints > 0)` source review | Validates zero AP cost skips AP planner and leaves AP unchanged. | Deterministic C# regression. | No live item mutation coverage. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# has no `item_purifications` static-data holder/parser.
- Live `CM_ITEM_PURIFICATION` is unported, including Java's ignored material object ids and missing base-item null guard.
- Java sends success before resource spends; live C# ordering must preserve or explicitly document any difference.
- Java material decrease can partially consume earlier materials if a later material decrease fails; this pure planner does not mutate materials.
- Java kinah spend may be a no-op because it calls `decreaseKinah(-necessaryKinah)` after prechecking positive kinah.
- `upgradeItem` state-copy/reroll behavior is broad and still missing.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 11
- Total artifacts ported: 1 pure ItemPurification AP validation/spend planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 11
- Total blocked artifacts: 8 blocked/not-started categories, including Java runtime artifact generation, item-purification static data, live packet handler, material/base/target item mutation, kinah mutation parity decision, upgrade-item state copy, persistence/fanout, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | ItemPurification AP planner | `ItemPurificationService` | `ItemPurificationApService.cs`, tests | Planner | No within same files | Medium | Completed in UOW-919. |
| B | C# purification static data | `ItemPurificationData`, `ItemPurificationTemplate`, `PurificationResult`, `RequiredMaterial` | `StaticData.cs`, new table/DTO files, tests | DTO/Data Port | Maybe | Medium | Shared static loader edits need careful ownership. |
| C | ItemCharge AP spend hardening | `ItemChargeService` | existing charge service/tests | Service/Test | Maybe | Medium | Separate from purification, but AP docs overlap. |
| D | Live purification packet/action | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | packet, inventory, data, repository files | Integration | No | High | Crosses too many missing mutation/persistence surfaces. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add C# `item_purifications` static-data DTO/table/parser with a tiny XML fixture test, if the existing static-data loader can be touched safely.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java `item_purifications.xml` shape and DataManager holder counts | read-only Java/static data | edits, docs | XML shape and count/default report. |
| Agent B | Inspect C# static-data loader insertion points and fixture patterns | read-only C# tests/loader | edits, docs | Minimal file list and parser plan. |
| Orchestrator | Implement static-data DTO/table/parser if safe | exact static-data files plus tests/docs | live packet/inventory mutation | Code, tests, docs, commit. |

## Do Not Parallelize

- Static-data loader edits with any other task that modifies `StaticData.cs`.
- Live purification packet wiring with static-data parser work.
- Inventory material/base/target mutation with AP planner or static-data parser work.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue `item_purifications` static-data parsing or another narrow AP caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
