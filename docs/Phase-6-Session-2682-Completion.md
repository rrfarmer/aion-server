# Phase 6 Session 2682 Completion

## UOW

[Phase 6] UOW-2682: Route Passport claims through Java-style login post-processing.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: Atreian Passport claim completion now invokes the same login update path Java calls after takeReward, instead of always sending a direct snapshot from the claim handler.
- Java source/runtime path: AtreianPassportService.takeReward -> optional reward/delete mutations -> AccountPassportsDAO.storePassportList -> onLogin(player) -> purgeExpiredPassports/checkOnlineDate/login row creation/checkPassportLimit/storePassport/send attend reward/sendPassport.
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync now calls PlayerEnterWorldService.ApplyAtreianPassportLoginForActivePlayerAsync; PlayerEnterWorldService exposes the existing Passport login mutation/snapshot helper for active-player claim processing.
- Client-visible/state/persistence effect: a live claim request can now append active Passport rows, increment Passport stamps, persist the login mutation, send the attendance reward system message, remove excess Passport rows, and emit the post-login Passport snapshot in Java-equivalent order.
- Why this is runtime progress: it changes live claim-path state, persistence, and server packet behavior; it is not a preview, metadata, documentation, adapter, readiness, or test-only UOW.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` performs claim-time reward/delete work and then calls `onLogin(player)`.
  - `onLogin(player)` purges expired Passport rows, creates active daily/cumulative/anniversary rows, increments stamps when `checkOnlineDate` passes, persists the login mutation, emits attendance/excess-removal messages, and sends `SM_ATREIAN_PASSPORT`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT.java`
  - The live client packet dispatches parsed Passport ids/timestamps into `AtreianPassportService.takeReward`.

## C# Changes

- Added `PlayerEnterWorldService.ApplyAtreianPassportLoginForActivePlayerAsync`, a small public wrapper around the existing Java-parity Passport login implementation.
- Changed `GameServerConnection.HandleAtreianPassportAsync` so claim processing calls the Passport login helper when the service is available.
- The claim path now sends excess-removal messages, attendance reward message `1402601`, and the Passport snapshot from the login result; a null login result returns without a fallback snapshot, matching disabled/inactive Java service behavior.
- Kept the service-less branch as a legacy snapshot fallback for connection tests that do not construct the runtime service.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportClaimsMatchingRestoredPassport` | Unit / live connection and packet serialization | `AtreianPassportService.takeReward -> onLogin` | A successful claim grants the reward, marks the Passport rewarded, then applies login post-processing that increments stamps, persists the login mutation, sends attendance reward message `1402601`, and sends the updated snapshot. | Socket-backed connection invocation with runtime-loaded Java XML, repository counters, live inventory/passport state, and serialized packet assertions. | Does not prove all Passport template combinations or live MySQL behavior. |
| `HandleInfrastructurePacketAsync_AtreianPassportFullInventoryBlocksStackMergeClaim` | Unit / live connection and packet serialization | `takeReward` full-inventory branch followed by `onLogin` | Full inventory still blocks reward mutation, while same-day post-claim login emits a Passport snapshot that includes Java-style active/fake rows. | Runtime-loaded Java XML and packet/state assertions against the post-login Passport collection. | Focused on same-day no-stamp login behavior. |
| `HandleInfrastructurePacketAsync_AtreianPassportDeletesExpiredRewardClaim` | Unit / live connection and packet serialization | `takeReward` expired reward deletion followed by `onLogin` | Expired claim deletes the requested Passport and then emits a post-login snapshot without the deleted row. | Repository deletion, live Passport collection, and serialized snapshot assertions. | Does not runtime-compare Java clock/timezone edge cases. |

## Validation Decision

```text
- Changed surface: live Passport claim handler plus shared Passport login helper exposure and existing connection tests.
- Specific behavior/contract: Java takeReward calls onLogin after claim processing, so C# claim completion must use the same login mutation/message/snapshot path.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed and unchanged, and no narrow Java behavioral fixture exists for AtreianPassportService.takeReward post-claim login behavior in this checkout.
- Broad-validation trigger: none. The change is isolated to one live packet handler and a shared helper already covered by enter-world Passport tests in the same filtered class.
- Broad .NET decision: skipped; the filtered test built the affected projects and exercised both claim and enter-world Passport runtime paths.
- Why this scope is sufficient: the focused connection tests observe live packet sends, repository call counters, inventory state, Passport state, stamp mutation, and serialized Passport snapshots for the changed claim behavior.
```

Results:

- Focused C# validation passed: 13/13.
- Existing unrelated nullable/analyzer warnings were emitted by the C# projects.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.takeReward` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAtreianPassportAsync` | Service / live packet handler | Partial | Unit Tested | Partial Parity | Claim completion now routes through Passport login post-processing. Complete claim parity is not claimed. |
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `PlayerEnterWorldService.ApplyAtreianPassportLoginForActivePlayerAsync` / `ApplyAtreianPassportLoginAsync` | Service | Partial | Unit Tested | Partial Parity | Existing login mutation helper is reused by both enter-world and claim paths. |
| `SM_SYSTEM_MESSAGE.STR_ATTEND_MSG_ATTEND_REWARD_GET` | `SmSystemMessage.AttendRewardGet` | Server packet | Ported | Unit Tested | Partial Parity | Message id `1402601` is now emitted from the live claim path when post-claim login stamps attendance. |

## Known Gaps

- Full Atreian Passport parity is not claimed.
- Java audit logging for non-existing and already rewarded Passport claim attempts remains absent and is not enough for a standalone Phase 6 runtime UOW.
- Live MySQL evidence was not run for Passport reward/login persistence.
- Exact Java timezone behavior for reward expiry remains not runtime-compared.
- The Passport implementation remains partial for edge-case template windows and broader multi-claim ordering beyond the focused covered cases.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect reward expiry timing in the live Passport claim path and port any Java-equivalent runtime difference around `Instant.now()`/`ServerTime.now()` and delete persistence.
2. Inspect claim handling for multiple requested Passport ids/timestamps and port any Java loop-order persistence/message gap that affects live state or packets.
3. Move to the next deferred enter-world, inventory, item-use, quest, AI, or handler runtime gap if Passport discovery no longer yields a small state/packet UOW.
