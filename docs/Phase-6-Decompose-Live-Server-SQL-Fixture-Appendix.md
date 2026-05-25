# Phase 6 Decompose Live-Server SQL Fixture Appendix

Date: May 25, 2026
Unit of Work: UOW-890
Status: Fixture appendix complete; not executed locally

## Purpose

Define a conservative SQL fixture checklist for the live Java selectable-decompose capture runbook.

Java remains the source of truth. This appendix does not prove parity and should not be treated as a runnable one-size-fits-all script. It documents the exact tables, columns, cleanup steps, and artifact fields a Java-capable operator should use when preparing `JD-SEL-DEC-001` and `JD-SEL-DEL-001` captures.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java observer/runtime capture | `PacketSendUtility`, server packets, live Java server | artifact files | Runtime Validation | No | High | Requires Java tooling/runtime control unavailable locally. |
| B | SQL fixture appendix | `InventoryDAO`, `players`, `inventory`, `IDFactory` used-id tables | docs only | Documentation Update | Yes | Low | No code changes and directly supports the live-server runbook. |
| C | Optional reward-add cube-update comparison | `ItemPacketService`, guarded C# comparison helper | test file later | Test Infrastructure | No | Medium | Should wait for runtime artifact evidence or a documented optional schema. |
| D | Artifact schema audit | capture docs | docs only | Documentation Update | Yes | Low | Can proceed separately, but the SQL fixture is the current capture blocker. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | SQL fixture appendix for selectable-decompose live capture | Documentation Update | `docs/Phase-6-Decompose-Live-Server-SQL-Fixture-Appendix.md`; progress and handoff docs | Production Java/C# code changes | Live-server capture runbook, Java schema, Java `InventoryDAO`, C# used-id repository | Fixture setup/cleanup guidance and artifact mapping requirements |

No sub-agents were used because the progress and handoff files are shared documentation owned by the orchestrator for this unit.

## Source Anchors

- `game-server/sql/aion_gs.sql`
- `login-server/sql/aion_ls.sql`
- `com.aionemu.gameserver.dao.InventoryDAO`
- `com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `Aion.GameServer.Data.CharacterCreationRepository`
- `Aion.GameServer.Data.MySqlUsedIdRepository`

## Schema Facts

The Java `inventory` table stores cube, equipment, warehouse, and account-warehouse items. Important columns for capture seeding:

| Column | Purpose | Capture Guidance |
|---|---|---|
| `item_unique_id` | Item object id and primary key | Must be unique across Java `IDFactory` used-id sources. Prefer high reserved ids. |
| `item_id` | Static item template id | Must exist in Java static data and match the chosen decomposable source. |
| `item_count` | Stack count | Use `2` for `JD-SEL-DEC-001`; use `1` for `JD-SEL-DEL-001`. |
| `item_owner` | Player id for cube storage | Must be the capture character id. |
| `is_equipped` | Equipment flag | Use `0` for source item captures. |
| `is_soul_bound` | Soul-bound flag | Use `0` unless the real Java item requires otherwise. |
| `slot` | Equipment/storage slot | Use `0` or another known-free slot for cube captures; record the chosen value. |
| `item_location` | Java `StorageType` id | Use cube storage (`0`) for first selectable captures. |
| `item_skin` | Skin/template override | Use the source item template id or `0` depending on the existing server convention; record the value. |

Java `InventoryDAO.INSERT_QUERY` writes 28 columns:

```text
item_unique_id, item_id, item_count, item_color, color_expires, item_creator,
expire_time, activation_count, item_owner, is_equipped, is_soul_bound, slot,
item_location, enchant, enchant_bonus, item_skin, fusioned_item, optional_socket,
optional_fusion_socket, charge, tune_count, rnd_bonus, fusion_rnd_bonus,
tempering, pack_count, is_amplified, buff_skill, rnd_plume_bonus
```

Use that column order when writing fixture inserts so Java DAO load behavior sees the same defaults as normal inventory records.

The Java/C# used-id surfaces include:

- `players.id`
- `inventory.item_unique_id`
- `player_registered_items.item_unique_id`
- `legions.id`
- `mail.mail_unique_id`
- `guides.guide_id`
- `houses.id`
- `player_pets.id`

Do not choose source or reserved reward object ids that collide with any of these.

## Preconditions

Before seeding a scenario:

1. Reset the game schema or choose a dedicated capture account/character.
2. Confirm the character can enter the Java game server without login-time item rewards.
3. Teleport or place the character in an empty known-list area.
4. Disable or avoid surveys, event rewards, mailbox item claims, broker returns, and quest reward fanout.
5. Choose real Java XML item ids for:
   - selectable source
   - reward index 0
   - reward index 1
6. Confirm the chosen reward counts are deterministic: `min_count == max_count`.
7. Record the exact Java Git SHA and static-data files used.

If the Java live server cannot use logical ids `101`, `201`, and `202`, use real Java ids and record `fixture.id_mapping` in each artifact.

## Capture Character Lookup

Use the character name chosen in the runbook:

```sql
SELECT id, name, account_id, account_name, race, player_class, world_id, x, y, z
FROM players
WHERE name = 'CapturePlayer';
```

Record:

- `players.id` as `fixture.player.object_id` if the runtime object id matches persisted id in the capture environment
- `race`
- `player_class`
- `world_id`
- starting location

If the runtime player object id differs from `players.id`, use the runtime id in packet fields and record the relationship in artifact notes.

## Cleanup Queries

Run cleanup before each scenario, scoped to the capture player:

```sql
DELETE FROM item_stones
WHERE item_unique_id IN (
  SELECT item_unique_id FROM inventory WHERE item_owner = @player_id AND item_location = 0
);

DELETE FROM inventory
WHERE item_owner = @player_id
  AND item_location = 0;
```

Optional noise cleanup for a dedicated reset character:

```sql
UPDATE players
SET mailbox_letters = 0
WHERE id = @player_id;

DELETE FROM surveys
WHERE owner_id = @player_id;
```

Do not delete account warehouse records (`item_location = 2`) unless the capture environment is disposable and the operator has confirmed the owner id is the account id, not player id.

## Reserved Object Id Check

Choose high object ids that do not collide with Java used-id sources:

```sql
SELECT 'players' AS source, id AS object_id FROM players WHERE id IN (@source_object_id, @reserved_reward_object_id)
UNION ALL
SELECT 'inventory', item_unique_id FROM inventory WHERE item_unique_id IN (@source_object_id, @reserved_reward_object_id)
UNION ALL
SELECT 'player_registered_items', item_unique_id FROM player_registered_items WHERE item_unique_id IN (@source_object_id, @reserved_reward_object_id)
UNION ALL
SELECT 'mail', mail_unique_id FROM mail WHERE mail_unique_id IN (@source_object_id, @reserved_reward_object_id)
UNION ALL
SELECT 'houses', id FROM houses WHERE id IN (@source_object_id, @reserved_reward_object_id)
UNION ALL
SELECT 'player_pets', id FROM player_pets WHERE id IN (@source_object_id, @reserved_reward_object_id);
```

If any row is returned, choose different ids.

Note: Java `ItemService.addItem` usually allocates reward object ids through `IDFactory`. A reserved reward id is useful only for collision planning and artifact mapping expectations; do not assume Java will allocate that exact id unless the runtime `IDFactory` state is controlled.

## Scenario Seed Templates

Replace placeholders before running. The template ids below are logical values; use real Java template ids unless the capture server has dedicated test static data.

### JD-SEL-DEC-001

Source count `2`, select index `1`, expected reward logical id `202 x3`.

```sql
INSERT INTO inventory (
  item_unique_id, item_id, item_count, item_color, color_expires, item_creator,
  expire_time, activation_count, item_owner, is_equipped, is_soul_bound, slot,
  item_location, enchant, enchant_bonus, item_skin, fusioned_item, optional_socket,
  optional_fusion_socket, charge, tune_count, rnd_bonus, fusion_rnd_bonus,
  tempering, pack_count, is_amplified, buff_skill, rnd_plume_bonus
) VALUES (
  @source_object_id, @java_source_item_id, 2, NULL, 0, NULL,
  0, 0, @player_id, 0, 0, @source_slot,
  0, 0, 0, @java_source_item_id, 0, 0,
  0, 0, 0, 0, 0,
  0, 0, 0, 0, 0
);
```

Artifact mapping example:

```json
{
  "logical_source_item_id": 101,
  "java_source_item_id": 188052590,
  "logical_reward_index_1": 202,
  "java_reward_index_1": 188052592,
  "logical_source_object_id": 5001,
  "java_source_object_id": 70001
}
```

After capture, record the generated Java reward object id:

```json
{
  "logical_reward_index_1_object_id": 1,
  "java_reward_index_1_object_id": 70002
}
```

### JD-SEL-DEL-001

Source count `1`, select index `0`, expected reward logical id `201 x2`.

```sql
INSERT INTO inventory (
  item_unique_id, item_id, item_count, item_color, color_expires, item_creator,
  expire_time, activation_count, item_owner, is_equipped, is_soul_bound, slot,
  item_location, enchant, enchant_bonus, item_skin, fusioned_item, optional_socket,
  optional_fusion_socket, charge, tune_count, rnd_bonus, fusion_rnd_bonus,
  tempering, pack_count, is_amplified, buff_skill, rnd_plume_bonus
) VALUES (
  @source_object_id, @java_source_item_id, 1, NULL, 0, NULL,
  0, 0, @player_id, 0, 0, @source_slot,
  0, 0, 0, @java_source_item_id, 0, 0,
  0, 0, 0, 0, 0,
  0, 0, 0, 0, 0
);
```

Artifact mapping example:

```json
{
  "logical_source_item_id": 101,
  "java_source_item_id": 188052590,
  "logical_reward_index_0": 201,
  "java_reward_index_0": 188052591,
  "logical_source_object_id": 5001,
  "java_source_object_id": 70011,
  "logical_reward_index_0_object_id": 1,
  "java_reward_index_0_object_id": 70012
}
```

## Post-Capture Verification Queries

For decrement scenario:

```sql
SELECT item_unique_id, item_id, item_count, item_owner, item_location, slot
FROM inventory
WHERE item_owner = @player_id
  AND item_location = 0
ORDER BY item_unique_id;
```

Expected shape:

- source item still exists with count `1`
- reward item exists with deterministic reward count
- no unrelated cube items exist unless explicitly documented

For delete scenario:

```sql
SELECT item_unique_id, item_id, item_count, item_owner, item_location, slot
FROM inventory
WHERE item_owner = @player_id
  AND item_location = 0
ORDER BY item_unique_id;
```

Expected shape:

- source item no longer exists
- reward item exists with deterministic reward count
- no unrelated cube items exist unless explicitly documented

## Artifact Requirements

Each Java artifact must include:

- exact SQL seed values or a reference to the checked-in fixture script
- `fixture.id_mapping` for any real Java item ids that differ from logical C# ids
- `fixture.id_mapping` for source and reward object ids if Java object ids differ from C# fixture ids
- `final_inventory` from post-capture SQL or observer/runtime state
- notes for any cleanup skipped or unrelated records left in place
- any runtime packet noise that was observed and filtered

Do not rewrite real Java ids into logical ids inside packet `decoded_fields`. The C# guarded comparison now normalizes declared mappings.

## Stop Conditions

Stop before writing artifacts if:

- source or reward template ids cannot be confirmed in Java static data
- reward min/max count is nondeterministic
- object ids collide with existing used-id sources
- login/event/mail/survey systems add unrelated cube items during capture
- Java `IDFactory` allocates a reward id that cannot be recorded clearly
- post-capture SQL does not match the observed packet side effects
- the capture requires deleting non-capture player/account data

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.CharacterCreationRepository` / inventory persistence helpers | Repository | Partial | Manual Only for appendix; Regression Tested elsewhere | Needs Verification | Appendix mirrors Java `InventoryDAO.INSERT_QUERY` column order for fixture seeding. It was not executed locally; transaction/autocommit and DAO load behavior remain unverified for live capture. |
| `com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO` | `Aion.GameServer.Data.MySqlUsedIdRepository` used-id query set | Repository | Partial | Manual Only | Needs Verification | Appendix includes `player_registered_items.item_unique_id` in collision checks because Java/C# used-id discovery treats it as part of the shared object-id space. Runtime `IDFactory` state remains unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Manual Only for appendix; Regression Tested in C# decompose tests | Needs Verification | Appendix seeds cube storage (`item_location = 0`) and requires post-capture SQL checks for source decrement/delete. Java persistence and quest callback side effects remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` / item services | Service | Partial | Manual Only for appendix; Regression Tested in C# decompose tests | Needs Verification | Appendix documents generated reward object-id recording but does not control or verify Java `IDFactory` allocation. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / guarded comparison helper | Service / Packet Side Effects | Partial | Manual Only for appendix; Regression Tested in C# comparison helper | Needs Verification | Appendix supports runtime capture of source/reward packet side effects but no Java artifact was generated. Reward-add trailing `SM_CUBE_UPDATE` remains a risk. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / guarded comparison helper | Client Packet Handler | Partial | Manual Only for appendix; Regression Tested in C# decompose tests | Partial Parity | Appendix defines SQL setup for the two selectable scenarios. It does not execute Java handler behavior or prove parity. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / SQL Fixture Design | Java `aion_gs.sql`, Java `InventoryDAO.INSERT_QUERY`, C# used-id repository | Defines live-server SQL fixture setup, cleanup, collision checks, and post-capture verification queries. | Static source/schema inspection only. | Not executed against a Java DB; no Java runtime artifact; no C# comparison against Java output. |

## Remaining Risks

- The appendix was not executed locally because Java 25/Maven/live-server capture tooling remains unavailable.
- Real Java template ids for deterministic selectable decomposable items still must be selected from static data.
- Java `IDFactory` reward object-id allocation is not controlled by the appendix unless the operator resets/inspects used-id state.
- Fixture SQL cannot prevent runtime packet noise from events, mailbox, surveys, or login rewards unless those systems are also controlled.
- Date/time, transaction/autocommit, and persistence timing behavior were not verified.
- Full item-info blob, byte capture, and live-client validation remain outside this unit.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 production code artifacts; 1 SQL fixture appendix added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, real template-id selection, SQL fixture execution, reward object-id control, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit improves capture readiness but adds no runtime evidence.

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, execute the live-server runbook with this appendix and the packet observer design to generate the first Java artifacts.

If tooling remains blocked, add a guarded comparison design or test path for optional Java-observed reward-add trailing `SM_CUBE_UPDATE`, or continue an isolated non-decompose Phase 6 gameplay slice that does not touch the shared decompose comparison helper.
