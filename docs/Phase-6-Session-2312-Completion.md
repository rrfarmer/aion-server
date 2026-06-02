# Phase 6 Session 2312 Completion - Portal Recruit Option Dialog

## Scope

Wired concrete runtime parity for portal/NPC dialog action `SELECT1_1` (`1012`) in the portal find-group flow.

Java source reviewed:

- `game-server/data/handlers/ai/portals/PortalDialogAI.java`
- `game-server/src/com/aionemu/gameserver/model/DialogAction.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/dataholders/AutoGroupData.java`

Java behavior used:

- `PortalDialogAI.onDialogSelect` handles `SELECT1_1`.
- If `!player.isInTeam()` and `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(getNpcId()) != null`, Java sends `new SM_DIALOG_WINDOW(getObjectId(), 1182)` and returns `true`.
- If the player is already in a team or the portal NPC has no recruitable masks, this branch does not send that dialog packet and falls through to the portal path branch when applicable.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Added `CmDialogSelect.Select1_1 = 1012`.
- Added `ShouldShowOpenInstanceRecruitDialog` on the find-group connection composition service.
- Routed live `CM_DIALOG_SELECT` action `1012` before portal-entry routing when Java's AI condition is true.
- Sends `SmDialogWindow(targetObjectId, 1182)` only for a solo player at a portal NPC with recruitable masks.
- Moved action `105` handling before portal-entry routing so both Java AI-handled portal find-group actions are evaluated before `PortalService.port`-style routing.

Added test:

- `ProcessPacketAsync_SelectOneOneShowsOpenInstanceRecruitDialog`

The test drives live `CM_DIALOG_SELECT` and verifies:

- solo player plus portal masks sends `SmDialogWindow` page `1182`,
- grouped player sends nothing,
- solo player at a portal NPC without masks sends nothing.

## Validation Decision

- Changed surface: live C# dialog dispatch plus a narrow find-group portal condition helper.
- Specific behavior/contract: Java `PortalDialogAI.SELECT1_1` shows the open-instance-recruit dialog option only for solo players at portal NPCs with recruitable masks.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_SelectOneOneShowsOpenInstanceRecruitDialog" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the edited C# project and tests.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_OpenInstanceRecruitSendsPortalMaskListOnly" --no-restore
```

Result: passed 1, failed 0, skipped 0.

- Focused Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped. This is find-group source-tree sanity; the `SELECT1_1` branch itself was verified by Java source review.

- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. The change is a narrow live dialog branch and does not change packet primitives, crypto, persistence, scheduler, or broad shared infrastructure.
- Broad .NET decision: skipped full project/solution validation. Focused boundary tests directly covered the scoped behavior and supplied the compile signal.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.portals.PortalDialogAI.onDialogSelect` action `SELECT1_1` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends `SmDialogWindow(targetObjectId, 1182)` for solo players at portal NPCs with recruitable masks. Portal path fall-through beyond this condition remains dependent on existing portal-entry routing. |
| `com.aionemu.gameserver.model.DialogAction.SELECT1_1` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.Select1_1` | Constant | Complete | Boundary Tested | Verified Parity | Java value `1012` matched and exercised through parsed `CM_DIALOG_SELECT`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW` page `1182` use | `Aion.GameServer.Network.Aion.ServerPackets.SmDialogWindow` | Server Packet | Partial | Boundary Tested | Partial Parity | Existing packet class is used for the Java page id and target object id. This UOW did not change or re-golden the packet byte shape. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_SelectOneOneShowsOpenInstanceRecruitDialog` | Boundary Runtime | Java source review of `PortalDialogAI.SELECT1_1` | Live C# `CM_DIALOG_SELECT` action `1012` sends `SmDialogWindow` page `1182` only when Java's solo-player and portal-mask conditions are true. | Focused C# boundary execution plus Java source review and targeted find-group Maven sanity. | Does not prove encrypted socket bytes, real-client dialog flow, or portal path fall-through behavior. |

## Remaining Gaps

- No verified parity claim for full `PortalDialogAI`.
- `PortalDialogAI.INSTANCE_PARTY_MATCH` action `77` remains open.
- Portal path fall-through for `SELECT1_1` when the recruit-option condition is false remains dependent on existing portal-entry routing and was not specifically covered here.
- No encrypted socket or real-client frame comparison.
- Full group/alliance invite response lifecycle parity remains open.

## Commit

Commit message:

```text
[Phase 6][UOW-2312] Wire portal recruit option dialog
```

## Next Recommended UOW

Continue concrete runtime parity with `PortalDialogAI.INSTANCE_PARTY_MATCH` (`77`) if C# has enough auto-group packet/service surface.

Java behavior to inspect:

- `PortalDialogAI.onDialogSelect` action `INSTANCE_PARTY_MATCH`
- `AutoGroupType.getAutoGroup(player.getLevel(), getNpcId())`
- `SM_AUTO_GROUP`
- `SM_DIALOG_WINDOW(getObjectId(), 0)`

Expected behavior:

- If an auto group type exists for the player's level and portal NPC, Java sends `SM_AUTO_GROUP(maskId)`.
- Java then sends `SM_DIALOG_WINDOW(getObjectId(), 0)` regardless of whether an auto group type was found.
