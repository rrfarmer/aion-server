# Phase 6 Session 2278 Handoff - Runtime Row Intake Handoff Evidence

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2278-Completion.md`
- `docs/Phase-6-Session-2278-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless a broad-validation trigger from `docs/orchestration-rules.md` is named before the command runs.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2278 surfaced value-projection handoff row evidence inside the runtime-row-value evidence intake gate.

Updated artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateServiceTests.cs`

The `ValueProjectionHandoff` row now includes `valueProjectionHandoffRows`, preserving row-pairing and accepted-boundary-row handoff evidence from the value-projection handoff gate.

No Java source, fixtures, live dispatch, packet sends, runtime comparison, capture execution, executable implementation, or verified parity status changed.

## Java Source Context

Reviewed source-of-truth Java:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Relevant Java facts:

- Action `2` reads `playerOrTeamId`, `message`, `groupType`, then calls `FindGroupService.addRecruitment(player, message, groupType)`.
- Action `6` reads `playerOrTeamId`, `message`, `groupType`, `classId`, `level`, then calls `FindGroupService.addApplication(player, message, groupType, classId, level)`.
- `addRecruitment` sends posted system message `1400392` before refreshed `SM_FIND_GROUP` action `0`.
- `addApplication` sends posted system message `1400393` before refreshed `SM_FIND_GROUP` action `4`.
- Both mutation-post actions use direct sends, with zero world broadcasts and zero invite dispatches expected.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateServiceTests|FullyQualifiedName~FindGroupMutationPostValueProjectionHandoffGateServiceTests" --no-restore
```

Result: passed 9, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in UOW-2278; Java source was reviewed directly for the non-live metadata context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed runtime-row-value intake gate plus the directly adjacent value-projection handoff gate.

Commit made:

```text
[Phase 6][UOW-2278] Surface handoff evidence in runtime row intake
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Runtime-row-value evidence intake changes: run runtime-row-value evidence intake tests plus directly adjacent value-projection handoff gate tests.
- Typed value-reader implementation readiness gate changes: run typed value-reader implementation readiness gate tests plus directly adjacent runtime-row-value evidence intake tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Surface runtime-row-value intake row evidence inside the typed value-reader implementation readiness gate's runtime-row-value intake stage. Keep the unit metadata-only: the typed reader gate should continue consuming `FindGroupMutationPostRuntimeRowValueEvidenceIntakeGate`, but its stage evidence can preserve the intake rows that now include value-projection and accepted-boundary-row handoff data.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTypedValueReaderImplementationReadinessGateService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTypedValueReaderImplementationReadinessGateServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only update typed-reader readiness metadata and tests.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTypedValueReaderImplementationReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateServiceTests" --no-restore
```

If that command is slow, first narrow to:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTypedValueReaderImplementationReadinessGateServiceTests" --no-restore
```

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
- Add another narrow command-decision consumer only if a current handoff or checklist still points directly to Java capture before `executorConsistencyAuditAccepted`.
- Review checklist strings for readability only if future tests/handoffs become hard to maintain.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates with required field metadata, accepted-boundary-row handoff metadata visible in checklist inventory, Java/C# row-pairing readiness that consumes that handoff, runtime checklist row identity metadata that names that consumer relationship, value-projection handoff metadata that preserves row-pairing handoff evidence, runtime-row-value intake metadata that now preserves value-projection handoff row evidence, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, projected-value materialization blockers, projected-value result-emission blockers, executor evidence bridge metadata, projected-value executor consistency audit metadata, a runtime-comparison handoff gate for that consistency audit, capture acceptance visibility for that gate, command-decision metadata for the next evidence command, checklist visibility for that command-decision gate, and clearer observation blocker wording, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
