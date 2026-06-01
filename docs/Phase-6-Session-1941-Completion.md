# Phase 6 Session 1941 Completion - CM_BUY_ITEM Action 0 Read Guard Capture

Date: 2026-06-01
Unit of Work: UOW-1941
Status: Completed

## Work Discovery

- Resumed from Session 1940's handoff and inspected Java `CM_BUY_ITEM.readImpl`, Java `TradeList`/`TradeItem`, Java client-packet buffer mechanics, C# `CmBuyItem`, and C# `CmBuyItemTests`.
- Determined non-empty `SM_REPURCHASE` golden coverage was not a safe small unit because Java `ItemInfoBlob` depends on item templates, item state, and player context.
- Selected a safe `CM_BUY_ITEM` read-path boundary that avoids audit logging and live state: private-store action `0` allows item index `0` and count `20000`.

## What Changed

- Added Java `CM_BUY_ITEM_ReadGuardGoldenTest`.
- The Java test invokes `CM_BUY_ITEM.readImpl` with a little-endian payload for seller `7001`, action `0`, amount `1`, item index `0`, and count `20000`, then asserts no audit and a matching `TradeList` entry.
- Updated the C# private-store parser test to use `CmBuyItem.MaxItemCount` for the same action `0` non-positive index allowance.
- Kept the unit disabled/non-live. No audit branch, live handler run path, private-store mutation, socket dispatch, or real-client validation was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-guard test passed with 1 game-server test.
- Focused C# `CmBuyItemTests` passed with 9 tests.
- Wider C# buy-item/private-store slice passed with 54 tests.
- Java/Maven reactor test run passed with 1 commons test and 14 game-server tests.
- Broad C# game-server suite passed with 4972 tests.

## Known Gaps

- Java audit branches are not runtime-captured here because isolated Java audit logging reaches staff/static-data services.
- The Java test uses test-only `Unsafe.allocateInstance` and reflection for an `AionConnection` shell and private field inspection. This is parser evidence only, not connection lifecycle parity.
- `CM_BUY_ITEM.runImpl`, live private-store seller state, repurchase/NPC/pet branches, encrypted frame decoding, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1941 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` private-store action `0` item-index/max-count read guard
  - Java `TradeList.addItem` / `TradeItem` action `0` payload storage evidence
- Full `CM_BUY_ITEM` remains Partial Parity. The specific action `0` parser boundary now has Java source-capture evidence aligned with C# tests.
