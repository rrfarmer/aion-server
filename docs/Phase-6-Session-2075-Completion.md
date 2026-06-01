# Phase 6 Session 2075 Completion - AutoGroup Static Data Corpus Evidence

Date: 2026-06-01
Unit of Work: UOW-2075
Status: Completed

## Scope

- Added full-corpus evidence for the C# `StaticData.AutoGroups` table introduced in UOW-2073.
- Compared C# loaded auto-group data directly against Java's source XML file.
- Kept production code unchanged in this unit.

## What Changed

- Extended `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts`.
- Added helper assertions against `game-server/data/static_data/auto_group/auto_group.xml`:
  - total `<auto_group>` element count;
  - `StaticData.GetElementCount("auto_group")`;
  - `StaticData.AutoGroups.Count`;
  - representative templates for mask ids `302`, `303`, and `401`;
  - representative portal-NPC recruitable mask lists;
  - all distinct recruitable mask ids using Java `AutoGroup.isRecruitableInstance` logic.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests" --no-restore`
  - Result: passed, 22 tests.
- Java/Maven:
  - Not run for this UOW.
  - Rationale: this unit was test-only C# evidence comparing directly against the Java static-data XML source and Java source-reviewed `AutoGroupData`/`AutoGroup.isRecruitableInstance` logic. No Java packet/parser/runtime code changed, and no narrower Java test for `AutoGroupData` was available in this workspace.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit changed tests only.
  - Focused static-data tests exercised the real merged Java static-data manifest and the new auto-group corpus assertions.
  - No production code, packet primitives, crypto, persistence writes, live connection dispatch case, packet sends, or live side effects were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.AutoGroupData` | `Aion.GameServer.Dataholders.AutoGroupTable` | Data Holder | Partial | Unit Tested / Regression Tested | Partial Parity | Corpus test compares Java `auto_group.xml` element count, C# table count, portal-NPC masks, and all recruitable masks. Full Java `DataManager.AUTO_GROUP` singleton lifecycle and all callers remain partially verified. |
| `com.aionemu.gameserver.model.autogroup.AutoGroup` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Static Data DTO | Partial | Unit Tested / Regression Tested | Partial Parity | Corpus test compares representative XML attributes and Java `isRecruitableInstance` predicate branches against C# summary behavior. `AutoGroupType` enum behavior is still not ported. |
| `com.aionemu.gameserver.dataholders.StaticData` / `DataManager.AUTO_GROUP` | `Aion.GameServer.Dataholders.StaticData.AutoGroups` | Static Data Bridge | Partial | Regression Tested | Partial Parity | Real manifest load now asserts `auto_group` element count and C# table count match Java source XML. Startup logging/static singleton semantics remain out of scope. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` auto-group assertions | Regression | Java `auto_group.xml`, `AutoGroupData.afterUnmarshal`, and `AutoGroup.isRecruitableInstance` source review | C# full static-data load contains all Java auto-group rows and builds representative recruitable mask indexes | C# regression assertions directly compare against Java XML source | Does not execute Java `DataManager` at runtime |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside this unit.
- No Java runtime comparison of `DataManager.AUTO_GROUP` was performed.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/Phase-6-Session-2075-Completion.md`
- `docs/Phase-6-Session-2075-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization and decide whether the disabled planner needs additional runtime facts before live find-group dispatch can be considered.

Safe alternative candidates:

- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Start a live-dispatch readiness checklist for `CM_FIND_GROUP` now that action `10` config/data facts are sourced.
- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
