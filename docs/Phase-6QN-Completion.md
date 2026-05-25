# Phase 6QN Completion Handoff - ItemPurification Runtime Input Bridge

Date: May 25, 2026
Unit of Work: UOW-944
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-944] Bridge item purification packet inputs`)

## Status

Phase 6 is still in progress. This unit adds a pure ItemPurification bridge from handler-level application plans plus post-mutation snapshots into concrete packet-plan inputs.

The handler still does not allocate target object ids, mutate inventory/AP, persist changes, or send packets. This unit intentionally stops at concrete packet plan assembly from caller-provided snapshots.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationHandlerPacketBridgeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QN-Completion.md`

## What Changed

- Added `ItemPurificationHandlerPacketBridgeService.CreateConcretePacketPlan`.
- The bridge accepts:
  - a ready `ItemPurificationHandlerPlan`
  - post-mutation inventory snapshots
  - item templates
  - cube snapshots keyed by packet operation index
- It delegates snapshot assembly to `ItemPurificationPacketInputSnapshotService.CreateInputs`.
- It then rebuilds a concrete `ItemPurificationPacketPlan` with inventory and cube packet inputs while preserving the success-message parameters from the handler packet plan.
- Added a handler-level regression proving concrete success, material update, base delete, cube update, target add, and cube update packet types are produced in Java-like order while AP/Kinah remain metadata-only.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | ItemPurification runtime-input bridge | `CM_ITEM_PURIFICATION`, `ItemPurificationService.decreaseMaterials`, `upgradeItem` | `ItemPurificationHandlerPacketBridgeService.cs`, `GameServerConnectionItemPurificationTests.cs` | Utility / Test Creation | No | Medium | Completed sequentially because it consumes shared handler test fixtures and existing packet-input services. |
| B | Packet-input snapshot audit | same packet fanout artifacts | read-only service/tests | Analysis | Yes | Low | Covered locally during implementation; no edits needed. |
| C | Kinah charge-all partial drift | ItemCharge charge-all Kinah path | charge handler tests | Test Creation | Yes, separate file from ItemPurification | Medium | Deferred. |
| D | Java runtime observer design | ItemPurification packet runtime path | docs only | Documentation | Yes | Low | Deferred until Java tooling is available or a design note becomes the next best slice. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationPacketInputSnapshotServiceTests|ItemPurificationPacketPlanServiceTests"
```

Result: passed, 23 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1623 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` plus `ItemPurificationHandlerPacketBridgeService` | Client Handler / Adapter | Partial | Regression Tested in C# | Partial Parity | Handler output can now be bridged into concrete packet-plan inputs when supplied with post-mutation snapshots. The handler still does not allocate ids, mutate, persist, or send. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationPacketPlanService` via handler bridge | Service / Validation Packet Order | Partial | Regression Tested in C# | Partial Parity | Bridge preserves upgrade-success system message before mutation packet fanout. Java runtime packet bytes remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPacketInputSnapshotService` and `ItemPurificationHandlerPacketBridgeService` | Service / Runtime Snapshot Bridge | Partial | Regression Tested in C# | Partial Parity | Post-mutation material update, base delete, and target add snapshots produce concrete packet inputs. Runtime mutation/persistence is still not executed here. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationInheritanceService` and `ItemPurificationHandlerPacketBridgeService` | Service / Target Item Packet Bridge | Partial | Regression Tested in C# | Partial Parity | Target add packet can be concretized from an injected target object id and post-mutation target item snapshot. Full Java `ItemFactory`, object-id allocation, socket/godstone/fusion persistence, and random bonus selection remain incomplete. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.ItemPurificationApplicationPlanService` / packet metadata | AP Spend Planner | Partial | Regression Tested in C# | Needs Verification | AP spend remains metadata in this bridge; concrete AP rank packets and live rank side effects are still deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE`, `SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`, `SmDeleteItem`, `SmCubeUpdate`, `SmInventoryAddItem` | Packet DTOs | Partial | Regression Tested in C# | Needs Verification | Bridge test proves concrete packet types appear in Java-like order for supplied snapshots. Byte-level Java comparison remains blocked. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ItemPurificationHandlerPacketBridge_ComposesConcretePacketsFromPostMutationSnapshots` | Regression | Java `ItemPurificationService.isPurificationAllowed` success message before `decreaseMaterials` / `upgradeItem` source review | Validates handler plan plus post-mutation snapshots produce concrete success, material update, base delete, cube, target add, and cube packets, with AP/Kinah metadata skipped until live packet support exists. | Deterministic C# bridge regression for Java packet-order planning. | Does not execute Java runtime, mutate inventory/AP, persist, send packets, allocate object ids, or compare packet bytes. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- ItemPurification still lacks live mutation, persistence, object-id allocation, random bonus selection, AP rank packet fanout, and actual send invocation from the handler.
- The bridge requires caller-supplied post-mutation inventory/cube snapshots; live code still needs a repository/runtime mutation boundary to produce them safely.
- Java missing-base null behavior and negative-Kinah deduction quirk remain documented but not runtime-verified.
- Full Java `ItemFactory`, `ItemSocketService`, godstone/fusion/manastone persistence, random bonus rerolling, AP rank side effects, quest notifications, and packet byte parity remain incomplete.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 ItemPurification runtime-input packet bridge
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, target object-id allocation, live inventory/AP persistence, live packet sending, and full upgrade-item side-effect parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Continue ItemPurification live adapter readiness with either target object-id allocation for the handler application plan or a send-adapter bridge that sends only when a concrete packet plan is already ready and still avoids persistence claims.

Safe next shapes:
- Add a focused target-object-id allocation adapter if a stable C# object-id service boundary can be identified without touching broad startup/DI.
- Or add a focused handler/send adapter test that sends a ready concrete packet plan through `ItemPurificationPacketSendAdapter` and proves metadata-only AP/Kinah operations remain skipped.
- Or choose the isolated Kinah charge-all partial-drift regression in `GameServerConnectionInventoryExpansionUseItemTests.cs`.

Do not combine these:
- object-id allocation
- repository transaction persistence
- live inventory mutation
- AP rank side-effect packets
- random bonus selection
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Target object-id allocation boundary analysis | read-only ID factory/runtime files | Low | Good explorer task before production wiring. |
| B | Ready concrete packet send adapter test | ItemPurification packet/send tests | Medium | Keep separate from handler mutation work. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files. |
| D | Java ItemPurification runtime packet observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionItemPurificationTests.cs`.
- Multiple agents changing ItemPurification workflow/application/packet services in the same unit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue with target object-id allocation analysis/adapter, ready packet send adapter, or the isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
