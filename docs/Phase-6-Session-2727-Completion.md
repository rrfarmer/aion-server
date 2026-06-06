# Phase 6 Session 2727 Completion

## UOW

[Phase 6] UOW-2727: Send legion warehouse open denials.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: invalid CM_DIALOG_SELECT OPEN_LEGION_WAREHOUSE selections now send Java denial packets for no-legion and no-permission branches.
- Java source/runtime path: LegionService.canOpenWarehouse(Player, Npc) membership/permission branches -> SM_SYSTEM_MESSAGE.STR_NO_GUILD_TO_DEPOSIT and STR_GUILD_WAREHOUSE_NO_RIGHT.
- C# runtime artifact wired: GameServerConnection.HandleOpenLegionWarehouseDialogAsync and SmSystemMessage.NoGuildToDeposit/GuildWarehouseNoRight.
- Client-visible/state/persistence effect: real clients receive Java-equivalent system-message denial packets from the live dialog dispatch path.
- Why this is runtime progress: it sends real server packets from a live client packet handler and removes a silent no-op branch.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `canOpenWarehouse` sends `STR_NO_GUILD_TO_DEPOSIT` when the player is not a legion member.
  - `canOpenWarehouse` sends `STR_GUILD_WAREHOUSE_NO_RIGHT` when the legion member lacks both deposit and withdrawal rights.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_NO_GUILD_TO_DEPOSIT()` uses message id `1300278`.
  - `STR_GUILD_WAREHOUSE_NO_RIGHT()` uses message id `1300322`.

## C# Changes

- Added `SmSystemMessage.NoGuildToDeposit()` with Java message id `1300278`.
- Updated `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` so a valid legion warehouse NPC selection by a non-legion player sends `SmSystemMessage.NoGuildToDeposit()` instead of returning silently.
- Kept the existing no-right live branch and added direct coverage for its Java id.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_OpenLegionWarehouseWithoutLegionSendsJavaDenial` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse`, `SM_SYSTEM_MESSAGE.STR_NO_GUILD_TO_DEPOSIT` source review | Live action `53` sends system-message id `1300278` for a player without legion membership. | Socket-backed connection fixture, live `HandleDialogSelectAsync`, decoded `SmSystemMessage` payload. | Targeting must already resolve a valid NPC/action; unsupported-NPC Java denial remains separate. |
| `HandleDialogSelectAsync_OpenLegionWarehouseWithoutWarehouseRightsSendsJavaDenial` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse`, `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT` source review | Live action `53` sends system-message id `1300322` for a legion member without deposit/withdrawal rights. | Socket-backed connection fixture, live `HandleDialogSelectAsync`, decoded `SmSystemMessage` payload. | Does not model disbanding or in-use warehouse state. |
| `SmSystemMessage_WritesDialogTooFarMessages` additions | Unit / packet contract | `SM_SYSTEM_MESSAGE` source review | `NoGuildToDeposit` and existing `GuildWarehouseNoRight` helpers write Java message ids. | Packet payload decoder verifies ids and empty parameter lists. | Not Java golden bytes. |

## Validation Decision

```text
- Changed surface: live dialog packet dispatch and system-message packet helper.
- Specific behavior/contract: OPEN_LEGION_WAREHOUSE no-legion and no-permission branches send Java system-message ids 1300278 and 1300322.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
- Result: passed; 304 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java test fixture exists for LegionService.canOpenWarehouse in this checkout.
- Broad-validation trigger: live dialog packet dispatch. Broad .NET was skipped because the focused command compiled the affected project and exercised the changed live dispatch plus adjacent packet contract.
- Why this scope is sufficient: the command directly executes the live client packet handler branches and decodes the emitted server packet payloads.
```

Note: the first focused C# run failed because the new local test helper decoded `SmSystemMessage` with the wrong field order. The helper was corrected to match the existing packet decoder, and the same focused command then passed.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `DialogService.onDialogSelect` | `GameServerConnection.HandleDialogSelectAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | `OPEN_LEGION_WAREHOUSE` now covers success, no-legion, and no-right live packet sends; many dialog actions remain outside this UOW. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect | Partial | Unit Tested | Partial Parity | Membership and permission denial branches are covered; config-disabled, unsupported NPC action, disbanding, and in-use lock branches remain gaps. |
| `SM_SYSTEM_MESSAGE.STR_NO_GUILD_TO_DEPOSIT` | `SmSystemMessage.NoGuildToDeposit` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300278` is verified by packet decoder and live dispatch; exact Java golden bytes were not captured. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT` | `SmSystemMessage.GuildWarehouseNoRight` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300322` is verified by packet decoder and live dispatch; exact Java golden bytes were not captured. |

## Known Gaps

- Java `LegionConfig.LEGION_WAREHOUSE` disabled and unsupported NPC action send `STR_CANT_USE_GUILD_STORAGE`; C# still returns early through targeting validation for unsupported actions.
- Java `Legion.isDisbanding()` sends `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`; C# has no live disbanding state wired here.
- Java `LegionWarehouse.setInUse` / `getCurrentUser` sends `STR_GUILD_WAREHOUSE_IN_USE`; C# has no shared live in-use warehouse lock yet.
- C# still stores legion warehouse items in `Player.InventoryItems` rather than a full Java `LegionWarehouse` aggregate.
- Exact Java golden bytes for the touched system messages were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Add live `STR_CANT_USE_GUILD_STORAGE` denial for `OPEN_LEGION_WAREHOUSE` when a player is a legion member but the selected NPC/action cannot use legion storage, matching Java `canOpenWarehouse`.
2. Model live legion warehouse in-use state and release it from close/logout/dialog-close paths, matching Java `LegionWarehouse.setInUse`.
3. Add live disbanding-state denial once a C# legion disbanding flag/source exists.
