# Phase 6 Session 2674 Handoff

## Completed UOW

[Phase 6] UOW-2674: Gate disabled Atreian Passport service.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT now returns immediately once Java's Passport service expiration window has elapsed.
- Java source/runtime path: AtreianPassportService.isAtreianPassportDisabled -> calculatePassportExpireDate -> findLastRewardTime over DataManager.ATREIAN_PASSPORT_DATA DAILY/CUMULATIVE period_end values -> takeReward returns before reward processing or packet emission.
- C# runtime artifact wired: AtreianPassportTable expiry calculation and GameServerConnection.HandleAtreianPassportAsync.
- Client-visible/state/persistence effect: during disabled periods the live handler no longer grants inventory rewards, marks/restores/deletes passport rows, calls Passport persistence methods, or sends SM_ATREIAN_PASSPORT.
- Why this is runtime progress: it changes live packet-handler control flow and suppresses real state mutation, persistence, and server packet output according to Java runtime behavior.
```

## Commit

`[Phase 6][UOW-2674] Gate disabled passport service`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/AtreianPassportTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `docs/Phase-6-Session-2674-Completion.md`
- `docs/Phase-6-Session-2674-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result:

- Passed: 8
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.isAtreianPassportDisabled` or `AtreianPassportService.takeReward` in this checkout.

## Conservative Parity Status

- `CM_ATREIAN_PASSPORT` now honors the Java disabled-service early return.
- The disabled branch suppresses reward grant, expired-claim delete, rewarded update, and refreshed Passport packet send.
- `AtreianPassportTable` computes the service expiration from Java XML daily/cumulative reward `period_end` values.
- Complete Atreian Passport parity is not claimed. Login-time creation/stamp behavior, audit logging, live DB evidence, and exact Java clock comparison remain open.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AtreianPassportService.isAtreianPassportDisabled` | `AtreianPassportTable.IsDisabled` | Service/data gate | Partial | Unit Tested | Partial Parity | Computes Java's latest daily/cumulative reward end plus 14-day disable threshold from runtime static data. |
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Adds disabled early return to previous reward grant and expired-delete slices. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- `AtreianPassportService.onLogin` remains the largest Passport runtime gap: stamp checks, daily/cumulative/anniversary row creation, fake stamps, excess purge, stamp persistence, and login system messages.
- Java audit logging for invalid/missing/already rewarded/deleted claims is still absent.
- `UpdateAccountPassportRewardedAsync` and `DeleteAccountPassportAsync` have focused handler evidence but not live MySQL integration evidence.
- The disabled gate currently compares against a UTC runtime clock. If a shared game-time service or Java golden fixture becomes available, recheck exact `ServerTime.now()` parity.

## Next Recommended Runtime UOW

Recommended candidate: port the smallest live `AtreianPassportService.onLogin` slice for creating/persisting a newly arrived Passport row and stamp state.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: C# login currently restores existing Passport rows but does not create the Java daily/cumulative login Passport entries or persist the stamp/count changes.
- Java source/runtime path: AtreianPassportService.onLogin -> getArrivedPassports -> addPassport -> AccountPassportsDAO.storePassportList plus account stamp persistence and login packet/message emission.
- C# runtime artifact likely involved: PlayerEnterWorldService login pipeline, PlayerPassport state, IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository, SM_ATREIAN_PASSPORT or related login packet emission.
- Client-visible/state/persistence effect expected: eligible login creates a real account Passport row, mutates stamp/passport state, persists through the existing account_passports shape, and sends the Java-equivalent client update/message.
- Why this is not preview-only/test-only/documentation-only: it mutates live login player/account state, persists runtime state, and emits live server packets/messages.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture is added or discovered. Start by reading Java `AtreianPassportService.onLogin`, `Passport`, `PassportsList`, and `AccountPassportsDAO` before editing.

## Other Safe Runtime Candidates

- Wire Java audit logging for invalid/missing/already rewarded Passport claim attempts if the logging path is live.
- Add opt-in DB integration for Passport reward/delete persistence only when coupled to a runtime Passport behavior change in the same UOW.
- Add a Java/C# golden comparison for the Passport disabled expiration calculation only if it directly unblocks the `onLogin` runtime UOW.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only Passport followups unless the user explicitly asks for them or they directly unblock a same-session runtime change.
