# Phase 6 Session 2625 Completion

## UOW

[Phase 6] UOW-2625: Load pet merchant templates into active pet sell.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_BUY_ITEM action 17 pet merchant sell could execute for test/world IWorldPetObject targets, but active player pet state did not consume Java pet static data and could still be classified as an unknown seller target when no world pet object was registered.
- Java source/runtime path: VisibleObjectSpawner.spawnPet(Player, int) creates Pet from DataManager.PET_DATA.getPetTemplate(templateId); PetTemplate.getPetFunction(PetFunctionType.MERCHANT) exposes PetFunction.getRatePrice(); CM_BUY_ITEM.runImpl pet branch calls TradeService.performSellToShop(player, tradeList, null, pf.getRatePrice()).
- C# runtime artifact wired: StaticData now loads pets/pets.xml into PetTemplateTable/PetTemplateSummary/PetFunctionSummary; GameServerConnection resolves active player pet seller ids through Player.HasPetSummon/PetSummonObjectId/PetSummonNpcId and uses PetTemplates.GetMerchantSellModifier for action 17.
- Client-visible/state/persistence effect: an active merchant pet seller id can now drive the existing live sell-to-shop executor, deleting/decreasing sold inventory, increasing Kinah, adding repurchase state, persisting sell rows, and sending delete/cube/Kinah packets using the Java pet function rate.
- Why this is runtime progress: this UOW loads Java XML/static pet data into runtime C# structures used by live CM_BUY_ITEM handling and changes live packet target resolution and sell side effects; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java`
  - `spawnPet(Player, int)` gets `PetTemplate` from `DataManager.PET_DATA`, creates `Pet`, assigns a known list, brings it into the world, and stores it on `Player`.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetTemplate.java`
  - `containsFunction(PetFunctionType)` and `getPetFunction(PetFunctionType)` inspect the template's pet functions.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunction.java`
  - `getRatePrice()` exposes the merchant sell modifier used by shop sell.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
  - `MERCHANT` is the Java enum value for merchant pet function checks.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`
  - The pet branch requires the `MERCHANT` function and calls shared sell-to-shop with `pf.getRatePrice()`.

## C# Changes

- Added `PetTemplateTable`, `PetTemplateSummary`, and `PetFunctionSummary`.
- Extended `StaticData` to parse `static_data/pets/pets.xml` `<pet>` and nested `<petfunction>` elements, including Java enum names and `rate_price`.
- Added `StaticData.PetTemplates`.
- Extended `GameServerConnection` buy-item target resolution so `sellerObjectId == player.PetSummonObjectId` can be classified as a pet target when no world pet object is available.
- Extended pet merchant target resolution to use `Player.PetSummonNpcId` plus `PetTemplates.GetMerchantSellModifier`.
- Added focused tests for static pet merchant XML parsing and active-pet action 17 live sell execution.
- Updated existing reflection-based static-data test fixtures to pass an empty `PetTemplateTable` in the new constructor slot.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LoadFromCacheAsync_ParsesPetMerchantFunctionRatePrice` | Unit/static data | `PetData` / `PetTemplate` / `PetFunction` | `pets.xml` merchant `rate_price` loads into runtime `StaticData.PetTemplates`. | C# parser assertion over Java-shaped XML. | Minimal XML fixture, not full static-data cache validation. |
| `ProcessPacketAsync_CmBuyItemActivePetMerchantSellUsesStaticPetTemplateRateLive` | Unit/live handler | `CM_BUY_ITEM.runImpl` pet branch and `PetTemplate.getPetFunction(MERCHANT)` | Active player pet id plus static pet merchant function rate executes live sell mutation/persistence/packets. | Runtime state, repository capture, and packet assertions show the Java rate is consumed by live handler side effects. | Uses test-injected pet template table; production pet spawn/world registration is still partial. |

## Validation Decision

```text
- Changed surface: static-data load plus live CM_BUY_ITEM pet target resolution and sell side effects.
- Specific behavior/contract: active pet seller object id resolves to pet target, merchant rate comes from pet template data, and action 17 invokes live sell-to-shop mutation/persistence/packet fanout.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~StaticDataPetTemplateTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for pet template XML load or CM_BUY_ITEM action 17 pet merchant sell.
- Broad-validation trigger: live handler/static-data boundary changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered static pet template loading, target classification, live handler mutation, persistence handoff, and packet fanout.
- Why this scope is sufficient: the focused tests cover both new runtime data hydration and the live handler path that consumes it.
```

Result: passed, 53/53.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.pet.PetTemplate` | `Aion.GameServer.Dataholders.PetTemplateTable` / `PetTemplateSummary` | Static data/runtime table | Partial | Unit Tested | Partial Parity | Merchant-relevant pet functions are loaded and queryable; broader pet template fields/functions remain partial. |
| `com.aionemu.gameserver.model.templates.pet.PetFunction` | `Aion.GameServer.Dataholders.PetFunctionSummary` | Static data/runtime table | Partial | Unit Tested | Partial Parity | `id`, `type`, `slots`, and `rate_price` are represented for runtime use. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Pet action 17 now executes live for world pet objects and active player pet static-data fallback. Full Java known-list/world pet object parity remains incomplete. |

## Summary Metrics

- Java artifacts discovered/touched: 5.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Production pet spawn/known-list registration still has not been proven to create full live pet world objects.
- Active pet fallback depends on `Player.HasPetSummon`, `PetSummonObjectId`, and `PetSummonNpcId` already being populated by upstream runtime code.
- Full Java `PetTemplate` fields and non-merchant function behavior are not ported.
- Live MySQL execution for pet merchant sell persistence was not run.
- Real client validation was not run.
- Java audit logging for sell-abuse cases remains modeled but not emitted through live C# audit infrastructure.
