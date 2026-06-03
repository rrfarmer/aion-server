# Phase 6 Session 2528 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2528: Revert incorrect soulbound guards from ItemTemplateSummary

## Session Summary (UOWs 2514-2528)

This session completed 15 UOWs across three themes:

**Vortex defender acceptance pipeline (2514-2521):** Full non-live observer, connection wiring, runtime location lookup, input resolver, world-position fallback, production startup wiring, `PendingVortexDefenderInvitationRequest.LocationId`.

**Item/dialog small ports (2522-2528):**
- 2522: `DialogService.onCloseDialog` + `PlayerMailboxState` + live `HandleCloseDialog`
- 2523: `IsBreakable`, `SmSystemMessage.UnbreakableItem`, `SmDeleteItem.DiscardDeleteType`, live `HandleDeleteItemAsync`
- 2524: DB persistence for item deletion via `PlayerEnterWorldService.DeleteInventoryItemAsync`
- 2525: All remaining `ItemMask` properties on `ItemTemplateSummary` (9 new)
- 2526: Soulbound guard for AWH/LWH storability (later reversed in 2528)
- 2527: Soulbound guard for IsTradeable/IsLegionTradeable (later reversed in 2528)
- 2528: Correction — `ItemTemplateSummary` maps to `ItemTemplate` (no instance-level soulbound guards). Tests document template vs runtime instance distinction.

## Key Architectural Invariant Established in UOW-2528

`ItemTemplateSummary` maps to Java's `ItemTemplate`, NOT to Java's `Item`. Template-level mask properties are pure mask bit checks. Runtime soulbound checks (`!item.isSoulBound()`) apply to individual `InventoryItem` instances and must use `InventoryItem.IsSoulBound`.

Callers composing template properties with instance soulbound state should use `InventoryItem.IsSoulBound` explicitly:
```csharp
bool isTradeable = item.Template.IsTradeable && !inventoryItem.IsSoulBound;
```

## Files Changed (UOW-2528)

- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogSideEffectServiceTests.cs`

## Validation Completed

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~NpcDialogSideEffectServiceTests" --no-restore
```

- Passed: 7 tests.

## Next Recommended UOW

[Phase 6] UOW-2529: Add `InventoryItemExtensions.IsTradeable(InventoryItem, ItemTemplateSummary)` for runtime soulbound-aware tradability

The correct place to compose template mask with instance soulbound is a helper that takes both:

```csharp
public static class InventoryItemExtensions
{
    // Java parity: Item.isTradeable() = template.IsTradeable && !item.isSoulBound()
    public static bool IsTradeable(this InventoryItem item, ItemTemplateSummary template)
        => template.IsTradeable && !item.IsSoulBound;
    
    public static bool IsStorableInAccountWarehouse(this InventoryItem item, ItemTemplateSummary template)
        => template.IsStorableInAccountWarehouse && !item.IsSoulBound;
    
    public static bool IsStorableInLegionWarehouse(this InventoryItem item, ItemTemplateSummary template)
        => template.IsStorableInLegionWarehouse && !item.IsSoulBound;
    
    public static bool IsLegionTradeable(this InventoryItem item, ItemTemplateSummary template)
        => template.IsLegionTradeable && !item.IsSoulBound;
}
```

This provides Java-parity compound checks that correctly combine template and instance state.

Suggested files to inspect:
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`

Suggested validation: `--filter "FullyQualifiedName~NpcDialogSideEffectServiceTests|FullyQualifiedName~ItemStorageRestriction"` (to confirm the storage plan service still gets correct inputs).

Broad-validation trigger: none.

Safe alternative candidates:
- Move to a different Phase 6 system (NPC spawn, movement, combat, quest).
- Enable live `VortexInvasionRuntime.AddDefender` on acceptance (broad-validation trigger).
- Port `CM_MOVE_ITEM` for item reordering within the same storage (simpler than cross-storage).

## Context Needed By Next Session

- `ItemTemplateSummary` = template-level mask properties only. No instance soulbound guards.
- `InventoryItem.IsSoulBound` = instance-level runtime state. Must be composed with template properties for full `Item.isTradeable()` parity.
- `ItemStorageRestrictionPlanService` (11 tests) takes `itemIsStorableInAccWarehouse` as a bool param. Callers must compose template + instance state before calling.
- All 9 new `ItemMask` properties added (IsSellable, IsStorableInWarehouse/AccountWarehouse/LegionWarehouse, IsRemovedOnLogout, CanCompositeWeapon, CanSplit, IsDeletable, IsLegionTradeable).
- `CM_DELETE_ITEM` is now fully live: breakability guard, in-memory removal, DB deletion, discard packet.
- Full Vortex defender acceptance pipeline (UOW-2514-2521) is production-ready at the non-live level.
