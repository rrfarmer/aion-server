# Phase 6 Session 2625 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2625: Load pet merchant templates into active pet sell. See
[Phase-6-Session-2625-Completion.md](Phase-6-Session-2625-Completion.md).

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
- `e9a5945` - `[Phase 6][UOW-2624] Execute pet merchant sell-to-shop live`
- Current commit - `[Phase 6][UOW-2625] Load pet merchant templates into active pet sell`

## Session Summary

- Java review confirmed pet spawn resolves `PetTemplate` from `DataManager.PET_DATA`, pet merchant checks use `PetTemplate.getPetFunction(PetFunctionType.MERCHANT)`, and action 17 sells through `TradeService.performSellToShop(..., pf.getRatePrice())`.
- C# now loads `pets/pets.xml` pet templates and pet functions into runtime `StaticData.PetTemplates`.
- C# buy-item target resolution now treats active player pet seller ids as pet targets when no world pet object is registered.
- C# pet merchant sell resolution now uses `Player.PetSummonNpcId` to look up the merchant `rate_price` and feed the existing live sell executor.
- Focused live handler coverage proves static pet merchant rate drives item deletion, Kinah increase, repurchase-state update, repository persistence capture, and sell packets.

## Files Changed In UOW-2625

- `dotnetConversion/src/Aion.GameServer/Dataholders/PetTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionOpenStaticDoorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataPetTemplateTests.cs`
- `docs/Phase-6-Session-2625-Completion.md`
- `docs/Phase-6-Session-2625-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~StaticDataPetTemplateTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests" --no-restore
```

Result: passed, 53/53.

Java/Maven: not run. No narrow Java fixture was discovered for pet template XML load or `CM_BUY_ITEM` action 17 pet merchant sell; Java behavior was verified by source review of pet spawn/template/function and buy-item code.

Broad .NET: skipped after focused coverage. Broad trigger existed because static-data and live handler target resolution changed, but the focused command directly covered the parser, target classification, handler execution, persistence handoff, and packet fanout while building affected projects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.pet.PetTemplate` | `Aion.GameServer.Dataholders.PetTemplateTable` / `PetTemplateSummary` | Static data/runtime table | Partial | Unit Tested | Partial Parity | Merchant-relevant functions are loaded into runtime static data. |
| `com.aionemu.gameserver.model.templates.pet.PetFunction` | `Aion.GameServer.Dataholders.PetFunctionSummary` | Static data/runtime table | Partial | Unit Tested | Partial Parity | Merchant `rate_price` is consumed by live buy-item handling. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Pet action 17 executes live for world pet objects and active player pet fallback. |

## Known Gaps

- Production pet spawn/known-list registration still has not been proven to create full live pet world objects.
- Active pet fallback depends on upstream runtime code populating `Player.HasPetSummon`, `PetSummonObjectId`, and `PetSummonNpcId`.
- Full Java `PetTemplate` fields and non-merchant function behavior remain partial.
- Live MySQL execution for pet merchant sell persistence was not run.
- Real client validation was not run.
- Java audit logging for sell-abuse cases remains modeled but not emitted through live C# audit infrastructure.

## Next Recommended Runtime UOW

**UOW-2626 candidate: hydrate active pet summon state from loaded pet/static or persisted pet data during live enter-world/spawn flow.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: active pet merchant sell can consume Player pet summon ids and PetTemplates, but the production enter-world/spawn path has not been proven to populate those active pet fields from persisted/common pet data.
- Java source method or runtime path: VisibleObjectSpawner.spawnPet(Player, int), Player.setPet(Pet), PetCommonData, and the Java pet owner enter-world/load path that restores current pet state.
- C# runtime artifact to wire or fix: PlayerEnterWorldRepository/PlayerEnterWorldService or the nearest C# pet spawn/load path should populate Player.HasPetSummon, PetSummonObjectId, and PetSummonNpcId from existing persisted/runtime pet state, then expose it to live packet handlers.
- Client-visible/state/persistence effect expected: a player with a restored/summoned merchant pet can use CM_BUY_ITEM action 17 without test-only setup or manual Player field injection.
- Why this is not preview-only/test-only/documentation-only if feasible: it restores or mutates live player pet runtime state used by the live buy-item handler and client packet side effects.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~StaticDataPetTemplateTests" --no-restore
```

Java/Maven: not expected unless a narrow Java pet restore/spawn fixture is discovered.

Broad-validation trigger: live enter-world/player pet runtime state changes. Start focused on pet restore/spawn and buy-item action 17; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Hydrate active pet summon ids from live/persisted pet load state if an existing repository projection already contains the pet template id.
- Register a concrete world pet object implementing `IWorldPetObject` if an existing C# pet spawn path already allocates object ids and world visibility.
- Execute a concrete pet auto-sell runtime activation path only if it mutates live pet selling state and sends `SM_PET(AUTOSELL)` from the live `CM_PET` handler.

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
- Java pet merchant functions load into C# runtime `StaticData.PetTemplates`, and active player pet state can feed action 17 sell-to-shop as of UOW-2625.
- Buy/sell/repurchase planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
