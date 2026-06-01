# Phase 6 Session 1958 Completion - BUY_AGAIN Repurchase Snapshot Diagnostic Threading

Date: 2026-06-01
Unit of Work: UOW-1958
Status: Completed

## Scope

- Threaded the disabled BUY_AGAIN `SM_REPURCHASE(Player, npcId)` snapshot diagnostic through the C# non-live dialog plan path.
- Kept the integration diagnostic-only and non-live, without sending packets or querying/mutating a live repurchase singleton map.

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, Session 1957 completion, and Session 1957 handoff.
- Inspected Java `DialogService.onDialogSelect` BUY_AGAIN handling.
- Inspected Java `DialogAction.BUY_AGAIN` and Java `SM_REPURCHASE(Player, npcId)`.
- Inspected C# `GameServerConnection.CreateNonLiveTradeDialogSelectPlan`, `QuestDialogNpcTargetBranchInputAssemblyPlanService`, `NpcDialogControllerDispatchPlanService`, `NpcDialogServiceSelectPlanService`, and existing BUY_AGAIN/repurchase tests.

## Changes

- Added optional `RepurchasePacketSnapshotPlan` fields to the dialog runtime snapshot, branch assembly plan, and controller dispatch input.
- Threaded the snapshot plan from branch assembly into controller dispatch and then into `NpcDialogServiceSelectPlanService`.
- `GameServerConnection` now creates a disabled `RepurchasePacketSnapshotPlan` for BUY_AGAIN when item templates are available.
- Preserved the existing raw `SmRepurchase` fallback when the richer snapshot diagnostic cannot produce a packet.
- Expanded BUY_AGAIN connection tests to assert the snapshot plan, non-live query/send flags, descriptor propagation, and serialized packet payload.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~NpcDialogControllerDispatchPlanServiceTests|FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# dialog/repurchase slice passed with 67 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite first timed out at 240 seconds, then passed with 5007 tests when rerun with a longer timeout.
- Full Maven reactor passed commons, chat-server, and game-server tests, then failed compiling login-server at `login-server/src/com/aionemu/loginserver/service/PlayerTransferService.java:42` with `illegal start of expression`.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.

## Known Gaps

- BUY_AGAIN repurchase packet planning remains disabled and does not send `SM_REPURCHASE`.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full Maven reactor validation is blocked by the current login-server compile error.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogControllerDispatchPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1958-Completion.md`
- `docs/Phase-6-Session-1958-Handoff.md`

## Parity Position

- Partial Parity for disabled BUY_AGAIN repurchase packet snapshot diagnostics.
- The implementation is source-reviewed and C# unit-tested, with Java golden packet tests reused as evidence, but live Java/C# socket dispatch, singleton state lookup, mutation timing, concurrency, transaction behavior, and complete dialog behavior remain unverified.
