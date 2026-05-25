# Phase 6NS Completion Handoff - Decompose Java Runtime Comparison Plan

Date: May 25, 2026
Unit of Work: UOW-871
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-871] Plan decompose Java runtime comparison`)

## Status

Phase 6 is still in progress. This unit did not change production code. It created a focused Java-runtime comparison plan for decompose packet order and crypt bytes after the C# socket-loop coverage added in UOW-868 through UOW-870.

The plan documents target scenarios, Java artifacts, current C# evidence, expected packet order, preferred Java in-process harness, live-server fallback, risks, and the next harness spike. Java runtime output was not captured in this unit, so parity remains unverified beyond the existing C# source-derived tests.

## Files Changed

- `docs/Phase-6-Decompose-Java-Runtime-Comparison-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NS-Completion.md`

## What Changed

- Added `docs/Phase-6-Decompose-Java-Runtime-Comparison-Plan.md`.
- Audited Java artifacts:
  - `AionConnection`
  - `Crypt`
  - `EncryptionKeyPair`
  - `AionServerPacket`
  - `SM_KEY`
  - `CM_USE_ITEM`
  - `CM_SELECT_DECOMPOSABLE`
  - `DecomposeAction`
  - `PacketSendUtility`
  - `ItemService` / `ItemPacketService`
- Defined comparison scenarios for:
  - selectable decompose
  - normal decompose source decrement
  - normal decompose source delete
- Documented expected Java packet order for each scenario.
- Documented that no ready-made game-core Java runtime packet comparison harness currently exists.
- Recommended starting with a Java harness spike for selectable decompose before attempting scheduled normal decompose.

## Tests

Code tests:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1445 tests.

Documentation-only validation:

- Plan reviewed against Java source listed above.
- No Java runtime harness or golden artifact generated.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionConnection` | `GameServerConnection` | Connection Lifecycle / Dispatch | Partial | Manual Only | Needs Verification | Java `initialized`, `processData`, send queue, and active-player behavior were audited for runtime comparison planning. No Java runtime capture exists yet. |
| `com.aionemu.gameserver.network.Crypt` | `GameCrypt` | Crypto Utility | Partial | Manual Only | Needs Verification | Java first-packet unencrypted behavior, random key generation, opcode transforms, and client/server key use were audited. Deterministic Java key injection remains unresolved. |
| `com.aionemu.gameserver.network.EncryptionKeyPair` | `GameEncryptionKeyPair` | Crypto Utility | Partial | Manual Only | Needs Verification | Java client decrypt validation, server encrypt, static key, and key increment by body length were audited. Game-protocol vectors are not generated yet. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `GameServerPacket` | Packet Serialization | Partial | Manual Only | Needs Verification | Java frame length/opcode/static code/flipped opcode/encrypt sequence was audited. No Java-generated decompose packet bytes were captured. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KEY` | `SmKey` | Packet / Handshake | Partial | Manual Only | Needs Verification | Java writes `con.enableCryptKey()` into the first unencrypted server packet. Deterministic Java key generation or captured live key is needed before byte comparison. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `CmSelectDecomposable` / `HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Manual Only | Needs Verification | Plan defines a selectable decompose comparison scenario and expected packet order. Java runtime artifact is not captured yet. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `CmUseItem` / `HandleUseItemAsync` | Client Packet Handler | Partial | Manual Only | Needs Verification | Plan defines normal decompose decrement/delete comparison scenarios. Java runtime scheduler and packet/order artifacts are not captured yet. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `DecomposeService` / `HandleDecomposeUseItemAsync` | Item Action / Scheduled Handler | Partial | Manual Only | Needs Verification | Java `canAct`, selectable branch, normal scheduled action, success message, reward add, and cancel observer were mapped to scenarios. Java scheduler runtime behavior remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `SendPacketAsync` / `BroadcastItemUsageAnimationAsync` | Packet Send / Broadcast Utility | Partial | Manual Only | Needs Verification | Java self-send and known-list broadcast semantics were audited. Runtime capture needs a minimal online player/client connection or live server. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService` | `InventoryAddService` / inventory packet writers | Item Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Java add/decrement/delete packet send points were identified. DAO/autocommit and packet byte behavior remain unverified. |

## Remaining Risks

- Java runtime comparison remains unimplemented; no artifact currently proves Java-vs-C# packet order or byte parity for decompose.
- Deterministic Java `Crypt` key generation may require reflection or a copied vector generator under `dotnetConversion/tools`.
- Java `PacketSendUtility` requires an online player and client connection; a minimal in-process harness may be awkward without test seams.
- Java static `DataManager`, inventory persistence, DAO/autocommit behavior, and scheduler behavior may force a live-server fixture rather than a small unit harness.
- Full packet byte parity, opcode/frame/crypto breadth, broadcast fanout, socket visibility, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 10
- Total artifacts ported: 0 code artifacts; 1 comparison-plan document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked artifacts: 8 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Start the Java harness spike from `docs/Phase-6-Decompose-Java-Runtime-Comparison-Plan.md`.

Suggested scope:

- Determine whether `AionConnection`/`Player`/`PacketSendUtility` can be wrapped or subclassed for a selectable-decompose packet-order capture without changing production Java.
- Identify the minimal `Player`, inventory, known-list, and `DataManager` setup needed for `CM_SELECT_DECOMPOSABLE`.
- Decide whether deterministic `Crypt` needs reflection or a copied Java vector generator under `dotnetConversion/tools`.
- If this is too broad, switch to the fallback C# verification unit for corrupt encrypted packet threshold and multi-packet key evolution in `GameCrypt`.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java harness feasibility spike | Java source/test-harness audit, possibly `dotnetConversion/tools` if a vector generator is selected | Maybe | Keep read-only first; only write after exact file ownership is clear. |
| C# corrupt encrypted packet test | `GameClientSocketServerSmokeTests.cs` or a dedicated test file | Yes if separate from harness work | Good fallback that avoids shared decompose fixture. |
| C# multi-packet key evolution test | dedicated test file for `GameCrypt` if feasible | Yes if production crypto is not modified | Useful fallback for crypt parity breadth. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Java comparison docs and progress/handoff docs.
- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Java harness/vector generator edits alongside C# crypto tests unless exact file ownership is isolated.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Start with Java harness feasibility for selectable decompose packet-order capture.
6. Run focused and full tests for any code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
