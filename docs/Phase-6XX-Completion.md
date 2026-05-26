# Phase 6XX Completion - UOW-1136 NPC Dialog Action Registry Audit

Date: May 26, 2026

## Unit Of Work

UOW-1136: `[Phase 6][UOW-1136] Add NPC dialog action registry audit`

## Summary

UOW-1136 adds a staged dialog action registry audit surface for Java `DialogAction.nameOf` and `NpcData.init` warning behavior. The top-level NPC dialog target adapter now derives unknown dialog action status from a C# registry before target branching, matching Java `CM_DIALOG_SELECT.runImpl`'s first gate more closely than a caller-supplied boolean alone.

This remains staged only. It does not call production `GameServerConnection`, live `DataManager`, logger fanout, packet sends, packet serialization, or Java runtime comparison.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/DialogActionRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NpcTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/DialogActionRegistryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcTemplateTableTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XX-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "DialogActionRegistryTests\|NpcTemplateTableTests\|QuestDialogNpcTargetBranchPlanServiceTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 56 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,126 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,333 tests. |

## Migration Parity Table - UOW-1136

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Services.DialogActionRegistry` | Utility / Registry | Partial | Unit Tested | Partial Parity | Models `nameOf` known/unknown gating for fixed constants, linear families, and known generated ranges. Full reflected-field map and exact generated `SELECT*` names are not fully ported. Reflection behavior differs intentionally by using explicit/derived rules. |
| `com.aionemu.gameserver.model.DialogAction.nameOf` | `DialogActionRegistry.NameOf` | Utility Method | Partial | Unit Tested | Partial Parity | Returns null for Java gaps and metadata for known ids. Exact names are partial; generated range labels are conservative placeholders with `NameIsExact=false`. |
| `com.aionemu.gameserver.dataholders.NpcData.init` | `Aion.GameServer.Dataholders.NpcTemplateTable.GetUnknownFunctionDialogIds` | Static Data Audit | Partial | Unit Tested | Partial Parity | Java logs unknown function dialog ids during async dataholder init. C# exposes deterministic metadata instead of logging; no logger side effect or Java startup comparison. |
| `com.aionemu.gameserver.dataholders.NpcData.isFunctionDialog` | `NpcTemplateTable.IsFunctionDialog` plus unknown-action metadata | Static Data Holder | Partial | Unit Tested | Partial Parity | C# still aggregates all function dialog ids, including unknown ids, matching Java set behavior. Unknown warning is now inspectable. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | Top-level staged adapter now derives unknown dialog action status before target branching using registry metadata. Production packet route remains disabled. |
| `com.aionemu.gameserver.model.templates.npc.NpcTemplate.supportsAction` | `NpcTemplateSummary.SupportsDialogAction` | Template Helper | Partial | Existing Unit Coverage | Needs Verification | This unit only audits action-name inputs around existing support checks. Collection/null behavior remains source-reviewed but not Java-runtime compared. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `DialogActionRegistryTests.NameOf_ReturnsExactNamesForPortedFixedJavaConstants` | Exact names for fixed ids used by staged dialog planners. | Source-reviewed Java constants. |
| `DialogActionRegistryTests.NameOf_DerivesExactNamesForLinearJavaConstantFamilies` | Deterministic linear family names and known status. | Source-reviewed Java constant numbering. |
| `DialogActionRegistryTests.NameOf_KnowsSparseFixedJavaConstantsThatAreNotYetNamedExactly` | Known ids without exact names do not trip Java unknown-action gate. | Source-reviewed Java constants; name string is a C# placeholder. |
| `DialogActionRegistryTests.NameOf_RecognizesGeneratedSelectRangeAsJavaKnown` | First and last known generated select ids are recognized. | Source-reviewed Java generated range. |
| `DialogActionRegistryTests.NameOf_ReturnsUnknownForJavaGaps` | `0`, `102`, `1010`, and outside-range ids behave like Java null. | Source-reviewed Java gaps. |
| `NpcTemplateTableTests.GetUnknownFunctionDialogIds_ReportsJavaDialogActionNameOfGaps` | Unknown function dialog ids are exposed while still participating in the function-dialog set. | Source-reviewed Java warning and set aggregation order. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DerivesUnknownDialogActionFromRegistryBeforeTargetBranching` | Unknown action id is rejected before target branch planning. | Source-reviewed Java first gate. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DerivesGeneratedSelectRangeAsKnownDialogAction` | Generated select-range action remains known and can continue to target branching. | Source-reviewed Java known generated range. |

## Remaining Risks

- Full Java `DialogAction` reflected-field map is not mechanically generated in C#.
- Exact names for all generated `SELECT*` actions and some sparse fixed actions remain placeholders.
- C# exposes unknown NPC function dialog ids as metadata instead of Java logger warnings.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not consume the staged top-level adapter.
- Live `DataManager`, packet sends, logger fanout, threading, reflection initialization behavior, and Java runtime comparison remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 partial registry/audit surface plus top-level adapter composition
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: full reflected constant map, exact generated action names, Java logger fanout, production routing, live DataManager integration, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit makes unknown dialog-action gating and NPC function-dialog warning metadata explicit in the staged planner.

## Next Recommended Unit Of Work

Add a source-generated or tool-generated exact `DialogAction` name table from Java `DialogAction.java`, replacing placeholder sparse/generated names without changing the staged planner API. If generated table work is too broad for one slice, begin the talk-range geometry audit instead.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Exact dialog action table generation | `DialogActionRegistry.cs`, generated test/golden data | Medium | Recommended next; parse Java `DialogAction.java` to avoid hand-maintaining thousands of constants. |
| B | Talk-range geometry audit | New non-live range planner/tests | Medium | Read Java `PositionUtil.isInTalkRange` and C# world object geometry mapping before implementation. |
| C | Trade-list adapter runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Identify exact live dependencies before any `GameServerConnection` wiring. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Analyze/generate exact dialog action names from Java source | New generated-data/test helper files only, if used | `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, docs, production routing |
| Agent B | Read-only talk-range Java/C# dependency audit | Read-only | All writes |

Use only if Agent A can avoid shared planner/docs files. Otherwise keep the exact table generation sequential.

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing NPC dialog adapter/controller/service planner files: assign exclusive ownership for composition work.
- `StaticData.cs`: shared parser surface; avoid concurrent edits.
- Phase 6 progress/handoff docs: orchestrator-owned.
