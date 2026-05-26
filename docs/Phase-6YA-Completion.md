# Phase 6YA Completion - UOW-1139 Dialog Action Full Source Coverage

Date: May 26, 2026

## Unit Of Work

UOW-1139: `[Phase 6][UOW-1139] Broaden dialog action source coverage`

## Summary

UOW-1139 broadens the staged dialog-action registry parity test from generated `SELECT*` constants to every public Java `DialogAction` integer constant. The C# registry implementation was already exact for the currently parsed source; this unit adds the mechanical guard that proves future registry changes continue matching the Java source table.

This remains non-live registry/test work. It does not call production `GameServerConnection`, live `DataManager`, logger fanout, packet sends, packet serialization, Java reflection initialization, or Java runtime comparison.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/DialogActionRegistryTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YA-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "DialogActionRegistryTests\|NpcTemplateTableTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 56 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,138 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,345 tests. |

## Migration Parity Table - UOW-1139

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Services.DialogActionRegistry` | Utility / Registry | Partial | Unit Tested | Partial Parity | Every Java public integer constant in `DialogAction.java` is now parser-checked against exact C# `NameOf` output. C# still uses explicit/algorithmic derivation instead of Java reflection; Java runtime initialization and duplicate-id exception behavior are not executed. |
| `com.aionemu.gameserver.model.DialogAction.nameOf` | `DialogActionRegistry.NameOf` | Utility Method | Partial | Unit Tested | Partial Parity | Full source-level positive map coverage now exists for 6,205 constants, plus selected source-reviewed gap checks. Unknown-id space is not exhaustively enumerated, and runtime reflection side effects remain unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `DialogActionRegistryTests.NameOf_MatchesEveryJavaPublicConstant` | Every parsed Java public dialog-action constant is known, exact, and name-equal in C#. | Deterministic parser over Java `DialogAction.java`. |
| `DialogActionRegistryTests.JavaDialogActionPublicConstants_HaveUniqueIdsForNameOfMap` | Current Java source has no duplicate public action ids, matching the successful reflected-map initialization assumption. | Deterministic parser over Java `DialogAction.java`; found 0 duplicate ids. |

## Remaining Risks

- `DialogActionRegistry` still does not execute Java reflection and cannot prove Java static-initializer exception behavior at runtime.
- Unknown-id behavior is covered by selected gaps, not exhaustive complement-space generation.
- Production dialog routing, logger fanout, live `DataManager`, packet sends, packet serialization, threading, and Java runtime comparison remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts; 1 registry parity verification surface broadened
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: Java runtime reflection initialization, exhaustive unknown-id complement testing, production routing/logger fanout, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit removes the remaining full-source `DialogAction` constant coverage gap from the staged registry tests.

## Next Recommended Unit Of Work

Begin the talk-range geometry audit around Java `PositionUtil.isInTalkRange` for NPCs and house objects, then decide whether a small non-live C# geometry helper/test surface can replace the current caller-supplied `IsInTalkRange` fact before production dialog routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Talk-range Java/C# audit | `PositionUtil.java`, current C# dialog request/controller planner tests; read-only first | Medium | Inspect NPC and house-object formulas, radii inputs, and current C# callers before adding helper code. |
| B | Trade-list runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Independent if it avoids dialog registry and talk-range helper files. |
| C | Dialog action unknown-id complement audit | New tests only | Low/Medium | Could add bounded gap/property coverage, but avoid exhaustive giant unknown-space tests. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only audit of Java `PositionUtil.isInTalkRange` and C# NPC dialog range callers | Read-only | All writes |
| Agent B | Read-only trade-list runtime-readiness audit | Read-only | All writes |

Keep implementation sequential if adding a C# talk-range helper or updating shared dialog planner/progress docs.

## Do Not Parallelize

- Shared dialog planner/request service files unless one owner is assigned.
- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `DialogActionRegistry.cs` and `DialogActionRegistryTests.cs`: registry source coverage is now guarded; keep future edits single-owner.
- Phase 6 progress/handoff docs: orchestrator-owned.
