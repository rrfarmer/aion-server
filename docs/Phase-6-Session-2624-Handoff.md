# Phase 6 Session 2624 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2624: Execute pet merchant sell-to-shop live. See
[Phase-6-Session-2624-Completion.md](Phase-6-Session-2624-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- `1daf029` - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`
- `c015455` - `[Phase 6][UOW-2613] Consume NPC shop required items live`
- `40ea371` - `[Phase 6][UOW-2614] Spend NPC shop abyss points live`
- `b848bbb` - `[Phase 6][UOW-2615] Mutate NPC shop limited counters live`
- `867b01a` - `[Phase 6][UOW-2616] Schedule NPC shop limited resets live`
- `3507068` - `[Phase 6][UOW-2617] Execute NPC shop abyss kinah buys live`
- `36b6746` - `[Phase 6][UOW-2618] Execute NPC shop abyss buys live`
- `a56bdbb` - `[Phase 6][UOW-2619] Execute NPC shop reward buys live`
- `816199d` - `[Phase 6][UOW-2620] Execute normal NPC sell-to-shop live`
- `46397e7` - `[Phase 6][UOW-2621] Execute abyss AP sell-to-shop live`
- `0eeb30d` - `[Phase 6][UOW-2622] Execute NPC shop repurchase live`
- `6c31b53` - `[Phase 6][UOW-2623] Execute partial AP sell-to-shop live`
- Current commit - `[Phase 6][UOW-2624] Execute pet merchant sell-to-shop live`

## Session Summary

- Java review confirmed `CM_BUY_ITEM` action 17 uses the pet `MERCHANT` function and calls the shared `TradeService.performSellToShop` path with `pf.getRatePrice()`.
- C# now has a minimal `IWorldPetObject` visible-object contract for merchant sell facts.
- C# target classification now recognizes pet world objects.
- C# live buy-item handling now builds a `TradeSellToShopPlan` with the pet sell modifier and executes the existing live sell mutation/persistence/packet path for action 17 pet targets.
- Focused live handler coverage proves item deletion, Kinah increase, repurchase-state update, repository persistence capture, and sell packets for the covered success path.

## Files Changed In UOW-2624

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/WorldNpc.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemKnownListTargetFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemKnownListTargetFactAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2624-Completion.md`
- `docs/Phase-6-Session-2624-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests" --no-restore
```

Result: passed, 66/66.

Java/Maven: not run. No narrow Java fixture was discovered for `CM_BUY_ITEM` action 17 pet merchant sell; Java behavior was verified by source review of `CM_BUY_ITEM` and `TradeService`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live handler/state/persistence boundary changed, but the focused command directly covered the edited pet target classification, shared sell planner contract, handler execution, persistence handoff, and packet fanout while building affected projects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Pet action 17 merchant sell now executes live when the target world object exposes merchant facts. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `Aion.GameServer.Model.GameObjects.IWorldPetObject` | Runtime visible object contract | Partial | Unit Tested | Partial Parity | Minimal merchant facts are represented for buy-item branch selection only. |
| `com.aionemu.gameserver.services.TradeService#performSellToShop` | `Aion.GameServer.Services.TradeSellToShopPlanService` plus `GameServerConnection.TryExecuteSellToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Shared sell executor is now consumed by NPC action 1 and pet action 17. |

## Known Gaps

- Production pet spawn/known-list registration has not been proven to create `IWorldPetObject` instances with merchant facts.
- Live MySQL execution for pet merchant sell persistence was not run.
- Real client validation was not run.
- Partial-stack pet merchant sell is covered by the reused sell planner/executor but was not separately asserted in this UOW.
- Java audit logging for sell-abuse cases remains modeled but not emitted through live C# audit infrastructure.

## Next Recommended Runtime UOW

**UOW-2625 candidate: register spawned merchant pets as live buy-item pet targets.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: action 17 pet merchant sell now executes for IWorldPetObject targets, but production pet spawn/known-list paths have not been proven to register merchant pets with those facts.
- Java source method or runtime path: Java pet spawn/known-list path that creates model.gameobjects.Pet visible objects plus PetTemplate/PetFunction lookup for PetFunctionType.MERCHANT.
- C# runtime artifact to wire or fix: the C# pet spawn/known-list/world registration path should create or expose IWorldPetObject with HasMerchantFunction and MerchantSellModifier populated from Java pet static data.
- Client-visible/state/persistence effect expected: real spawned merchant pets become actionable targets for CM_BUY_ITEM action 17, enabling the live sell mutation/persistence/packet path from UOW-2624 without test-only pet objects.
- Why this is not preview-only/test-only/documentation-only if feasible: it loads/exposes runtime pet merchant data into live world objects used by the live client packet handler.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerKnownListPet|FullyQualifiedName~CmPetTests" --no-restore
```

Java/Maven: not expected unless a narrow Java pet spawn or pet template fixture is discovered.

Broad-validation trigger: live world/known-list/pet runtime state changes. Start focused on pet spawn/known-list and buy-item action 17; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Register spawned merchant pets as live buy-item pet targets if the existing C# pet spawn/static-data path can expose merchant function facts.
- Execute a concrete pet auto-sell runtime path if a live `CM_PET` branch already reaches pet selling state and can consume the shared sell executor.
- Add live MySQL validation for shop sell persistence only if an opt-in database fixture directly validates an already-live behavior and records row-level compatibility.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action 13 normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action 13 normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action 13 normal AP-cost buys execute live and persist as of UOW-2614.
- Successful `CM_BUY_ITEM` NPC action 13 normal limited-item buys mutate live limited counters as of UOW-2615.
- Limited-item counters reset from live scheduler callbacks as of UOW-2616.
- `CM_BUY_ITEM` NPC action 13 `ABYSS_KINAH` buys execute live and persist as of UOW-2617.
- `CM_BUY_ITEM` NPC action 13 `ABYSS` buys execute live without Kinah as of UOW-2618.
- `CM_BUY_ITEM` NPC action 13 `REWARD` buys execute live without Kinah as of UOW-2619.
- `CM_BUY_ITEM` NPC action 1 normal sell-to-shop executes live for covered whole-item sells as of UOW-2620.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop executes live for exact-count deletes as of UOW-2621.
- `CM_BUY_ITEM` NPC action 2 repurchase executes live for the covered success path as of UOW-2622.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop partial-stack decreases execute live as of UOW-2623.
- `CM_BUY_ITEM` pet action 17 merchant sell-to-shop executes live for `IWorldPetObject` targets as of UOW-2624.
- Buy/sell/repurchase planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
