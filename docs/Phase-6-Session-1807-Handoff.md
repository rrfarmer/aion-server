# Phase 6 Session 1807 Handoff - Craft Bonus Item Guard Planning

Date: 2026-05-31
Unit of Work: UOW-1807
Status: Completed

## What Changed

- Added planner-level `craftType` handling for the Java bonus craft item guard.
- Ported `CraftService.getBonusReqItem(skillId)` mapping.
- Added `CraftStartValidationStatus.MissingBonusItem`.
- Added tests for material-before-bonus ordering, missing bonus item failure, and ready continuation when the bonus item exists.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused craft/packet tests passed with 272 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4537 tests.

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No live system-message or cancel packet sending.
- No material or bonus item consumption.
- No DP spend/task scheduling path.
- Live CM_CRAFT parsing still does not feed selected materials or craft type into this planner.

## Next Recommended Unit of Work

- Next sequential task: port non-live validation failure orchestration that combines `FailurePacket` and `CreateStartCancelPacketPlan(...)` outputs in Java order, still without live sending.

Safe alternative candidates:

- Start material and bonus item consumption planning.
- Start live CM_CRAFT selected-material/craft-type adapter work.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.startCrafting`, `checkCraft`, and `sendCancelCraft` ordering.
- Keep live packet sending out of scope until the non-live orchestration shape is tested.
