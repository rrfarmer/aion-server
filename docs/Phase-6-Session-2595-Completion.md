# Phase 6 Session 2595 Completion

## UOW

[Phase 6] UOW-2595: Set private-store name from live packet

## Status

Completed and validated with focused live packet coverage. The C# `CM_PRIVATE_STORE_NAME` branch now mutates live
private-store message state and sends `SM_PRIVATE_STORE_NAME` from live packet dispatch.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: CM_PRIVATE_STORE_NAME now sets the live private-store message instead of only recording a disabled composition plan.
- Java source method or runtime path: CM_PRIVATE_STORE_NAME.runImpl -> PrivateStoreService.openPrivateStore -> activePlayer.getStore().setStoreMessage(name) -> broadcast SM_PRIVATE_STORE_NAME.
- C# runtime artifact wired or fixed: Player.PrivateStoreMessage, GameServerConnection CmPrivateStoreName branch, HandleOpenPrivateStoreNameAsync, SmPrivateStoreName.
- Client-visible/state effect changed: an open private store receives the requested message and visible clients/self receive SM_PRIVATE_STORE_NAME; missing store returns silently.
- Why this is not preview-only/test-only/documentation-only: it runs from live client packet dispatch, mutates live player/store state, and emits a real server packet.
```

## Java Source Reviewed

- `CM_PRIVATE_STORE_NAME.runImpl` calls `PrivateStoreService.openPrivateStore(activePlayer, name)`.
- `PrivateStoreService.openPrivateStore` sets `activePlayer.getStore().setStoreMessage(name)` and broadcasts `SM_PRIVATE_STORE_NAME(activePlayer)` including the source player.

## C# Changes

- Added `Player.PrivateStoreMessage` as the live C# field corresponding to Java `PrivateStore.storeMessage`.
- Updated `CmPrivateStoreName` dispatch to call `HandleOpenPrivateStoreNameAsync`.
- Added `HandleOpenPrivateStoreNameAsync` to return silently when no store is open, set the live message, and send/broadcast `SmPrivateStoreName`.
- Reset `PrivateStoreMessage` when opening a fresh private store and when closing one.
- Updated live handler tests for successful name mutation/fanout and missing-store silence.

## Known Gaps

- C# still lacks Java's full `PrivateStore` object; message/items are represented directly on `Player`.
- Live private-store purchase side effects remain deferred.
- Registry fanout is not separately tested with multiple visible players; fallback direct send is covered.
- Private-store state remains volatile and not persisted.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_PRIVATE_STORE_NAME packet handling plus private-store open/close message reset.
- Specific behavior/contract: open store message mutates to packet string and SM_PRIVATE_STORE_NAME serializes player object id plus message; missing store sends nothing.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPrivateStoreTests|FullyQualifiedName~CmPrivateStoreTests|FullyQualifiedName~PrivateStoreOpenPlanServiceTests|FullyQualifiedName~PrivateStoreNameOpenCompositionPlanServiceTests" --no-restore -> 17/17 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live player state and packet output changed.
- Broad .NET decision: skipped after focused live packet/state/parser coverage; the filter built Aion.GameServer and directly covered the modified ProcessPacketAsync path.
- Why this scope is sufficient: the passing tests dispatch encoded CM_PRIVATE_STORE_NAME packets and assert live store-message mutation plus serialized packet output.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PrivateStoreService.openPrivateStore` message mutation | `HandleOpenPrivateStoreNameAsync` / `Player.PrivateStoreMessage` | Live state | Partial | Unit Tested | Partial Parity | Uses direct player field instead of Java `PrivateStore` object. |
| `SM_PRIVATE_STORE_NAME` broadcast | `SmPrivateStoreName` from live handler | Packet/fanout | Partial | Unit Tested | Partial Parity | Direct-send fallback covered; registry broadcast used when available. |
| Missing store precondition | `HandleOpenPrivateStoreNameAsync` silent return | Runtime guard | Partial | Unit Tested | Partial Parity | Avoids sending or mutating when no C# store state is open. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPrivateStoreNameSetsStoreMessageAndSendsNamePacket` | Unit/live handler | `PrivateStoreService.openPrivateStore` | Open store gets message and sends `SM_PRIVATE_STORE_NAME` | Source-reviewed Java + live C# handler assertion | Does not cover registry fanout with other visible players. |
| `ProcessPacketAsync_CmPrivateStoreNameMissingStoreReturnsSilently` | Unit/live handler | Java store precondition | Missing store does not mutate or send packets | C# safety behavior aligned with Java expectation that a store exists | Java would throw if misused internally; live packet path should not crash. |

## Summary Metrics

- Focused UOW validation: 17 tests passed.
- Runtime progress: private-store name changes now mutate live state and send a real packet.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: full private-store purchase side effects, full Java `PrivateStore` object model, group loot distribution.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Private-store buy/sell execution remains non-live; store name/open state does not imply purchase parity.
- Existing disabled private-store plan services remain in the tree and should not be treated as runtime parity evidence.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2596 candidate: inspect `CM_BUY_ITEM` player-target action `0` private-store purchase and port only a smallest safe
live side-effect branch if it can mutate seller/buyer inventory/kinah state and send Java-equivalent packets.

If purchase execution is too broad, select another deferred live packet/state/persistence path instead of adding
private-store purchase planners or evidence-only adapters.
