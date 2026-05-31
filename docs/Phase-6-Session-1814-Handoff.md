# Phase 6 Session 1814 Handoff - Static-Data CM_CRAFT Ready-Path Coverage

Date: 2026-05-31
Unit of Work: UOW-1814
Status: Completed

## What Changed

- Added static-data-backed CM_CRAFT handler composition coverage.
- Loaded real `DataManager` static data in `GameServerConnectionCraftTests`.
- Used real morph recipe `155000001` from `recipe_templates.xml`.
- Verified real packet processing reaches `CmCraftStartCompositionPlanStatus.ReadyForDpSpendAndTaskStart`.
- Verified ready-path planner details:
  - component decrease for `152000901 x1`
  - morph interval `200`
  - DP requirement `200`
- Preserved no-live-side-effect behavior: no packets are sent, no DP is spent, no inventory is mutated, and no task is started.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 305 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4557 tests.

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No DP spend, inventory mutation, persistence, packet send, `CraftingTask` creation, or scheduler start.
- Non-morph static-object ready-path coverage remains pending.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: begin non-live inventory mutation planning for material/bonus consumption so the successful `checkCraft` consumption plan can be turned into explicit updated/deleted inventory item intents.

Safe alternative candidates:

- Add a live-safe craft finish cooldown application mutation plan.
- Broaden CM_CRAFT handler composition coverage for non-morph target facts when StaticObject modeling is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.checkCraft` material and bonus item decrease order and C# `CraftStartConsumptionPlan`.
- Keep the next unit non-live unless it explicitly scopes and verifies inventory mutation side effects.
