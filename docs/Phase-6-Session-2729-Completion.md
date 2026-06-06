# Phase 6 Session 2729 Completion

## UOW

[Phase 6] UOW-2729: Lock live legion warehouse opens.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: OPEN_LEGION_WAREHOUSE now claims a shared live in-use lock, rejects a different same-legion opener with Java's in-use denial, and releases the lock from CM_CLOSE_DIALOG.
- Java source/runtime path: LegionWarehouse.setInUse/getCurrentUser/unsetInUse, LegionService.canOpenWarehouse in-use branch, CM_CLOSE_DIALOG, and DialogService.onCloseDialog legion warehouse release branch.
- C# runtime artifact wired: LegionWarehouseRuntime, GameServerRuntimeContext.LegionWarehouses, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, GameServerConnection.HandleCloseDialog, and SmSystemMessage.GuildWarehouseInUse.
- Client-visible/state/persistence effect: the first opener receives live warehouse open packets, a concurrent different legion member receives system-message id `1300280`, the current opener can reopen, and closing the supporting NPC dialog releases the runtime lock.
- Why this is runtime progress: it mutates shared live runtime state and sends a real server packet from live client packet handlers; it is not preview-only, metadata-only, documentation-only, or test-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - `setInUse(int playerObjId)` uses `compareAndSet(0, playerObjId)`.
  - `getCurrentUser()` returns the current object id or `0` when free.
  - `unsetInUse(int playerObjId)` uses `compareAndSet(playerObjId, 0)`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `canOpenWarehouse` sends `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_IN_USE()` when another player has the warehouse in use.
  - `onLogout` also calls `legion.getLegionWarehouse().unsetInUse(player.getObjectId())`; this is a remaining runtime gap after this UOW.
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
  - `onCloseDialog` sends the NPC AI `DIALOG_FINISH` event and releases the legion warehouse lock when the NPC supports `OPEN_LEGION_WAREHOUSE` and the player is a legion member.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_CLOSE_DIALOG.java`
  - Reads the target object id and delegates to `DialogService.onCloseDialog`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_GUILD_WAREHOUSE_IN_USE()` uses message id `1300280`.

## C# Changes

- Added `LegionWarehouseRuntime`, a shared runtime lock table keyed by legion id with Java-equivalent set/get/unset semantics for the in-use user id.
- Added `GameServerRuntimeContext.LegionWarehouses` so production connections share one legion warehouse runtime state.
- Updated `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` to reject a different current user with `SmSystemMessage.GuildWarehouseInUse()` before sending open packets.
- Updated `GameServerConnection.HandleCloseDialog` to resolve the target NPC, feed the close-dialog side-effect plan with the legion warehouse action/member predicates, and release the live lock when Java would.
- Updated `NpcDialogCloseSideEffectPlan.ShouldMutateLiveLegionWarehouse` to reflect the actual release predicate.
- Added `SmSystemMessage.GuildWarehouseInUse()` with Java message id `1300280`.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LegionWarehouseRuntimeTests.InUseStateMatchesJavaCompareAndSetSemantics` | Unit / runtime state | `LegionWarehouse.setInUse/getCurrentUser/unsetInUse` | Same owner can hold the lock, another owner cannot overwrite it, wrong-owner release fails, owner release frees it. | Direct runtime state assertions. | Does not model full Java `Storage` contents. |
| `HandleDialogSelectAsync_OpenLegionWarehouseLocksAgainstOtherLegionMemberLikeJava` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse` in-use branch | A different same-legion opener receives system-message id `1300280`. | Socket-backed connection fixture, live `HandleDialogSelectAsync`, decoded `SmSystemMessage` payload. | No exact Java golden bytes. |
| `HandleDialogSelectAsync_OpenLegionWarehouseAllowsCurrentUserToReopenLikeJava` | Unit / live dialog dispatch | `setInUse` failure plus `getCurrentUser() == player.getObjectId()` allowance | The current opener can select the warehouse again and receives the normal live open packet sequence. | Decoded `SmLegionEdit`, `SmWarehouseInfo`, and `SmDialogWindow` payloads. | The C# item-template dependency is still checked before lock acquisition. |
| `HandleCloseDialog_OpenLegionWarehouseReleasesLockLikeJava` | Unit / live close-dialog dispatch | `CM_CLOSE_DIALOG` -> `DialogService.onCloseDialog` release branch | Closing the warehouse-supporting NPC dialog releases the lock so another legion member can open. | Live close-dialog handler plus subsequent open packet sequence. | AI `DIALOG_FINISH` remains non-live. |
| `HandleCloseDialog_NonLegionWarehouseNpcDoesNotReleaseLockLikeJava` | Unit / live close-dialog dispatch | `DialogService.onCloseDialog` NPC action guard | Closing a different NPC does not release the warehouse lock. | Subsequent live open attempt still receives `1300280`. | Uses world lookup as C# approximation of Java known-list target resolution. |
| `SmSystemMessage_WritesDialogTooFarMessages` addition | Unit / packet contract | `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_IN_USE` source review | `GuildWarehouseInUse` writes Java message id `1300280`. | Packet payload decoder verifies id and empty parameter list. | Not Java golden bytes. |

## Validation Decision

```text
- Changed surface: live dialog packet dispatch, live close-dialog side effect, shared runtime state, and system-message packet helper.
- Specific behavior/contract: OPEN_LEGION_WAREHOUSE in-use locking and CM_CLOSE_DIALOG release match reviewed Java runtime paths.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~NpcDialogSideEffectServiceTests|FullyQualifiedName~LegionWarehouseRuntimeTests" --logger "console;verbosity=minimal"
- Result: passed; 320 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for LegionWarehouse locking or DialogService close-dialog behavior in this checkout.
- Broad-validation trigger: shared live runtime state mutation and live dialog packet dispatch. Broad .NET was skipped because the focused command compiled the affected project and exercised the changed live open, close, runtime, and packet contracts.
- Why this scope is sufficient: the command executes the live client packet handlers, proves the shared runtime lock behavior, decodes emitted server packets, and keeps adjacent close-dialog side-effect behavior covered.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `LegionWarehouse.setInUse/getCurrentUser/unsetInUse` | `LegionWarehouseRuntime` | Runtime state | Partial | Unit Tested | Partial Parity | In-use owner semantics are modeled by legion id; full Java warehouse storage, history, and limits are outside this runtime holder. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect / live handler branch | Partial | Unit Tested | Partial Parity | Membership, unsupported-action, permission, success, and in-use branches are live; config-disabled and disbanding checks remain missing. |
| `DialogService.onCloseDialog` | `GameServerConnection.HandleCloseDialog` | Live client packet side effect | Partial | Unit Tested | Partial Parity | Legion warehouse lock release is live for supporting NPCs and legion members; AI `DIALOG_FINISH` remains non-live, and C# uses world lookup rather than Java known-list lookup. |
| `CM_CLOSE_DIALOG` | `CmCloseDialog` plus `GameServerConnection.HandleCloseDialog` | Client packet handler | Partial | Unit Tested | Partial Parity | Target object id is consumed to drive live close side effects; broader close-dialog AI behavior remains incomplete. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_IN_USE` | `SmSystemMessage.GuildWarehouseInUse` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300280` verified by packet decoder and live dispatch; exact Java golden bytes were not captured. |

## Known Gaps

- Java `LegionService.onLogout` and legion member removal release the warehouse lock; C# does not yet release this runtime lock on logout/disconnect or legion leave/kick paths.
- Java `LegionConfig.LEGION_WAREHOUSE` disabled also sends `STR_CANT_USE_GUILD_STORAGE`; C# does not yet expose an equivalent live config check in this handler.
- Java `Legion.isDisbanding()` sends `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`; C# has no live disbanding state wired here.
- C# checks item-template availability before acquiring the lock to avoid a stale lock when C# static data is missing. Java has the static data available behind the service path.
- C# still represents legion warehouse items through `Player.InventoryItems` location `3` rather than a full Java `LegionWarehouse` aggregate.
- AI `DIALOG_FINISH` from `DialogService.onCloseDialog` remains non-live.
- Exact Java golden bytes for the touched packet were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 6
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Release the live legion warehouse in-use lock from logout/disconnect, matching Java `PlayerLeaveWorldService.leaveWorld` -> `LegionService.onLogout` -> `LegionWarehouse.unsetInUse(player.getObjectId())`.
2. Release the live lock from legion leave/kick/member-removal paths once the corresponding C# runtime paths are located.
3. Add live disbanding-state denial once a C# legion disbanding flag/source exists.
