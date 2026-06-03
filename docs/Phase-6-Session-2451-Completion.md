# Phase 6 Session 2451 Completion

## UOW

[Phase 6] UOW-2451: Add focused Vortex removal packet dispatch executor

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderRemovalPacketDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexInvaderRemovalPacketDispatchServiceTests.cs`

## Implementation Notes

- Added `VortexInvaderRemovalPacketDispatchService`, an opt-in socket-boundary executor for `VortexInvaderRemovalResult.SystemMessages`.
- The executor is disabled by default and records intended sends without calling `IGameClientConnectionRegistry`, matching existing C# socket-boundary patterns.
- When explicitly enabled with a registry, it sends each `SmSystemMessage` to `VortexInvaderRemovalResult.PlayerObjectId` in Java order.
- It records missing registry, missing connection, sent, disabled, and failure-stop outcomes.
- Java `Invasion.kickPlayer` sends Vortex messages sequentially through `PacketSendUtility.sendPacket`; the C# executor stops after the first send exception before attempting later messages.
- Scope is intentionally limited to the packet-send executor. It does not wire live Vortex removal dispatch into `GameServerConnection`, zone leave handlers, or the offline timeout scheduler.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DispatchAsync_DisabledExecutorRecordsMessagesWithoutCallingRegistry` | Unit | `PacketSendUtility.sendPacket` socket boundary | Disabled executor records `1401452`, `1401474` without live sends | C# boundary test against Java-reviewed message order | Not live production wiring |
| `DispatchAsync_EnabledSendsRemovalMessagesToPlayerInJavaOrder` | Unit | `Invasion.kickPlayer` | Enabled executor sends both Vortex removal messages to the removed player in Java order | Registry capture proves recipient/order/message IDs | Uses fake registry, not real socket |
| `DispatchAsync_EnabledStopsAfterFirstSendExceptionLikeSequentialJavaCalls` | Unit | Sequential Java calls in `Invasion.kickPlayer` | First send exception stops later message attempts | Captured registry order and result status | Java client connection exception behavior is not separately fixture-tested |
| `DispatchAsync_MissingRegistryRecordsUnsentMessages` | Unit | `PacketSendUtility.sendPacket` boundary | Enabled executor without registry records unsent message metadata | C# boundary guard test | Not applicable to production DI until wired |
| `DispatchAsync_OnlineRemovalWithoutMessagesDoesNotCallRegistry` | Unit | `PacketSendUtility.sendPacket` only executes when packets exist | Online removal with no packet intents does not send | C# guard test | This state should be rare after UOW-2450 |
| `DispatchAsync_OfflineOrUnremovedResultDoesNotCallRegistry` | Unit | Java `PacketSendUtility.sendPacket` online gate and `Invasion.kickPlayer` removal guard | Offline/unremoved results do not send packets | C# guard test | Offline Vortex timeout packet behavior remains silent |

## Validation Decision

- Changed surface: opt-in packet dispatch executor and tests for Vortex invader removal system-message intents.
- Specific behavior/contract: Java `Invasion.kickPlayer` uses `PacketSendUtility.sendPacket` to send Vortex removal system messages to the removed online player in method order.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexInvaderRemovalPacketDispatchServiceTests|FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

- Result: Passed, 291 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and the Java behavior was directly reviewed in `Invasion.kickPlayer` and `PacketSendUtility.sendPacket`.
- Broad-validation trigger: none. The executor is not registered or invoked by production live Vortex paths in this UOW and remains disabled by default unless explicitly constructed with `enabled: true`.
- Broad .NET decision: skipped; focused validation built the affected project and covered the new executor plus adjacent Vortex runtime, packet, and alliance-timeout surfaces.
- Why this scope is sufficient: no packet primitives, persistence mapping, database schema, crypto, scheduler, or live handler wiring changed.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvaderRemovalPacketDispatchService` plus `VortexInvasionRuntime.RemoveInvaderPlayer` | Vortex packet dispatch | Partial | Unit Tested | Partial Parity | Invader removal message intents can now be sent through an opt-in C# executor in Java order. Live Vortex handler wiring, defender message `1401476`, alliance removal/disband, and sync packet fanout remain incomplete. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync` via `VortexInvaderRemovalPacketDispatchService` | Socket boundary | Partial | Unit Tested | Partial Parity | Direct send boundary is represented for Vortex removal system messages. The executor records disabled/missing-connection/failure outcomes; no real client socket validation was run. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Vortex packet dispatch is still not wired into production zone leave, stop-invasion, or online removal call paths.
- Defender kick message `1401476`, defender prompt/alliance path, and Java alliance removal/disband behavior remain unported.
- Full Java teleport packet fanout after home teleport is still outside the current `PlayerTeleportService.TeleportToWorldPosition` slice.
- No Java-generated packet golden was added for the Vortex system messages; current evidence is Java source review plus C# packet tests.
