# Phase 6 Session 2173 Completion - FindGroup Mutation Java Trace Artifact Validator

Date: 2026-06-02
Unit of Work: UOW-2173
Status: Completed

## Scope

This unit added a validator target for future Java `CM_FIND_GROUP` mutation-post trace artifacts for actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `2` calls `FindGroupService.addRecruitment(player, message, groupType)`, mutates recruitment state, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED()`, then refreshes recruitments with `SM_FIND_GROUP` action `0`.
- Action `6` calls `FindGroupService.addApplication(player, message, groupType, classId, level)`, mutates application state, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()`, then refreshes applications with `SM_FIND_GROUP` action `4`.

This UOW does not add Java instrumentation, does not generate Java trace artifacts, does not capture live C# runtime traces, does not enable live `CM_FIND_GROUP` dispatch, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupMutationPostJavaTraceArtifactValidatorService`.
- Added validation records:
  - `FindGroupMutationPostJavaTraceArtifactValidationReport`,
  - `FindGroupMutationPostJavaTraceArtifactValidationIssue`,
  - `FindGroupMutationPostJavaTraceArtifactMetadata`,
  - `FindGroupMutationPostJavaTraceArtifactValidationTraceRow`.
- Added issue enum `FindGroupMutationPostJavaTraceArtifactValidationIssueCode`.
- The validator rejects invalid JSON, unsupported schema version, unexpected trace name, missing trace rows, missing fields, invalid field types, non-Java trace source, unsupported actions, action mapping mismatches, and nonzero world-broadcast/invite counts.
- The validator derives required fields and action mappings from `FindGroupMutationPostJavaTraceArtifactSchemaReportService`, which itself reuses the mutation-post boundary comparison schema.
- Added representative valid-artifact and rejection tests.
- Updated design notes to record the validator target while keeping runtime comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production Java trace artifact validator service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed and no generated Java trace artifact exists yet. This UOW validates the future artifact shape from reviewed Java source and the schema target.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new validator plus adjacent Java trace schema target, runtime preflight, and mutation-post comparison schema surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 18
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Java Trace Artifact Validator Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post Java trace artifact validator is represented and tied to the schema target, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Java Trace Artifact Validator Target / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Validator enforces Java action `2`/`6` mutation kind, posted system message id, refreshed show-list action, Java trace source, and zero broadcast/invite counts. No Java artifact, live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_AcceptsRepresentativeActionTwoAndSixArtifact` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment/addApplication` source review | Representative action `2`/`6` JSON artifact shape, Java trace source, mutation kind, posted message ids, and refreshed list actions. | Focused validator assertion. | Representative JSON is synthetic; no Java artifact generated. |
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_RejectsUnsupportedSchemaVersion` | Unit | Schema target | Unsupported artifact schema version is rejected. | Focused validator assertion. | No generated artifact path covered. |
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_RejectsMissingTraceRows` | Unit | Schema target | Artifact must contain at least one trace row. | Focused validator assertion. | No file-system artifact discovery. |
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_RejectsMissingRequiredField` | Unit | Mutation-post comparison schema | Missing schema-v1 fields are rejected. | Focused validator assertion. | Field-level semantic values remain limited to validator checks. |
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_RejectsUnsupportedAction` | Unit | Java action dispatch source review | Action outside `2`/`6` is rejected for this mutation-post artifact family. | Focused validator assertion. | Other trace families remain separate. |
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_RejectsActionMappingMismatch` | Unit | Java posted-message/refreshed-list mapping source review | Action-specific mutation kind, posted message id, and refreshed action must match Java mapping. | Focused validator assertion. | Does not validate packet bytes. |
| `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.Validate_RejectsNonJavaTraceSource` | Unit | Schema target | Java artifact rows must use `traceSource` value `Java`. | Focused validator assertion. | C# trace-row validation remains separate. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java trace artifact generation/comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The Java trace artifact validator is non-live contract tooling; no Java instrumentation, Java serializer, generated Java artifact, C# live trace row, encrypted socket capture, or real-client runtime comparison has executed.
- Representative validator JSON is synthetic and does not prove Java can emit the row shape without altering behavior.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java instrumentation design/runbook rows for `FindGroupService.addRecruitment/addApplication` mutation-post trace emission, including where to emit rows without changing Java map/send ordering.

Safe candidates:

- Add a file/directory report service for generated action `2`/`6` Java trace artifacts once artifact paths are chosen.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2173-Completion.md`
- `docs/Phase-6-Session-2173-Handoff.md`
