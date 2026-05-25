# Phase 6OU Completion Handoff - AP Conditioning Payment Planner Wiring

Date: May 25, 2026
Unit of Work: UOW-899
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-899] Route charge AP payments through abyss planner`)

## Status

Phase 6 is still in progress. This unit wires AP-based conditioning payments through the shared `AbyssPointsService` plan path, covering direct `CM_CHARGE_ITEM` and the charge-all confirmation AP payment branch.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OU-Completion.md`

## What Changed

- Reviewed Java:
  - `com.aionemu.gameserver.services.item.ItemChargeService`
  - `com.aionemu.gameserver.services.abyss.AbyssPointsService`
  - `com.aionemu.gameserver.model.items.ChargeInfo`
  - `com.aionemu.gameserver.model.templates.item.Improvement`
- Replaced direct `player.AbyssRank.AddAp` calls in AP conditioning payment branches with `AbyssPointsService.CreateAddApPlan`.
- Persisted the planned rank update through existing item-charge repository calls.
- Applied the planned rank and sent the AP planner's player packets only after persistence succeeds.
- Added direct charge AP payment regression coverage.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 40 tests. During development, the first run caught a compile-time variable-shadowing error, and a later run caught an assertion issue around negative inventory update masks; both were fixed.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1468 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService` | `Aion.GameServer.Services.ItemChargeService` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` / `HandleChargeAllQuestionResponseAsync` | Service / Handler | Partial | Regression Tested in C# for direct AP payment | Partial Parity | AP conditioning payment now uses the shared AP planner for single-item and charge-all payment branches. Charge-all AP branch is wired but not newly regression-tested here. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit + Regression Tested in C# | Partial Parity | AP spend packets now come from the AP planner for conditioning payment. Full Legion, siege, AP cap config, large-AP logging, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Regression Tested through direct charge AP payment | Needs Verification | Test validates AP decreases from `1000` to `500`. Java AP-cap config remains absent. |
| `com.aionemu.gameserver.model.items.ChargeInfo` | `Aion.GameServer.Model.GameObjects.InventoryItem.Charge` / `ItemChargeService.Level1ChargePoints` | Model / Value Object | Partial | Regression Tested in C# | Needs Verification | Direct AP payment test validates level-1 charge state. Observer-driven burn behavior remains partial. |
| `com.aionemu.gameserver.model.templates.item.Improvement` | `Aion.GameServer.Dataholders.ItemImprovement` | Static Data DTO | Partial | Regression Tested with fixture XML | Needs Verification | Fixture uses Java-shaped AP conditioning metadata. Broader runtime XML comparison is absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Test validates `STR_MSG_USE_ABYSSPOINT` (`1300965`) amount `500`, plus charge success/all-complete packet presence. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Test validates rank packet emission after AP spend. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Test validates charge update packet presence and runtime charge state. Existing helper was not used for negative charge update mask decoding. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleChargeItemAsync_ApPaymentSendsAbyssPointsPlannerPackets` | Regression | Java `ItemChargeService.chargeItem`, `processAPPayment`, and `AbyssPointsService.addAp` source review | AP spend of `500`, rank packet emission, level-1 charge state, and charge success/stats/all-complete packet order. | Deterministic C# connection-level regression grounded in Java source. | No Java runtime artifact; charge-all AP branch not directly covered; no rank-threshold side-effect assertion. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Charge-all AP payment planner path is code-wired but not directly covered by a new regression.
- Java AP-cap config is not represented in `PlayerAbyssRank.AddAp` or `AbyssPointsService`.
- Full Legion and siege execution remain incomplete for broader AP callers.
- Rank-threshold AP payment fanout remains partial and was not exercised here.
- Packet bytes were not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 partial AP payment caller wiring across direct and charge-all conditioning payment paths
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, charge-all AP regression, Java AP cap config, rank-threshold AP payment fanout, full Legion/siege execution for other AP callers, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue AP caller convergence by wiring another existing AP reward/spend path through `AbyssPointsService`, or add focused charge-all AP payment regression if the fixture can stay compact.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Charge-all AP regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Maybe | Safe if no production charge code changes run in parallel. |
| AP cap config audit | `PlayerAbyssRank.cs`, config docs/tests | Yes if read-only | Useful before implementing AP cap support. |
| Remaining AP caller wiring | caller-specific files plus `GameServerConnection.cs` if needed | No | Keep sequential when shared connection/payment paths are touched. |
| Remaining `SM_LEGION_EDIT` packet types | `SmLegionEdit.cs`, packet tests | Yes if no AP caller wiring is active | Separate packet-focused unit. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Still tooling-blocked locally. |

## Do Not Parallelize

- AP caller wiring with other edits to `GameServerConnection.cs`.
- AP planner changes with `PlayerAbyssRank` AP-cap changes unless one owner controls both.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, wire another compact AP caller through `AbyssPointsService`, add charge-all AP regression coverage, or choose another isolated gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
