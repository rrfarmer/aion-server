# Phase 6 Session 1805 Handoff - Craft Skill Validation Guards

Date: 2026-05-31
Unit of Work: UOW-1805
Status: Completed

## What Changed

- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with skill presence and skillpoint guards from Java `CraftService.checkCraft`.
- Added optional `SkillTemplateTable` lookup so failure messages can carry the Java skill l10n parameter.
- Added `CraftStartValidationStatus.MissingCraftSkill` and `CraftStartValidationStatus.CraftSkillTooLow`.
- Added `RequiredSkillPoint` and `CurrentSkillLevel` to `CraftStartValidationPlan`.
- Added `SmSystemMessage.CombineCantUse()` and `SmSystemMessage.CombineOutOfSkillPoint()`.
- Added focused tests for guard ordering, level evidence, and message IDs.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1805-Completion.md`
- `docs/Phase-6-Session-1805-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.gameobjects.player.SkillList.isSkillPresent`
- `com.aionemu.gameserver.model.gameobjects.player.SkillList.getSkillLevel`
- `com.aionemu.gameserver.model.templates.skill.SkillTemplate.getL10n`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Services.CraftService`
- `Aion.GameServer.Services.CraftStartValidationPlan`
- `Aion.GameServer.Services.CraftStartValidationStatus`
- `Aion.GameServer.Dataholders.SkillTemplateTable`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.CraftServiceTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused craft/packet tests passed with 269 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4534 tests.

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` skill presence guard | `CraftService.CreateStartCraftingValidationPlan` `MissingCraftSkill` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Checks `Player.Skills` after cooldown guard and attaches message id `1330042`; live fanout remains pending. |
| `CraftService.checkCraft` skillpoint guard | `CraftService.CreateStartCraftingValidationPlan` `CraftSkillTooLow` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Compares `PlayerSkill.SkillLevel` against `RecipeTemplateSummary.SkillPoint` and attaches message id `1330044`; live fanout remains pending. |
| `SkillTemplate.getL10n` message parameter | `SkillTemplateSummary.GetClientName` through `CraftService` | Static Data Parameter | Partial | Unit Tested | Partial Parity | Encoded l10n is used when skill template data is provided; missing table/template falls back to an empty string. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_CANT_USE` | `SmSystemMessage.CombineCantUse` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330042` verified. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_OUT_OF_SKILL_POINT` | `SmSystemMessage.CombineOutOfSkillPoint` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330044` verified. |

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No live failure packet or cancel packet sending.
- Material validation/consumption, bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- Recipe components and max-production fields are not yet projected.
- Missing skill template data is not yet treated as a hard static-data error.

## Risks

- Material validation requires recipe component projection before it can be responsibly modeled.
- Live failure fanout must preserve Java ordering: failing guard system message, then `sendCancelCraft` from `startCrafting`.
- The current planner can prove guard order and packet ids, but not live craft runtime parity.

## Next Recommended Unit of Work

- Next sequential task: port recipe component projection and material validation planning from `CraftService.checkCraft`, while still avoiding inventory mutation and scheduler startup.

Safe alternative candidates:

- Wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending.
- Start recipe max-production/static-data projection needed by later craft result planning.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.checkCraft`, `RecipeTemplate.getComponent`, Java inventory material checks, and the C# static recipe data surface.
- Keep inventory mutation out of scope until material planning and failure fanout are explicit.
