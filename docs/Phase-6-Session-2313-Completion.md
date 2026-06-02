# Phase 6 Session 2313 Completion - Portal Instance Party Match Dialog

## Scope

Wired concrete runtime parity for portal/NPC dialog action `INSTANCE_PARTY_MATCH` (`77`).

Java source reviewed:

- `game-server/data/handlers/ai/portals/PortalDialogAI.java`
- `game-server/src/com/aionemu/gameserver/model/DialogAction.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroup.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java`

Java behavior used:

- `PortalDialogAI.onDialogSelect` handles `INSTANCE_PARTY_MATCH`.
- Java calls `AutoGroupType.getAutoGroup(player.getLevel(), getNpcId())`.
- `AutoGroupType.getAutoGroup` iterates enum values in declaration order and returns the first template whose level range contains the player and whose NPC id list contains the portal NPC id.
- If an auto group is found, Java sends `new SM_AUTO_GROUP(maskId)`.
- Java then always sends `new SM_DIALOG_WINDOW(getObjectId(), 0)` and returns `true`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Added:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`

Implemented:

- Added `CmDialogSelect.InstancePartyMatch = 77`.
- Added Java enum-order `AutoGroupTable.GetAutoGroupForNpc(playerLevel, portalNpcId)`.
- Added `SmAutoGroup` packet for Java `SM_AUTO_GROUP` window `0` payload shape.
- Routed live `CM_DIALOG_SELECT` action `77` before portal-entry routing.
- Sends `SmAutoGroup` when the Java-equivalent auto-group lookup finds a match, then sends `SmDialogWindow(targetObjectId, 0)` in all action `77` cases.

Added tests:

- `SmAutoGroup_WritesJavaWindowZeroPayload`
- `ProcessPacketAsync_InstancePartyMatchSendsAutoGroupThenDialogClose`

## Validation Decision

- Changed surface: live C# dialog dispatch plus new server packet and static-data lookup helper.
- Specific behavior/contract: Java `PortalDialogAI.INSTANCE_PARTY_MATCH` sends optional `SM_AUTO_GROUP(maskId)` followed by mandatory `SM_DIALOG_WINDOW(target, 0)`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_InstancePartyMatchSendsAutoGroupThenDialogClose" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the edited C# project and tests.

- Adjacent packet command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmAutoGroup_WritesJavaWindowZeroPayload" --no-restore
```

Result: passed 1, failed 0, skipped 0.

- Focused Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped. This is find-group source-tree sanity, not a portal `INSTANCE_PARTY_MATCH` fixture; the portal branch was verified by Java source review.

- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. The change is a narrow live dialog branch plus a new packet class; focused boundary and packet-shape tests cover the scoped risk.
- Broad .NET decision: skipped full project/solution validation. Focused tests supplied the compile signal and directly covered the Java-derived behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.portals.PortalDialogAI.onDialogSelect` action `INSTANCE_PARTY_MATCH` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends optional `SmAutoGroup` followed by `SmDialogWindow(target, 0)` for action `77`. Full `PortalDialogAI` still has portal path and broader AI behavior outside this UOW. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType.getAutoGroup(int, int)` | `Aion.GameServer.Dataholders.AutoGroupTable.GetAutoGroupForNpc` | Enum Lookup / Data Holder | Partial | Boundary Tested | Partial Parity | C# models the Java enum mask order needed by portal action `77`. Other `AutoGroupType` methods and auto-instance creation remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` window `0` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Server Packet | Partial | Packet Shape Tested | Partial Parity | Window `0` payload shape is covered. Other Java constructors/windows are present in C# but not exhaustively tested in this UOW. |
| `com.aionemu.gameserver.model.DialogAction.INSTANCE_PARTY_MATCH` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.InstancePartyMatch` | Constant | Complete | Boundary Tested | Verified Parity | Java value `77` matched and exercised through parsed `CM_DIALOG_SELECT`. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_InstancePartyMatchSendsAutoGroupThenDialogClose` | Boundary Runtime | Java source review of `PortalDialogAI.INSTANCE_PARTY_MATCH`, `AutoGroupType.getAutoGroup`, and `SM_AUTO_GROUP` | Live C# `CM_DIALOG_SELECT` action `77` sends `SmAutoGroup` then `SmDialogWindow(target, 0)` when the lookup matches, and sends only the dialog close when it does not. | Focused C# boundary execution plus Java source review. | Does not prove encrypted socket bytes or full auto-group service behavior. |
| `SmAutoGroup_WritesJavaWindowZeroPayload` | Packet Shape | Java source review of `SM_AUTO_GROUP.writeImpl` | C# packet window `0` writes mask id, window id, map id, message id, title id, zero, trailing zero, and empty UTF-16 string in Java order. | Focused C# packet payload test. | Other windows/constructors are not exhaustively verified. |

## Remaining Gaps

- No verified parity claim for full `PortalDialogAI`.
- Portal path fall-through behavior remains dependent on existing portal-entry routing.
- `SmAutoGroup` windows other than `0` need targeted coverage before use.
- Full `AutoGroupType` runtime service behavior and auto-instance creation remain unported.
- Full group/alliance invite response lifecycle parity remains open.

## Commit

Commit message:

```text
[Phase 6][UOW-2313] Wire portal instance party match
```

## Next Recommended UOW

Move away from portal find-group branches unless a small portal path fall-through test is needed. The next concrete runtime candidate is group/alliance invite response lifecycle after find-group action `12`, because previous UOWs wired application-result dispatch but the full response lifecycle remains open.
