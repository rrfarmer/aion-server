# Phase 6 Session 2732 Completion

## UOW

[Phase 6] UOW-2732: Honor Java legion warehouse enabled config.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live OPEN_LEGION_WAREHOUSE now rejects warehouse opens when `gameserver.legion.warehouse` is disabled.
- Java source/runtime path: LegionConfig.LEGION_WAREHOUSE, game-server/config/main/legions.properties, and LegionService.canOpenWarehouse config/unsupported-action branch.
- C# runtime artifact wired: GameServerOptions.LoadFromJavaConfig, GameServerLegionOptions, and GameServerConnection.HandleOpenLegionWarehouseDialogAsync.
- Client-visible/state/persistence effect: disabled C# config sends Java system-message id 1300279 from the live dialog handler before disbanding, permission, in-use, or open packets.
- Why this is runtime progress: it loads an existing Java config key into runtime C# options and sends a real server packet from a live client path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
  - `LEGION_WAREHOUSE` uses key `gameserver.legion.warehouse` with default `true`.
- `game-server/config/main/legions.properties`
  - Ships `gameserver.legion.warehouse = true`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `canOpenWarehouse` sends `STR_CANT_USE_GUILD_STORAGE` when `!LegionConfig.LEGION_WAREHOUSE || !npc.supportsAction(OPEN_LEGION_WAREHOUSE)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_CANT_USE_GUILD_STORAGE()` writes message id `1300279`.

## C# Changes

- Added `GameServerOptions.Legion` and `GameServerLegionOptions.WarehouseEnabled`.
- Loaded `gameserver.legion.warehouse` from Java config with default `true`.
- Updated live `OPEN_LEGION_WAREHOUSE` handling to send `SmSystemMessage.CantUseGuildStorage()` when the config is disabled.
- Added focused tests for default config loading, `mygs.properties` override behavior, and the disabled-config live packet branch.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` update | Unit / config loading | `LegionConfig.LEGION_WAREHOUSE` and `legions.properties` | Default Java config enables legion warehouse. | `GameServerOptions.LoadFromJavaConfig` reads repo Java config and returns `WarehouseEnabled == true`. | Does not test every `LegionConfig` property. |
| `LoadFromJavaConfig_AppliesMyGsOverridesLast` update | Unit / config override | Java config override order through `mygs.properties` | `mygs.properties` can disable `gameserver.legion.warehouse`. | Temporary Java-shaped config root proves override maps to runtime options. | No environment-variable override test for this key. |
| `HandleDialogSelectAsync_OpenLegionWarehouseDisabledByConfigSendsJavaDenial` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse` config-disabled branch | A member with rights and a valid warehouse NPC receives system-message id `1300279` when the option is disabled. | Socket-backed connection fixture and decoded `SmSystemMessage` payload from live handler. | No Java golden bytes. |

## Validation Decision

```text
- Changed surface: Java config loading plus live dialog packet dispatch.
- Specific behavior/contract: disabled `gameserver.legion.warehouse` maps to runtime C# options and causes OPEN_LEGION_WAREHOUSE to send Java message id 1300279 before later warehouse-open branches.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
- Result: passed; 316 tests passed.
- Focused Java/Maven command: not run; Java source and config were reviewed unchanged, and no narrow Java fixture exists for this config branch in the checkout.
- Broad-validation trigger: live config loading plus live dialog packet dispatch. Broad .NET was skipped because the focused command compiled the affected project and exercised the exact client-visible packet branch.
- Why this scope is sufficient: the command proves the option loader contract, the live denial packet, and the existing `SmSystemMessage.CantUseGuildStorage` packet id contract.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.LegionConfig.LEGION_WAREHOUSE` | `Aion.GameServer.Configuration.GameServerLegionOptions.WarehouseEnabled` | Config | Partial | Unit Tested | Partial Parity | The warehouse flag is loaded with Java key/default and `mygs.properties` override order; other `LegionConfig` values are not ported here. |
| `com.aionemu.gameserver.services.LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Membership, config-disabled, unsupported-action, disbanding, permission, in-use, same-user reopen, close release, logout release, and success branches are live. Full Java `LegionWarehouse` aggregate behavior remains incomplete. |
| `SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE` | `SmSystemMessage.CantUseGuildStorage` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300279` is covered by existing packet tests and live dispatch; no Java golden bytes. |

## Known Gaps

- C# still models legion warehouse contents through player `InventoryItems` location `3`, not a full Java `LegionWarehouse` aggregate.
- Legion warehouse capacity for item move/split into storage type `3` still appears incomplete.
- Exact Java golden bytes for `1300279` were not captured.
- Other Java `LegionConfig` properties remain unported unless already represented elsewhere.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Add Java-derived legion warehouse capacity checks to live item move/split into storage type `3`, matching `IStorage.getStorageIsFullMessage` and `LegionWarehouse.updateLimit`.
2. Persist Java-equivalent legion warehouse item state from live save/logout paths if the existing database shape and C# item ownership model can be safely wired.
3. Revisit legion leave/kick/member-removal only after a live CM_LEGION membership-removal path exists.
