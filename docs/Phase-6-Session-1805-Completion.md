# Phase 6 Session 1805 Completion - Add Craft Skill Validation Guards

Date: 2026-05-31
Unit of Work: UOW-1805
Status: Complete

## Scope

Port the next deterministic Java `CraftService.checkCraft` validation slice after recipe ownership and craft cooldown checks: craft skill presence and required skillpoint validation. This unit remains planner-level and intentionally stops before live failure fanout, material validation/consumption, bonus item consumption, DP spend, task interval calculation, scheduler startup, and craft completion.

## Completed Work

- Extended `CraftService` with optional `SkillTemplateTable` access for Java skill l10n message parameters.
- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with Java guard ordering for:
  - `!player.getSkillList().isSkillPresent(skillId)`
  - `player.getSkillList().getSkillLevel(skillId) < recipeTemplate.getSkillpoint()`
- Added `CraftStartValidationStatus.MissingCraftSkill` and `CraftStartValidationStatus.CraftSkillTooLow`.
- Added `RequiredSkillPoint` and `CurrentSkillLevel` evidence fields to `CraftStartValidationPlan`.
- Added `SmSystemMessage.CombineCantUse()` for Java message `1330042`.
- Added `SmSystemMessage.CombineOutOfSkillPoint()` for Java message `1330044`.
- Added focused tests proving cooldown-before-skill ordering, missing-skill behavior, low-skill behavior, sufficient-skill continuation, and exact system-message IDs.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 269 tests.
- Broad game-server suite excluding the previously order-sensitive `GameServerConnectionInventoryExpansionUseItemTests` passed with 4534 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.gameobjects.player.SkillList.isSkillPresent`
- `com.aionemu.gameserver.model.gameobjects.player.SkillList.getSkillLevel`
- `com.aionemu.gameserver.model.templates.skill.SkillTemplate.getL10n`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## Migration Parity Table - UOW-1805

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` skill presence guard | `CraftService.CreateStartCraftingValidationPlan` `MissingCraftSkill` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# checks `Player.Skills` after cooldown guard and attaches `STR_COMBINE_CANT_USE`. Live packet/cancel fanout remains pending. |
| `CraftService.checkCraft` skillpoint guard | `CraftService.CreateStartCraftingValidationPlan` `CraftSkillTooLow` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# compares `PlayerSkill.SkillLevel` to `RecipeTemplateSummary.SkillPoint` and attaches `STR_COMBINE_OUT_OF_SKILL_POINT`. Live packet/cancel fanout remains pending. |
| `SkillTemplate.getL10n` message parameter | `SkillTemplateSummary.GetClientName` through `CraftService` | Static Data Parameter | Partial | Unit Tested | Partial Parity | C# passes encoded skill l10n when `SkillTemplateTable` is available. Missing skill template data currently falls back to an empty string rather than throwing like Java might. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_CANT_USE` | `SmSystemMessage.CombineCantUse` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330042` verified through packet/system-message tests. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_OUT_OF_SKILL_POINT` | `SmSystemMessage.CombineOutOfSkillPoint` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330044` verified through packet/system-message tests. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateStartCraftingValidationPlan_RejectsMissingCraftSkillAfterCooldownValidation` | Unit Added | Java `checkCraft` cooldown guard before skill presence guard | Active cooldown wins before missing skill; learned recipe without skill produces message id `1330042`. | Source-derived planner regression plus system-message ID evidence. | Live send/cancel orchestration remains pending. |
| `CreateStartCraftingValidationPlan_RejectsLowCraftSkillAfterPresenceValidation` | Unit Added | Java `checkCraft` skill presence guard before skillpoint guard | Present skill below recipe skillpoint produces message id `1330044` and records current/required levels. | Source-derived planner regression plus system-message ID evidence. | No material validation or mutation in this unit. |
| `GamePacketTests` system-message assertions | Unit Updated | Java `SM_SYSTEM_MESSAGE` constants | New C# factories preserve Java message ids `1330042` and `1330044`. | Packet/system-message assertion evidence. | No runtime Java packet capture in this unit. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- `FailurePacket` remains planner evidence only; no live system-message or cancel packet fanout is wired.
- Material validation/consumption, bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- Recipe components and max-production recipe data are still not modeled in `RecipeTemplateSummary`.
- Missing C# skill template data falls back to an empty message parameter; a future data-integrated live path should decide whether to fail hard or guarantee the table is present.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 2 validation branches, 2 status values, 2 system-message factories, skill l10n parameter hookup, and focused test updates.
- Total artifacts with verified parity: 2 system-message factory rows.
- Total artifacts needing verification: 3 grouped validation/static-data rows pending live orchestration and broader recipe data.
- Total blocked artifacts: live start-craft execution, live validation failure fanout, material validation, DP spend, and scheduler startup.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the next smallest `CraftService.checkCraft` slice: recipe component projection and material validation planning, still without consuming inventory or starting a scheduler.
- Safe alternatives:
  - wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending
  - start recipe max-production/static-data projection needed by later material validation
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1805-Completion.md`
- `docs/Phase-6-Session-1805-Handoff.md`
