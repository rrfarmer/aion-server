# Trade List Legion-Level Live Lookup Audit

Date: May 26, 2026
Unit of Work: UOW-1169

## Purpose

This audit identifies the minimum C# runtime surface needed to replace the staged no-legion fallback used by trade-list `BUY` planning.

Java remains the source of truth. This audit does not enable live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, or no-sell packet sends.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/sql/aion_gs.sql`

## Java Behavior

`DialogService.onDialogSelect` computes the `BUY` goods-list gate as:

```java
int legionLevel = player.getLegion() == null ? 0 : player.getLegion().getLegionLevel();
```

It then treats a tab as sellable only when:

```java
goodsList != null && goodsList.getLegionLevel() <= legionLevel
```

`SM_TRADELIST` repeats the same legion-level filter in its constructor. This means Java has two separate filtering points:

1. `DialogService` decides whether to send `SM_TRADELIST` or no-sell `SM_SYSTEM_MESSAGE`.
2. `SM_TRADELIST` filters the actual tab ids written to the packet.

The source of `player.getLegion()` is `Player.getLegion()`, which returns `legionMember != null ? legionMember.getLegion() : null`. `Legion.getLegionLevel()` returns the in-memory `legionLevel`, defaulting to `1` when a new `Legion` is constructed and loaded from the `legions.level` database column by `LegionDAO.loadLegion`.

## Java Hydration Path

The relevant Java player/legion path is:

1. `PlayerService` loads the player and calls `LegionService.getInstance().getLegionMember(playerCommonData)`.
2. `LegionService.getLegionMember` loads or returns cached `LegionMember`.
3. `LegionMemberDAO.loadLegionMember` reads `legion_members.legion_id` for the player.
4. `LegionService.getLegion(legionId)` returns a cached legion or calls `LegionDAO.loadLegion(legionId)`.
5. `LegionDAO.loadLegion` reads `legions.level` and calls `legion.setLegionLevel(resultSet.getInt("level"))`.
6. `Player.setLegionMember` attaches that `LegionMember` to the player.
7. `DialogService` and `SM_TRADELIST` read the live level through `player.getLegion().getLegionLevel()`.

Database schema source:

```sql
CREATE TABLE `legions` (
  `id` int NOT NULL,
  `name` varchar(32) NOT NULL,
  `level` int NOT NULL DEFAULT '1',
  ...
)
```

No date/time, precision, rounding, serialization, or threading conversion is involved in the level value itself. The main Java runtime behavior risk is cache freshness: Java reads level through the cached `Legion` object, and level changes call `Legion.setLegionLevel`.

## Current C# State

Relevant C# files:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`

Current observations:

- `Player` has `LegionId`, `LegionName`, and legion emblem fields, but no `LegionLevel`.
- `PlayerEnterWorldRepository` already joins `legion_members lm` and `legions l`, but selects only `lm.legion_id` and `l.name AS legion_name` from the legion table.
- `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` calls `NpcDialogTradeRuntimeFactAdapterService.CreatePlan` with `player.ObjectId`, `player.LegionId`, and the configured vendor-buy modifier. It does not pass a player legion level.
- `NpcDialogTradeRuntimeFactAdapterService` therefore uses `PlayerLegionLevel ?? 0` and marks the source as `Staged default: Player.getLegion() unavailable, Java no-legion fallback`.
- Existing tests prove the adapter can carry an injected level, but they deliberately keep `IsLive = false`.

## Minimum Future C# Work

The smallest future code unit to move from staged no-legion fallback toward live facts should:

1. Add `Player.LegionLevel`, defaulting to `0` for no legion.
2. Extend `PlayerEnterWorldRepository` to select `l.level AS legion_level`.
3. Hydrate `Player.LegionLevel = ReadInt(reader, "legion_level")`.
4. Pass `PlayerLegionLevel: player.LegionLevel` into `NpcDialogTradeRuntimeFactAdapterInput`.
5. Keep `NpcDialogTradeRuntimeFactAdapterPlan.IsLive = false` unless vendor modifier ownership and trade packet sends are also live-ready.
6. Add tests proving:
   - no legion maps to level `0`;
   - a joined legion row maps to the actual `legions.level`;
   - the dialog trade runtime plan carries the hydrated level into `NpcDialogTradeListFactAdapterInput`;
   - restricted goods-list tabs remain blocked when `GoodsList.LegionLevel > PlayerLegionLevel`;
   - allowed tabs pass when `GoodsList.LegionLevel <= PlayerLegionLevel`.

This would still not be enough to enable live sends. It would only replace one staged runtime fact.

## Unsupported Or Unverified Behavior

- No C# live `LegionService` equivalent currently owns cached legion mutation and level-up fanout.
- Player enter-world hydration can read `legions.level`, but live level changes during a session would require a mutation/update path or cache invalidation strategy.
- The C# trade boundary is still non-sending, so the hydrated level would only affect staged plans until Java vector artifacts and live send gates are complete.
- `SM_TRADELIST` repeats the legion-level filter in Java; any future C# send path must ensure both branch-level sellability and packet-plan tab filtering receive the same level.
- The C# adapter should not mark the fact source as fully live until the owner of legion level during session lifetime is clear.
- No Java runtime vector currently proves a legion-restricted vendor scenario.

## Readiness Gates Before Live Send Wiring

Before live `BUY` send wiring, require:

1. Java runtime vector for `buy-legion-restricted`.
2. Java runtime vector for `buy-mixed-legion-tabs`.
3. C# artifact verifier comparing packet order and tab ids for those vectors.
4. C# player legion level hydration from `legions.level`, with tests.
5. Defined behavior for live level changes during a session.
6. No mismatch between branch-level filtering and packet-plan filtering.

## Next Recommended Unit

Implement the narrow C# `Player.LegionLevel` hydration and adapter handoff, while keeping `IsLive = false` and all trade packet sends disabled. Alternatively, audit vendor-buy modifier live ownership if the next session stays docs-only.

## UOW-1170 Implementation Update

UOW-1170 implemented the narrow hydration path identified by this audit:

- `Player.LegionLevel` now exists as a projected player field.
- `PlayerEnterWorldRepository` selects `l.level AS legion_level`.
- `PlayerEnterWorldRepository` hydrates `Player.LegionLevel`.
- `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` passes `PlayerLegionLevel` only when `player.LegionId != 0`; no-legion players still use the explicit Java fallback level `0`.
- `HandleDialogSelectAsync_BuyRestrictedGoodsUsesHydratedLegionLevelWithoutSending` verifies that a level-5 legion player can pass the staged restricted-goods filter while packets remain unsent and the runtime fact plan remains `IsLive = false`.

This update does not add a live legion service/cache, does not verify Java runtime vectors, and does not enable live trade packet sends.
