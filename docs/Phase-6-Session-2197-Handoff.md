# Phase 6 Session 2197 Handoff - FindGroup Mutation C# Validator Fixture Alignment

Date: 2026-06-02
Unit of Work: UOW-2197
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, Java capture fixture scaffold, Java test-side instrumentation scaffold, Java test-side scenario builder, Java test-side serializer scaffold, Java test-side artifact writer scaffold, Java test-side artifact validator scaffold, C# deterministic Java fixture validator alignment, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2197 Summary

This UOW added a focused C# validator test that feeds deterministic Java fixture-shaped action `2` and `6` JSON into `FindGroupMutationPostJavaTraceArtifactValidatorService`.

The test proves the C# validator accepts the same fixture row shape used by the Java test-side scenario builder:

- action `2`: `Recruitment`, active player `1001`, mutated entry `2002`, race `ELYOS`, posted system message id `1400392`, refreshed list action `0`,
- action `6`: `Application`, active/mutated player `4004`, race `ASMODIANS`, posted system message id `1400393`, refreshed list action `4`,
- `traceSource=Java`,
- zero world broadcasts and invite dispatches,
- executor and registry observations remain false because these are fixture rows, not live runtime rows.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureScenarioBuilder`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactValidatorServiceTests`

Only the C# test file changed in this UOW; no C# production code changed.

## Validation In UOW-2197

Validation decision:

- Changed surface: focused C# test-only validator contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests" --no-restore
```

- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because no broad trigger applied; the filtered C# test already built the affected project/dependencies.
- Why this scope is sufficient: the filtered C# command proves the validator accepts the deterministic Java fixture JSON shape and keeps existing validator rejection tests passing, while the focused Maven command confirms the Java fixture scenario source remains stable.

Result:

- C# filtered test: passed 8, failed 0, skipped 0. Existing nullable/xUnit analyzer warnings were emitted from unrelated files during build.
- Java/Maven: passed 20, failed 0, skipped 0.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactValidatorServiceTests`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureScenarioBuilder.java` | C# Validator Fixture Alignment Test | Partial | Unit Tested | Partial Parity | The C# validator accepts deterministic Java fixture JSON rows for action `2`/`6`, but production `CM_FIND_GROUP` does not call hooks and no runtime-backed artifact row is captured. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactValidatorServiceTests` | Mutation-Post Artifact Validator Contract | Partial | Unit Tested | Partial Parity | The C# validator accepts Java-derived fixture mappings for `addRecruitment` and `addApplication`, but production hook integration, repository artifact generation, C# artifact validation from disk, live C# rows, registry observation, and comparison execution are missing. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Production Java hook integration, runtime-backed Java artifact generation, C# repository artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The deterministic Java fixture JSON is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add production Java hook placement design-to-code preflight tests that assert the planned hook insertion points around `FindGroupService.addRecruitment/addApplication` would observe mutation-before-posted-message-before-refreshed-list ordering without writing artifacts or changing production send order.

Safe candidates:

- Add production hook integration only after deciding exact no-op hook placement and proving it does not alter `PacketSendUtility` ordering.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add fixture-side payload byte construction only if future Java packet parser capture needs deterministic raw packet bytes.

## Files Changed In UOW-2197

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2197-Completion.md`
- `docs/Phase-6-Session-2197-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2197] Align mutation Java fixture JSON with C# validator
```
