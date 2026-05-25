# Phase 6QV Completion Handoff - ItemPurification Handler Opt-in Execution Seam

Date: May 25, 2026
Unit of Work: UOW-952
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-952] Expose item purification live execution seam`)

## Status

Phase 6 is still in progress. This unit exposes ItemPurification live execution through an explicit opt-in handler helper while keeping normal `CM_ITEM_PURIFICATION` packet dispatch non-mutating and plan-only.

The helper composes the existing planner, live execution service, live mutation adapter, and concrete packet send adapter. It is not automatically invoked from `HandleInfrastructurePacketAsync`. Repository persistence, transactions, quest notifications, AP side-effect execution, and Java runtime byte capture remain separate work.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QV-Completion.md`

## What Changed

- Added `GameServerConnection.HandleItemPurificationLiveExecutionAsync`.
- The helper:
  - resolves item templates from overrides or runtime static data
  - reuses `HandleItemPurificationAsync` for Java-shaped planning/allocation/random-bonus behavior
  - calls `ItemPurificationLiveExecutionService.ExecuteAsync`
  - accepts explicit cube expansion counters
  - accepts an optional connection-registry override for tests
- Normal `HandleInfrastructurePacketAsync` remains unchanged and still routes `CmItemPurification` to the plan-only handler.
- Added a handler regression that uses the opt-in helper to mutate inventory/AP and send concrete packets through a supplied registry.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Handler opt-in result seam | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | `GameServerConnection.cs`, handler tests | Integration Fix / Tests | No | Medium | Large shared handler file must be exclusive. |
| B | Persistence plan analysis | `Storage`, DAOs, `AbyssRankDAO`, inventory DAO paths | read-only | Java/C# Analysis | Yes | Low | Useful next step before automatic live handler execution. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate from ItemPurification files; deferred. |
| D | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-952 implementation, tests, docs, commit | `GameServerConnection.cs`, `GameServerConnectionItemPurificationTests.cs`, progress/handoff docs | unrelated files | Code, tests, parity docs, commit |

No write sub-agents were spawned because the unit touched shared handler code.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationLiveExecutionServiceTests|ItemPurificationLiveMutationServiceTests|ItemPurificationMutationSnapshotServiceTests|ItemPurificationPacketInputSnapshotServiceTests|ItemPurificationPacketPlanServiceTests|AbyssPointsServiceTests"
```

Result: passed, 48 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1644 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationLiveExecutionAsync` | Client Handler / Opt-in Execution Seam | Partial | Regression Tested in C# | Partial Parity | C# handler now has an explicit opt-in helper that composes planning, live mutation, and packet fanout. The automatic packet-dispatch path remains plan-only and non-mutating until persistence/side effects are ready. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationLiveExecutionService` via handler opt-in seam | Service / Success Message Fanout | Partial | Regression Tested in C# | Partial Parity | Handler opt-in seam reaches the success-message-first execution service. Java validation packet byte parity and runtime failure behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationLiveMutationService` via handler opt-in seam | Service / Mutation Execution | Partial | Regression Tested in C# | Partial Parity | Opt-in handler execution applies generated inventory/AP mutation for ready plans. Java `Storage` persistent states, deleted item queue, quest callbacks, packet construction inside storage, transactions, and rollback remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationLiveMutationService` / `ItemPurificationInheritanceService` via handler opt-in seam | Service / Target Add Execution | Partial | Regression Tested in C# | Partial Parity | Target item add can now be reached from the handler helper. Full Java `ItemFactory`, `ItemSocketService`, storage add callbacks, persistence, and quest get notifications remain incomplete. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.ItemPurificationPacketSendAdapter` via handler opt-in seam | Packet Send Boundary | Partial | Regression Tested in C# | Needs Verification | Handler helper can send concrete packets through an injected registry. Socket/runtime ordering is fake-registry tested only; packet bytes and Java runtime captures remain unverified. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService` via handler opt-in seam | AP Spend Boundary | Partial | Regression Tested in C# | Partial Parity | AP spend is reached through handler opt-in execution. Java AP rank side-effect execution, rank-limit equipment, abyss skill updates, Legion/Siege hooks, and persistence remain deferred. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleItemPurificationLiveExecutionAsync_ReturnsLiveExecutionResultWhenExplicitlyRequested` | Regression | Java `CM_ITEM_PURIFICATION.runImpl` plus ItemPurification service source review | Validates explicit handler helper returns ready live execution, mutates inventory/AP, preserves kinah no-op, and sends concrete success/update/delete/cube/add/cube packets. | Deterministic C# handler regression for opt-in live execution composition. | Does not persist, execute quest/AP side effects, compare packet bytes, or change automatic production dispatch. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` packet dispatch still uses the plan-only handler path and does not invoke live execution.
- Persistence/transaction writes, rollback behavior, quest notifications, AP rank side-effect execution, Legion/Siege callbacks, rank-limit equipment checks, and real storage expansion state are still not executed.
- C# inventory mutation still replaces copied snapshots rather than mutating Java `Storage`/`ItemStorage`; persistent flags, deleted item queue, storage capacity, locks/threading, and collection ordering remain unverified.
- Kinah remains intentionally preserved because Java's `decreaseKinah(-necessaryKinah)` path is a no-op under the `amount > 0` guard; no runtime capture has confirmed production behavior.
- Packet send ordering is tested through fake registries, not Java runtime sockets or packet byte golden files.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 ItemPurification handler opt-in live execution seam
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, persistence/transaction writes, automatic live handler invocation, quest/AP side-effect execution, and packet-byte/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Perform ItemPurification persistence plan analysis before enabling automatic live handler execution.

Suggested shape:
- Read Java `Storage`, `ItemStorage`, inventory DAO, abyss rank DAO, `ItemPurificationService`, and current C# repository surfaces.
- Identify exactly which inventory item updates/deletes/adds and AP rank writes need one transaction.
- Produce a small C# plan/service interface if isolated, or a docs-only analysis if repository surfaces are too broad for one safe UOW.
- Keep automatic live handler send disabled until persistence and side effects are safe.

Do not combine with:
- automatic live packet sending from `HandleInfrastructurePacketAsync`
- broad repository refactors
- quest notifications
- AP rank side-effect execution
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Persistence plan analysis for ItemPurification inventory/AP writes | read-only Java/C# repository inspection or one new docs file | Low | Good next step before transactions. |
| B | Narrow repository interface sketch for ItemPurification transaction | new service/interface/test files only if existing repo contracts are clear | Medium | Avoid changing shared repositories unless scoped. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| D | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionItemPurificationTests.cs`.
- Multiple agents changing shared repository/transaction files.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose persistence plan analysis, a narrow repository interface sketch, or isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
