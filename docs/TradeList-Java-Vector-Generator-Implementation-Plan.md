# Trade List Java Vector Generator Implementation Plan

Date: May 26, 2026
Unit of Work: UOW-1168

## Purpose

This document turns the trade-list runtime vector design into an implementation plan for a future Java-side generator. It does not generate artifacts, modify Java runtime code, or claim runtime parity.

Java remains the source of truth. C# live sends for `BUY`, `TRADE_IN`, and no-sell fallback packets must remain disabled until generated Java artifacts are compared by C# tests.

## Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/dataholders/TradeListData.java`
- `game-server/src/com/aionemu/gameserver/dataholders/GoodsListData.java`
- `game-server/src/com/aionemu/gameserver/services/LimitedItemTradeService.java`
- `game-server/src/com/aionemu/gameserver/services/trade/PricesService.java`

## Proposed Runner Shape

Add a test-only Java utility outside production startup, preferably under a future package such as:

- `game-server/test/com/aionemu/gameserver/parity/trade/TradeListVectorGenerator.java`
- `game-server/test/com/aionemu/gameserver/parity/trade/TradeListVectorScenario.java`
- `game-server/test/com/aionemu/gameserver/parity/trade/TradeListPacketCapture.java`
- `game-server/test/com/aionemu/gameserver/parity/trade/CanonicalPacketWriter.java`

If the repository test layout cannot compile these classes without invasive Maven changes, use a standalone debug module or CLI class under a clearly named parity package. Do not place generator hooks in normal server startup paths.

## CLI Contract

The generator should accept deterministic inputs and write one JSON file per scenario:

```text
TradeListVectorGenerator \
  --scenario buy-sellable-normal \
  --java-root <repo-root> \
  --output parity-artifacts/trade-list/java \
  --player-object-id 1001 \
  --npc-object-id 9001 \
  --npc-id 203060 \
  --player-legion-level 0 \
  --vendor-buy-modifier 100
```

Recommended options:

| Option | Required | Notes |
|---|---|---|
| `--scenario` | Yes | One of the scenario ids from `TradeList-Java-Golden-Vector-Design.md`. |
| `--output` | Yes | Output directory; write `<scenario>.json`. |
| `--player-object-id` | Yes | Fixed object id for packet bytes and limited-item buy-count lookup. |
| `--npc-object-id` | Yes | Fixed object id written by trade packets. |
| `--npc-id` | Yes | Static-data lookup id. |
| `--player-legion-level` | Yes | `0` for no legion, or positive level for restricted-goods cases. |
| `--vendor-buy-modifier` | Yes | Overrides or configures `PricesService.getVendorBuyModifier()` input. |
| `--goods-list-id` | Scenario-dependent | Useful for minimal fixture scenarios. |
| `--limited-item` | Scenario-dependent | Repeatable `itemId:buyCount:sellLimit` rows. |
| `--java-commit` | Optional | If omitted, read from `git rev-parse HEAD`. |

## Scenario Build Strategy

Prefer small deterministic fixtures over full server startup. The fixture should build enough Java objects to execute the source branch and packet constructors:

1. Create a `Player` with fixed object id and optional legion-level state.
2. Create an `Npc` with fixed object id, NPC id, localized name, `canSell`, and `canBuy` facts.
3. Load or inject `TradeListTemplate` and `GoodsList` rows matching the scenario.
4. Configure `PricesService.getVendorBuyModifier()` or capture the active value explicitly.
5. Configure `LimitedItemTradeService.getLimitedTradeNpc(npcId)` only for limited-item scenarios.
6. Invoke `DialogService.onDialogSelect(dialogActionId, player, npc, 0, 0)` when fixture setup can safely satisfy static dependencies.
7. If direct `DialogService` invocation requires too much global server state, call the packet constructors plus a copied test-only branch assertion that mirrors the exact Java conditions. Mark such vectors as constructor-level until the full branch is executed.

Constructor-level vectors are useful, but they must not be treated as full `DialogService` parity evidence. The artifact should include `captureLevel = "dialog-service"` or `captureLevel = "packet-constructor"`.

## Capture Strategy

The preferred capture point is `PacketSendUtility.sendPacket(Player, AionServerPacket)` because it observes the same packets Java sends after branch decisions. For a test-only runner, use one of these approaches:

| Approach | Status | Risk |
|---|---|---|
| Static capture hook guarded by a test-only system property | Preferred if accepted by maintainers | Must never affect production sends when unset. |
| Player connection test double that records `sendPacket` calls | Preferred if Java constructors allow it | Requires enough `Player`/connection setup to pass `player.isOnline()`. |
| Direct packet-constructor capture | Fallback | Does not prove `DialogService` branch routing or packet order. |
| Production packet-class edits for logging | Avoid | High risk of runtime side effects and noisy source churn. |

For each captured packet, record:

- packet class name
- send order
- semantic key
- decoded runtime facts
- canonical payload bytes
- packet body bytes
- encrypted wire frame only when crypt state is deterministic

## Canonical Byte Writer

Java `AionServerPacket.write` writes length, obfuscated opcode, static code, opcode complement, body, then encrypts the slice after the length. The parity artifact should prefer stable test bytes:

| Byte Form | Required | Generation Rule |
|---|---|---|
| `bodyHex` | Yes | Invoke `writeImpl` against a test buffer, excluding opcode and encryption. |
| `canonicalPayloadHex` | Yes | Opcode in the same convention used by C# packet tests plus `bodyHex`. |
| `wireFrameHex` | Optional | Full `AionServerPacket.write` output only with deterministic connection crypt state. |

Because `writeImpl` is protected and packet fields are private, `CanonicalPacketWriter` will likely need reflection. Document every reflected method or field, including:

- `AionServerPacket.setBuf` inherited from `BaseServerPacket`, if used
- `SM_TRADELIST.targetObjId`
- `SM_TRADELIST.playerObjId`
- `SM_TRADELIST.tradeNpcType`
- `SM_TRADELIST.buyPriceModifier`
- `SM_TRADELIST.showBuyTab`
- `SM_TRADELIST.showSellTab`
- `SM_TRADELIST.tradeTablist`
- `SM_TRADELIST.limitedItems`
- `SM_TRADE_IN_LIST.npc`
- `SM_TRADE_IN_LIST.tlist`
- `SM_TRADE_IN_LIST.buyPriceModifier`

If reflection becomes too brittle, add package-private test-only accessors in the parity test source set rather than changing production packet semantics.

## Output Layout

Recommended generated artifact layout:

```text
parity-artifacts/
  trade-list/
    java/
      schema-v1.json
      buy-sellable-normal.json
      buy-limited-items.json
      buy-no-template.json
      buy-all-goods-missing.json
      buy-legion-restricted.json
      buy-mixed-legion-tabs.json
      buy-non-default-price.json
      trade-in-sellable.json
      trade-in-no-template.json
```

Do not check in generated artifacts until they are reproducible from a documented command and reviewed against Java source.

## C# Verifier Follow-Up

After Java artifacts exist, add a focused C# verifier that:

1. Reads every JSON file.
2. Builds equivalent C# packet plans from artifact inputs and runtime facts.
3. Serializes `SmTradeList`, `SmTradeInList`, or `SmSystemMessage`.
4. Compares `bodyHex` first, then `canonicalPayloadHex`.
5. Compares packet order and decoded fields.
6. Reports fixture or runtime-fact mismatches separately from serializer mismatches.

Target C# files:

- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeInListPacketPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTradeList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTradeInList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeListPacketPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeInListPacketPlanServiceTests.cs`

## Readiness Gates

Before enabling live C# sends, require all of these:

1. Java generator compiles and runs from a documented command.
2. Minimum scenario matrix artifacts exist with `captureLevel = "dialog-service"` where feasible.
3. C# verifier compares every generated artifact.
4. No packet-order mismatches exist for `BUY` and `TRADE_IN`.
5. No `SM_TRADELIST`, `SM_TRADE_IN_LIST`, or no-sell `SM_SYSTEM_MESSAGE` payload mismatches remain unexplained.
6. Live legion-level lookup is implemented or intentionally staged with documented behavior.
7. Limited-item rows are either fully supported or the unsupported behavior is blocked before live sends.
8. NPC AI/controller routing to the dialog-service branch is verified.

## Known Risks

- Java global static singletons may make small fixture setup difficult.
- `PacketSendUtility.sendPacket` ignores offline players, so a connection double must satisfy `player.isOnline()`.
- `DialogService.onDialogSelect` may require object templates, localized NPC names, and static data holders to be initialized in the same order as server startup.
- Reflection over packet private fields can break silently on Java source changes.
- `SM_TRADE_IN_LIST.writeImpl` writes an empty body when the template is null, npc id is zero, or tab count is zero; live Java reaches no-template fallback before constructing this packet, so constructor-level vectors must not obscure branch behavior.
- `SM_TRADELIST` filters tabs twice: once in `DialogService` for the no-sell decision and again in the packet constructor. The generator must preserve this difference.
- Integer price math must remain Java-style integer arithmetic.
- Date/time behavior for limited-item sales windows is not part of the minimum packet vectors unless explicitly added later.

## Next Recommended Unit

Audit live legion-level lookup prerequisites for moving beyond staged `BUY` runtime facts, or begin a non-invasive Java generator skeleton only after the Java test/CLI build path is confirmed.
