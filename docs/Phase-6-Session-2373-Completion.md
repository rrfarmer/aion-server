# Phase 6 Session 2373 Completion - Cover Autogroup Multi-Member Success Fanout

## Scope

Added focused evidence for Java online-recipient filtering in successful autogroup registration fanout.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/world/World.java`

Java behavior used:

- `AutoGroupUtility.sendSuccessfulRegistration(...)` iterates every queued member object id.
- For each id, it calls `World.getInstance().getPlayer(objectId)`.
- It sends the successful-registration packet sequence only when the player is online.
- Packet order per online player remains: optional periodic close-icon window `6`, success system message, waiting window `1`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added a registry-backed live ingress test for group successful-registration fanout.
- The test creates a three-member group, marks two members online in a fake registry, and verifies:
  - no direct active-connection packets are sent when the registry path is used,
  - only online members receive packets,
  - each online member receives the Java packet order.
- Added small test fixture support for injecting `IGameClientConnectionRegistry` and `PlayerGroupRuntime` into the existing autogroup connection fixture.

Known limitations:

- This UOW is test-only; no production behavior changed.
- It covers successful-registration fanout only, not queue matching or instance creation.

## Validation Decision

- Changed surface: test-only fixture and live packet-ingress test.
- Specific behavior/contract: successful autogroup registration fanout should iterate group member ids, skip offline members, and preserve Java packet order for each online member.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore
```

Result: passed 18, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `AutoGroupUtility.sendSuccessfulRegistration`; Java source review identified the online-player filtering branch.
- Broad-validation trigger: none; this UOW changed tests only.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected project/dependencies and exercised the edited test class plus adjacent registration service behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendSuccessfulRegistration(...)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendAutoGroupSuccessfulRegistrationAsync(...)` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Multi-member online filtering and packet order are now covered through the registry path. Queue matching, instance creation, and broader autogroup lifecycle remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | Model | Partial | Unit Tested | Partial Parity | Member object-id list drives fanout order in the C# test. Full Java `LookingForParty` timing and comparison behavior remain partial/missing. |
| `com.aionemu.gameserver.world.World.getPlayer(int)` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync(...)` | Runtime Lookup/Dispatch | Partial | Unit Tested | Partial Parity | Registry online set models Java online-player filtering for this fanout branch. Broader world lookup parity remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupSuccessfulRegistrationFansOutOnlyToOnlineMembersLikeJava` | Unit | Java source review | Group registration fanout sends Java packet order to online members only and skips the offline member. | Focused C# ingress test using a registry-backed online set. | Does not compare against Java runtime output. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 0
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` remain partial/missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.

## Commit

Commit message:

```text
[Phase 6][UOW-2373] Cover autogroup multi-member fanout
```
