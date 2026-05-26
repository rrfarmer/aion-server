# Phase 6XM Completion - UOW-1125 Function Dialog Static-Data Bridge

Date: May 26, 2026

## Unit Of Work

UOW-1125: `[Phase 6][UOW-1125] Add NPC function dialog static-data bridge`

## Summary

UOW-1125 ports the static-data prerequisite for Java `NpcData.isFunctionDialog`. `NpcTemplateTable` now builds a global function-dialog ID set from all loaded NPC template `FunctionDialogIds`, while keeping per-NPC `SupportsDialogAction` behavior unchanged.

This is staged only. It does not wire production `CM_DIALOG_SELECT`, known-list lookup, interaction guards, controller dispatch, audit logging, or packets.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/NpcTemplateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcTemplateTableTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XM-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcTemplateTableTests\|StaticDataLoadingTests" --nologo` | Passed: 19 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,239 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1125

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.NpcData` | `Aion.GameServer.Dataholders.NpcTemplateTable.IsFunctionDialog` | Static Data Repository | Partial | Unit Tested + Static Data Tested | Partial Parity | C# now aggregates all NPC template function dialog IDs into a global lookup like Java `functionDialogIds`. Java async static-data init timing and unknown `DialogAction.nameOf` warning behavior are not ported here. |
| `com.aionemu.gameserver.model.templates.npc.NpcTemplate` | `Aion.GameServer.Dataholders.NpcTemplateSummary.SupportsDialogAction` | Static Template DTO | Partial | Unit Tested + Existing Static Data Tested | Needs Verification | Tests preserve per-template action support separately from the new global function-dialog lookup. Full Java JAXB `TalkInfo` behavior and template validation warnings remain unverified. |
| `com.aionemu.gameserver.model.templates.npc.TalkInfo` | `Aion.GameServer.Dataholders.NpcTemplateSummary.FunctionDialogIds` | Static DTO Dependency | Partial | Static Data Tested | Needs Verification | Existing XML loader already reads `func_dialogs`; this unit validates those IDs now also feed the global lookup. Serialization and malformed/unknown-action warning parity remain incomplete. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchPlanService` plus future static-data input adapter | Packet Dependency | Partial | Existing Unit Coverage | Needs Verification | The new lookup can supply the planner's `IsFunctionDialog` input later, but production socket routing and non-self branch input assembly remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcTemplateTableTests.IsFunctionDialog_ReturnsTrueWhenAnyTemplateDeclaresDialogAction` | Global function-dialog lookup returns true when any NPC template declares the dialog action and false otherwise. | Source-reviewed Java `NpcData.init` global set aggregation. |
| `NpcTemplateTableTests.IsFunctionDialog_DoesNotMakeEveryNpcSupportTheGlobalDialogAction` | Global function-dialog recognition remains separate from per-NPC support. | Source-reviewed Java distinction between `NpcData.isFunctionDialog` and `NpcTemplate.supportsAction`. |
| `StaticDataLoadingTests.LoadsStaticDataFromXmlDirectory` | Real static-data broker function dialog ID `33` feeds both per-template support and global function-dialog lookup. | Deterministic C# XML load coverage; no Java runtime comparison. |

## Remaining Risks

- `NpcData.init` also warns for unknown dialog actions through Java `DialogAction.nameOf`; C# does not yet validate or log unknown function dialog IDs at load time.
- The new lookup is not yet wired into `QuestDialogNpcTargetBranchPlanService` input assembly or `GameServerConnection.HandleDialogSelectAsync`.
- Java JAXB/load timing and C# XML parsing behavior may still diverge for malformed or duplicate `func_dialogs`.
- Controller dispatch, interaction guards, audit logging, and packet sends remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 partial static-data repository bridge
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: unknown dialog action warnings, live input adapter, production socket routing, and Java runtime static-data comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit removes one static-data prerequisite for the non-self dialog branch.

## Next Recommended Unit Of Work

Add a read-only input adapter planner that composes `QuestDialogNpcTargetBranchPlanService` inputs from C# NPC template data: derive `IsFunctionDialog` from `NpcTemplateTable.IsFunctionDialog`, derive `NpcSupportsAction` from the target `NpcTemplateSummary`, and leave known-list/player interaction/controller dispatch as explicit disabled dependencies.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | NPC dialog target input adapter planner | New service/tests near quest dialog planning | Medium | Recommended next; should remain non-live. |
| B | DialogService interaction audit | Read-only audit doc | Medium | Map summon/sub-dialog restrictions before live interaction guard. |
| C | NpcController dialog dispatch audit | Read-only audit doc | Medium | Map talk-range, AI, and `DialogService` fallback dependencies. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `NpcTemplateTable.cs` and static-data loader paths: use exclusive ownership if touched.
- Phase 6 progress/handoff docs: orchestrator-owned.
