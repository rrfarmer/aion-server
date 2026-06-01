# Phase 6 Session 1953 Completion - Repurchase State Lifecycle Planner

Date: 2026-06-01
Unit of Work: UOW-1953
Status: Completed

## Scope

- Added a disabled C# state lifecycle planner for Java `RepurchaseService` map replacement, lookup, removal, and `canRepurchase` membership checks.
- Kept the work supplied-facts only and non-live, without enabling singleton state mutation or packet dispatch.

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, Session 1952 completion, and Session 1952 handoff.
- Inspected Java `RepurchaseService`, `RepurchaseList`, `AionObject.hashCode/equals`, `SM_REPURCHASE_GoldenTest`, and `CM_BUY_ITEM_ReadGuardGoldenTest`.
- Inspected C# `RepurchasePlanService`, `RepurchasePacketSnapshotPlanService`, `Player.RepurchaseItems`, and existing repurchase tests.

## Changes

- Added `RepurchaseStatePlanService`.
- Added disabled plan records for:
  - `addRepurchaseItems` map-entry replacement with a new snapshot.
  - nonzero object-id dedupe that mirrors Java `HashSet<Item>` equality via `AionObject`.
  - `getRepurchaseItems` found and missing-key empty snapshot behavior.
  - `removeRepurchaseItems` present-key removal and absent-key no-op behavior.
  - `canRepurchase` object-id membership checks.
- Added `RepurchaseStatePlanServiceTests` covering replacement/dedupe, get, remove, and can-repurchase behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchaseStatePlanServiceTests|FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# repurchase state/packet/execution planner slice passed with 23 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 4999 tests.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.

## Known Gaps

- The planner is disabled and does not introduce a live singleton `ConcurrentHashMap` equivalent.
- Java `HashSet` bucket iteration order is not emulated; packet snapshot order still depends on caller-supplied facts.
- Java dummy object-id `0` identity behavior is not fully modeled.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchaseStatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchaseStatePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1953-Completion.md`
- `docs/Phase-6-Session-1953-Handoff.md`

## Parity Position

- Partial Parity for disabled repurchase state lifecycle planning.
- Source-reviewed behavior is covered by C# unit tests and existing Java repurchase parser/packet tests, but live singleton state, concurrency, ordering, and mutation timing remain unverified.
