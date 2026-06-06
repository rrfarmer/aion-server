# Phase 6 Session 2683 Completion

## UOW

[Phase 6] UOW-2683: Match Passport full-inventory guard ordering.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: Atreian Passport claim requests now evaluate full inventory before reward template level and claim-time expiry checks.
- Java source/runtime path: AtreianPassportService.takeReward -> ppl.getPassport -> rewarded/deleted guard -> player.getInventory().isFull() -> STR_WAREHOUSE_FULL_INVENTORY -> break -> onLogin(player).
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync full-cube branch ordering, InventoryCapacity.GetFreeCubeSlots, SmSystemMessage.FullInventory, and post-claim Passport login snapshot/purge behavior.
- Client-visible/state/persistence effect: a full-cube claim now sends message `1300762` before invalid-level handling, skips reward mutation, and preserves Java's ordering where post-claim `onLogin` may still purge expired rows and send the resulting snapshot.
- Why this is runtime progress: it changes live packet/state/persistence ordering in the client packet handler; it is not preview-only, test-only, documentation-only, or evidence-only work.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` checks `player.getInventory().isFull()` immediately after finding an available Passport row.
  - Java sends `SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY()` and breaks before looking up reward permit level or reward expiry.
  - Java still calls `onLogin(player)` after the loop, so expired rows can be purged by login processing after the full-inventory packet.
- `game-server/data/static_data/events/login_events.xml`
  - Passport `43` is a real active level-gated cumulative reward in the Java XML.
  - Passport `1` is a real expiring daily reward in the Java XML.

## C# Changes

- Moved the full-cube guard in `GameServerConnection.HandleAtreianPassportAsync` ahead of reward template lookup, reward permit level, and reward expiry handling.
- Kept the later `InventoryAddService.CreateAddItemPlan` full-inventory fallback for add-plan failures after the Java-order precheck.
- Preserved post-claim `ApplyAtreianPassportLoginForActivePlayerAsync` behavior, including login purge and snapshot emission.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportFullInventoryPrecedesInvalidLevel` | Unit / live connection and packet serialization | `AtreianPassportService.takeReward` full-inventory-before-level ordering | A full-cube player claiming level-gated Passport `43` receives full-inventory message `1300762`, no invalid-level message `1402573`, no reward mutation, and a post-login Passport snapshot. | Socket-backed connection invocation with runtime-loaded Java XML, packet list assertions, repository counters, and live Passport state. | Does not prove every level-gated Passport template. |
| `HandleInfrastructurePacketAsync_AtreianPassportFullInventoryPrecedesExpiredDelete` | Unit / live connection and packet serialization | `takeReward` full-inventory-before-expiry ordering plus post-claim `onLogin` purge | A full-cube expired Passport claim sends full-inventory first, skips reward mutation, then lets post-claim login purge the expired row and snapshot the remaining state. | Runtime-loaded Java XML, repository delete counter, packet ordering, and serialized snapshot assertions. | Does not runtime-compare Java clock/timezone edge cases. |

## Validation Decision

```text
- Changed surface: live Passport claim handler branch ordering plus focused connection tests.
- Specific behavior/contract: Java checks Inventory.isFull before reward permit level and claim-time expiry, then still calls onLogin.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source and XML were reviewed unchanged, and no narrow Java behavioral fixture exists for this ordering in this checkout.
- Broad-validation trigger: none. The change is isolated to one live packet handler branch and the existing Passport connection test class.
- Broad .NET decision: skipped; the filtered test built the affected project and exercised reward, invalid-level, full-inventory, expiry, and post-login Passport paths.
- Why this scope is sufficient: the focused connection tests observe the exact packet ordering, repository calls, live Passport state, and serialized snapshot affected by the Java ordering change.
```

Results:

- Focused C# validation passed: 15/15.
- Existing unrelated nullable/analyzer warnings were emitted by the C# projects.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.takeReward` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAtreianPassportAsync` | Service / live packet handler | Partial | Unit Tested | Partial Parity | Full-inventory ordering now matches Java before invalid-level and claim-time expiry branches. Complete claim parity is not claimed. |
| `SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY` | `SmSystemMessage.FullInventory` | Server packet | Ported | Unit Tested | Partial Parity | Message id `1300762` is now emitted before invalid-level or claim-time expiry handling for full-cube Passport claims. |
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `PlayerEnterWorldService.ApplyAtreianPassportLoginForActivePlayerAsync` / `ApplyAtreianPassportLoginAsync` | Service | Partial | Unit Tested | Partial Parity | Post-claim login continues to own expired-row purge and snapshot after the full-inventory branch. |

## Known Gaps

- Full Atreian Passport parity is not claimed.
- Java audit logging for missing/already rewarded claims remains absent and is not enough for a standalone runtime UOW.
- Live MySQL evidence was not run for Passport reward/login/delete persistence.
- Exact Java timezone behavior for `Instant.now()` claim expiry and `ServerTime.now()` login purge remains not runtime-compared.
- Multi-id/multi-timestamp claim loop ordering still needs discovery before broader claim parity can be claimed.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect claim handling for multiple requested Passport ids/timestamps and port any Java loop-order persistence/message gap that affects live state or packets.
2. Inspect Passport claim expiry clock behavior against Java and port any concrete live state/persistence difference found beyond the already documented ordering behavior.
3. Move to the next deferred enter-world, inventory, item-use, quest, AI, zone, instance, command, or dynamic handler runtime gap if Passport discovery no longer yields a small state/packet UOW.
