# Phase 6 Session 2527 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2526: Add soulbound guard to IsStorableInAccountWarehouse and IsStorableInLegionWarehouse

## Commits Made

- `[Phase 6][UOW-2526] Add soulbound guard to AWH/LWH storability and validate with tests`

## Summary

UOW-2526 corrected `IsStorableInAccountWarehouse` and `IsStorableInLegionWarehouse` to include the `!IsSoulBound` guard that matches Java's `Item.isStorableInAccWarehouse()` and `isStorableInLegWarehouse()`. `IsStorableInWarehouse` (regular WH) has no soulbound guard in Java and was left unchanged. Tests verify soulbound items are rejected from AWH/LWH even when the mask bit is set.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogSideEffectServiceTests.cs`
- `docs/Phase-6-Session-2526-Completion.md`
- `docs/Phase-6-Session-2527-Handoff.md`

## Java Artifacts Touched

- `Item.isStorableInAccWarehouse()` — `(mask & STORABLE_IN_AWH) && !isSoulBound()`
- `Item.isStorableInLegWarehouse()` — `(mask & STORABLE_IN_LWH) && !isSoulBound()`

## C# Artifacts Touched

- `ItemTemplateSummary.IsStorableInAccountWarehouse` (added `!IsSoulBound`)
- `ItemTemplateSummary.IsStorableInLegionWarehouse` (added `!IsSoulBound`)

## Validation Completed

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~NpcDialogSideEffectServiceTests" --no-restore
```

- Passed: 7 tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Parity Status |
| --- | --- | --- |
| `Item.isStorableInAccWarehouse()` | `ItemTemplateSummary.IsStorableInAccountWarehouse` | Verified Parity |
| `Item.isStorableInLegWarehouse()` | `ItemTemplateSummary.IsStorableInLegionWarehouse` | Verified Parity |

## Known Gaps

- `ItemStorageRestrictionPlanService` (11 tests) already uses these properties for item movement validation. Confirm it now receives the correct soulbound-aware behavior. Its `itemIsStorableInAccWarehouse` parameter is supplied by callers who read from `ItemTemplateSummary`.
- `CM_MOVE_ITEM` remains deferred; item movement across storage types needs `ItemMoveService.moveItem` port.

## Remaining Risks

- No deferred connection handlers are simple enough to port without complex system dependencies (windstream, static doors, legion warehouse Kinah, armsfusion, etc.).

## Next Recommended UOW

[Phase 6] UOW-2527: Port `Item.isSellable()` compound check to ItemTemplateSummary

Java's `Item.isSellable()` is used in NPC sell-back validation:
```java
public boolean isSellable() {
    return (getItemMask() & ItemMask.SELLABLE) == ItemMask.SELLABLE && !isSoulBound();
}
```

`IsSellable` currently only checks the mask bit. Adding `&& !IsSoulBound` fixes parity with Java for the NPC sell-back window.

Suggested files to inspect:
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Item.java` (isSellable)
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`

Suggested validation: `--filter "FullyQualifiedName~NpcDialogSideEffectServiceTests"` (no-restore).

Broad-validation trigger: none.

Safe alternative candidates:
- Port `Item.isEquipable(player)` compound check (race/class/level validation).
- Enable live `VortexInvasionRuntime.AddDefender` on acceptance (broad-validation trigger).
- Move to a different Phase 6 system (NPC spawn engine, player stats, combat damage).

## Context Needed By Next Session

- `ItemTemplateSummary.IsStorableInAccountWarehouse` and `IsStorableInLegionWarehouse` now correctly exclude soulbound items.
- `ItemTemplateSummary.IsStorableInWarehouse` (regular WH) has NO soulbound guard — matches Java.
- `ItemTemplateSummary.IsSellable` still only checks the mask bit — Java adds `!isSoulBound()` guard.
- `ItemTemplateSummary.IsTradeable` — check if Java adds any additional guard beyond the mask bit.
- Full session summary in `docs/Phase-6-Session-2526-Handoff.md` (13 UOWs this session: 2514-2526).
