# Phase 6OT Completion Handoff - AP Extraction Planner Wiring

Date: May 25, 2026
Unit of Work: UOW-898
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-898] Wire AP extraction through abyss planner`)

## Status

Phase 6 is still in progress. This unit wires the first real AP caller, AP extraction, through the shared C# `AbyssPointsService` planning boundary added in UOW-897.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/AbyssPointsService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ApExtractService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ApExtractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OT-Completion.md`

## What Changed

- Reviewed Java:
  - `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction`
  - `com.aionemu.gameserver.services.abyss.AbyssPointsService`
  - `com.aionemu.gameserver.model.gameobjects.player.AbyssRank`
  - `com.aionemu.gameserver.model.items.storage.Storage`
- Added `AbyssPointsService.CreateAddApPlan` for non-mutating, transaction-bound AP planning.
- Kept `AbyssPointsService.AddAp` mutating to match Java callers that can apply immediately.
- Updated `ApExtractService.CreateMutationPlan` to carry `AbyssPointsAddPlan`.
- Updated `GameServerConnection.HandleApExtractUseItemAsync` to send AP planner packets after persistence succeeds.
- Added AP extraction connection-level regression coverage.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ApExtractServiceTests --no-restore
```

Result: passed, 2 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 39 tests. A first parallel run collided on the shared build output; the sequential rerun passed.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1467 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Handler | Partial | Regression Tested in C# | Partial Parity | AP extraction now routes AP gain through the shared AP planner after DB mutation succeeds. Java audit logging on target-delete failure and Java runtime packet comparison remain missing. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit + Regression Tested in C# | Partial Parity | Added transaction-safe non-mutating planning while preserving mutating `AddAp`. Full legion execution, siege execution, AP cap config, large-AP logging, and runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Regression Tested through AP extraction and existing unit tests | Needs Verification | AP extraction persists and applies the planned rank update. Java AP-cap config is still absent. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveApExtractActionMutationAsync` / runtime inventory mutation | Repository / Runtime Mutation | Partial | Regression Tested in C# | Needs Verification | Target delete and extraction-tool decrement are preserved. C# plans first, persists, then mutates runtime state; Java mutates storage then calls AP service. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | AP extraction regression validates AP gain message id `1320000` and amount `980`. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression verifies rank packet emission after item-consumption packets. No Java byte comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRankUpdate` | Server Packet | Partial | Existing Unit/Regression Tested | Needs Verification | AP plans can carry rank-change intent and the connection still invokes existing side-effect handling. This unit does not add threshold-crossing AP extraction coverage. |
| `com.aionemu.gameserver.model.templates.item.Acquisition` | `Aion.GameServer.Dataholders.ItemTemplateSummary.RequiredAbyssPoints` | Static Data DTO | Partial | Regression Tested with fixture XML | Needs Verification | Fixture uses Java-shaped `<acquisition ap="4900" />`; broader loader behavior is covered elsewhere. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets` | Regression | Java `ApExtractAction.act` and `AbyssPointsService.addAp` source review | Target delete, tool decrement, AP update to `980`, AP gain message, and `SM_ABYSS_RANK` emission after persistence. | Deterministic C# connection-level regression grounded in Java source. | No Java runtime artifact; no legion contribution execution; no rank-threshold side-effect assertion. |
| `CreateMutationPlan_DeletesTargetConsumesToolAndAddsAbyssPoints` | Unit | Java `ApExtractAction.act` source review plus C# transaction boundary | Target/tool mutation planning, AP amount, AP plan amount, and non-mutating planning before persistence. | Deterministic C# unit test. | Non-mutating plan path is an intentional C# persistence-safety split. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `ApExtractAction` target-delete audit logging is unsupported.
- Full Legion contribution aggregate, persistence, and online-member broadcast remain unported.
- Active siege counter updates remain intent-only.
- Java AP-cap config is not represented in `PlayerAbyssRank.AddAp` or `AbyssPointsService`.
- Rank-threshold AP extraction fanout was not newly tested in this unit.
- Packet bytes were not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 partial AP extraction caller wiring plus 1 AP service transactional planning refinement
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, AP target-delete audit logging, full Legion contribution execution, active siege counters, Java AP cap config, rank-threshold AP extraction fanout, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Wire the AP planner into one additional compact AP caller that already mutates AP, preferably AP-based conditioning payment or another existing payment/reward surface, while preserving that caller's persistence boundary.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| AP conditioning payment planner wiring | `GameServerConnection.cs`, `PlayerEnterWorldService.cs`, `ItemChargeService` tests | No | Likely touches shared payment execution and AP persistence path; keep sequential. |
| AP cap config audit | `PlayerAbyssRank.cs`, config docs/tests | Yes if read-only | Useful before implementing cap support. |
| Remaining `SM_LEGION_EDIT` packet types | `SmLegionEdit.cs`, packet tests | Yes if no AP caller wiring is active | Keep separate from caller wiring. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Runtime capture should have one owner and still needs tooling. |
| Independent non-AP gameplay slice | isolated files only | Maybe | Safe if it avoids AP/decompose/emotion files and shared docs until final bookkeeping. |

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
5. If still tooling-blocked, wire one more compact AP caller through `AbyssPointsService` or choose another isolated gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
