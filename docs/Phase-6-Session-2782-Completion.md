# Phase 6 Session 2782 Completion

## Unit of Work

[Phase 6][UOW-2782] Wire legion bonus icon activation

## Runtime Progress Gate

- Deferred/live behavior advanced: C# now evaluates Java's online legion-member bonus threshold after a live legion invite acceptance.
- Java source of truth: `Legion.addBonus`, `LegionService.addLegionMember`, and `SM_ICON_INFO.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.AcceptLegionInviteAsync`, `LegionBonusRuntime`, `GameServerRuntimeContext.LegionBonuses`, and `SmIconInfo`.
- Client-visible/state/persistence effect changed: when an accepted invite brings an online legion to ten members, live code records the legion bonus as active and sends `SM_ICON_INFO(1, true)` to the online legion members.
- Why this is not preview-only/test-only/documentation-only: it mutates runtime legion bonus state and emits a real server packet from the live invite-acceptance path.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ICON_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Rates.java`

## C# Runtime Changes

- Added `SmIconInfo` with opcode 175 and Java payload shape `D(0), D(buffId), C(display)`.
- Added `LegionBonusRuntime` to track active legion bonus state by legion id with Java's ten-online-member activation threshold.
- Exposed the runtime state through the singleton `GameServerRuntimeContext`.
- Wired invite acceptance to collect online same-legion players, activate the bonus once, and send icon-on packets to those players.

## Validation Decision

- Changed surface: live legion invite acceptance, runtime legion bonus state, and a new client-visible server packet.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GamePacket" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java fixture exists for `SM_ICON_INFO` or `Legion.addBonus`.
- Broad-validation trigger: live packet and runtime state mutation.
- Broad .NET decision: skipped. The focused command compiles the affected project and exercises the new packet serialization plus the live invite-accept threshold path.

## Validation Result

- Focused C# result: Passed, 381 total, 0 failed, 0 skipped.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmIconInfo_WritesJavaConditionalBonusShape` | Unit | `SM_ICON_INFO.writeImpl` and `ServerPacketsOpcodes` | Opcode 175 and payload `D(0), D(1), C(1)`. | Packet byte assertions against reviewed Java field order. | No Java golden packet fixture. |
| `HandleQuestionResponseAsync_LegionInviteAcceptActivatesOnlineBonusAtJavaThreshold` | Unit | `LegionService.addLegionMember` and `Legion.addBonus` | Live invite acceptance activates bonus state at ten online members and sends icon-on packets to online legion members. | Live handler side-effect assertions and runtime-state assertion. | Login/logout/removal bonus re-evaluation remains open. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `SM_ICON_INFO` | `SmIconInfo` | Server Packet | Ported | Unit Tested | Partial Parity | Packet shape and opcode are covered; no Java golden frame fixture. |
| `Legion.addBonus` | `GameServerConnection.AddLegionBonusIfEligibleAsync` plus `LegionBonusRuntime` | Runtime State/Packet Fanout | Partial | Unit Tested | Partial Parity | Invite-accept activation is wired; login sync, logout/removal deactivation, and XP-rate consumption remain incomplete. |
| `Legion.hasBonus` / `Rates.calcXpRate` | `LegionBonusRuntime.IsActive` | Runtime State | Partial | Unit Tested | Partial Parity | Runtime state exists but is not yet consumed by reward-rate live code. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 4 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked/not-started artifacts: 3 legion bonus runtime paths remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java `Legion.removeBonus()` is not yet wired for logout, leave, kick, or other member-count drops.
- Java `LegionService.onLogin` icon sync is not yet wired.
- `Rates.calcXpRate` XP bonus consumption is not yet connected to the C# runtime bonus state.
- No Java golden or runtime comparison fixture was generated for `SM_ICON_INFO`.
