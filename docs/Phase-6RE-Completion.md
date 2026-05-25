# Phase 6RE Completion Handoff - ItemCharge Kinah Missing-Current Regression

Date: May 25, 2026
Unit of Work: UOW-961
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-961] Add item charge-all Kinah missing-item regression`)

## Status

Phase 6 is still in progress. This unit adds one focused regression for the ItemCharge charge-all Kinah accept path.

No production code changed. The new test is a conservative C# approximation of Java's charge-all behavior: Java captures the original filtered `Item` collection at question time and pays the quoted amount before calling `chargeItems(..., requirePayment=false)`, while C# stores pending item snapshots and revalidates against the current inventory snapshot at accept time.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RE-Completion.md`

## What Changed

- Added `HandleQuestionResponseAsync_ChargeAllKinahPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsMissing`.
- The regression starts with two pending charge-all items, removes one item from the current inventory snapshot before accept, accepts the Kinah charge-all question, and verifies:
  - full quoted Kinah payment is persisted/spent
  - only the remaining current item is charged and captured for persistence
  - the pending request is cleared
  - Kinah update, charge update, success message, stats packet, and all-complete message are emitted in order
- Updated progress docs with the Migration Parity Table, tests, risks, metrics, and next work.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | ItemPurification DB integration | `InventoryDAO`, `ItemStoneListDAO`, `AbyssRankDAO` | repository integration tests, possibly DB fixtures | Test Creation | No for this unit | Medium | Likely touches shared fixture/repository surfaces; safer as its own sequential unit. |
| B | ItemCharge missing-current Kinah regression | `ItemChargeService`, `CM_CHARGE_ITEM` / question response path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Sequential | Medium | Completed in this unit; single test file plus shared docs. |
| C | Java ItemPurification observer design | ItemPurification runtime path | docs only | Java Analysis / Documentation | Yes | Low | Useful next if DB integration is deferred. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-961 regression, progress, handoff, commit | `GameServerConnectionInventoryExpansionUseItemTests.cs`, progress/handoff docs | production C# files, ItemPurification repository fixtures | Test, parity docs, commit |

No sub-agents were spawned because the selected regression and shared docs are single-owner files.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests|GameServerConnectionChargeAllQuestionResponseTests"
```

Result: passed, 70 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1655 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` and `Aion.GameServer.Services.ItemChargeService` | Service / Charge-All Payment and Revalidation | Partial | Regression Tested | Partial Parity | Added C# regression for the Kinah charge-all path where quoted payment is spent before accept-time item revalidation and only the current chargeable item mutates/sends packets. Java keeps original item references, so missing-current object lifetime semantics still need runtime comparison. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` / charge-all question response flow | Client Handler | Partial | Regression Tested | Needs Verification | No production handler change. Regression covers the migrated question-response path after a pending charge-all request exists; it does not execute real client packet scheduling, Java `ResponseRequester`, or Java runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.Item` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model / Charge State | Partial | Regression Tested | Partial Parity | Test covers missing/current item handling for equipped Kinah charge-all items in the C# snapshot model. Java `Item` object reference lifetime, `ChargeInfo` observer attachment, item stat refresh internals, equality/hash behavior, and threading around equipment collections remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemChargeAllMutationAsync` via `EmptyPlayerEnterWorldRepository` capture | Storage / Kinah Persistence Boundary | Partial | Regression Tested | Needs Verification | Fake repository captures the full quoted Kinah spend and one charged item. Real `Storage.tryDecreaseKinah` persistent-state behavior, inventory dirty queues, MySQL integration, rollback behavior, and Java behavior for removed captured item references remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` packet observer for charge-all response | Packet Send Boundary | Partial | Regression Tested in C# | Needs Verification | Regression checks packet type/order and selected message ids for C# emission. It does not compare Java packet bytes, socket ordering, or Java behavior when a captured item reference is no longer in current storage. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleQuestionResponseAsync_ChargeAllKinahPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsMissing` | Regression | Java `ItemChargeService.startChargingEquippedItems`, `chargeItems`, `processKinahPayment`, and `CM_CHARGE_ITEM` source review | Validates quoted Kinah payment is spent in full, the missing pending item is skipped by C# accept-time revalidation, the current chargeable item is persisted/charged, and Kinah update/charge update/success/stat/complete packets are emitted. | Deterministic C# regression coverage for the C# snapshot approximation of Java's pay-before-charge flow. | Does not execute Java runtime, real DB writes, real socket bytes, Java equipment/stat observer internals, or Java captured-object removal semantics. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Java keeps the original filtered `Item` collection inside the charge-all request handler; C# stores pending item snapshots and revalidates against current inventory. The missing-current regression is intentionally conservative and still needs Java runtime confirmation.
- ItemCharge real DB integration for charge-all Kinah persistence is not tested.
- C# repository method uses one transaction for charged items plus payment; Java storage dirty-state persistence may commit differently.
- ChargeInfo observer attachment/burn paths and broader combat integration remain incomplete.
- AP charge-all side effects beyond currently modeled AP rank packets remain partial.
- Java `Item`/equipment collection threading and `ChargeInfo` stat refresh internals are not fully modeled.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 new production artifacts; 1 charge-all Kinah missing-current regression added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, live DB integration, combat observer/stat refresh integration, and real socket packet comparison
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add live DB integration coverage for `SaveItemPurificationMutationAsync`, or add Java ItemPurification observer design notes for future packet/DB capture while local Java tooling is unavailable.

Suggested DB integration shape:
- Inspect existing opt-in MySQL fixture patterns before editing.
- Cover material update/delete, base update/delete, target insert, inherited `item_stones`, and AP rank save.
- Keep production `CM_ITEM_PURIFICATION` dispatch unchanged.
- Do not claim Java parity unless Java runtime or deterministic DB comparison evidence exists.

Suggested observer-design shape:
- Draft the Java capture format for ItemPurification packet and DB observations.
- Include generated target object-id normalization.
- Do not require local Java 25/Maven to be available.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | ItemPurification DB integration test design/implementation | repository integration tests, possibly DB fixtures | Medium | Sequential if shared repository fixtures need edits. |
| B | Java ItemPurification observer design | docs only | Low | Safe docs-only work if DB fixture work is too broad. |
| C | Isolated planner/live-adapter regression | a single existing test file | Medium | Choose only if it does not touch shared fixtures or production dispatch. |

## Do Not Parallelize

- Multiple agents editing `PlayerEnterWorldRepository.cs` or shared DB fixtures.
- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing progress and handoff docs.
- Any automatic `CM_ITEM_PURIFICATION` production dispatch work with DB integration or quest/AP side-effect work.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Read `docs/ItemPurification-Persistence-Plan.md` and `docs/ItemPurification-Automatic-Dispatch-Readiness.md`.
3. Confirm branch status and latest commit.
4. Run Parallel Work Discovery before selecting the next write unit.
5. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
6. If still tooling-blocked, choose ItemPurification DB integration coverage, Java observer design notes, or another isolated regression.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
