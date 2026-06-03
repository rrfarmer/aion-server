# Phase 6 Session 2388 Handoff

## Last Completed UOW
- `UOW-2388`: Refilled queued quick-entry registrations after `CM_AUTO_GROUP` cancel-enter.

## Current State
- `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)` now mirrors Java `checkQueueForQuickEntries(autoInstance)` for the available C# runtime model:
  - scans queued entries in order;
  - accepts only quick-entry registrations;
  - delegates admission to the open quick-entry runtime gate;
  - removes the accepted search entry;
  - plans ready window `4`;
  - removes the accepted leader's additional registrations and plans cancel window `2`.
- `GameServerConnection` window `103` cancel-enter now attempts that refill before sending the cancelling player cancel window `2`, when quick registration is allowed and the runtime snapshot still has registered players.
- Focused validation passed: 60 autogroup service/runtime/connection tests.

## Useful Java Anchors
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `cancelEnter(Player player, int instanceMaskId)`
  - `destroyOrAddPlayersFromQuickEntries(AutoInstance autoInstance)`
  - `checkQueueForQuickEntries(AutoInstance autoInstance)`
  - `onLeaveInstance(Player player)`
- `game-server/src/com/aionemu/gameserver/instance/AutoInstance.java`
  - `addLookingForParty(LookingForParty lfp)`
  - `unregister(Player player)`
  - `destroyIfPossible()`
- `game-server/src/com/aionemu/gameserver/instance/AutoPvpInstance.java`
  - race/capacity admission behavior for quick-entry refill.

## Useful C# Anchors
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
  - `TryRefillQueuedQuickEntry(...)`
  - `TryAttachOpenQuickEntry(...)`
  - `RemoveAdditionalRegistrationsByLeader(...)`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
  - `CancelEnter(...)`
  - `OnLeaveInstance(...)`
  - `TryAddOpenQuickEntry(...)`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `HandleAutoGroupAsync(...)`, window `103`.

## Suggested Next UOW
- Wire Java `AutoGroupService.onLeaveInstance(...)` to the same destroy-or-refill behavior if a live C# leave boundary is present.
- If no live leave boundary is ready, add a focused runtime/service planner that represents `destroyOrAddPlayersFromQuickEntries(autoInstance)` after `OnLeaveInstance(...)`, then bridge it in the next UOW.

## Watch Points
- Java calls `destroyIfPossible(autoInstance)` before refilling. The cancel-enter bridge currently uses the available registered-count snapshot as a conservative non-destroy guard; a future UOW should model online-inside-player facts if that data becomes available.
- Do not widen to full autogroup lifecycle cleanup in the same UOW unless discovery shows the boundary is already tiny.
- Keep validation focused unless the live dispatch surface expands beyond the autogroup service/runtime/connection tests.
