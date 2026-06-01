# Phase 6 Session 1962 Completion - BUY_AGAIN Missing-Template Diagnostic

Date: 2026-06-01
Unit of Work: UOW-1962
Status: Completed

## Scope

- Preserved disabled BUY_AGAIN repurchase packet snapshot diagnostics when supplied repurchase item facts lack item templates.
- Kept the work diagnostic-only and non-live, without sending `SM_REPURCHASE`, querying a Java-style singleton map, mutating repurchase state, or emulating a live Java exception.

## Work Discovery

- Re-read Session 1961 completion and handoff after UOW-1961.
- Inspected Java `DialogService.onDialogSelect` BUY_AGAIN.
- Inspected Java `SM_REPURCHASE(Player, int)` constructor and `writeImpl`.
- Inspected C# `GameServerConnection.CreateNonLiveTradeDialogSelectPlan`.
- Inspected C# `RepurchasePacketSnapshotPlanService`.
- Inspected C# dialog service/controller plan propagation and existing BUY_AGAIN/repurchase packet tests.

## Changes

- Updated `CreateNonLiveTradeDialogSelectPlan` so an explicit `RepurchasePacketSnapshotPlan` wins over the fallback packet helper.
- If static item templates are available and the snapshot planner returns `BlockedMissingTemplate`, the dialog descriptor now carries the blocked snapshot and a null packet.
- The older fallback helper still applies only when no static item-template table is available to create the richer snapshot plan.
- Added a socket-boundary BUY_AGAIN test proving the missing-template snapshot reaches the final dialog descriptor without a fallback packet.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~NpcDialogControllerDispatchPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# BUY_AGAIN/repurchase snapshot slice passed with 68 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5009 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Known Gaps

- BUY_AGAIN repurchase packet planning remains disabled and informational only.
- No live `SM_REPURCHASE` dispatch, singleton map query, packet encryption proof, inventory/Kinah mutation, repository write, transaction behavior, or real-client validation is performed.
- The missing-template state records malformed/supplied-facts diagnostics. Java normally holds `Item` instances with templates, and exact Java runtime behavior for impossible missing-template item facts was not runtime-compared.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Full item-info blob parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1962-Completion.md`
- `docs/Phase-6-Session-1962-Handoff.md`

## Parity Position

- Partial Parity for disabled BUY_AGAIN missing-template diagnostic propagation.
- The implementation is source-reviewed and C# unit-tested, with Java packet/parser tests rerun, but live Java/C# packet construction, exact Java malformed-template behavior, socket ordering, singleton state, transaction behavior, concurrency, and complete repurchase behavior remain unverified.
