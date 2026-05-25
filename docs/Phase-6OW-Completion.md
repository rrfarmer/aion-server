# Phase 6OW Completion Handoff - AP Cap Planner Wiring

Date: May 25, 2026
Unit of Work: UOW-901
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-901] Apply AP cap in abyss planner`)

## Status

Phase 6 is still in progress. This unit closes the documented Java AP cap gap for planner-backed AP callers by wiring `gameserver.enable.ap.cap` and `gameserver.ap.cap.value` into the shared AP planner path.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerAbyssRank.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AbyssPointsService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ApExtractService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AbyssPointsServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemChargeServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OW-Completion.md`

## What Changed

- Reviewed Java:
  - `com.aionemu.gameserver.model.gameobjects.player.AbyssRank.addAp`
  - `com.aionemu.gameserver.configs.main.CustomConfig`
  - `com.aionemu.gameserver.services.abyss.AbyssPointsService`
- Updated `PlayerAbyssRank.AddAp` with optional Java AP cap behavior.
- Extended `AbyssPointsAddOptions` with `EnableApCap` and `ApCapValue`.
- Routed AP cap options through:
  - AP extraction
  - direct conditioning AP payment
  - charge-all conditioning AP payment
- Added tests for near-cap gain and Java's above-cap clamp behavior.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~AbyssPointsServiceTests --no-restore
```

Result: passed, 8 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ItemChargeServiceTests --no-restore
```

Result: passed, 8 tests after rerunning sequentially. The first parallel focused run hit a shared build-output lock.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1471 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Unit Tested | Partial Parity | `AddAp` now supports Java AP cap math, including the above-cap positive-gain clamp to cap. Daily/weekly AP still increment by the original positive amount like Java. GP rank thresholds, reset timing, persistent state, and runtime comparison remain unverified. |
| `com.aionemu.gameserver.configs.main.CustomConfig` | `Aion.GameServer.Configuration.GameServerCustomOptions` | Configuration | Partial | Existing Config Tested / Unit Tested through consumers | Needs Verification | C# already loaded the AP cap keys; this unit wires them into planner-backed AP callers. No new config precedence test was added. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit Tested | Partial Parity | Planner accepts cap options and reports actual post-cap delta. Full Legion, siege execution, large-AP logging, and runtime comparison remain missing. |
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` / `GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Handler | Partial | Existing Regression Tested | Needs Verification | AP extraction receives loaded AP cap options via connection path. No cap-enabled AP extraction regression was added. |
| `com.aionemu.gameserver.services.item.ItemChargeService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` / `HandleChargeAllQuestionResponseAsync` | Service / Handler | Partial | Existing Regression Tested | Needs Verification | Direct and charge-all AP payment paths pass cap options into the planner. Negative AP payments are usually unaffected unless Java cap condition clamps an above-cap value. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Unit Tested through planner | Needs Verification | AP gain message amount now uses capped actual delta in planner test. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Unit Tested through planner | Needs Verification | Planner emits rank packet when capped AP changes. No Java byte comparison. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerAbyssRank_AddApAppliesJavaCapAndAboveCapClamp` | Unit | Java `AbyssRank.addAp` and `CustomConfig` source review | Cap-limited gain to `1000`, daily/weekly increment by original positive AP, and above-cap clamp from `1500` to `1000`. | Deterministic C# unit test grounded in Java source. | No Java runtime artifact; persistent-state and reset timing are not represented. |
| `AddAp_UsesJavaApCapOptionsForAppliedAmount` | Unit | Java `AbyssPointsService.addAp` plus `AbyssRank.addAp` source review | AP planner applies cap options, reports actual `Added=100`, mutates AP to cap, and emits AP gain/rank packets. | Deterministic C# unit test grounded in Java source. | No byte-level Java comparison; no live config-loading assertion in this unit. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No AP extraction or conditioning integration regression explicitly enables AP cap.
- Java `AbyssRank` persistent-state changes are not modeled in `PlayerAbyssRank`.
- Java daily/weekly AP reset behavior and `lastUpdate` timing remain outside this slice.
- Full Legion and siege execution remain incomplete for broader AP callers.
- Packet bytes were not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 AP cap rank-math refinement plus AP option wiring for 3 existing AP caller paths
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, cap-enabled caller integration artifacts, persistent-state modeling, daily/weekly reset timing, full Legion/siege execution for other AP callers, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue AP caller convergence by inspecting C# coverage for Java `TradeService`, `QuestService`, `PvpService`, `NpcController`, and `ItemPurificationService` AP usages; wire the smallest already-ported AP reward/spend path through `AbyssPointsService`.

If no compact AP caller is available, choose isolated packet work such as remaining `SM_LEGION_EDIT` edit types.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| AP caller coverage analysis | read-only Java/C# search | Yes | Multiple Java AP caller families can be analyzed independently without writes. |
| Remaining `SM_LEGION_EDIT` packet types | `SmLegionEdit.cs`, packet tests | Yes if no AP caller wiring is active | Separate packet-focused unit. |
| AP cap integration regression | AP extraction or charge test fixture | Maybe | Safe as test-only if no production AP/rank changes run in parallel. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Still tooling-blocked locally. |
| Independent non-AP gameplay slice | isolated files only | Maybe | Safe if it avoids AP/decompose/emotion files and shared docs until final bookkeeping. |

## Do Not Parallelize

- AP rank math/planner changes with AP caller wiring unless one owner controls both.
- Multiple AP caller wiring tasks touching `GameServerConnection.cs`.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, inspect remaining AP callers and wire the smallest already-ported AP path through `AbyssPointsService`, or choose isolated packet work.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
