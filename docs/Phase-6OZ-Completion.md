# Phase 6OZ Completion Handoff - Direct Charge AP Cap Regression

Date: May 25, 2026
Unit of Work: UOW-904
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-904] Cover charge AP cap clamp`)

## Status

Phase 6 is still in progress. This unit adds a connection-level regression for Java's AP cap behavior during direct AP conditioning payment.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OZ-Completion.md`

## What Changed

- Reviewed Java `ItemChargeService.processAPPayment`, `AbyssPointsService.addAp`, and `AbyssRank.addAp`.
- Added `HandleChargeItemAsync_ApPaymentHonorsConfiguredAbyssPointCapClamp`.
- Captured the Java edge case where a negative AP payment from an already-above-cap AP value can remove more than the requested payment:
  - Start AP: `1600`
  - Requested payment: `500`
  - AP cap: `1000`
  - Result AP: `1000`
  - Actual AP-use message amount: `600`
- Added `EmptyPlayerEnterWorldRepository.ChargePaymentAbyssRank` capture for direct charge persistence-boundary assertions.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 43 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1482 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService` | `Aion.GameServer.Services.ItemChargeService` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` | Service / Handler | Partial | Regression Tested in C# | Partial Parity | Direct AP conditioning payment now has cap-enabled connection coverage for Java's above-cap negative-payment clamp. Charge-all cap-specific coverage remains missing. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit + Regression Tested in C# | Partial Parity | Planner cap support is exercised through direct AP payment. Legion, siege, large-AP logging, and other AP callers remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Unit + Regression Tested in C# | Partial Parity | Regression validates AP `1600`, payment `500`, cap `1000` results in AP `1000` and actual removed delta `600`. Reset timing and persistent-state flags remain unmodeled. |
| `com.aionemu.gameserver.configs.main.CustomConfig` | `Aion.GameServer.Configuration.GameServerCustomOptions` / `GameServerOptions` | Configuration | Partial | Regression Tested through connection fixture options | Needs Verification | Test supplies Java-equivalent cap options directly; config loading was not newly tested. |
| `com.aionemu.gameserver.dao.AbyssRankDAO` / item charge persistence boundary | `Aion.GameServer.Data.PlayerEnterWorldRepository.SaveItemChargeMutationAsync` / `EmptyPlayerEnterWorldRepository.ChargePaymentAbyssRank` | Repository Boundary / Test Support | Partial | Regression Tested through test double | Needs Verification | Test-helper capture verifies capped rank AP `1000` passed to persistence. Live SQL transaction behavior remains unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo` | `Aion.GameServer.Model.GameObjects.InventoryItem.Charge` / `ItemChargeService.Level1ChargePoints` | Model / Value Object | Partial | Regression Tested in C# | Needs Verification | Regression validates item reaches level-1 conditioning points during cap-clamped AP payment. Burn behavior remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates AP-use message amount `600`, matching actual capped removed delta. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates rank packet emission after cap-clamped direct AP payment. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates charge update packet remains emitted after cap-clamped AP payment. Update-mask byte parity remains unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleChargeItemAsync_ApPaymentHonorsConfiguredAbyssPointCapClamp` | Regression | Java `ItemChargeService.processAPPayment`, `AbyssPointsService.addAp`, `AbyssRank.addAp`, and `CustomConfig` source review | Validates direct AP conditioning payment with cap enabled clamps AP `1600` to `1000`, persists capped rank, sends AP-use amount `600`, charges the item, and emits rank/charge/stats/complete packets. | Deterministic C# connection-level regression grounded in Java source. | No Java runtime artifact; no byte-level packet comparison; no charge-all cap-specific regression. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Charge-all AP cap flow is still covered through shared planner/option wiring and non-cap charge-all AP regression, not a dedicated cap-specific integration test.
- Remaining AP callers in Trade, Quest, PvP, NPC reward, item purification, and distribution systems still need convergence through `AbyssPointsService`.
- Daily/weekly AP reset behavior, persistent-state flags, full Legion contribution fanout, ranking cache, and siege callback execution remain incomplete.
- Packet bytes were not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported: 0 new production gameplay artifacts; 1 direct charge AP cap integration regression plus 1 test-helper persistence capture added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, charge-all AP cap integration artifact, remaining AP caller convergence, persistent-state/reset timing, full Legion contribution fanout, ranking cache/siege execution, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue AP caller convergence by inspecting C# coverage for Java `TradeService`, `QuestService`, `PvpService`, `NpcController`, and `ItemPurificationService` AP usages; wire the smallest already-ported AP reward/spend path through `AbyssPointsService` if one is compact.

If AP caller work remains too broad, add a dedicated charge-all AP cap integration regression or continue isolated Legion domain groundwork.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| AP caller coverage analysis | read-only Java/C# search | Yes | Multiple Java AP caller families can be analyzed independently without writes. |
| Charge-all AP cap integration regression | AP charge-all tests and shared fixture | Maybe | Safe if test-only and no production AP/rank changes run in parallel. |
| Legion domain analysis | Java Legion classes, C# model search | Yes if read-only | Prepare future Legion aggregate work without touching packet files. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Still tooling-blocked locally. |
| Independent non-AP gameplay slice | isolated files only | Maybe | Safe if it avoids AP/Legion/decompose/emotion files and shared docs until final bookkeeping. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `GameServerConnection.cs` or shared AP services.
- Charge/direct/charge-all AP cap fixture edits with AP production-rank edits unless one owner controls both.
- Legion packet edits with Legion domain edits unless one owner controls both.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, inspect remaining AP callers and wire the smallest already-ported AP path through `AbyssPointsService`, add charge-all AP cap integration coverage, continue isolated Legion groundwork, or choose another isolated gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
