# Phase 6 Session 1956 Handoff - CM_BUY_ITEM Repurchase State Context

Date: 2026-06-01
Unit of Work: UOW-1956
Status: Completed

## What Changed

- Threaded optional repurchase snapshot context through `CmBuyItemSideEffectOutcomePlanService.CreateDisabledPlan`.
- CM_BUY_ITEM action 2 repurchase outcomes can now carry `RepurchaseOutcomePlan.StateItemRemovalPlan` when caller snapshot facts are supplied.
- `GameServerConnection.HandleBuyItem` supplies a one-player disabled `RepurchaseStateSnapshot` from `Player.RepurchaseItems` for repurchase diagnostics.
- Existing no-context callers remain valid and leave `StateItemRemovalPlan` null.
- This unit does not implement live singleton state, Java `HashSet` bucket iteration order, socket dispatch, persistence, transaction behavior, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# CM_BUY_ITEM/repurchase slice first failed at compile time due test-only `Assert.NotNull` return-value use, then passed with 53 tests after fixing the assertions.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite first timed out at 180 seconds without a test failure report, then passed with 5005 tests when rerun with a longer timeout.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.

## Known Gaps

- `StateItemRemovalPlan` remains disabled and informational only.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java `HashSet` bucket iteration order is not emulated.
- Returned Java set mutability, concurrent map/set timing, logout removal integration, live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1956-Completion.md`
- `docs/Phase-6-Session-1956-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM`
- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.model.gameobjects.AionObject`

## C# Artifacts Touched

- `Aion.GameServer.Services.CmBuyItemSideEffectOutcomePlanService`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.RepurchaseOutcomePlanService`
- `Aion.GameServer.Services.RepurchaseStateSnapshot`
- `Aion.GameServer.Tests.CmBuyItemSideEffectOutcomePlanServiceTests`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Parity Table Updates

- Added Session 1956 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl` action 2 side-effect dispatch
  - `RepurchaseService.repurchaseFromShop` current set mutation
  - `AionObject.hashCode/equals` repurchase set dependency through the CM_BUY_ITEM outcome

## Next Recommended Unit of Work

- Next sequential task: inspect Java `PlayerLeaveWorldService.leaveWorld` and `RepurchaseService.removeRepurchaseItems`, then add a disabled logout repurchase-state removal payload so logout diagnostics can record singleton map-entry removal without enabling live mutation.

Safe alternative candidates:

- Add another narrow Java golden item-info vector only if the fixture remains simple, such as equipped-slot nonzero or one basic manastone socket.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase state work, inspect logout/leave-world paths before adding any new state payload. Keep the work disabled and supplied-facts only unless live mutation is explicitly scoped and objectively validated.
- Avoid claiming Java `HashSet` iteration parity; UOW-1956 only carries supplied-snapshot removal facts through CM_BUY_ITEM diagnostics.
