# Phase 6RC Completion Handoff - ItemCharge Kinah Charge-All Drift Regression

Date: May 25, 2026
Unit of Work: UOW-959
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-959] Add item charge-all Kinah drift regression`)

## Status

Phase 6 is still in progress. This unit adds a narrow regression for ItemCharge charge-all Kinah behavior when one pending equipped item becomes stale before the player accepts the charge-all question.

No production code changed.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RC-Completion.md`

## What Changed

- Added `HandleQuestionResponseAsync_ChargeAllKinahPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsStale`.
- Added a two-item Kinah charge-all test fixture helper.
- The regression proves:
  - quoted question-time Kinah payment is still spent in full
  - stale already-charged item is skipped at accept-time revalidation
  - the still-chargeable item is persisted and charged
  - Kinah update, charge update, item charge success, stats, and charge-all complete packets are emitted in the expected C# order
- Updated progress docs with the Migration Parity Table, risks, metrics, and next work.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Kinah charge-all stale/current drift regression | `ItemChargeService`, `CM_CHARGE_ITEM`, charge-all question response path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes, isolated | Medium | Completed in this unit; one shared test file, so executed sequentially by orchestrator. |
| B | ItemPurification automatic-dispatch readiness policy | ItemPurification handler/service/DAO path | docs | Documentation Update | Yes | Low | Separate docs-only candidate for next work. |
| C | Java ItemPurification observer design | Java ItemPurification runtime path | docs only until tooling exists | Java Analysis | Yes | Low | Do not claim runtime parity while Java tooling is blocked. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-959 charge-all Kinah regression, docs, commit | `GameServerConnectionInventoryExpansionUseItemTests.cs`, progress/handoff docs | unrelated runtime files, ItemPurification handler files | Focused regression, parity docs, commit |

No sub-agents were spawned because the selected work touched one shared handler test file.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests|GameServerConnectionChargeAllQuestionResponseTests"
```

Result: passed, 69 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1654 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` and `Aion.GameServer.Services.ItemChargeService` | Service / Charge-All Payment and Revalidation | Partial | Regression Tested | Partial Parity | Added C# regression for Java charge-all behavior where the quoted Kinah payment is charged before accept-time item revalidation, and only currently chargeable items mutate/send packets. Remaining gaps: Java runtime packet byte/order comparison, real DB transaction behavior, rank-side effects for AP variants, and combat observer invocation. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` / charge-all question response flow | Client Handler | Partial | Regression Tested | Needs Verification | No production handler change. Regression covers the migrated question-response path after a pending charge-all request exists; it does not execute real client packet scheduling or Java runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.Item` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model / Charge State | Partial | Regression Tested | Partial Parity | Test covers stale/current `Charge` state handling for equipped Kinah charge-all items. Java `ChargeInfo` observer attachment, item stat refresh internals, equality/hash behavior, and threading around equipment collections remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemChargeAllMutationAsync` via `EmptyPlayerEnterWorldRepository` capture | Storage / Kinah Persistence Boundary | Partial | Regression Tested | Needs Verification | Fake repository captures the full quoted Kinah spend and one charged item. Real `Storage.tryDecreaseKinah` persistent-state behavior, inventory dirty queues, MySQL integration, and rollback behavior remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` packet observer for charge-all response | Packet Send Boundary | Partial | Regression Tested in C# | Needs Verification | Regression checks packet type/order and selected message ids for C# emission. It does not compare Java packet bytes or real socket behavior. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleQuestionResponseAsync_ChargeAllKinahPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsStale` | Regression | Java `ItemChargeService.startChargingEquippedItems`, `chargeItems`, `processKinahPayment`, and `CM_CHARGE_ITEM` source review | Validates quoted Kinah payment is spent in full, stale already-charged pending item is skipped, current chargeable item is persisted/charged, and Kinah update/charge update/success/stat/complete packets are emitted. | Deterministic C# regression coverage for the accept-time revalidation behavior derived from Java source. | Does not execute Java runtime, real DB writes, real socket bytes, or Java equipment/stat observer internals. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- ItemCharge real DB integration for charge-all Kinah persistence is not tested.
- C# repository method uses one transaction for charged items plus payment; Java storage dirty-state persistence may commit differently.
- ChargeInfo observer attachment/burn paths and broader combat integration remain incomplete.
- AP charge-all side effects beyond currently modeled AP rank packets remain partial.
- Java `Item`/equipment collection threading and `ChargeInfo` stat refresh internals are not fully modeled.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 new production artifacts; 1 charge-all Kinah stale/current regression added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, live DB integration, combat observer/stat refresh integration, and real socket packet comparison
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add ItemPurification automatic-dispatch readiness policy docs, or add a similarly narrow ItemCharge charge-all missing-current Kinah regression if Java source review confirms the behavior.

Suggested ItemPurification policy-doc shape:
- Keep `HandleInfrastructurePacketAsync` unchanged.
- Document prerequisites for enabling automatic dispatch: DB integration, rollback/failure policy, quest callbacks, AP side effects, and runtime packet/DB comparison.
- Tie the policy to the existing opt-in seams and failure-ordering tests from UOW-956 through UOW-958.

Suggested ItemCharge missing-current shape:
- Re-read Java `ItemChargeService.startChargingEquippedItems` and `chargeItems`.
- Build from the new Kinah two-item fixture.
- Confirm whether Java spends quoted Kinah when one pending item is missing but another remains chargeable.
- Keep changes test-only unless a production mismatch is discovered.

Do not combine with:
- automatic live ItemPurification dispatch
- quest item get/remove callbacks
- AP rank side-effect execution
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | ItemPurification automatic-dispatch readiness policy | docs | Low | Separate from ItemCharge tests. |
| B | ItemCharge charge-all missing-current Kinah regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Same test file as UOW-959; sequential with other edits to this file. |
| C | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple agents editing ItemPurification handler/service tests.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose ItemPurification automatic-dispatch policy docs, ItemCharge missing-current Kinah regression, or Java observer design notes.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
