# Phase 6 Session 2768 Completion

## Unit of Work

[Phase 6][UOW-2768] Broadcast legion permission changes

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_LEGION` exOpcode `0x0D` permission edits now propagate permission state and broadcast `SM_LEGION_EDIT(0x02)` to online same-legion players instead of only updating the requester.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x0D`, `LegionService.changePermissions`, and `SM_LEGION_EDIT(0x02, legion)`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionPermissionChangeAsync`, online same-legion permission state propagation, same-legion online fanout, and `SmLegionEdit.Permissions`.
- Client-visible/state effect changed: valid permission edits mutate the active player's four permission fields, mutate online same-legion players' four permission fields, send the edit packet to requester and bystanders, and exclude outsiders.
- Why this is not preview-only/test-only/documentation-only: it mutates live legion/player permission state and sends real server packets from a live client handler.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`

## C# Runtime Changes

- Added `BroadcastLegionPermissionUpdateAsync` for Java-style same-legion fanout.
- Propagated deputy, centurion, legionary, and volunteer permission values to online same-legion `Player` instances.
- Preserved the existing `SmLegionEdit.Permissions` packet serializer and Java field order.
- Kept non-brigade-general rejection side-effect free, with no bystander mutation or broadcast.

## Validation Decision

- Changed surface: live client handler, online player permission state, and same-legion packet fanout.
- Specific behavior/contract: Java `LegionService.changePermissions` checks brigade-general rights, sets the legion permissions, and broadcasts `SM_LEGION_EDIT(0x02)` in deputy/centurion/legionary/volunteer order.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.changePermissions` or `SM_LEGION_EDIT`.
- Broad-validation trigger: live state mutation and connection fanout were touched.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, permission state propagation, fanout filtering, and packet serialization contract. No shared packet primitive, crypto, scheduler, schema, or persistence abstraction changed.
- Why this scope is sufficient: the change is isolated to the existing `CM_LEGION 0x0D` handler and existing `SM_LEGION_EDIT` packet serializer.

## Validation Result

- First focused C# run: failed to compile due to a syntax error in the edited handler; fixed before final validation.
- Final focused C# result: Passed, 82 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_EditPermissionsWithoutBrigadeGeneralSendsNoRightLikeJava` | Unit | `LegionService.changePermissions` brigade-general guard | Non-brigade-general edit sends `STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT`, preserves requester and bystander permission state, and emits no bystander packet. | Live handler side-effect assertion. | Uses test registry, not a real client. |
| `HandleInfrastructurePacketAsync_EditPermissionsMutatesRuntimeStateAndBroadcastsLikeJava` | Unit | `LegionService.changePermissions` valid branch | Valid edit mutates active and same-legion bystander permission fields, sends `SM_LEGION_EDIT(0x02)` to requester and bystander, and excludes outsider. | Live handler, state, and packet payload assertions. | C# approximates Java's shared `Legion` aggregate with online `Player` fields. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x0D` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection.HandleLegionPermissionChangeAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, brigade-general rejection, active mutation, same-legion online propagation, requester packet, and bystander fanout are covered. No real-client verification. |
| `com.aionemu.gameserver.services.LegionService.changePermissions` | `GameServerConnection.HandleLegionPermissionChangeAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Ports permission mutation and broadcast update. Java shared `Legion` permission state is approximated by active and online same-legion `Player` permission fields. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x02` | `SmLegionEdit.Permissions` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Packet writes edit type and four permission shorts in Java order. No Java golden fixture. |

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported or advanced in this UOW: 3 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked artifacts: 1 (no Java golden packet fixture for `SM_LEGION_EDIT`)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- No Java golden packet fixture was generated for `SM_LEGION_EDIT`.
- Full Java `Legion` aggregate permission state is still approximated by active and online same-legion `Player` permission fields.
- No real-client validation was performed for the permission edit broadcast.
