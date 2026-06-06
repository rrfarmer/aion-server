# Phase 6 Session 2685 Completion

## UOW

[Phase 6] UOW-2685: Use Passport reward L10n for invalid-level message.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: Atreian Passport invalid-level reward claims now send the Java-equivalent reward item L10n/client string in system message `1402573`.
- Java source/runtime path: AtreianPassportService.takeReward -> rewardPermitLevel guard -> itemTemplate.getL10n() -> SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_INVALID_LEVEL(minLevel, itemName).
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync invalid-level branch and SmSystemMessage parameter emission.
- Client-visible/state/persistence effect: a low-level player claiming a level-gated Passport reward now receives message `1402573` with the encoded client item name instead of the raw XML template name; reward state and persistence remain unchanged.
- Why this is runtime progress: it changes a real server packet emitted from the live Passport claim handler and is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - Java initializes `itemName` to `""`.
  - If the reward item template exists and `itemTemplate.getL10n()` is not null, Java sends that L10n string in `STR_MSG_ATTEND_REWARD_INVALID_LEVEL`.
- `game-server/src/com/aionemu/gameserver/model/templates/L10n.java`
  - `getL10n()` delegates to `ChatUtil.l10n(getL10nId())`.
- `game-server/data/static_data/events/login_events.xml`
  - Passport `43` is a level-gated active cumulative reward with `reward_permit_level="46"`.

## C# Changes

- Changed the Passport invalid-level branch in `GameServerConnection.HandleAtreianPassportAsync` to send `rewardTemplate.GetClientName() ?? string.Empty`.
- Removed the previous raw `rewardTemplate.Name` parameter for message `1402573`.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportInvalidLevelUsesRewardItemClientName` | Unit / live connection and packet serialization | `AtreianPassportService.takeReward` invalid-level branch | Low-level claim of Passport `43` sends message `1402573` with permit level `46` and the reward item's encoded client/L10n name, leaves the Passport unrewarded, performs no reward/delete/login persistence, and sends the post-login snapshot. | Socket-backed connection invocation with runtime-loaded Java XML and item templates, packet parameter assertions, repository counters, and live Passport state. | Does not prove every level-gated Passport template or null-L10n fallback. |

## Validation Decision

```text
- Changed surface: live Passport claim handler packet parameter plus focused connection test.
- Specific behavior/contract: Java sends itemTemplate.getL10n(), or an empty string when unavailable, for STR_MSG_ATTEND_REWARD_INVALID_LEVEL.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source and XML were reviewed unchanged, and no narrow Java behavioral fixture exists for this packet parameter in this checkout.
- Broad-validation trigger: none. The change is isolated to one live Passport claim branch and its existing connection test class.
- Broad .NET decision: skipped; the filtered test built the affected project and exercised the edited live handler plus adjacent Passport claim/login cases.
- Why this scope is sufficient: the focused connection test observes the exact system-message id and parameter list, repository counters, Passport state, and serialized snapshot affected by this branch.
```

Results:

- Focused C# validation passed: 17/17.
- Existing unrelated nullable/analyzer warnings were emitted by the C# projects.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.takeReward` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAtreianPassportAsync` | Service / live packet handler | Partial | Unit Tested | Partial Parity | Invalid-level branch now sends reward item client/L10n name. Complete claim parity is not claimed. |
| `SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_INVALID_LEVEL` | `SmSystemMessage` id `1402573` emission from `HandleAtreianPassportAsync` | Server packet | Partial | Unit Tested | Partial Parity | Covered branch sends permit level and encoded reward item client name for Passport `43`; null-L10n fallback remains untested. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate.getL10n` | `ItemTemplateSummary.GetClientName` | Data/model helper | Partial | Unit Tested indirectly | Partial Parity | Existing helper supplies encoded client name used by the live Passport invalid-level packet. |

## Known Gaps

- Full Atreian Passport parity is not claimed.
- Null/missing reward item L10n fallback for invalid-level claims is not covered by a runtime test.
- C# still iterates `player.Passports` rows while Java iterates request map entries and timestamp sets; exact ordering for mixed valid/invalid/deleted rows remains partial.
- Java audit logging for missing/already rewarded claims remains absent and is not enough for a standalone runtime UOW.
- Live MySQL evidence was not run for Passport reward/login/delete persistence.
- Exact Java timezone behavior for claim expiry and login purge remains not runtime-compared.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect and, only if a concrete gap is confirmed, port request-map iteration ordering for mixed valid/invalid Passport claim rows where C# player-row iteration changes packet or persistence order.
2. Inspect Passport claim expiry clock behavior against Java and port any concrete live state/persistence difference found beyond the already fixed guard ordering.
3. Move to the next deferred enter-world, inventory, item-use, quest, AI, zone, instance, command, or dynamic handler runtime gap if Passport discovery no longer yields a small state/packet UOW.
