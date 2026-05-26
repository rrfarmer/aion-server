# Phase 6XZ Completion - UOW-1138 NPC Dialog Generated Select Names

Date: May 26, 2026

## Unit Of Work

UOW-1138: `[Phase 6][UOW-1138] Derive NPC dialog generated select names`

## Summary

UOW-1138 replaces generated `SELECT*` placeholder names in `DialogActionRegistry` with exact Java names. The implementation derives names algorithmically for Java's generated pre-order select trees, including `SELECT0`, `SELECT_NONE`, the special `SELECT1_*_5` constants, and the `SELECT11` through `SELECT15` range.

This remains non-live adapter/registry work. It does not call production `GameServerConnection`, live `DataManager`, logger fanout, packet sends, packet serialization, Java reflection initialization, or Java runtime comparison.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/DialogActionRegistry.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/DialogActionRegistryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XZ-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "DialogActionRegistryTests\|NpcTemplateTableTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 55 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,137 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,344 tests. |

## Migration Parity Table - UOW-1138

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Services.DialogActionRegistry` | Utility / Registry | Partial | Unit Tested | Partial Parity | Generated `SELECT*` names now match every Java source constant via parser-backed tests. C# still uses explicit/algorithmic derivation rather than Java reflection, and full non-SELECT reflected-map parity remains partial. |
| `com.aionemu.gameserver.model.DialogAction.nameOf` | `DialogActionRegistry.NameOf` | Utility Method | Partial | Unit Tested | Partial Parity | Known/unknown behavior for generated `SELECT*` constants and gaps is now covered against Java source. Java runtime reflection initialization and duplicate-id exception behavior are not executed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | Top-level staged adapter now surfaces exact `SELECT1` metadata for generated select actions. Production packet route remains disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `DialogActionRegistryTests.NameOf_ReturnsExactNamesForGeneratedSelectConstants` | Representative generated select ids, including `SELECT0`, `SELECT_NONE`, special `_5` constants, and `SELECT15_4_4_4_4`, return exact names. | Source-reviewed Java constants. |
| `DialogActionRegistryTests.NameOf_MatchesEveryJavaGeneratedSelectConstant` | Every Java source constant whose name starts with `SELECT` has an exact C# registry result. | Deterministic parser over Java `DialogAction.java`. |
| `DialogActionRegistryTests.NameOf_ReturnsUnknownForJavaGaps` | `5107` and `6499` join existing gap coverage as unknown ids. | Source-reviewed/generated range gaps. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DerivesGeneratedSelectRangeAsKnownDialogAction` | Generated select action now carries exact `SELECT1` metadata before target branching. | Source-reviewed Java first gate plus registry exact-name coverage. |

## Remaining Risks

- `DialogActionRegistry` still does not execute Java reflection and does not model duplicate-id initializer exceptions.
- Non-`SELECT` constants are covered by explicit/derived rules, not a full source-generated table for every Java public field.
- Production dialog routing, logger fanout, live `DataManager`, packet sends, threading, and Java runtime comparison remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 partial exact generated-name derivation
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: Java reflection initializer semantics, full all-field generated table, production routing/logger fanout, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit removes the generated `SELECT*` exact-name placeholder gap from the staged dialog action registry.

## Next Recommended Unit Of Work

Either add a full Java-source parser/golden comparison for every `DialogAction` public constant, not only generated `SELECT*`, or begin the talk-range geometry audit around Java `PositionUtil.isInTalkRange` before production dialog routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Full dialog action source comparison | `DialogActionRegistryTests.cs`, possibly `DialogActionRegistry.cs` | Medium | Parser already exists for generated selects; broaden carefully to all constants. |
| B | Talk-range geometry audit | New non-live range planner/tests | Medium | Independent if it avoids dialog registry/planner files. |
| C | Trade-list adapter runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Independent analysis task; avoid production routing. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only talk-range Java/C# dependency audit | Read-only | All writes |
| Agent B | Analyze full `DialogAction` parser coverage gaps | Read-only or separate notes only | `DialogActionRegistry.cs`, docs, planner files unless explicitly assigned |

Keep implementation sequential if changing `DialogActionRegistry.cs` or shared progress/handoff docs.

## Do Not Parallelize

- `DialogActionRegistry.cs`: single-owner for all registry behavior.
- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing NPC dialog adapter/controller/service planner files: assign exclusive ownership for composition work.
- Phase 6 progress/handoff docs: orchestrator-owned.
