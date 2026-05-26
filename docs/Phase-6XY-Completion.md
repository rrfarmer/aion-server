# Phase 6XY Completion - UOW-1137 NPC Dialog Fixed Action Names

Date: May 26, 2026

## Unit Of Work

UOW-1137: `[Phase 6][UOW-1137] Fill NPC dialog fixed action names`

## Summary

UOW-1137 tightens `DialogActionRegistry` exact-name coverage for Java's sparse fixed dialog action constants before the generated `SELECT*` range. Remaining non-exact dialog action names are now limited to the huge generated `SELECT1` through `SELECT15_4_4_4_4` family.

This remains staged only. It does not call production `GameServerConnection`, live `DataManager`, logger fanout, packet sends, packet serialization, Java reflection initialization, or Java runtime comparison.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/DialogActionRegistry.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/DialogActionRegistryTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XY-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "DialogActionRegistryTests\|NpcTemplateTableTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 45 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,127 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,334 tests. |

## Migration Parity Table - UOW-1137

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Services.DialogActionRegistry` | Utility / Registry | Partial | Unit Tested | Partial Parity | Fixed pre-`SELECT1` constants now have exact names or deterministic derived exact names for linear reward families. Generated `SELECT*` exact names remain placeholders with `NameIsExact=false`. |
| `com.aionemu.gameserver.model.DialogAction.nameOf` | `DialogActionRegistry.NameOf` | Utility Method | Partial | Unit Tested | Partial Parity | Null gap behavior remains covered; fixed sparse names are improved. Reflection field enumeration is still represented manually/derivationally rather than by Java runtime reflection. |
| `com.aionemu.gameserver.dataholders.NpcData.init` | `NpcTemplateTable.GetUnknownFunctionDialogIds` consuming `DialogActionRegistry` | Static Data Audit Dependency | Partial | Existing Unit Coverage | Partial Parity | Unknown function-dialog audit now benefits from exact fixed-name coverage. Logger side effect and Java startup comparison remain absent. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `DialogActionRegistryTests.NameOf_ReturnsExactNamesForPortedFixedJavaConstants` | Sparse fixed ids including `RESURRECT_PET`, `OPEN_PERSONAL_WAREHOUSE`, `AP_SELL`, and `TRADE_IN_UPGRADE` return exact Java names. | Source-reviewed Java constants. |

## Remaining Risks

- Generated `SELECT1` through `SELECT15_4_4_4_4` names are still range placeholders, not exact Java strings.
- `DialogActionRegistry` still does not mechanically mirror Java reflection over every public field.
- Production dialog routing, logger fanout, live `DataManager`, packet sends, and Java runtime comparison remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 partial exact-name coverage improvement
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: generated `SELECT*` exact names, full reflected constant map, production routing/logger fanout, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit narrows the dialog action registry gap by making sparse fixed action names exact.

## Next Recommended Unit Of Work

Generate or derive exact names for the generated `SELECT*` action range from Java `DialogAction.java`, preferably through a deterministic source parser/golden test so the C# registry does not hand-maintain thousands of names.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Generated `SELECT*` exact-name derivation | `DialogActionRegistry.cs`, focused tests, optional generated fixture | Medium | Recommended next; parse Java `DialogAction.java` rather than manual transcription. |
| B | Talk-range geometry audit | New non-live range planner/tests | Medium | Independent if it avoids dialog registry/planner files. |
| C | Trade-list adapter runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Independent analysis task; avoid production routing. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only/generated-name analysis for `SELECT*` constants | Read-only Java analysis or new generated fixture files | `DialogActionRegistry.cs`, docs, planner files unless explicitly assigned |
| Agent B | Read-only talk-range dependency audit | Read-only | All writes |

If exact registry implementation is attempted, keep it sequential because `DialogActionRegistry.cs` is a shared surface.

## Do Not Parallelize

- `DialogActionRegistry.cs`: single-owner when implementing exact generated names.
- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing NPC dialog adapter/controller/service planner files: assign exclusive ownership for composition work.
- Phase 6 progress/handoff docs: orchestrator-owned.
