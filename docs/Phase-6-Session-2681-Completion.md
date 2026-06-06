# Phase 6 Session 2681 Completion

## UOW

[Phase 6] UOW-2681: Block Passport reward claims when the cube is full.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: Atreian Passport reward claims now honor Java's full-inventory guard before any reward merge, Passport rewarded update, or inventory persistence mutation.
- Java source/runtime path: AtreianPassportService.takeReward -> player.getInventory().isFull() -> send SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY() -> break -> AccountPassportsDAO.storePassportList only if previous mutations exist -> onLogin/sendPassport.
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync full-cube branch, InventoryCapacity.GetFreeCubeSlots, live SmSystemMessage.FullInventory packet send, Passport snapshot send, and claim-path repository calls.
- Client-visible/state/persistence effect: a claim packet for an available Passport reward with a full cube now sends system message 1300762, leaves inventory and Passport state unchanged, skips reward and Passport persistence calls, and sends the unchanged Passport snapshot.
- Why this is runtime progress: it fixes live claim-time packet/state/persistence behavior and prevents a Java-forbidden reward mutation.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` checks `player.getInventory().isFull()` before reward permit level, reward expiry, and `ItemService.addItem`.
  - Full inventory sends `SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY()` and breaks out of the inner timestamp loop.
  - Because the guard runs before `ItemService.addItem`, Java does not merge into existing stackable reward stacks when the cube is full.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_WAREHOUSE_FULL_INVENTORY()` maps to message id `1300762`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT.java`
  - The client packet forwards parsed Passport ids/timestamps to `AtreianPassportService.takeReward`.

## C# Changes

- Added a Java-order full-cube guard in `GameServerConnection.HandleAtreianPassportAsync` before `InventoryAddService.CreateAddItemPlan`.
- Reused `InventoryCapacity.GetFreeCubeSlots(player, staticData.ItemTemplates)` to match the live cube slot model and avoid changing general inventory add behavior.
- The full-cube branch sends `SmSystemMessage.FullInventory()` and breaks before reward plan creation, inventory persistence, Passport rewarded persistence, or live state mutation.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportFullInventoryBlocksStackMergeClaim` | Unit / live connection and packet serialization | `AtreianPassportService.takeReward` full-inventory guard | A full cube with an existing partial stack for the reward item sends message `1300762`, does not merge the stack, does not mark the Passport rewarded, does not call reward/passport persistence, and sends an unchanged Passport snapshot. | Socket-backed connection invocation with runtime-loaded Java XML and direct packet/state/repository assertions. | C# still sends a direct Passport snapshot rather than invoking full `onLogin` after claim; complete `takeReward` parity is not claimed. |

## Validation Decision

```text
- Changed surface: live Passport claim handler branch in GameServerConnection plus one connection-level test.
- Specific behavior/contract: Java blocks Passport reward claims at Inventory.isFull before stack merge or Passport mutation, sends STR_WAREHOUSE_FULL_INVENTORY, and leaves reward state unchanged.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed and unchanged, and no narrow Java behavioral fixture exists for AtreianPassportService.takeReward full-inventory handling in this checkout.
- Broad-validation trigger: none. The change is isolated to one live packet handler branch and its existing connection test class.
- Broad .NET decision: skipped; the filtered test built the affected project and exercised the edited live handler plus adjacent Passport claim/login cases.
- Why this scope is sufficient: the focused connection tests observe the real packet send list, repository call counters, live inventory state, live Passport state, and serialized Passport snapshot for the changed claim behavior.
```

Results:

- Focused C# validation passed: 13/13.
- Existing unrelated nullable/analyzer warnings were emitted by the C# projects.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.takeReward` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAtreianPassportAsync` | Service / live packet handler | Partial | Unit Tested | Partial Parity | Full-inventory guard now matches Java ordering for stackable rewards. Remaining gaps include audit logging and the exact post-claim `onLogin` behavior. |
| `SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY` | `SmSystemMessage.FullInventory` | Server packet | Ported | Unit Tested | Partial Parity | Message id `1300762` is emitted from the live Passport claim branch. Packet encoding was covered by existing packet tests; this UOW validates branch emission. |
| `CM_ATREIAN_PASSPORT.runImpl` | `GameServerConnection.HandleAtreianPassportAsync` | Client packet dispatch | Partial | Unit Tested | Partial Parity | The C# live handler executes the parsed claim request and now blocks full-cube claims. Parser parity was already covered; complete service dispatch parity remains partial. |

## Known Gaps

- Full Atreian Passport claim parity is not claimed.
- C# sends a Passport snapshot directly after claim handling; Java calls `onLogin(player)`, which can also purge/login-update depending on state and time.
- Java audit logging for non-existing and already rewarded Passport claim attempts remains absent.
- Live MySQL evidence was not run for Passport reward claim persistence.
- Exact Java timezone behavior for reward expiry remains not runtime-compared.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect Java `AtreianPassportService.takeReward` post-claim `onLogin(player)` behavior against the C# direct snapshot branch and port a smallest missing live state/persistence/packet effect if one is found.
2. Inspect reward expiry timing in the live claim path and port any Java-equivalent runtime difference found around `Instant.now()`/`ServerTime.now()` and delete persistence.
3. Move to the next deferred enter-world or inventory packet/state gap if Passport claim discovery finds no runtime difference beyond logging.
