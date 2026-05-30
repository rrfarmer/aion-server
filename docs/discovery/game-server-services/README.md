# Game-Server Services Discovery Index

Date: 2026-05-29

## Purpose

This directory tracks high-level Java-to-C# discovery for the Java game-server service surface.

The organizing rule is Java-first:

- one document per Java top-level service file under `game-server/src/com/aionemu/gameserver/services`
- one document per Java services subpackage under `game-server/src/com/aionemu/gameserver/services/*`

These documents are discovery-only. They do not claim runtime parity.

See also: [Completion Estimate](Completion-Estimate.md)
See also: [Parity-Risk Ownership Trace](Parity-Risk-Ownership-Trace.md)

## Status Legend

- `Present`: a close C# service counterpart is obvious.
- `Partial`: some C# coverage is obvious, but parity is incomplete or unclear.
- `Refactored`: the Java area appears present, but spread across multiple C# services.
- `Not obvious`: no clear C# counterpart is obvious from the current C# game-server service surface.

## Package Areas

- [abyss](packages/abyss.md)
- [antihack](packages/antihack.md)
- [autogroup](packages/autogroup.md)
- [ban](packages/ban.md)
- [conquerorAndProtectorSystem](packages/conquerorAndProtectorSystem.md)
- [craft](packages/craft.md)
- [cron](packages/cron.md)
- [drop](packages/drop.md)
- [event](packages/event.md)
- [findgroup](packages/findgroup.md)
- [instance](packages/instance.md)
- [item](packages/item.md)
- [mail](packages/mail.md)
- [panesterra](packages/panesterra.md)
- [player](packages/player.md)
- [reward](packages/reward.md)
- [rift](packages/rift.md)
- [siege](packages/siege.md)
- [summons](packages/summons.md)
- [teleport](packages/teleport.md)
- [toypet](packages/toypet.md)
- [trade](packages/trade.md)
- [transfers](packages/transfers.md)
- [vortex](packages/vortex.md)
- [worldraid](packages/worldraid.md)

## Top-Level Service Areas

- [AccountService](top-level/AccountService.md)
- [AdminService](top-level/AdminService.md)
- [AnnouncementService](top-level/AnnouncementService.md)
- [ArmsfusionService](top-level/ArmsfusionService.md)
- [AtreianPassportService](top-level/AtreianPassportService.md)
- [AutoGroupService](top-level/AutoGroupService.md)
- [BaseService](top-level/BaseService.md)
- [BonusPackService](top-level/BonusPackService.md)
- [BrokerService](top-level/BrokerService.md)
- [ChallengeTaskService](top-level/ChallengeTaskService.md)
- [ClassChangeService](top-level/ClassChangeService.md)
- [CommandsAccessService](top-level/CommandsAccessService.md)
- [CronJobService](top-level/CronJobService.md)
- [CubeExpandService](top-level/CubeExpandService.md)
- [CuringZoneService](top-level/CuringZoneService.md)
- [DatabaseCleaningService](top-level/DatabaseCleaningService.md)
- [DebugService](top-level/DebugService.md)
- [DialogService](top-level/DialogService.md)
- [DuelService](top-level/DuelService.md)
- [EnchantService](top-level/EnchantService.md)
- [ExchangeService](top-level/ExchangeService.md)
- [FactionPackService](top-level/FactionPackService.md)
- [FlyRingService](top-level/FlyRingService.md)
- [GameTimeService](top-level/GameTimeService.md)
- [HousingBidService](top-level/HousingBidService.md)
- [HousingService](top-level/HousingService.md)
- [HTMLService](top-level/HTMLService.md)
- [KiskService](top-level/KiskService.md)
- [LegionDominionService](top-level/LegionDominionService.md)
- [LegionService](top-level/LegionService.md)
- [LifeStatsRestoreService](top-level/LifeStatsRestoreService.md)
- [LimitedItemTradeService](top-level/LimitedItemTradeService.md)
- [NameRestrictionService](top-level/NameRestrictionService.md)
- [NpcShoutsService](top-level/NpcShoutsService.md)
- [PeriodicSaveService](top-level/PeriodicSaveService.md)
- [PrivateStoreService](top-level/PrivateStoreService.md)
- [PunishmentService](top-level/PunishmentService.md)
- [PvpService](top-level/PvpService.md)
- [QuestService](top-level/QuestService.md)
- [RecipeService](top-level/RecipeService.md)
- [RepurchaseService](top-level/RepurchaseService.md)
- [RespawnService](top-level/RespawnService.md)
- [RiftService](top-level/RiftService.md)
- [RoadService](top-level/RoadService.md)
- [ShieldService](top-level/ShieldService.md)
- [SiegeService](top-level/SiegeService.md)
- [SkillLearnService](top-level/SkillLearnService.md)
- [SocialService](top-level/SocialService.md)
- [StaticDoorService](top-level/StaticDoorService.md)
- [StigmaService](top-level/StigmaService.md)
- [SurveyService](top-level/SurveyService.md)
- [TownService](top-level/TownService.md)
- [TradeService](top-level/TradeService.md)
- [TribeRelationService](top-level/TribeRelationService.md)
- [UpgradeArcadeService](top-level/UpgradeArcadeService.md)
- [VortexService](top-level/VortexService.md)
- [WarehouseService](top-level/WarehouseService.md)
- [WeatherService](top-level/WeatherService.md)
- [WorldRaidService](top-level/WorldRaidService.md)

## Working Notes

- Statuses are high-level and based on current checked-in names and structure.
- `Not obvious` means either missed or not yet ported; this index intentionally does not try to infer intent.
- The next detailed pass should start with `player`, `item`, `quest`, `teleport`, `siege`, and `worldraid`.

## At-a-Glance Signals

### Largest Refactor Zones

- `player`
- `item`
- `quest`
- `teleport`
- `dialog`
- `kisk`

### Highest-Signal Gaps

- `siege`
- `worldraid`
- `reward`
- `antihack`
- `transfers`
- `panesterra`

### Likely Mixed Areas

- `housing`
- `mail`
- `reward`
- `toypet`
- `trade`
- `pvp`
- `weather`