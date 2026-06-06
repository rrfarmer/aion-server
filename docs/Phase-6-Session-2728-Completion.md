# Phase 6 Session 2728 Completion

## UOW

[Phase 6] UOW-2728: Send cannot-use legion warehouse denial.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_DIALOG_SELECT OPEN_LEGION_WAREHOUSE now sends Java's cannot-use-legion-storage denial when a legion member targets an NPC that does not support the action.
- Java source/runtime path: LegionService.canOpenWarehouse(Player, Npc) `!npc.getObjectTemplate().supportsAction(DialogAction.OPEN_LEGION_WAREHOUSE)` -> SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE.
- C# runtime artifact wired: GameServerConnection.HandleOpenLegionWarehouseDialogAsync and SmSystemMessage.CantUseGuildStorage.
- Client-visible/state/persistence effect: real clients receive system-message id `1300279` from live dialog dispatch instead of a silent return.
- Why this is runtime progress: it sends a real server packet from a live client packet handler and preserves Java guard ordering.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `canOpenWarehouse` checks legion membership first.
  - For a legion member, `!LegionConfig.LEGION_WAREHOUSE || !npc.getObjectTemplate().supportsAction(DialogAction.OPEN_LEGION_WAREHOUSE)` sends `STR_CANT_USE_GUILD_STORAGE`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_CANT_USE_GUILD_STORAGE()` uses message id `1300279`.
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
  - `OPEN_LEGION_WAREHOUSE` delegates to `LegionService.openLegionWarehouse`.

## C# Changes

- Added `SmSystemMessage.CantUseGuildStorage()` with Java message id `1300279`.
- Updated `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` to preserve Java guard order:
  - invalid or unknown targets still return silently,
  - non-legion players still receive `STR_NO_GUILD_TO_DEPOSIT`,
  - legion members targeting an unsupported legion warehouse action now receive `STR_CANT_USE_GUILD_STORAGE`,
  - permission and success paths remain unchanged.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_OpenLegionWarehouseUnsupportedActionSendsJavaDenial` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse`, `SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE` source review | Live action `53` sends system-message id `1300279` for a legion member targeting an NPC without the open-legion-warehouse action. | Socket-backed connection fixture, live `HandleDialogSelectAsync`, decoded `SmSystemMessage` payload. | Does not cover config-disabled because no live C# `LegionConfig.LEGION_WAREHOUSE` equivalent is wired in this path. |
| `HandleDialogSelectAsync_OpenLegionWarehouseUnsupportedActionWithoutLegionKeepsJavaMembershipOrder` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse` source review | Unsupported target plus no legion still sends no-legion id `1300278`, matching Java guard order. | Socket-backed connection fixture and decoded system-message payload. | Does not exercise valid legion storage open. |
| `SmSystemMessage_WritesDialogTooFarMessages` addition | Unit / packet contract | `SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE` source review | `CantUseGuildStorage` writes Java message id `1300279`. | Packet payload decoder verifies id and empty parameter list. | Not Java golden bytes. |

## Validation Decision

```text
- Changed surface: live dialog packet dispatch and system-message packet helper.
- Specific behavior/contract: OPEN_LEGION_WAREHOUSE unsupported-action branch sends Java system-message id 1300279 while preserving membership-first ordering.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~NpcDialogTargetingServiceTests" --logger "console;verbosity=minimal"
- Result: passed; 310 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java test fixture exists for LegionService.canOpenWarehouse in this checkout.
- Broad-validation trigger: live dialog packet dispatch. Broad .NET was skipped because the focused command compiled the affected project and exercised the changed live dispatch plus adjacent packet and targeting contracts.
- Why this scope is sufficient: the command directly executes the live client packet handler branch, decodes the emitted server packet payload, and keeps targeting-service behavior covered.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `DialogService.onDialogSelect` | `GameServerConnection.HandleDialogSelectAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | `OPEN_LEGION_WAREHOUSE` success/no-legion/no-right/unsupported-action branches are live; other dialog actions remain mixed live/non-live. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect | Partial | Unit Tested | Partial Parity | Membership, unsupported-action, and permission branches are covered; config-disabled, disbanding, and in-use branches remain gaps. |
| `SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE` | `SmSystemMessage.CantUseGuildStorage` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300279` is verified by packet decoder and live dispatch; exact Java golden bytes were not captured. |
| `NpcTemplate.supportsAction(DialogAction.OPEN_LEGION_WAREHOUSE)` | `NpcTemplateSummary.SupportsDialogAction` via `NpcDialogTargetingService` | Target/action guard | Partial | Unit Tested | Partial Parity | Existing targeting result is consumed in Java-equivalent order for this branch; broader template action model remains outside this UOW. |

## Known Gaps

- Java `LegionConfig.LEGION_WAREHOUSE` disabled also sends `STR_CANT_USE_GUILD_STORAGE`; C# does not yet expose an equivalent live config check in this handler.
- Java `Legion.isDisbanding()` sends `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`; C# has no live disbanding state wired here.
- Java `LegionWarehouse.setInUse` / `getCurrentUser` sends `STR_GUILD_WAREHOUSE_IN_USE`; C# has no shared live in-use warehouse lock yet.
- Java `DialogService.onCloseDialog` releases the legion warehouse lock; C# close-dialog currently only closes mailbox state live.
- C# still stores legion warehouse items in `Player.InventoryItems` rather than a full Java `LegionWarehouse` aggregate.
- Exact Java golden bytes for the touched system message were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Add a small live legion warehouse in-use state for `OPEN_LEGION_WAREHOUSE` and release it from `CM_CLOSE_DIALOG`, matching Java `LegionWarehouse.setInUse`, `getCurrentUser`, and `DialogService.onCloseDialog`.
2. Add `STR_GUILD_WAREHOUSE_IN_USE` denial once the in-use state exists.
3. Add live disbanding-state denial once a C# legion disbanding flag/source exists.
