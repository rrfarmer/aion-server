# Phase 6 Session 1828 Handoff - Craft Cooldown Persistence Planning

Date: 2026-05-31
Unit of Work: UOW-1828
Status: Completed

## What Changed

- Added exact Java SQL constants for `CraftCooldownsDAO` craft cooldown persistence.
- Added disabled persistence descriptors for:
  - delete all craft cooldown rows for the player
  - insert each active cooldown row
- Added a disabled adapter plan that records the SQL execution boundary without opening a connection or executing SQL.
- Preserved Java ordering: delete first, then insert active cooldowns.
- Preserved Java active filter: skip only entries where `reuseTime < currentTimeMillis`.
- Confirmed discovery finding: C# loads craft cooldowns on enter-world, but live logout save still does not persist craft cooldowns.
- All behavior remains non-live and disabled by default.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 327 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4579 tests.

## Known Gaps

- No live craft cooldown DB writes occur.
- `SavePlayerLogoutAsync` still does not save `Player.CraftCooldowns`.
- Java craft cooldown persistence opens separate connections for delete and each insert; no live connection/error behavior is implemented.
- No finish-time cooldown packet/fanout is sent.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: integrate the disabled craft cooldown persistence descriptor/adapter into a non-live finish-craft cooldown composition plan so application projection and persistence intent can be inspected together without wiring live logout/database writes.

Safe alternative candidates:

- Add a disabled `SM_RECIPE_COOLDOWN` finish-time packet/fanout plan if Java evidence confirms packet dispatch timing.
- Add disabled finish-craft skill XP/common XP application planning from Java `finishCrafting`.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.finishCrafting`, `Cooldowns.put`, `CraftCooldownsDAO.storeCraftCooldowns`, and the new C# cooldown application/persistence plans.
- Keep the next unit non-live unless explicitly scoping and verifying live database writes and rollback/error behavior.
