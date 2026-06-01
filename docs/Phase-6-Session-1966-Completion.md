# Phase 6 Session 1966 Completion - Mixed Private-Store Item Skip Diagnostic

Date: 2026-06-01
Unit of Work: UOW-1966
Status: Completed

## What Changed

- Added focused C# diagnostics tests for a mixed private-store purchase where one seller inventory item is present and another bought item is missing.
- Verified against Java source that `PrivateStoreService.sellStoreItem`:
  - computes total price before iterating bought items
  - mutates seller/buyer item state, sends seller notification, and logs the sale only inside the `item != null` branch
  - skips missing seller inventory items without returning
  - transfers the full precomputed Kinah price after the loop
- Tightened the disabled C# persistence adapter so private-store sold-item update intent is derived only from seller item updates/deletes, matching Java's `decreaseItemFromPlayer` branch.
- Added mixed-case send/live facade assertions showing present-item notification and exchange-log intent remain recorded, skipped missing items do not add a store update, and full Kinah transfer intent remains recorded.
- This unit remains non-live. No inventory mutation, Kinah transfer, store mutation, packet send, exchange-log write, transaction, encrypted frame capture, or real-client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# private-store/CM_BUY_ITEM slice passed with 62 tests.
- The first broad C# run timed out before producing a pass/fail result; the rerun with a longer timeout passed with 5019 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/services/PrivateStoreService.java`.
- C# source reviewed/updated: `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`.
- Objective evidence is limited to Java source review, C# unit tests, broad C# tests, and Maven test runs.
- No verified live parity is claimed for private-store runtime mutation, DB writes, packet order, exchange-log formatting, transaction behavior, concurrency timing, or real-client behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PrivateStorePurchasePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PrivateStoreLiveExecutorFacadePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1966-Completion.md`
- `docs/Phase-6-Session-1966-Handoff.md`

## Remaining Risks

- Mixed private-store present/missing seller item behavior is diagnostic only.
- Java can still charge the buyer and credit the seller for skipped missing seller items; C# records this reviewed behavior but does not execute it live.
- Java `LinkedHashMap` ordering, live store mutation timing, invalid-index warning logs, race behavior, encrypted frame capture, and real-client validation remain pending.
- Full item-info blob parity for advanced item state remains partial.

## Next Recommended Unit

- Inspect `CM_BUY_ITEM` amount signedness (`readUH()` in Java versus C# unsigned packet reads), then add a focused parser/planner parity test if a Java runtime vector is practical.

Safe alternatives:

- Inspect Java delete-path cube-size sends for another non-repurchase inventory diagnostic where `sendItemDeletePacket` is already represented by a C# planner.
- Inspect live `CM_PET` actionType 4 composition only if it can remain disabled and source-reviewed.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Run a clean Maven validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
