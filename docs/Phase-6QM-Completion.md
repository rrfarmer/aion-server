# Phase 6QM Completion Handoff - ItemPurification Handler Composition Bridge

Date: May 25, 2026
Unit of Work: UOW-943
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-943] Compose item purification handler plans`)

## Status

Phase 6 is still in progress. This unit advances the ItemPurification live-adapter path by composing the existing pure workflow, application, and packet-plan layers from `GameServerConnection.HandleItemPurificationAsync`.

The handler still does not mutate inventory/AP, allocate target object ids, persist changes, or send packets. This is an intentional bridge step so the next units can wire runtime inputs and side effects in small verified slices.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationHandlerPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QM-Completion.md`

## What Changed

- Added `ItemPurificationHandlerPlan`, carrying `Workflow`, `Application`, and `PacketPlan`.
- Updated `HandleItemPurificationAsync` to compose:
  1. `ItemPurificationWorkflowService.CreateWorkflowPlan`
  2. `ItemPurificationApplicationPlanService.CreateApplicationPlan`
  3. `ItemPurificationPacketPlanService.CreatePacketPlan`
- Preserved Java handler behavior already modeled in C#: active player is authoritative, packet player/material object ids are ignored, and missing static data returns no plan.
- Added optional handler parameters for injected `targetObjectId` and `rerolledRandomBonusId`, allowing focused tests to exercise the ready application-plan path without wiring live object-id allocation yet.
- Updated existing handler tests to assert workflow/application/packet-plan statuses.
- Added a handler composition regression proving target-id injection produces Java-order application operations and packet operations without mutating current player state.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | ItemPurification handler composition bridge | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | `GameServerConnection.cs`, `ItemPurificationHandlerPlan.cs`, `GameServerConnectionItemPurificationTests.cs` | Integration Fix/Test | No | Medium | Completed sequentially because handler and handler-test edits are shared. |
| B | Java ItemPurification behavior analysis | `CM_ITEM_PURIFICATION`, `ItemPurificationService`, storage/AP/item factory helpers | read-only | Java Analysis | Yes | Low | Completed by explorer. |
| C | C# ItemPurification gap analysis | handler, client packet, workflow/tests | read-only | C# Analysis | Yes | Low | Completed by explorer. |
| D | Kinah charge-all partial drift | ItemCharge charge-all Kinah path | charge handler tests | Test Creation | Maybe | Medium | Deferred; less aligned with latest ItemPurification pivot. |

## Sub-Agent Outputs Integrated

- Java explorer confirmed packet parse order, ignored player/material object ids, validation order, success-message-before-mutation ordering, material/AP/Kinah/base delete/target add side effects, and Java's negative-Kinah no-op quirk.
- C# explorer confirmed the current handler stopped at workflow planning and identified the handler composition/live adapter as the narrow next gap.
- Both agents were closed after their reports were integrated.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationWorkflowServiceTests|ItemPurificationApplicationPlanServiceTests|ItemPurificationPacketPlanServiceTests|ItemPurificationPacketInputSnapshotServiceTests|ItemPurificationApServiceTests|ItemPurificationMaterialMutationServiceTests"
```

Result: passed, 41 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1622 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` and `Aion.GameServer.Network.Aion.ClientPackets.CmItemPurification` | Client Handler / Packet | Partial | Regression Tested in C# | Partial Parity | Handler now composes workflow, application, and packet-plan metadata using the active player, base item object id, and result item id while still ignoring packet player/material object ids. Live mutation/sending is not yet wired. Java has no missing-base null guard; C# remains safer and documents the difference. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationWorkflowService` and `Aion.GameServer.Services.ItemPurificationApService` | Service / Validation | Partial | Regression Tested in C# | Partial Parity | Existing validation ordering feeds the handler plan. Success message packet metadata now appears before mutation metadata in the handler packet plan. Java runtime message byte comparison is still unavailable. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationMaterialMutationService` and `Aion.GameServer.Services.ItemPurificationApplicationPlanService` | Service / Mutation Plan | Partial | Regression Tested in C# | Partial Parity | Handler now exposes Java-order material, AP, Kinah no-op, base delete, and target add operation metadata. Runtime state mutation, persistence, AP rank side effects, and packet fanout remain not executed by the handler. Java negative-Kinah deduction quirk remains represented as `PreserveKinahNoOp`. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationInheritanceService` via `ItemPurificationWorkflowService` | Service / Target Item Projection | Partial | Regression Tested in C# | Partial Parity | Injected target-object-id handler test confirms target item id, object id, and enchant-minus-five projection. Full Java `ItemFactory`, socket/godstone/fusion persistence, random bonus selection, and object id allocation remain incomplete. |
| `com.aionemu.gameserver.model.templates.item.purification.ItemPurificationTemplate` | `Aion.GameServer.Dataholders.ItemPurificationTable` and `ItemPurificationSummary` | Static Data / DTO | Partial | Existing Unit/Static Data Tested | Needs Verification | Handler composition uses existing static table summaries. JAXB/static-data edge cases and full live data counts remain covered only by existing static-data tests, not Java runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` via `ItemPurificationPacketPlanService` | Packet DTO | Partial | Regression Tested in C# | Needs Verification | Handler packet plan includes upgrade-success system message first. Byte-level Java packet comparison remains blocked. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleItemPurificationAsync_UsesActivePlayerBaseItemAndIgnoresPacketMaterialObjectIdsWithoutMutation` | Regression | Java `CM_ITEM_PURIFICATION.runImpl` source review plus C# handler composition | Existing test updated to assert workflow plus application and packet-plan statuses; validates active-player/base-item lookup, ignored material object ids, no live mutation, and target object id still required. | Deterministic C# handler regression based on Java control-flow review. | Does not execute Java runtime; live mutation/sending remains deferred. |
| `HandleItemPurificationAsync_ReturnsMissingBaseItemPlanWithoutThrowing` | Regression / Intentional Difference | Java `ItemPurificationService.isPurificationAllowed` lacks a null guard for `baseItem`; C# intentionally returns a failed plan | Existing test updated to assert workflow/application/packet-plan failure statuses. | Deterministic C# guard regression documenting a safer intentional difference. | Java would likely throw; runtime exception behavior not reproduced. |
| `HandleItemPurificationAsync_ComposesApplicationAndPacketPlansWhenTargetObjectIdProvided` | Regression | Java `CM_ITEM_PURIFICATION.runImpl` -> `isPurificationAllowed` -> `decreaseMaterials` -> `upgradeItem` source review | Validates injected target-object-id handler path composes material delete, AP spend, Kinah no-op, base delete, target add, and packet-plan success-first order without mutating player state. | Deterministic C# handler composition regression for Java operation ordering. | Does not allocate target ids, persist mutation, send packets, or compare Java runtime packets. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `HandleItemPurificationAsync` still does not mutate inventory/AP, allocate target object ids, persist changes, or send packets; it now exposes composed plans for future live adapters.
- Java missing-base behavior likely throws because `baseItem.getItemId()` is used without a null guard; C# intentionally returns `MissingBaseItem` and documents the safer difference.
- Java validates Kinah but calls `decreaseKinah(-necessaryKinah)`, which appears to be a no-op because storage only decreases positive amounts; C# preserves this as `PreserveKinahNoOp`, but no runtime comparison exists.
- Full Java `ItemFactory`, `ItemSocketService`, godstone/fusion/manastone persistence, random bonus rerolling, AP rank side effects, quest notifications, and packet byte parity remain incomplete.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 handler-level ItemPurification composition bridge
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, target object-id allocation, live inventory/AP persistence, full packet fanout, and full upgrade-item side-effect parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Continue ItemPurification live adapter readiness with a narrow runtime-input bridge: given a ready application plan and post-mutation inventory snapshot, compose packet-input snapshots and packet send adapter output from `HandleItemPurificationAsync` or a small adjacent adapter without executing persistence.

Suggested shape:
- Add a small handler/application result or helper that accepts a ready `ItemPurificationApplicationPlan`, post-mutation inventory items, item templates, and cube snapshots.
- Use `ItemPurificationPacketInputSnapshotService.CreateInputs`.
- Use `ItemPurificationPacketPlanService.CreatePacketPlan` with concrete inputs.
- Do not yet send packets from `GameServerConnection` unless the unit also owns a focused send-adapter test and avoids persistence claims.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Runtime-input bridge for ItemPurification packet plans | `GameServerConnection.cs`, `GameServerConnectionItemPurificationTests.cs` or a new small service/test | Medium | Single writer if editing handler. |
| B | ItemPurification packet-input service gap audit | read-only service/tests | Low | Safe explorer companion. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files. |
| D | Java runtime observer design for ItemPurification packets | docs only | Low | Do not claim runtime parity until tooling exists. |

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
5. If still tooling-blocked, continue ItemPurification runtime-input bridge or choose the isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
