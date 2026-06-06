# Phase 6 Session 2663 Completion

## UOW

[Phase 6] UOW-2663: Send house-script overflow response.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: the live CM_HOUSE_SCRIPT dispatch now handles Java's oversized compressed-script guard.
- Java source/runtime path: CM_HOUSE_SCRIPT.readImpl/runImpl -> SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE -> SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_SCRIPT_OVERFLOW.
- C# runtime artifact wired: GameServerConnection CmHouseScript branch and SmSystemMessage.HousingScriptOverflow.
- Client-visible/state/persistence effect: a client that submits an oversized house script now receives the Java overflow system message instead of silence.
- Why this is runtime progress: this sends a real server packet from the live client-packet dispatch path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_SCRIPT.java`
  - Reads address, script id, total size, compressed size, and only reads content when the compressed size is within `SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE`.
  - `runImpl` sends `SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_SCRIPT_OVERFLOW()` before house ownership or script mutation when the compressed size is too large.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_HOUSE_SCRIPTS.java`
  - Defines the max compressed script size from packet body capacity, static body size, dynamic header size, and padding.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_MSG_HOUSING_SCRIPT_OVERFLOW()` sends message id `1401399`.

## C# Changes

- Added `SmSystemMessage.HousingScriptOverflow()` with Java message id `1401399`.
- Changed the live `CmHouseScript` branch in `GameServerConnection` to send the overflow message when the parsed compressed size exceeds `CmHouseScript.MaxCompressedScriptSize`.
- Left normal script mutation, persistence, and broadcast deferred because C# does not yet model `PlayerScripts` or house script storage/fanout.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_OversizedScriptSendsJavaOverflowSystemMessage` | Unit / live dispatch regression | `CM_HOUSE_SCRIPT.runImpl -> STR_MSG_HOUSING_SCRIPT_OVERFLOW` | Oversized `CM_HOUSE_SCRIPT` through `GameServerConnection.ProcessPacketAsync` sends `SmSystemMessage` id `1401399`. | Exercises the live packet dispatch path with an active player and Java-derived max-size guard. | Does not validate normal script save/delete/broadcast behavior. |

## Validation Decision

```text
- Changed surface: live client-packet dispatch and server system-message factory.
- Specific behavior/contract: Java CM_HOUSE_SCRIPT oversized compressed payload sends STR_MSG_HOUSING_SCRIPT_OVERFLOW (1401399).
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for CM_HOUSE_SCRIPT in this checkout, and Java source/message id were reviewed directly.
- Broad-validation trigger: live packet dispatch branch changed.
- Broad .NET decision: skipped after focused validation because the filtered test compiled the changed packet branch and exercised the live dispatch path.
- Why this scope is sufficient: the UOW only adds one deterministic overflow response branch; the test parses and dispatches the packet through GameServerConnection.
```

Results:

- Focused C# validation passed: 4/4 tests.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_SCRIPT` overflow branch | `Aion.GameServer.Network.Aion.GameServerConnection` `CmHouseScript` branch | Client packet handler | Partial | Unit Tested | Partial Parity | Oversized-script response is live. Normal ownership validation, `PlayerScripts` mutation, persistence, and `SM_HOUSE_SCRIPTS` broadcast remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE` | `Aion.GameServer.Network.Aion.ClientPackets.CmHouseScript.MaxCompressedScriptSize` | Packet constant/parser | Partial | Unit Tested | Partial Parity | Existing parser tests cover the max-size read guard; this UOW uses the guard in live dispatch. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_SCRIPT_OVERFLOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.HousingScriptOverflow` | Server packet factory | Complete | Unit Tested | Verified Parity | Java source reviewed; deterministic message id `1401399` is asserted through live dispatch. |

## Known Gaps

- C# still does not model Java `PlayerScripts`, `HouseScriptsDAO`, or `SM_HOUSE_SCRIPTS`.
- Normal house script add/remove behavior remains deferred.
- Active-house ownership audit behavior remains deferred.
- Real client validation was not run.

## Next Runtime UOW Candidates

1. Continue `CM_HOUSE_SCRIPT` only if adding live `PlayerScripts` storage/fanout can be kept small and backed by Java `HouseScriptsDAO`/`SM_HOUSE_SCRIPTS`.
2. Otherwise, find another live packet branch that can send an existing Java-equivalent server packet or mutate already-modeled state.
3. Avoid script readiness/planner scaffolding unless it immediately enables a live handler in the same UOW.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 1
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
