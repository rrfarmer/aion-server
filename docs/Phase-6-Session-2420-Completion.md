# Phase 6 Session 2420 Completion

Status: Phase 6 continues; UOW-2420 completed a docs-only audit of Java legion logout cleanup. No C# code was changed because the current port has packet/restriction legion warehouse slices but no live `LegionService`, legion warehouse runtime, legion member model, or legion repository contract that could safely host Java logout behavior.

## Scope

- UOW: UOW-2420 legion logout cleanup audit.
- Reviewed Java logout warehouse persistence and legion member cleanup.
- Searched C# game-server code for exact legion warehouse/member/runtime/repository surfaces.
- Recorded the gap conservatively instead of introducing a speculative broad legion persistence abstraction.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - Calls `LegionService.getInstance().LegionWhUpdate(player)` after group/alliance logout timestamp handling and before effect removal/task cancellation.
  - Calls `LegionService.getInstance().onLogout(player)` only when `player.isLegionMember()` is true, after `setOnline(false)` and `setLastOnline(...)`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `LegionWhUpdate(Player)` gets the player's legion, combines `legion.getLegionWarehouse().getItemsWithKinah()` and deleted items, then calls `InventoryDAO.store(allItems, player.getObjectId(), player.getAccount().getId(), legion.getLegionId())` and `ItemStoneListDAO.save(allItems)`, swallowing/logging exceptions.
  - `onLogout(Player)` unsets legion warehouse in-use state for the player, refreshes/broadcasts legion member info, stores legion, stores legion member, and removes the legion bonus when online membership drops below Java's threshold.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - `unsetInUse(int playerObjId)` uses an atomic compare-and-set from the player's object id to `0`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
  - `removeBonus()` sends `SM_ICON_INFO(1, false)` when online legion members fall below `10` and `hasBonus` changes from true to false.
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
  - `storeLegion(Legion)` updates legion name, level, contribution, permissions, disband time, and dominion fields.
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
  - `storeLegionMember(LegionMember)` updates nickname, rank, self intro, challenge score by player object id.
- `game-server/src/com/aionemu/gameserver/services/PeriodicSaveService.java`
  - Periodic legion warehouse save uses the same warehouse item/deleted item plus item-stone pattern with null player/account ids.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `LeaveWorldAsync` currently performs find-group cleanup, question denial, repurchase cleanup, aggregate `SavePlayerLogoutAsync`, and group/alliance last-online runtime updates.
  - No legion logout hook, legion warehouse in-use state, legion warehouse item save, or legion member store hook exists.
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - `IPlayerEnterWorldRepository.SavePlayerLogoutAsync` persists modeled player logout state.
  - It hydrates player legion facts for entry/trade-list use but does not expose legion warehouse, legion member, or legion state persistence contracts.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionEdit.cs`
  - Packet shape exists for legion edit updates, including warehouse kinah.
  - This is not a legion runtime or persistence surface.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmLegionWarehouseKinah.cs`
  - Client packet parser exists for warehouse kinah requests.
  - It is not wired to Java `LegionService.LegionWhUpdate` or logout cleanup.
- `dotnetConversion/src/Aion.GameServer/Services/ItemStorageRestrictionPlanService.cs`
  - Models account/legion warehouse deposit/withdrawal restrictions.
  - It does not model warehouse in-use state, warehouse item persistence, or legion member logout cleanup.
- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
  - Has supplied-snapshot metadata for legion warehouse item/kinah unlock packets.
  - It is not a live legion warehouse runtime.

## Implemented

- Added this completion document.
- Added the UOW-2420 handoff document.
- No production or test code changed.

## Findings

- Java has two legion logout responsibilities:
  - Pre-player-save warehouse persistence: save current and deleted legion warehouse items plus item stones.
  - Post-online/last-online legion cleanup: unset warehouse in-use, broadcast/store member info, store legion, store legion member, and remove legion bonus.
- C# has no exact target for either responsibility yet.
- Current C# legion-related code is limited to packet parsing/writing, item restriction planning, player legion facts, and supplied metadata for unrelated systems.
- A safe implementation requires at least a non-live legion logout cleanup plan or repository contract first; live persistence should wait until Java SQL/error/ordering behavior is explicitly modeled and tested.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` legion calls | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | No Tests | Partial Parity | C# logout has no legion warehouse persistence or legion member cleanup hook. Group/alliance timestamp slices exist from prior UOWs. Java ordering is known: `LegionWhUpdate` before effect removal and `onLogout` after online/last-online update. |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` | No exact C# equivalent | Service | Not Started | No Tests | Needs Verification | Missing legion warehouse runtime, deleted-item list, warehouse kinah item aggregation, `InventoryDAO.store` legion-owner parameters, and `ItemStoneListDAO.save` equivalent for legion warehouse logout. |
| `com.aionemu.gameserver.services.LegionService.onLogout` | No exact C# equivalent | Service | Not Started | No Tests | Needs Verification | Missing `LegionMember`, legion runtime, warehouse in-use CAS, `SM_LEGION_UPDATE_MEMBER` fanout, legion/member DAO persistence, and bonus removal packet behavior. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse.unsetInUse` | No exact C# equivalent | Runtime Model | Not Started | No Tests | Needs Verification | Java compare-and-sets current user from player object id to zero. C# has no modeled legion warehouse current-user state. |
| `com.aionemu.gameserver.model.team.legion.Legion.removeBonus` | No exact C# equivalent | Runtime Model | Not Started | No Tests | Needs Verification | Java clears `hasBonus` when online members are below 10 and sends `SM_ICON_INFO(1, false)` to remaining online members. C# quest reward can consume a supplied legion bonus flag, but no live legion bonus runtime exists. |
| `com.aionemu.gameserver.dao.LegionDAO.storeLegion` | No exact C# equivalent | Repository | Not Started | No Tests | Needs Verification | C# repository does not expose legion table update for logout. SQL field list and error behavior need modeling before live wiring. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.storeLegionMember` | No exact C# equivalent | Repository | Not Started | No Tests | Needs Verification | C# repository does not expose legion member nickname/rank/self-intro/challenge score update for logout. |
| `com.aionemu.gameserver.services.PeriodicSaveService.LegionWarehouseSaveTask` | No exact C# equivalent | Scheduled Service | Not Started | No Tests | Needs Verification | Java periodic warehouse save shares the warehouse item/deleted item and item-stone persistence pattern; C# has no periodic legion warehouse save. |

## Validation Decision

- Changed surface: documentation-only audit.
- Specific behavior/contract: completion/handoff docs accurately record Java legion logout cleanup and current C# absence without claiming verified parity.
- Focused C# command:
  - Not applicable; no C# source or test code changed.
- Focused Java/Maven command:
  - Not run; no Java source or fixture changed, and Java evidence was direct source review.
- Documentation hygiene command:
  - `git diff --check`
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; documentation-only unit with no generated artifacts, run scripts, fixtures, C# code, or Java code changed.
- Why this scope is sufficient:
  - `git diff --check` validates patch hygiene for the only changed surface, while Java/C# source review supplies the audit evidence.

## Known Remaining Gaps

- No C# legion warehouse runtime or repository.
- No C# legion member model/runtime repository.
- No C# `LegionService.onLogout` equivalent.
- No C# warehouse in-use CAS state.
- No C# legion bonus online-member runtime or `SM_ICON_INFO` fanout from logout.
- No live legion warehouse periodic save.
- No objective Java/C# runtime comparison for legion logout because the C# target surface is not present.

## Summary Metrics

- Java artifacts reviewed: 7.
- C# artifacts reviewed: 6.
- Production files changed: 0.
- Test files changed: 0.
- Documentation files changed: 2.
- Artifacts with verified parity: 0.
- Artifacts needing verification: 7.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
