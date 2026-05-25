# Phase 6OV Completion Handoff - Charge-All AP Payment Regression

Date: May 25, 2026
Unit of Work: UOW-900
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-900] Cover charge-all AP planner payment`)

## Status

Phase 6 is still in progress. This test-only unit adds focused coverage for the charge-all AP payment planner branch wired in UOW-899.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OV-Completion.md`

## What Changed

- Reviewed Java:
  - `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems`
  - `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler`
  - `com.aionemu.gameserver.services.abyss.AbyssPointsService`
- Added charge-all AP payment acceptance regression by seeding:
  - `PendingChargeAllRequest`
  - `QuestionResponseRegistry`
  - Java charge2-all confirmation question id
- Verified the AP planner packet path after `CM_QUESTION_RESPONSE` acceptance.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 41 tests. During development, the first focused run caught an init-only fixture setup issue for equipped items; it was fixed.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1469 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` / `PendingChargeAllRequest` | Service / Handler | Partial | Regression Tested in C# | Partial Parity | Charge-all AP accept path is now covered for AP spend planner packets, rank update, charge update, stats, and all-complete order. Question-window creation was already implemented separately. |
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry` | Request / Response Registry | Partial | Regression Tested in C# | Needs Verification | Regression seeds and consumes a charge-all request like Java `ResponseRequester.respond`. Concurrency and expiry behavior remain unverified. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit + Regression Tested in C# | Partial Parity | Charge-all AP spend now has direct regression coverage for planner packet execution. Full Legion, siege, AP cap config, large-AP logging, and runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Regression Tested through charge-all AP payment | Needs Verification | Test validates AP decreases from `1000` to `500`. Java AP-cap config remains absent. |
| `com.aionemu.gameserver.model.items.ChargeInfo` | `Aion.GameServer.Model.GameObjects.InventoryItem.Charge` / `ItemChargeService.Level1ChargePoints` | Model / Value Object | Partial | Regression Tested in C# | Needs Verification | Equipped item reaches level-1 charge after charge-all accept. Observer-driven burn behavior remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Test validates AP spend message id `1300965` amount `500`, plus charge success/all-complete packet presence. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Test validates rank packet emission after charge-all AP spend. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Test validates charge update packet presence and runtime charge state; negative charge mask bytes are not decoded here. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleQuestionResponseAsync_ChargeAllApPaymentSendsAbyssPointsPlannerPackets` | Regression | Java `ItemChargeService.startChargingEquippedItems`, `RequestResponseHandler.acceptRequest`, `processAPPayment`, and `AbyssPointsService.addAp` source review | Charge-all AP confirmation spends `500` AP through planner packets, clears pending request, charges the equipped item to level 1, and emits AP spend/rank/charge/success/stats/all-complete packets. | Deterministic C# connection-level regression grounded in Java source. | No Java runtime artifact; no multi-item charge-all coverage; no rank-threshold side-effect assertion. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Charge-all question creation and acceptance are covered separately but not compared against Java runtime.
- Java AP-cap config is not represented in `PlayerAbyssRank.AddAp` or `AbyssPointsService`.
- Multi-item charge-all ordering, mixed charge bars, and partial item filtering need broader runtime coverage.
- Rank-threshold AP payment fanout remains partial and was not exercised here.
- Packet bytes were not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 0 production code artifacts; 1 charge-all AP payment regression added for UOW-899 wiring
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, Java AP cap config, rank-threshold AP payment fanout, multi-item charge-all runtime parity, full Legion/siege execution for other AP callers, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue AP caller convergence by wiring another existing AP reward/spend path through `AbyssPointsService`, or pivot to AP cap config support if no compact caller is available.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| AP cap config support | `PlayerAbyssRank.cs`, config/options tests | Maybe | Keep sequential if changing rank math used by many callers. |
| Remaining AP caller wiring | caller-specific files plus `GameServerConnection.cs` if needed | No | Shared AP/payment paths should have one owner. |
| Remaining `SM_LEGION_EDIT` packet types | `SmLegionEdit.cs`, packet tests | Yes if no AP caller wiring is active | Separate packet-focused unit. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Still tooling-blocked locally. |
| Independent non-AP gameplay slice | isolated files only | Maybe | Safe if it avoids AP/decompose/emotion files and shared docs until final bookkeeping. |

## Do Not Parallelize

- AP caller wiring with other edits to `GameServerConnection.cs`.
- AP planner/rank math changes with caller wiring unless one owner controls both.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, wire another compact AP caller through `AbyssPointsService`, implement AP cap support, or choose another isolated gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
