# Phase 6OY Completion Handoff - AP Extraction Cap Integration Regression

Date: May 25, 2026
Unit of Work: UOW-903
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-903] Cover AP extraction cap wiring`)

## Status

Phase 6 is still in progress. This unit adds a connection-level regression proving the AP extraction path honors Java AP cap options through the shared AP planner and hands the capped rank to the persistence boundary.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OY-Completion.md`

## What Changed

- Added `HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap`.
- Supplied `GameServerOptions.Custom.EnableApCap=true` and `ApCapValue=1000` to the AP extraction/use-item fixture.
- Updated the fixture so `GameServerConnection` and `PlayerEnterWorldService` share the supplied options.
- Added `EmptyPlayerEnterWorldRepository.ApExtractAbyssRank` capture for AP extraction persistence-boundary assertions.
- Verified AP extraction from AP `900` applies only capped delta `100`, reaches AP `1000`, sends the capped AP gain message, emits `SmAbyssRank`, deletes the target, and decreases the extraction tool.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 42 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1481 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Handler | Partial | Regression Tested in C# | Partial Parity | AP extraction now has connection-level cap-option coverage. Java runtime packet/artifact comparison remains unavailable. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit + Regression Tested in C# | Partial Parity | Planner cap support is exercised through AP extraction. Legion, siege, large-AP logging, and other AP callers remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Unit + Regression Tested in C# | Partial Parity | Regression validates AP `900 + 980` clamps to `1000` and reports delta `100`. Reset timing and persistent-state flags remain unmodeled. |
| `com.aionemu.gameserver.configs.main.CustomConfig` | `Aion.GameServer.Configuration.GameServerCustomOptions` / `GameServerOptions` | Configuration | Partial | Regression Tested through connection fixture options | Needs Verification | Test supplies Java-equivalent cap options directly; config loading was not newly tested. |
| `com.aionemu.gameserver.dao.AbyssRankDAO` / `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.PlayerEnterWorldRepository.SaveApExtractActionMutationAsync` / `EmptyPlayerEnterWorldRepository.ApExtractAbyssRank` | Repository Boundary / Test Support | Partial | Regression Tested through test double | Needs Verification | Test-helper capture verifies the capped rank passed to persistence. Live SQL transaction behavior remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates AP extraction gain message `1320000` uses capped delta `100`. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates rank packet emission after capped AP extraction. Ranking-position lookup and byte comparison remain unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression keeps Java-shaped target deletion in the cap-enabled path. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression keeps Java-shaped tool decrease in the cap-enabled path. Update-mask byte parity remains unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap` | Regression | Java `ApExtractAction.act`, `AbyssPointsService.addAp`, `AbyssRank.addAp`, and `CustomConfig` source review | Validates capped AP extraction delta, capped persistence rank, target deletion, tool decrease, gain message, and rank packet. | Deterministic C# connection-level regression grounded in Java source. | No Java runtime artifact; no byte-level packet comparison; no charge AP cap integration regression. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Charge AP cap flow is still covered through shared planner/unit coverage and option wiring, not a dedicated connection-level cap regression.
- Remaining AP callers in Trade, Quest, PvP, NPC reward, item purification, and distribution systems still need convergence through `AbyssPointsService`.
- Daily/weekly AP reset behavior, persistent-state flags, full Legion contribution fanout, ranking cache, and siege callback execution remain incomplete.
- Packet bytes were not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported: 0 new production gameplay artifacts; 1 AP extraction cap integration regression plus 1 test-helper persistence capture added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, charge AP cap integration artifact, remaining AP caller convergence, persistent-state/reset timing, full Legion contribution fanout, ranking cache/siege execution, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue AP caller convergence by inspecting C# coverage for Java `TradeService`, `QuestService`, `PvpService`, `NpcController`, and `ItemPurificationService` AP usages; wire the smallest already-ported AP reward/spend path through `AbyssPointsService` if one is compact.

If AP caller work remains too broad, add a dedicated charge AP cap integration regression or continue isolated Legion domain groundwork.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| AP caller coverage analysis | read-only Java/C# search | Yes | Multiple Java AP caller families can be analyzed independently without writes. |
| Charge AP cap integration regression | AP charge tests and shared fixture | Maybe | Safe if test-only and no production AP/rank changes run in parallel. |
| Legion domain analysis | Java Legion classes, C# model search | Yes if read-only | Prepare future Legion aggregate work without touching packet files. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Still tooling-blocked locally. |
| Independent non-AP gameplay slice | isolated files only | Maybe | Safe if it avoids AP/Legion/decompose/emotion files and shared docs until final bookkeeping. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `GameServerConnection.cs` or shared AP services.
- AP cap test fixture edits with AP production-rank edits unless one owner controls both.
- Legion packet edits with Legion domain edits unless one owner controls both.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, inspect remaining AP callers and wire the smallest already-ported AP path through `AbyssPointsService`, add charge AP cap integration coverage, continue isolated Legion groundwork, or choose another isolated gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
