# Phase 6QD Completion Handoff - ItemPurification Concrete Packet Send Adapter

Date: May 25, 2026
Unit of Work: UOW-934
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-934] Add ItemPurification concrete packet send adapter`)

## Status

Phase 6 is still in progress. This unit adds a concrete-packet-only send adapter for the ItemPurification packet plan while deliberately avoiding live handler wiring, inventory/AP mutation, cube packet synthesis, and persistence.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketSendAdapter.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QD-Completion.md`

## What Changed

- Added `ItemPurificationPacketSendAdapter`.
- The adapter accepts a ready `ItemPurificationPacketPlan`, sends only non-null `ConcretePacket` operations through `IGameClientConnectionRegistry.SendPacketToPlayerAsync`, preserves plan order, and reports skipped metadata-only operations.
- Missing, not-ready, and empty packet plans are rejected without sending anything.
- `GameServerConnection` was intentionally left untouched.
- Cube-size, AP, and Kinah operations remain metadata-only and skipped by the adapter.
- Kept object-id allocation, live storage mutation, AP mutation, repository persistence, cube counts, quest hooks, and connection handler integration out of scope.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Concrete-packet-only send adapter | `PacketSendUtility.sendPacket`, ItemPurification packet fanout sites | new adapter service, focused tests | Integration Fix | Write sequential | Medium | Completed by orchestrator; isolated from `GameServerConnection`. |
| B | Direct live handler wiring | `CM_ITEM_PURIFICATION.runImpl`, `ItemPurificationService` | `GameServerConnection.cs` | Integration Fix | No | High | Deferred because it crosses into mutation, allocation, AP side effects, and persistence. |
| C | Cube update prerequisites | `SM_CUBE_UPDATE.cubeSize`, storage/cube models | read-only | Java/C# Analysis | Yes | Medium | Completed by read-only explorer; cube needs per-operation post-mutation counts/expansion fields. |
| D | ItemCharge AP hardening fallback | AP spend caller classes | separate service/tests | Later | Medium | Independent fallback if live fanout remains too broad. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationPacketPlanServiceTests
```

Result: passed, 9 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1597 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.ItemPurificationPacketSendAdapter.SendConcretePacketsAsync` | Packet Send Adapter | Partial | Regression Tested in C# | Partial Parity | C# now has a send seam for concrete packet-plan operations only. It preserves plan order and uses the existing connection registry send abstraction, but it is not wired to live `CM_ITEM_PURIFICATION` handling and does not synthesize metadata-only packets. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `ItemPurificationPacketSendAdapter` over `SmSystemMessage.ItemUpgradeSuccess` | Service Message Send Boundary | Partial | Regression Tested in C# | Partial Parity | The adapter can send the concrete success system message when present in a ready packet plan. Live l10n lookup, validation failure packets, and Java runtime packet capture remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `ItemPurificationPacketSendAdapter` over `SmInventoryUpdateItem` | Inventory Update Send Boundary | Partial | Regression Tested in C# | Partial Parity | Concrete update packets are sent only when already materialized by caller-provided post-mutation snapshots. Full payload parity, live mutation timing, and Java runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `ItemPurificationPacketSendAdapter` over `SmDeleteItem` | Inventory Delete Send Boundary | Partial | Regression Tested in C# | Partial Parity | Concrete delete packets are sent in plan order when present. Cube follow-up operations are skipped as metadata-only, so the Java delete-then-cube fanout remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `ItemPurificationPacketSendAdapter` over `SmInventoryAddItem` | Inventory Add Send Boundary | Partial | Regression Tested in C# | Partial Parity | Concrete add packets are sent in plan order when present. Cube follow-up operations, target `Storage.add`, quest hooks, and persistence remain out of scope. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `ItemPurificationPacketOperationType.CubeSizeUpdate` skipped by adapter | Packet Intent / Cube Size | Partial | Regression Tested in C# adapter | Needs Verification | Adapter explicitly skips cube metadata operations. Java needs per-operation post-mutation storage counts and expansion fields, and C# cube counting needs normalization before concrete cube parity is safe. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `ItemPurificationPacketOperationType.AbyssPointsUpdate` skipped by adapter | AP Packet Placeholder | Not Started for live AP packets | Regression Tested in C# adapter | Needs Verification | AP operation remains metadata-only and is not sent or mutated. AP/rank/legion/siege side effects still require a dedicated runtime boundary. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SendConcretePacketsAsync_SendsConcretePacketsInPlanOrderAndSkipsMetadata` | Regression | Java `PacketSendUtility.sendPacket`, `ItemPurificationService`, and `ItemPacketService` source review | Validates concrete success/update/delete/add packets are sent through the registry in packet-plan order and AP/Kinah/cube metadata operations are skipped. | Deterministic C# regression grounded in Java fanout order, without live mutation. | Does not wire the live handler or compare Java runtime packets. |
| `SendConcretePacketsAsync_RejectsPacketPlanThatStillNeedsRuntimeInputs` | Regression | C# runtime-input blockers plus Java fanout side-effect review | Validates the adapter sends nothing when the packet plan is not ready, preventing partial client-visible fanout from incomplete plans. | Deterministic safety regression for the C# adapter boundary. | Java may have already sent validation success before later runtime failures; full live error/rollback parity remains unmodeled. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The adapter is not wired into `GameServerConnection` and does not execute live ItemPurification.
- Sending concrete packets without corresponding live mutation would be semantically unsafe, so the adapter rejects not-ready plans and should only be used after a runtime boundary owns mutation/snapshots.
- Cube-size packets remain metadata-only; Java requires per-operation post-mutation storage counts plus expansion fields, and C# cube item counting still needs Java `ItemStorage` normalization.
- AP packet/rank/legion/siege fanout remains metadata-only.
- Object-id allocation, `Storage.add`, dirty-state persistence, `ItemStoneListDAO.save`, Kinah parity decision, quest hooks, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 narrow ItemPurification concrete-packet send adapter slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification mutation/emission, cube/AP concrete fanout, live object-id allocation/storage mutation, repository dirty-state persistence, and AP/rank side effects
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a small `ItemPurificationCubePacketInput` / `CubeUpdateSnapshot` prerequisite for cube-size metadata operations, capturing explicit per-operation cube fields (`itemsCount`, `npcExpands`, `questExpands`, `itemExpands`) without reading live storage, then bridge `SmCubeUpdate` only when those snapshots are supplied.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Cube snapshot input design | read-only Java/C# cube/storage files | Medium | Confirm object-id or operation-index keying for per-operation snapshots. |
| B | ItemCharge AP hardening fallback | ItemCharge service/test files | Medium | Independent AP caller work if cube snapshot design expands too much. |
| C | Live-send integration audit | read-only `GameServerConnection` and purification planners | Medium | Later check only; do not wire live handler with cube/AP metadata still missing. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Inspect cube snapshot keying and payload fields | read-only Java/C# cube/storage files | edits, docs | Recommendation for cube input record and validation rules. |
| Agent B | Inspect current C# cube count semantics vs Java `ItemStorage.size` | read-only C# player/storage/capacity files | edits, docs | Count-normalization risk report. |
| Orchestrator | Add cube snapshot bridge only if keying is clear | packet plan service/tests, docs | live storage mutation, connection handler, AP mutation | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live handler edits with cube/AP packet construction.
- Concrete cube/AP packet emission with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add cube snapshot prerequisites only if per-operation keying is clear, or choose another AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
