# Phase 6 Session 2526 Completion

## UOW

[Phase 6] UOW-2526: Add soulbound guard to IsStorableInAccountWarehouse and IsStorableInLegionWarehouse

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `com.aionemu.gameserver.model.gameobjects.Item.isStorableInAccWarehouse()` — `(mask & STORABLE_IN_AWH) == STORABLE_IN_AWH && !isSoulBound()`
- `com.aionemu.gameserver.model.gameobjects.Item.isStorableInLegWarehouse()` — `(mask & STORABLE_IN_LWH) == STORABLE_IN_LWH && !isSoulBound()`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogSideEffectServiceTests.cs`

## Implementation Notes

- Updated `IsStorableInAccountWarehouse` to add `&& !IsSoulBound` (matches Java `!isSoulBound()` guard).
- Updated `IsStorableInLegionWarehouse` to add `&& !IsSoulBound` (same guard).
- `IsStorableInWarehouse` has NO soulbound guard in Java — kept as mask-only check (intentional difference from AWH/LWH).
- Extended the `ItemMaskPropertiesMatchJavaItemMaskConstants` test to cover:
  - AWH/LWH storable returns true when bit is set and not soulbound
  - AWH/LWH storable returns false when bit is set BUT item is soulbound (Java parity)
  - All-bits-set template with SOUL_BOUND bit set → AWH/LWH false

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ItemTemplateSummary_ItemMaskPropertiesMatchJavaItemMaskConstants` (extended) | Unit | `Item.isStorableInAccWarehouse/isStorableInLegWarehouse` source review | Soulbound items are rejected from AWH/LWH despite having the mask bit set; warehouse (WH) has no soulbound guard | Focused C# unit test validates all three cases: bit-set-not-soulbound, bit-set-soulbound, zero-mask | No Java fixture/golden |

## Validation Decision

- Changed surface: two property expressions updated; test extended.
- Specific behavior/contract: Java's AWH/LWH storability checks include `!isSoulBound()` beyond the mask bit; WH does not.
- Focused C# command: `dotnet test ... --filter "FullyQualifiedName~NpcDialogSideEffectServiceTests" --no-restore`
- Result: Passed, 7 tests (unchanged count — existing test extended).
- Focused Java/Maven command: skipped.
- Broad-validation trigger: none.
- Why sufficient: focused test covers the soulbound guard with direct mask + soulbound combinations.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `Item.isStorableInAccWarehouse()` | `ItemTemplateSummary.IsStorableInAccountWarehouse` | Property | Complete | Unit Tested | Verified Parity | Now includes `!IsSoulBound` guard matching Java. |
| `Item.isStorableInLegWarehouse()` | `ItemTemplateSummary.IsStorableInLegionWarehouse` | Property | Complete | Unit Tested | Verified Parity | Now includes `!IsSoulBound` guard matching Java. |
