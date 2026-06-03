# Phase 6 Session 2526 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2525: Add remaining ItemMask properties to ItemTemplateSummary

## Session Summary (UOWs 2514-2525)

This session completed 12 UOWs covering two major themes: the Vortex defender acceptance pipeline and a set of item/dialog small-scope ports.

**Vortex defender acceptance pipeline (UOW-2514 through UOW-2521):**
- 2514: `VortexDefenderAcceptanceRuntimeObserverService` (composite transition + participant report)
- 2515: Wired observer into `GameServerConnection`
- 2516: `VortexInvasionRuntime.FindDefenderLocationId` + connection wiring
- 2517: `VortexDefenderAcceptanceInputResolverService` (derives defender snapshots from runtime)
- 2518: Wired resolver into connection
- 2519: World-position fallback for location ID resolution
- 2520: Production startup wiring via `GameClientSocketServer`
- 2521: `PendingVortexDefenderInvitationRequest.LocationId` threaded through chain; observer self-resolves from payload

**Item and dialog ports (UOW-2522 through UOW-2525):**
- 2522: `DialogService.onCloseDialog` → `NpcDialogCloseSideEffectPlanService` + `PlayerMailboxState` constants + live `HandleCloseDialog` connection activation
- 2523: `ItemMask.BREAKABLE` → `IsBreakable` property; `SmSystemMessage.UnbreakableItem`; `SmDeleteItem.DiscardDeleteType`; live `HandleDeleteItemAsync` (replaces deferred stub)
- 2524: `PlayerEnterWorldService.DeleteInventoryItemAsync` + DB persistence wired into `HandleDeleteItemAsync`
- 2525: Full `ItemMask` property set on `ItemTemplateSummary` (SELLABLE, STORABLE_IN_WH, STORABLE_IN_AWH, STORABLE_IN_LWH, REMOVE_LOGOUT, CAN_COMPOSITE_WEAPON, CAN_SPLIT, DELETABLE, LEGION_TRADEABLE)

## Commits Made

- `[Phase 6][UOW-2514] Add guarded Vortex defender acceptance runtime observer`
- `[Phase 6][UOW-2515] Wire Vortex defender acceptance observer into game connection`
- `[Phase 6][UOW-2516] Add defender location lookup and wire into game connection`
- `[Phase 6][UOW-2517] Add defender acceptance input resolver for production wiring`
- `[Phase 6][UOW-2518] Wire defender acceptance input resolver into game connection`
- `[Phase 6][UOW-2519] Add world-position fallback for defender acceptance location resolution`
- `[Phase 6][UOW-2520] Wire Vortex acceptance dependencies at production startup`
- `[Phase 6][UOW-2521] Add LocationId to defender invitation payload and thread through chain`
- `[Phase 6][UOW-2522] Port DialogService.onCloseDialog for NPC dialog close side effects`
- `[Phase 6][UOW-2523] Add IsBreakable to ItemTemplateSummary and implement CM_DELETE_ITEM`
- `[Phase 6][UOW-2524] Add DB persistence for item deletion in CM_DELETE_ITEM`
- `[Phase 6][UOW-2525] Add remaining ItemMask properties to ItemTemplateSummary`
- `[Phase 6][Session 2521] Create handoff for next session` (session meta-commit)

## Test Summary

- Tests at session start: ~132
- Tests at session end: 146+ (NpcDialogSideEffectService 7, VortexLocationServiceTests 148+, GameServerConnectionVortexQuestionResponseTests 4+)

## Java Artifacts Covered (Session)

| Java Artifact | C# Artifact | Parity Status |
| --- | --- | --- |
| `Invasion.acceptRequest → addPlayer → defenders.put` chain | `VortexDefenderAcceptanceRuntimeObserverService` | Partial Parity |
| `VortexService.removeDefenderPlayer` iteration | `VortexInvasionRuntime.FindDefenderLocationId` | Partial Parity |
| `Invasion.updateAlliance` location closure | `PendingVortexDefenderInvitationRequest.LocationId` | Partial Parity |
| `DialogService.onCloseDialog` | `NpcDialogCloseSideEffectPlanService` + `HandleCloseDialog` | Partial Parity (mailbox live, AI/legion deferred) |
| `PlayerMailboxState.CLOSED/REGULAR/EXPRESS` | `PlayerMailboxState` class | Verified Parity |
| `ItemMask.BREAKABLE` + `isBreakable()` | `ItemTemplateSummary.IsBreakable` | Verified Parity |
| `CM_DELETE_ITEM.runImpl` | `GameServerConnection.HandleDeleteItemAsync` | Partial Parity (cube slot only; DB wired) |
| All `ItemMask.*` constants | 9 new `ItemTemplateSummary` properties | Verified Parity |

## Key Observations for Next Session

1. `VortexInvasionRuntime` is already registered as a DI singleton in `Program.cs` (line 165). `GameClientSocketServer` now accepts it as an optional parameter and passes it + world lookup + location service to each connection at construction.
2. `PendingVortexDefenderInvitationRequest.LocationId = 0` by default — backward compatible. `UpdateAlliance` threads `location.Id`; observer self-resolves from payload.
3. `CM_DELETE_ITEM` is now fully live: breakability guard, in-memory removal, DB deletion, packet send.
4. All `ItemMask` bits are now exposed as `ItemTemplateSummary` properties.
5. The C# codebase is extremely comprehensive — almost all Java services have C# equivalents. Remaining gaps are complex systems: legion, siege, windstream, armsfusion, static doors, NPC AI, quest engine, PvP headhunting.

## Next Recommended UOW

[Phase 6] UOW-2526: Add `ItemTemplateSummary.IsStorableInAccWarehouse` soulbound guard

Java's `Item.isStorableInAccWarehouse()` includes an additional `!isSoulBound()` check beyond the mask bit:
```java
public boolean isStorableInAccWarehouse() {
    return (getItemMask() & ItemMask.STORABLE_IN_AWH) == ItemMask.STORABLE_IN_AWH && !isSoulBound();
}
```
Similarly for `isStorableInLegWarehouse()`. Currently `ItemTemplateSummary.IsStorableInAccountWarehouse` only checks the mask bit.

This could be:
1. A composite property on `ItemTemplateSummary`: `IsStorableInAccountWarehouse => (Mask & StorableInAwhMask) == StorableInAwhMask && !IsSoulBound`
2. OR a convenience method that takes soulbound state: `IsStorableInAccountWarehouse(bool isSoulBound)`.

Approach 1 is cleaner since `IsSoulBound` is already on the template.

Suggested files to inspect:
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Item.java` (isStorableInAccWarehouse, isStorableInLegWarehouse)
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogSideEffectServiceTests.cs`

Suggested validation:
```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~NpcDialogSideEffectServiceTests" --no-restore
```

Broad-validation trigger: none (property update only).

Safe alternative candidates:
- Port a deferred connection handler that doesn't depend on complex systems not yet ported.
- Enable live `VortexInvasionRuntime.AddDefender` on acceptance (broad-validation trigger).
- Port `CM_MOVE_ITEM` for in-cube item reordering (same storage to same storage path is simpler than cross-storage).

## Context Needed By Next Session

- `ItemTemplateSummary.IsStorableInWarehouse/AccountWarehouse/LegionWarehouse` check only the mask bit. Java adds `!isSoulBound()` for account and legion storage.
- `PlayerMailboxState.Closed = 0x00`, `Regular = 0x01`, `Express = 0x02`.
- `SmDeleteItem.DiscardDeleteType = 0x15` (Java `ItemDeleteType.DISCARD`).
- `SmSystemMessage.UnbreakableItem(string?)` uses message ID 1400340.
- `HandleDeleteItemAsync` in the connection: cube slot only (location == 0), breakability guard, in-memory removal, DB deletion, discard packet.
- `VortexDefenderAcceptanceRuntimeObserverService.Observe` self-resolves `locationId` from `PendingVortexDefenderInvitationRequest.LocationId` when caller passes 0.
- Full Vortex defender acceptance pipeline (UOW-2514 through 2521) is complete at the non-live level; live `AddDefender` remains the major gap.
