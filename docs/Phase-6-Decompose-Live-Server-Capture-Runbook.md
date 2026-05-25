# Phase 6 Decompose Live-Server Capture Runbook

Date: May 25, 2026
Unit of Work: UOW-884
Status: Runbook complete; no Java runtime artifact captured in this environment

## Purpose

This runbook defines a live Java server fallback for producing the selectable-decompose JSON artifacts described in `docs/Phase-6-Decompose-Java-Capture-Contract.md`.

Java remains the source of truth. This document does not prove parity by itself. A capture only becomes runtime evidence after a Java-capable environment records JSON artifacts for the required scenarios and C# tests compare against them deterministically.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java loopback proof validation | `LoopbackCaptureProof`, `AionConnection`, `Crypt` | none | Java Runtime Validation | No | High | Still requires Java 25 JDK and Maven/classpath support unavailable locally. |
| B | Guarded C# artifact comparison test | future Java JSON artifacts, C# projection helper | decompose test file | Test Infrastructure | Maybe | Medium | Useful after artifact paths are agreed, but less helpful before a Java capture operator knows how to generate the files. |
| C | Live-server capture runbook | `CM_SELECT_DECOMPOSABLE`, item/storage packet services, DB fixture | docs only | Documentation Update | Yes | Low | Can be completed without Java tooling and gives the next Java-capable environment exact capture steps. |
| D | System-message factory mapping | `SM_SYSTEM_MESSAGE` factories | decompose test file | Test Refinement | No | Low | Narrow improvement, but the artifact generation path is the current blocker. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Live-server decompose capture runbook | Documentation Update | `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`; progress and handoff docs | Production Java/C# code changes | Phase 3 mixed-mode validation runbook, Java capture contract, UOW-883 C# JSON projection | Deterministic live-server steps, fixture checklist, packet capture requirements, artifact file naming, and stop conditions |

No sub-agents were used because the write targets are shared documentation and progress/handoff files.

## Prerequisites

- Java 25 JDK available to the Java build/runtime.
- Maven available if rebuilding Java server artifacts outside Docker.
- Docker available for the existing mixed-mode scripts, or an equivalent Java server environment.
- MySQL/MariaDB schema initialized from:
  - `login-server/sql/aion_ls.sql`
  - `game-server/sql/aion_gs.sql`
  - `chat-server/sql/aion_cs.sql`
- A patched Aion client or trusted packet driver that can enter the Java game server and trigger `CM_SELECT_DECOMPOSABLE`.
- A clean capture character/account reserved for this work.

Known local blocker at runbook creation:

- This workstation has Java 8 runtime only, no `javac`, and no Maven on PATH, so the runbook was not executed here.

## Baseline Startup

Prefer the existing mixed-mode scripts from `docs/PHASE-3-MIXED-MODE-VALIDATION.md`:

```powershell
powershell -ExecutionPolicy Bypass -File dotnetConversion/scripts/start-mixed-mode-db.ps1 -ResetSchema
dotnet run --project dotnetConversion/src/Aion.LoginServer/Aion.LoginServer.csproj
powershell -ExecutionPolicy Bypass -File dotnetConversion/scripts/start-mixed-mode-java.ps1 -Build -Detached
```

Expected Java server shape:

- Java game server listens on `127.0.0.1:7777`.
- Login server listens on `127.0.0.1:2106`.
- Java game server connects to the login server as GS `1`.
- Java chat server is optional for this capture unless the client cannot enter world without it.

Stop commands:

```powershell
powershell -ExecutionPolicy Bypass -File dotnetConversion/scripts/stop-mixed-mode-java.ps1
```

## Fixture Strategy

Preferred fixture ids from the capture contract:

| Logical Field | Preferred Value | Notes |
|---|---:|---|
| player object id | `1001` | Exact value is useful but not required for live capture. Record the actual value if different. |
| source object id | `5001` | Exact value is useful but not required for live capture. Record the actual value if different. |
| selectable source item id | `101` | Test-only id used by C# fixtures. Live Java XML may require real item ids. |
| reward index 0 | `201 x2` | Delete scenario reward in C# fixtures. |
| reward index 1 | `202 x3` | Decrement scenario reward in C# fixtures. |
| select unknown dword | `0` | Java reads and ignores this field. |

If the live Java server cannot use the test-only item ids, choose real Java XML decomposable ids with deterministic reward counts. The artifact must then include `fixture.id_mapping`, for example:

```json
{
  "id_mapping": {
    "logical_source_item_id": 101,
    "java_source_item_id": 188052590,
    "logical_reward_index_0": 201,
    "java_reward_index_0": 188052591,
    "logical_reward_index_1": 202,
    "java_reward_index_1": 188052592
  }
}
```

Do not silently rewrite ids in the JSON. The C# comparison should know whether it is comparing logical fixture ids or mapped live ids.

## Database Fixture Checklist

Before the capture:

1. Create or select a single capture account and character.
2. Ensure the character can enter world and has no visible nearby players.
3. Remove unrelated cube items unless they are needed to stabilize IDFactory/object-id allocation.
4. Insert exactly one selectable source item for each run:
   - decrement run: source count `2`, select index `1`
   - delete run: source count `1`, select index `0`
5. Ensure source and rewards are stackable and deterministic.
6. Clear or record cooldowns, mailbox claims, surveys, event rewards, and other login-time item fanout that could pollute packet order.
7. Record the exact SQL used to seed inventory and any static-data override files.

Recommended isolation:

- Start from a reset game database.
- Capture one scenario per server restart if object-id determinism matters.
- Use an empty known-list area or GM command teleport location with no nearby players.
- Disable or avoid event systems that award items at login.

## Packet Observation Requirements

The first acceptable live-server capture must reach capture levels 1 and 2 for both scenarios.

Required scenarios:

| Scenario Id | Source Count | Select Index | Expected Packet Order |
|---|---:|---:|---|
| `JD-SEL-DEC-001` | `2` | `1` | `SM_ITEM_USAGE_ANIMATION`, `SM_SYSTEM_MESSAGE`, `SM_INVENTORY_UPDATE_ITEM`, `SM_SECONDARY_SHOW_DECOMPOSABLE`, `SM_INVENTORY_ADD_ITEM` |
| `JD-SEL-DEL-001` | `1` | `0` | `SM_ITEM_USAGE_ANIMATION`, `SM_SYSTEM_MESSAGE`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE`, `SM_SECONDARY_SHOW_DECOMPOSABLE`, `SM_INVENTORY_ADD_ITEM` |

Required decoded fields:

- `SM_ITEM_USAGE_ANIMATION`: player object id, target object id, item object id, item id, time, end, unknown fields.
- `SM_SYSTEM_MESSAGE`: message id or Java factory name, parameter list, trailing flags.
- `SM_INVENTORY_UPDATE_ITEM`: source object id, item name, decoded source count, update type mask/name.
- `SM_DELETE_ITEM`: source object id, delete type mask/name.
- `SM_CUBE_UPDATE`: action, storage, post-delete item count, expansion fields.
- `SM_SECONDARY_SHOW_DECOMPOSABLE`: source object id, unknown dword, reward count.
- `SM_INVENTORY_ADD_ITEM`: add type mask/name, reward object id, item id, item name, count, slot, cloth flag.

Byte capture:

- Level 3 unencrypted body bytes are valuable but optional for first live artifacts.
- Level 4 encrypted frame bytes are deferred unless the operator can recover/decrypt frames deterministically.
- If byte fields are unavailable, set them to `null` and include a reason in `unsupported`.

## Artifact Paths

Store Java live-server artifacts under:

```text
docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEC-001.json
docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEL-001.json
```

Use the schema from `docs/Phase-6-Decompose-Java-Capture-Contract.md`:

```json
{
  "schema_version": 1,
  "scenario_id": "JD-SEL-DEC-001",
  "capture_date": "YYYY-MM-DD",
  "java_revision": "git-sha",
  "capture_method": "live-java-server",
  "capture_levels": [0, 1, 2],
  "fixture": {},
  "client_packet": {},
  "packets": [],
  "final_inventory": [],
  "unsupported": [],
  "risks": []
}
```

## Capture Implementation Options

Choose one observation method and record it in `capture_method_details`.

### Client-Side Frame Capture

Use when packet capture/decryption tooling is available.

1. Start the live Java server.
2. Log in with the capture character.
3. Begin packet capture filtered to the client/server connection.
4. Trigger the selectable decompose scenario.
5. Decode observed server packet classes and fields.
6. Stop capture immediately after the reward mutation packet.

Risks:

- Requires reliable opcode/frame decryption or a trusted decoded client log.
- Nearby player fanout can pollute packet order.

### Java Instrumentation Logger

Use when a small local Java-only diagnostic patch is acceptable in the capture environment.

1. Add a diagnostic packet observer around `PacketSendUtility.sendPacket`, `broadcastPacketAndReceive`, or `AionConnection.sendPacket`.
2. Filter to the capture player object id.
3. Log packet Java class names and decoded fields before encryption.
4. Trigger one scenario per clean run.
5. Remove or isolate the diagnostic patch after artifacts are produced.

Risks:

- Production Java source must not keep broad diagnostic code.
- Reflection/private-field shortcuts must be documented if used.
- If only class names are logged, Level 2 is incomplete.

### Server Log Plus Manual Decoder

Use only as a last resort.

1. Add temporary targeted log lines in Java packet constructors or `writeImpl`.
2. Capture class order and key fields.
3. Manually assemble the JSON artifact.

Risks:

- High transcription risk.
- Does not capture bytes.
- Must include the exact Java diff or log patch in artifact notes.

## Pass/Fail Gate

A live capture is acceptable only if:

- `java_revision` is the exact Git SHA used by the running Java game server.
- The artifact records `capture_method = live-java-server`.
- Both required scenarios have JSON artifacts.
- Packet order is observed, not inferred.
- Decoded fields include source mutation, secondary clear, and reward mutation fields.
- Missing bytes are explicitly listed in `unsupported`.
- ID mappings are explicit if real Java ids differ from logical fixture ids.
- Any additional packets are either removed by fixture isolation or documented with Java evidence.

Do not mark any C# row as `Verified Parity` until a C# test compares these Java artifacts against C# output and passes deterministically.

## Stop Conditions

Stop and document the blocker if:

- The character cannot enter world without unrelated packet noise.
- The selectable source item cannot be seeded deterministically.
- Reward RNG cannot be made deterministic.
- Packet order includes unrelated login/event item packets during the scenario.
- Object ids or item ids cannot be mapped clearly.
- The capture method cannot decode Level 2 fields.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / C# JSON projection helper | Client Packet Handler | Partial | Manual Only for runbook; Regression Tested in C# projection | Needs Verification | Runbook defines live Java capture steps for both selectable scenarios. No Java artifact was captured in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send observer / projection helper | Utility | Partial | Manual Only | Needs Verification | Runbook recommends self-send isolation and optional Java instrumentation at send boundaries. Broadcast/threading behavior remains uncaptured. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Manual Only | Needs Verification | Runbook requires source decrement/delete side effects to be captured from Java runtime. Persistence and quest callback behavior remain outside first artifacts. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / server packets | Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Runbook requires Java `SM_CUBE_UPDATE` after delete and source/reward packet fields. No runtime artifact yet. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` | Service | Partial | Manual Only | Needs Verification | Runbook covers deterministic reward counts and id mapping requirements. Java reward object-id allocation remains uncaptured. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` decomposable item data | Data Holder | Partial | Manual Only | Needs Verification | Runbook allows test ids or real XML ids with explicit mapping. Java XML/static-data runtime parity remains unverified. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Runbook | Java capture contract, Phase 3 mixed-mode startup docs, and Java source references | Defines how to produce live Java selectable-decompose JSON artifacts. | No runtime evidence; planning only. | No Java artifact, no C# comparison against Java JSON, no byte capture, no live-client result in this unit. |

## Remaining Risks

- Local Java 25/Maven tooling remains unavailable, so this runbook was not executed here.
- Live client setup may need account/character creation, client patching, and DB fixture work not fully scripted here.
- Real Java XML ids may differ from C# fixture ids and require explicit mapping.
- Packet observation may need temporary Java diagnostics; any such patch must be isolated and documented.
- Login/event systems can emit unrelated item packets and pollute capture order.
- Byte-level capture remains deferred.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 production code artifacts; 1 live-server capture runbook added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, live Java JSON artifact generation, fixture SQL/script automation, packet observer implementation, byte capture, C# Java-artifact comparison tests, object-id parity, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit improves the Java artifact path but adds no runtime evidence.

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run `LoopbackCaptureProof` or execute this live-server runbook to generate the first Java JSON artifacts.

If tooling remains blocked, add a guarded C# comparison test that looks for future Java artifacts at `docs/parity-artifacts/java/decompose/selectable/*.json`, reports absence as an explicit skip/needs-verification condition, and compares packet order/decoded fields when the files exist.
