# Phase 6QP Completion Handoff - ItemPurification Target Object ID Allocation

Date: May 25, 2026
Unit of Work: UOW-946
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-946] Allocate item purification target ids`)

## Status

Phase 6 is still in progress. This unit removes the immediate ItemPurification target object-id planning blocker by allocating through the existing C# `IDFactory` boundary when the handler plan is otherwise ready.

The handler remains non-persistent and mostly non-mutating. It still does not mutate inventory/AP, persist changes, call the send bridge, synthesize AP rank packets, select random bonus ids, or execute full Java `ItemFactory` / `ItemSocketService` side effects.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QP-Completion.md`

## What Changed

- Added an internal handler composition helper in `GameServerConnection`.
- `HandleItemPurificationAsync` now:
  - builds the current handler workflow/application/packet plan first
  - keeps explicit `targetObjectId` overrides unchanged
  - allocates `_idFactory.NextId()` only when the first application plan reports `NeedsTargetObjectIdAllocation`, `_idFactory` exists, and random-bonus selection is not pending
  - rebuilds the workflow/application/packet plan with the allocated target id
  - releases the allocated id if the rebuilt application plan is not ready
- Added handler regressions for successful allocation, random-bonus pending no-allocation, and release/reuse when a rebuilt pre-persistence plan is still not ready.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | ItemPurification target-id allocation | `CM_ITEM_PURIFICATION`, `ItemPurificationService.upgradeItem`, `ItemFactory`, `IDFactory` | `GameServerConnection.cs`, `GameServerConnectionItemPurificationTests.cs` | Integration Fix / Tests | No | Medium | Shared handler and shared ItemPurification test fixture require single writer ownership. |
| B | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate from ItemPurification files; deferred. |
| C | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling is available; no runtime parity claim possible now. |
| D | Packet-byte comparison gap audit | ItemPurification packet fanout | read-only packet tests/golden tooling | Parity Verification | Yes | Low | Read-only unless Java artifact format changes are selected. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-946 implementation, tests, docs, commit | `GameServerConnection.cs`, `GameServerConnectionItemPurificationTests.cs`, progress/handoff docs | unrelated files | Code, tests, parity docs, commit |
| Explorer | Allocation/release audit | read-only repo inspection | all writes | Edge-case report and parity risks |

## Sub-Agent Outputs Integrated

- Explorer confirmed Java allocates during `ItemFactory.newItem()` inside `ItemPurificationService.upgradeItem`, after validation and material decrease.
- Explorer recommended no allocation on validation/material/template failures and no allocation while random-bonus selection is pending.
- Explorer recommended releasing only IDs allocated by the handler when a later pre-persistence step fails before the target item becomes durable/owned.
- Explorer noted C# status priority can report target-id allocation before random-bonus selection; this unit uses the explicit `RequiresRandomBonusSelection` flag to prevent allocation leaks and documents the status-priority risk.
- Explorer was closed after integration.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationApplicationPlanServiceTests|ItemPurificationWorkflowServiceTests|ItemPurificationPacketPlanServiceTests"
```

Result: passed, 29 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1627 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` | Client Handler / Adapter | Partial | Regression Tested in C# | Partial Parity | Handler now allocates a target object id from `_idFactory` when the only immediate blocker is target-id allocation, but still does not mutate inventory/AP, persist, send live packets, or invoke the send bridge directly. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationInheritanceService` plus `GameServerConnection.HandleItemPurificationAsync` | Service / Target Item Projection | Partial | Regression Tested in C# | Partial Parity | C# now models Java `ItemFactory.newItem` object-id allocation in the handler planning path. Full Java item creation, storage add, socket/godstone/fusion persistence, random bonus rerolling, and quest notifications remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemFactory` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` plus `Aion.GameServer.Utils.IdFactory.IDFactory` | Factory / ID Allocation Boundary | Partial | Regression Tested in C# | Partial Parity | Java allocates through `IDFactory.getInstance().nextId()` inside `ItemFactory.newItem`. C# allocates through injected `_idFactory.NextId()` only after validation/material planning and before target-item packet planning. No item template miss/null factory behavior is ported here. |
| `com.aionemu.gameserver.utils.idfactory.IDFactory` | `Aion.GameServer.Utils.IdFactory.IDFactory` | Utility | Partial | Existing Unit Tested + Regression Tested in C# | Needs Verification | Handler tests prove allocation and release/reuse at this boundary for ItemPurification. Full Java bitmap/id invalid-mask behavior has existing unit coverage but no runtime comparison in this unit; threading semantics remain source-reviewed only. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationMaterialMutationService` and `ItemPurificationApplicationPlanService` | Service / Mutation Plan | Partial | Regression Tested in C# | Partial Parity | Release test uses a C# pre-persistence failure where base-item delete needs verification after allocation. Java runtime side effects and captured mutable item behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.TuningAction` | `Aion.GameServer.Services.ItemPurificationInheritanceService` random-bonus injection seam | Runtime Random Selection | Partial | Regression Tested in C# | Needs Verification | Handler explicitly avoids allocating a target id while random-bonus selection is pending. Java `TuningAction.getRandomStatBonusIdFor` is still not implemented; caller-supplied reroll ids remain the temporary seam. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleItemPurificationAsync_AllocatesTargetObjectIdWhenFactoryAvailable` | Regression | Java `ItemPurificationService.upgradeItem` -> `ItemFactory.newItem` -> `IDFactory.nextId` source review | Validates handler planning allocates object id `9001`, rebuilds to a ready application/packet plan, and does not mutate inventory/AP. | Deterministic C# handler regression for Java allocation timing after validation/material planning. | Does not execute Java runtime, persist target item, or compare packet bytes. |
| `HandleItemPurificationAsync_DoesNotAllocateWhenRandomBonusSelectionIsPending` | Regression | Java `upgradeItem` random bonus reroll branch via `TuningAction.getRandomStatBonusIdFor` source review | Validates no id is consumed while random-bonus selection is still pending, even though the target id is also absent. | Deterministic C# guard regression preventing allocation leaks across the temporary random-bonus seam. | Status priority still reports `NeedsTargetObjectIdAllocation`; the pending random-bonus flag documents the additional blocker. |
| `HandleItemPurificationAsync_ReleasesAllocatedTargetObjectIdWhenRebuiltPlanIsNotReady` | Regression | Java allocation occurs before inventory add; C# pre-persistence rebuild can still detect an unready plan | Validates an allocated id is released and reusable if the rebuilt application plan reaches `NeedsBaseItemDeleteVerification`. | Deterministic C# release/reuse regression for the handler allocation boundary. | The specific base-item-as-material scenario is a C# planner safety case, not a captured Java runtime trace. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `HandleItemPurificationAsync` still does not mutate inventory/AP, persist changes, call the send bridge, synthesize AP rank packets, select random bonus ids, or execute full Java `ItemFactory`/`ItemSocketService` behavior.
- Successful handler allocation reserves an id in the C# `IDFactory` even though the live target item is not yet persisted; this matches the future pre-persistence bridge but remains partial until live mutation is wired.
- Random-bonus status priority still surfaces `NeedsTargetObjectIdAllocation` first; tests assert `RequiresRandomBonusSelection` to prevent allocation while the reroll seam is unresolved.
- Java missing-base null behavior and negative-Kinah deduction quirk remain documented but not runtime-verified.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 ItemPurification target object-id allocation slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, random-bonus selection, live inventory/AP persistence, live handler send/mutation invocation, and full upgrade-item side-effect parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Continue ItemPurification live adapter readiness with either a non-persistent live mutation snapshot preview from the ready application plan or the random-bonus selection seam for differing stat-bonus sets.

Suggested shape for random-bonus seam:
- Inspect Java `TuningAction.getRandomStatBonusIdFor` and `ItemRandomBonusData.areBonusSetsEqual`.
- Add a small C# selector/planner seam that can produce a deterministic injected random bonus id in tests.
- Wire only enough handler composition to proceed when random-bonus selection is available.
- Keep actual item mutation and persistence separate.

Suggested shape for mutation snapshot preview:
- Given a ready `ItemPurificationApplicationPlan`, produce post-mutation inventory snapshots without mutating `Player.InventoryItems`.
- Feed those snapshots into the existing packet-input bridge.
- Keep repository writes and live send invocation separate.

Do not combine with:
- repository transaction persistence
- live inventory/AP mutation
- AP rank side-effect packets
- quest notifications
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Random-bonus Java analysis | read-only Java `TuningAction`, item random bonus data/templates | Low | Good explorer task before implementation. |
| B | Mutation snapshot preview implementation | likely new service/test files plus existing ItemPurification tests | Medium | Single writer if it touches shared ItemPurification fixtures. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| D | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionItemPurificationTests.cs`.
- Multiple agents changing ItemPurification workflow/application/packet services in the same unit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose random-bonus selection seam, mutation snapshot preview, or isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
