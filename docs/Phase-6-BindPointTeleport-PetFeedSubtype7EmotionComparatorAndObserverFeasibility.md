# Phase 6 - Pet Feed Subtype 7 Emotion Comparator and Observer Feasibility

Date: May 27, 2026
Unit of Work: UOW-1335

## Scope

This unit extends the guarded pet feed subtype `7` artifact reader so future Java artifacts can compare the end-feeding `SM_EMOTION` packet bytes, and records read-only Java send-timing findings for the next Java observer unit.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION`
- `com.aionemu.gameserver.utils.PacketSendUtility`
- `com.aionemu.gameserver.network.aion.AionConnection`
- `com.aionemu.gameserver.network.aion.AionServerPacket`

## Implemented

- Extended `PetFeedSubtype7JavaVectorArtifactReaderTests` to validate future `SM_EMOTION` end-feeding artifact fields:
  - opcode `37`
  - `EmotionType.EndFeeding` id `51`
  - serialized player state
  - serialized speed
  - semantic target object id for constructor context
- Added C# comparison support for generated `SM_EMOTION` body/canonical payload hex through `SmEmotion`.
- Kept subtype `7` `SM_PET` comparison behavior unchanged.
- Corrected the inline schema sample from placeholder `SM_EMOTION` opcode/type values to Java/C# constants.

## Read-Only Observer Feasibility Findings

A read-only sub-agent inspected Java packet send timing and made no file changes.

Key findings:

- `PacketSendUtility.sendPacket(Player, AionServerPacket)` only checks `player.isOnline()` and delegates to `player.getClientConnection().sendPacket(packet)`.
- The inherited connection send queues the packet object; it does not serialize bytes immediately.
- Production serialization occurs later in `AionConnection.writeData`, where the queued packet is removed and `packet.write(this, data)` is invoked.
- `AionServerPacket.write` writes the placeholder length, encoded opcode/static/checksum bytes, calls `writeImpl(con)`, flips the buffer, stamps length, slices past the length field, and then calls `con.encrypt(...)`.
- For subtype `7`, Java queues `new SM_PET(7, 0, 0, pet)` before reward item add, refeed scheduling, `setRefeedTime`, DAO persistence, and `progress.reset`, but subtype `7` reads `PetCommonData` values during serialization.

Observer design implication:

- Hooking only `PacketSendUtility` is too early to resolve subtype `7` mutable-state timing.
- A future Java observer should hook around `AionServerPacket.write` or immediately after `packet.write` in `AionConnection.writeData`.
- The safest parity artifact is clear serialized bytes after `writeImpl` and length stamping but before encryption. Optional wire bytes can be captured separately after encryption, but encryption mutates the buffer and depends on connection crypt state.

Likely future Java files if tooling work proceeds:

- `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`
- `game-server/src/com/aionemu/gameserver/network/aion/AionConnection.java`
- a small test-only observer interface/class under `game-server/src/com/aionemu/gameserver/network/aion`
- pet-feed test support around `PetService.checkFeeding`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader"` passed 2 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 141 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

## Migration Parity Table - UOW-1335

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` end-feeding packet | `PetFeedSubtype7JavaVectorArtifactReaderTests`; `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` | Packet / Artifact Comparator | Partial | Unit Tested source-derived | Needs Verification | Reader can now compare generated `SM_EMOTION` bytes when future Java artifacts provide body/canonical hex. No Java runtime artifact exists yet. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rewarded full branch | `PetFeedSubtype7JavaVectorArtifactReaderTests` | Service Flow / Artifact Schema | Partial | Unit Tested schema only | Needs Verification | Schema covers subtype `2`, subtype `6`, subtype `5`, end-feeding emotion, and subtype `7` order. Live Java packet serialization timing remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `docs/Phase-6-BindPointTeleport-PetFeedSubtype7EmotionComparatorAndObserverFeasibility.md` | Network Utility / Java Analysis | Not Started | Manual Only | Needs Verification | Source review confirms this hook is queue-time only and too early for subtype `7` byte parity. No Java hook was implemented. |
| `com.aionemu.gameserver.network.aion.AionConnection.writeData` | observer feasibility notes | Network Connection / Java Analysis | Not Started | Manual Only | Needs Verification | Source review identifies production send-time serialization location. Future observer would likely hook here or in `AionServerPacket.write`. |
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | observer feasibility notes | Packet Serialization / Java Analysis | Not Started | Manual Only | Needs Verification | Source review identifies clear-before-encrypt bytes as best parity artifact point. No Java runtime artifacts were generated. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ParsePetFeedSubtype7Artifact_ReadsSchemaV1PacketFields` | Unit / Artifact Schema | `SM_EMOTION.writeImpl`; UOW-1333 vector design | Now validates end-feeding `SM_EMOTION` opcode, type id, serialized state, and speed fields in addition to subtype `7` sequence semantics. | Deterministic schema validation and C# serializer construction path. | Does not compare generated Java bytes because artifacts are absent. |
| `FindPetFeedSubtype7JavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Guarded Unit / Artifact Discovery | UOW-1333 vector design; `SM_EMOTION.writeImpl` | Will compare future Java `SM_PET` and end-feeding `SM_EMOTION` body/canonical payload hex when artifacts exist. | Guarded comparison path exists. | No artifacts are present, so no runtime parity is claimed. |

## Remaining Risks

- No Java runtime subtype `7` vector artifacts exist yet.
- `SM_EMOTION` comparison is source-derived until Java body/canonical hex artifacts are generated.
- Java send-time serialization happens after `PacketSendUtility.sendPacket`, so future observer work must avoid queue-time-only captures.
- Clear-before-encrypt capture requires careful buffer copying around `AionServerPacket.write`.
- Wire-byte capture is crypt-state dependent and should not be used as the only parity artifact.
- Live C# scheduler, reward creation, DAO persistence, inventory mutation, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 guarded comparator extension in an existing test class
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime packet observer, generated subtype `7` artifacts, deterministic feed fixture, live common-data wiring, scheduler, reward creation, DAO persistence, inventory mutation, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Create a docs-only Java subtype `7` observer implementation plan that turns the feasibility findings into concrete artifact schema phases and a minimal test-only hook design. Keep Java production behavior unchanged until the hook can be isolated and reviewed.
