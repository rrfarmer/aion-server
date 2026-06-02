# Phase 6 Session 2132 Completion - FindGroup Live Dispatch Action Gate Matrix

Date: 2026-06-02
Unit of Work: UOW-2132
Status: Completed

## Scope

- Added a concise action-by-action gate matrix for Java `CM_FIND_GROUP` live-dispatch readiness.
- Mapped each parsed Java action to the remaining blocked live evidence gate.
- Kept live `GameServerConnection.ProcessPacketAsync` `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `readImpl` parses actions `0`/`1`/`2`/`3`/`4`/`5`/`6`/`7`/`8`/`9`/`10`/`11`/`12`/`13`/`15`/`17`/`20`/`25`.
  - `runImpl` executes actions `0`/`1`/`2`/`3`/`4`/`5`/`6`/`7`/`8`/`9`/`10`/`11`/`12`/`13`/`15`/`17`.
  - Actions `20` and `25` are parsed but have no `runImpl` branch.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Reviewed the `FindGroupService` method target for every executable `runImpl` branch.

## What Changed

- Added `FindGroupLiveDispatchActionGateMatrixService`.
- Added focused tests proving:
  - the matrix covers all parsed Java actions and excludes server-packet-only action codes `14` and `16`;
  - actions `1` and `5` map to the world-broadcast gate;
  - actions `3` and `7` map to the shared-singleton lifecycle gate;
  - direct-packet actions map to the direct-packet gate;
  - action `12` maps to both action-12 invite and direct-packet gates;
  - parsed-only actions `20` and `25` remain ready no-ops only.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` with the matrix evidence.

## Validation

- Changed surface:
  - Production readiness-report service plus focused tests and documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLiveDispatchActionGateMatrixServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests" --no-restore`
  - Final result: passed, 6 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.readImpl`/`runImpl` and `FindGroupService` method targets; no focused Java test target was identified for this C# readiness-report matrix.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped readiness-report surface.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl/runImpl` | `Aion.GameServer.Services.FindGroupLiveDispatchActionGateMatrixService` | Readiness Report | Partial | Unit Tested | Partial Parity | Matrix covers every parsed Java action and conservatively maps executable branches to missing live gates. This is readiness tracking, not live behavioral parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupLiveDispatchActionGateMatrixService` | Readiness Report | Partial | Unit Tested | Partial Parity | Matrix records the Java method target for each executable client action, including direct-packet, world-broadcast, singleton-only update, and action `12` invite/decline paths. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupLiveDispatchActionGateMatrixServiceTests.CreateMatrix_CoversEveryParsedJavaActionAndExcludesServerPacketOnlyCodes` | Unit | Java `CM_FIND_GROUP.readImpl/runImpl` | Parsed action coverage includes `20`/`25` and excludes server-packet-only `14`/`16` | Focused C# unit test plus reviewed Java source | Does not execute live dispatch |
| `FindGroupLiveDispatchActionGateMatrixServiceTests.CreateMatrix_MapsExecutableBranchesToRemainingLiveEvidenceGates` | Unit | Java `CM_FIND_GROUP.runImpl`; Java `FindGroupService` | Executable branches remain blocked and map to direct, world-broadcast, or singleton gates | Focused C# unit test plus reviewed Java source | Does not prove live connection-registry order or singleton runtime behavior |
| `FindGroupLiveDispatchActionGateMatrixServiceTests.CreateMatrix_RecordsActionTwelveAndParsedOnlyActionsAsSpecialCases` | Unit | Java `CM_FIND_GROUP.runImpl`; Java `FindGroupService.sendInstanceApplicationResult` | Action `12` keeps invite/direct gates; actions `20`/`25` stay parsed-only no-ops | Focused C# unit test plus reviewed Java source | Does not prove live invite request mutation or declined whisper socket behavior |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes.
- Total artifacts ported or represented in this UOW: 2 C# readiness-report surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The matrix is a readiness report only; it does not execute live sends or prove client-visible behavior.
- Direct-packet ordering relative to the triggering client packet remains unverified.
- Shared singleton lifecycle still needs live `CM_FIND_GROUP` execution proof against the same C# state store.
- Race fanout, invite request mutation, and runtime/socket comparison remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchActionGateMatrixService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchActionGateMatrixServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2132-Completion.md`
- `docs/Phase-6-Session-2132-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add live-readiness tests around connection-registry direct packet ordering relative to the triggering client packet, still without enabling live `ProcessPacketAsync` dispatch.

Safe alternative candidates:

- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a small readiness report for world-broadcast race filtering and ordering relative to direct packet sends.
