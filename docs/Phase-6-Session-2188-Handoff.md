# Phase 6 Session 2188 Handoff - FindGroup Mutation Projected Row Comparison Dry-Run Contract

Date: 2026-06-02
Unit of Work: UOW-2188
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Full .NET suite/full solution build is not routine validation. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, mutation-post comparison key-projection metadata, mutation-post artifact comparison preflight, mutation-post Java artifact capture runbook, mutation-post C# live trace-row fixture plan, mutation-post trace-row readiness aggregate, mutation-post comparison execution result contract, mutation-post comparison input envelope, mutation-post comparison execution blocker report, and mutation-post projected-row comparison dry-run contract exist as non-live readiness artifacts.

## UOW-2188 Summary

This UOW added `FindGroupMutationPostProjectedRowComparisonDryRunContractService`, a non-live dry-run contract for a future action `2`/`6` projected-row comparison executor.

The contract names:

- required equality input fields,
- ignored runtime context fields,
- planned output kinds,
- Java action-specific packet expectations.

It is blocked by the execution blocker report by default and does not execute comparison or produce mismatch results.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContract`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunField`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunAction`
- `Aion.GameServer.Tests.FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests`

## Validation In UOW-2188

Validation decision:

- Changed surface: focused non-live projected-row comparison dry-run contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist for row comparison.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new dry-run contract and its immediate blocker-report, result-contract, and envelope dependencies without spending time on unrelated suites.

Result:

- Passed: 22
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Projected Row Comparison Dry-Run Contract | Blocked | Unit Tested | Partial Parity | The contract defines future equality inputs and output shape for action `2`/`6`, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionBlockerReportService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService` | Mutation-Post Projected Row Comparison Dry-Run Contract | Partial | Unit Tested | Partial Parity | The dry-run contract preserves Java mutation-before-posted-message-before-refreshed-list fields and action-specific packet ids, but it only names planned executor shape and proves no runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a projected-row comparison result skeleton for action `2`/`6` that can represent matched/missing/mismatched rows using the dry-run output contract, while still refusing to instantiate real comparison results until live Java/C# rows exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2188

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2188-Completion.md`
- `docs/Phase-6-Session-2188-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2188] Add find group mutation comparison dry-run contract
```
