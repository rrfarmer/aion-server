# Phase 6 Session 1808 Completion - Add Craft Failure Orchestration Plan

Date: 2026-05-31
Unit of Work: UOW-1808
Status: Complete

## Scope

Port the non-live failure orchestration shape for Java `CraftService.startCrafting`: when `checkCraft(...)` fails, any failure system message emitted during `checkCraft` is ordered before the `sendCancelCraft(...)` update/animation packets. This unit intentionally does not send packets live, mutate inventory, spend DP, start scheduler work, or complete crafting.

## Completed Work

- Added `CraftService.CreateStartFailureOrchestrationPlan(...)`.
- Added `CraftStartFailureOrchestrationPlan` and `CraftStartFailureOrchestrationStatus`.
- Composed existing `CraftStartValidationPlan.FailurePacket` and `CraftStartCancelPacketPlan` packets into Java order:
  - optional `checkCraft` failure packet
  - `SM_CRAFT_UPDATE` cancel packet
  - `SM_CRAFT_ANIMATION` cancel broadcast packet
- Added a `CancelNotPlanned` status for validation failures where `sendCancelCraft` prerequisites are unavailable.
- Added focused tests for failure-message-before-cancel ordering, audit-only failures, ready validation no-op behavior, and missing cancel prerequisites.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 276 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4541 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.services.craft.CraftService.sendCancelCraft`

## Migration Parity Table - UOW-1808

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` failure branch | `CraftService.CreateStartFailureOrchestrationPlan` | Orchestration Planner | Partial | Unit Tested | Partial Parity | C# composes non-live packet order for `checkCraft` failure then `sendCancelCraft`; no live sending occurs. |
| `CraftService.checkCraft` failure packet ordering | `CraftStartFailureOrchestrationPlan.OrderedPackets` | Packet Ordering | Partial | Unit Tested | Partial Parity | Tests prove failure packet precedes cancel update/animation when present; failures without system messages plan cancel packets only. |
| `CraftService.sendCancelCraft` reuse | `CraftStartCancelPacketPlan` through orchestration planner | Packet Planner | Partial | Unit Tested | Partial Parity | Existing cancel update/animation packet plan is reused; live broadcast/send remains pending. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No live packet sending or broadcasting.
- Validation, cancellation, material/bonus consumption, DP spend, task interval, scheduler startup, and craft completion are still separate planner surfaces.
- `CancelNotPlanned` documents C# planner prerequisite gaps; Java may still attempt `sendCancelCraft` and fail later for null prerequisites.

## Next Recommended Unit of Work

- Start material and bonus item consumption planning for the successful `checkCraft` path, still without mutating live player inventory.
- Safe alternatives:
  - start live CM_CRAFT selected-material/craft-type adapter work
  - add start-craft success task interval planning
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1808-Completion.md`
- `docs/Phase-6-Session-1808-Handoff.md`
