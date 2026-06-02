# Phase 6 Session 2208 Completion - FindGroup Mutation Guarded Fixture Result Contract

Date: 2026-06-02
Unit of Work: UOW-2208
Status: Completed

## Scope

This unit added a non-live guarded fixture result contract for future `CM_FIND_GROUP` action `2`/`6` mutation-post C# boundary rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonInputEnvelopeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `FindGroupMutationPostGuardedFixtureResultContractService`.
- The contract records:
  - explicit trace guard requirement,
  - production `ProcessPacketAsync` `CmFindGroup` dispatch remains deferred,
  - packet sending is disabled by default,
  - action `2` and action `6` accepted-live-row requirements,
  - executor and registry observation requirements,
  - zero world-broadcast/invite side-effect guard,
  - comparison-envelope handoff requirement.
- Added candidate-row classification for future trace exports:
  - accepts only C# action `2`/`6` rows with boundary acceptance, executor observation, registry send ordering, expected posted-message/refreshed-list packet ids, and zero broadcast/invite counts,
  - rejects disabled shape rows as non-live,
  - rejects unsupported action, non-C# source, missing boundary/executor/registry observations, unexpected packet shape, or unexpected side effects.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only guarded fixture result contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. This unit does not modify `GameServerConnection.ProcessPacketAsync`, live dispatch, shared runtime primitives, packet primitives, persistence, common world state, or live side effects.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the added service is a non-live result contract that depends on the existing guarded skeleton and mutation-post trace schema; focused tests cover the contract plus both adjacent surfaces.

Result:

- Focused C# command: passed 15, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Client Packet Boundary / Fixture Result Contract | Partial | Unit Tested | Partial Parity | Contract defines accepted future C# live rows for actions `2`/`6` but does not run the boundary or send packets. Live rows, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Service Mutation / Fixture Result Contract | Partial | Unit Tested | Partial Parity | Action `2` accepted rows must match Java recruitment mutation-post packet shape: posted system message `1400392`, refreshed action `0`, boundary/executor/registry observed, zero broadcast/invite counts. Evidence is contract-level only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Service Mutation / Fixture Result Contract | Partial | Unit Tested | Partial Parity | Action `6` accepted rows must match Java application mutation-post packet shape: posted system message `1400393`, refreshed action `4`, boundary/executor/registry observed, zero broadcast/invite counts. Evidence is contract-level only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostGuardedFixtureResultContractServiceTests.Create_DefaultContractIsNonLiveAndDoesNotSendPacketsByDefault` | C# unit | Java action `2`/`6` boundary remains future live evidence | Contract is non-live, keeps production dispatch disabled, and sends no packets by default. | Focused C# test. | No live boundary execution. |
| `Create_DefaultRequirementsBlockOnMissingActionTwoAndSixRows` | C# unit | Java `CM_FIND_GROUP.runImpl` action `2`/`6` mappings | Contract requires one accepted live boundary row for each action. | Focused C# test. | No runtime row capture. |
| `Create_RejectsDisabledShapeRowsWithoutMarkingThemLive` | C# unit | Disabled C# projections are not Java/C# runtime evidence | Disabled sample exports remain shape-valid but non-live. | Focused C# test. | No executor/registry observation. |
| `Create_AcceptsActionTwoAndSixLiveRowsForComparisonHandoff` | C# unit | Java posted-message/refreshed-list action mapping | Candidate rows with boundary, executor, registry, expected packets, and zero side effects can satisfy the handoff contract. | Focused C# test with synthetic live-shaped rows. | Synthetic rows are not captured runtime evidence. |
| `Create_RejectsRowsWithUnexpectedSideEffectsOrPacketShape` | C# unit | Java action `2`/`6` mutation-post traces do not broadcast or invite | Contract rejects mismatched system-message ids and unexpected world-broadcast side effects. | Focused C# test. | No live packet send observation. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# fixture-result contract service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, executor observation, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The guarded fixture result contract is metadata/classification only and cannot prove Java/C# runtime parity.
- Synthetic live-shaped rows in tests prove contract classification, not runtime behavior.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Wire the guarded fixture result contract into the mutation-post readiness/comparison handoff so future accepted live rows can flow into the existing input envelope without treating disabled projections as live.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedFixtureResultContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostGuardedFixtureResultContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2208-Completion.md`
- `docs/Phase-6-Session-2208-Handoff.md`
