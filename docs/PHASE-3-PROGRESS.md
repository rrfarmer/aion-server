# Phase 3: Port Login Server - Progress Notes

**Date**: May 18, 2026  
**Status**: In progress

## Completed In This Slice

- Added C# login protocol packet models for the Aion client boundary:
  - `SM_INIT`
  - `SM_AUTH_GG`
  - `SM_LOGIN_OK`
  - `SM_LOGIN_FAIL`
  - `SM_SERVER_LIST`
  - `SM_PLAY_OK`
  - `SM_PLAY_FAIL`
  - `SM_UPDATE_SESSION`
  - account banned/kick packets
- Added client packet parsers and state-based packet factory for:
  - `CM_AUTH_GG`
  - `CM_LOGIN`
  - `CM_SERVER_LIST`
  - `CM_PLAY`
  - `CM_UPDATE_SESSION`
- Added game-server bridge packet models for the first registration/auth slice:
  - `CM_GS_AUTH`
  - `CM_ACCOUNT_AUTH`
  - `CM_ACCOUNT_RECONNECT_KEY`
  - `CM_ACCOUNT_DISCONNECTED`
  - `CM_ACCOUNT_LIST`
  - `CM_GS_PONG`
  - `SM_GS_AUTH_RESPONSE`
  - `SM_ACCOUNT_AUTH_RESPONSE`
  - `SM_ACCOUNT_RECONNECT_KEY`
  - `SM_PING`
- Added length-prefixed packet frame helpers matching the Java dispatcher framing rule: packet length includes the two-byte length field.
- Added hosted login client and game-server socket listener scaffolding under `Aion.LoginServer`.
- Added an in-memory game-server registry seam for tests and later DB-backed `GameServersDAO` parity.
- Corrected `PacketBuffer.WriteS` and `ReadS` to match Java `BaseServerPacket.writeS` / `BaseClientPacket.readS`: UTF-16 little-endian, null-terminated, not length-prefixed.
- Added focused packet parity tests for login and game-server auth packet shapes.
- Added a direct C# port of Java login `BlowfishCipher`.
- Added a C# `LoginCryptEngine` matching the Java login crypto state machine:
  - static first-packet key
  - delayed update to generated Blowfish key after first server packet
  - first-packet XOR pass
  - 8-byte padding behavior
  - checksum append/verify for later packets
- Added crypto tests for Blowfish reversibility, first-packet padding/key update, later packet decrypt, and tamper rejection.
- Added login RSA keypair generation with Java-compatible 1024-bit/F4 keys.
- Added RSA modulus scrambling matching Java `EncryptedRSAKeyPair.encryptModulus`.
- Added raw RSA no-padding decrypt for `CM_LOGIN` credential blocks.
- Added a cached 10-key `LoginKeyGenerator` equivalent to Java `KeyGen`.
- Added normal-login credential decrypt tests for username, password, and OTP extraction.
- Wired encrypted server packet serialization into `LoginClientConnection`.
- Wired encrypted client packet reads and checksum rejection into `LoginClientConnection`.
- Added direct MySQL repository ports for core login schema access:
  - `AccountRepository`
  - `AccountTimeRepository`
  - `BannedIpRepository`
  - `GameServersRepository`
  - `PremiumRepository`
- Added Java-compatible SHA-1/Base64 password hashing via `AccountUtils.EncodePassword`.
- Added `LoginAuthService` for the first DB-backed `AccountController.login` slice:
  - banned IP check
  - auto-create account
  - password hash validation
  - activation check
  - expiration and penalty checks
  - forced IP mask check
  - account time update
  - last IP and membership update
- Wired successful `CM_LOGIN` auth to `SM_LOGIN_OK` session-key creation.
- Added a Docker helper script for a local login MySQL container: `dotnetConversion/scripts/start-login-db.ps1`.
- Added an opt-in MySQL integration test that initializes `login-server/sql/aion_ls.sql` and round-trips `AccountRepository`.
- Added in-memory auth service tests for fast development coverage.
- Added `LoginSessionRegistry` for Java-style `accountsOnLS` tracking.
- Added duplicate login behavior for accounts already on the login server:
  - existing login-server session receives `SM_ACCOUNT_KICK`
  - incoming login receives `SM_LOGIN_FAIL(STR_L2AUTH_S_ALREADY_LOGIN)`
- Added login disconnect cleanup for sessions that have not joined a game server yet:
  - remove account from login-server session registry
  - update account time on logout
- Added `CM_SERVER_LIST` handling for authenticated sessions:
  - session key validation
  - no-server-list failure
  - `SM_SERVER_LIST` response using registered game servers
- Added `CM_PLAY` handling for authenticated sessions:
  - session key validation
  - server-down failure
  - min-access-level failure
  - full-server failure
  - successful `SM_PLAY_OK` and joined-GS marker
- Added game-server registration IP mask enforcement in `GameServerRegistry`.
- Added unit tests for login session registration, duplicate login rejection, and game-server registration auth.
- Added startup loading for registered game servers via `GameServersRepository`.
- Added Java-style live game-server session tracking:
  - game-server disconnect marks the server offline and clears tracked accounts
  - duplicate game-server registration is rejected
  - `SM_REQUEST_KICK_ACCOUNT` can be sent to the owning game server
- Added `CM_ACCOUNT_AUTH` handling from game server to login server:
  - validates the full `SessionKey` against `accountsOnLS`
  - consumes the login-server session after successful game-server auth
  - adds the account to the selected game server
  - updates `account_data.last_server`
  - includes account time, membership, toll, access level, and allowed HDD serial in `SM_ACCOUNT_AUTH_RESPONSE`
- Added reconnect flow parity for the core handoff:
  - `ReconnectingAccount`
  - `CM_ACCOUNT_RECONNECT_KEY`
  - `SM_ACCOUNT_RECONNECT_KEY`
  - `CM_UPDATE_SESSION`
  - `SM_UPDATE_SESSION`
- Added `CM_ACCOUNT_DISCONNECTED` cleanup to remove accounts from the game-server registry and update account time.
- Added initial `CM_ACCOUNT_LIST` sync handling and corrected its count field to Java's 32-bit integer shape.
- Added `CM_GS_CHARACTER` and `SM_GS_CHARACTER_RESPONSE`.
- Updated `CM_SERVER_LIST` to request per-game-server character counts and send `SM_SERVER_LIST` only once every registered server has a count.
- Added packet and registry tests for account-list parsing, request-kick packets, reconnect state, and character-count fanout.
- Added Java-style game-server ping/pong lifecycle:
  - `SM_PING` every 5 seconds after GS auth
  - `CM_GS_PONG` resets the unanswered counter
  - connection closes after more than two unanswered pings
- Added account metadata/control bridge handling:
  - `CM_ACCOUNT_CONNECTION_INFO`
  - `CM_ACCOUNT_TOLL_INFO`
  - `CM_PREMIUM_CONTROL`
  - `CM_CHANGE_ALLOWED_HDD_SERIAL`
- Added premium response packet `SM_PREMIUM_RESPONSE`.
- Added DB-backed repositories and services for:
  - `AccountsLogDAO`
  - `BannedMacDAO`
  - `BannedHddDAO`
- Added MAC/HDD ban bridge handling:
  - `CM_MACBAN_CONTROL`
  - `CM_HDDBAN_CONTROL`
  - `SM_MACBAN_LIST`
  - `SM_HDDBAN_LIST`
- Added startup loading for MAC/HDD ban maps.
- Updated `CM_ACCOUNT_LIST` to send MAC/HDD ban lists after account sync, matching Java's follow-up packet sequence.
- Aligned `PremiumDAO.getPoints` reward consumption with Java's single-row `rs.next()` behavior.
- Added packet tests for ping, account connection info, premium control/response, and ban-list payloads.
- Added account/IP ban bridge handling:
  - `CM_BAN`
  - `SM_BAN_RESPONSE`
  - account penalty time updates
  - banned IP insert/remove path
  - account kick after ban request
- Added login-server control handling:
  - `CM_LS_CONTROL`
  - `SM_LS_CONTROL_RESPONSE`
  - access-level and membership updates through `AccountRepository.UpdateAccountAsync`
- Added player-transfer bridge spine:
  - `CM_PTRANSFER_CONTROL`
  - `SM_PTRANSFER_RESPONSE`
  - `PlayerTransferRepository`
  - `PlayerTransferService`
  - scheduled new-task verification matching Java's 10-second initial delay and 7-minute interval
  - transfer request/error/ok/task-stop state transitions
- Added player-transfer packet tests for all newly modeled response shapes.

## Remaining Gaps

- `CM_LOGIN` now reaches a DB-backed auth service, but not every Java auth branch is ported yet.
- Full Java `AccountController` parity is not complete yet: external auth success path and brute-force ban escalation remain.
- Account/game-server bridge parity is still partial: core auth, reconnect, disconnect, account-list, character-count, premium/toll, MAC/HDD bans, allowed-HDD, account/IP ban control, player transfer, and LS control paths are present, but real mixed Java GS/client interoperability is still unvalidated.
- C# login server is not ready for Java game-server or real client interoperability yet.

## Parity Watch Notes

- The Java `CryptEngine.verifyChecksum` source in this repository reads the final checksum block but does not compare or XOR it before returning. The C# port compares the calculated checksum against the appended checksum so packets produced by the same algorithm verify correctly. This must be validated against a real Java login/client exchange before Phase 3 can be called complete.
- Java compilation tools are not installed in this workspace, so Java-generated crypto golden vectors could not be produced locally yet.
- `LoginClientConnection` now uses encrypted frames, but live client interoperability has not been validated yet.
- Docker CLI is installed, but Docker Desktop's Linux engine was not running during this slice, so the MySQL container integration test could not be executed here. The normal test suite keeps the integration test dormant unless `AION_LOGIN_DB_INTEGRATION=1`.

## Remaining Phase 3 Parity Checklist

### 1. Login Client Crypto And Handshake

- Port Java `network/ncrypt/CryptEngine` exactly:
  - static first Blowfish key: `6B 60 CB 5B 82 CE 90 B1 CC 2B 6C 55 6C 6C 6C 6C` (ported)
  - first server packet special path: add checksum space, align to 8 bytes, XOR pass, encrypt with static key, then update to generated Blowfish key (ported)
  - later packet path: checksum append, 8-byte alignment, encrypt with current key (ported)
  - decrypt path: Blowfish decrypt plus checksum verification (ported, needs Java/client validation)
- Port or prove byte parity for Java `BlowfishCipher` (direct port added; needs Java-generated vectors).
- Port Java `KeyGen` behavior:
  - 10 cached RSA keypairs (ported)
  - 1024-bit RSA with public exponent F4 (ported)
  - generated 16-byte Blowfish keys (ported)
- Port Java `EncryptedRSAKeyPair.encryptModulus` scrambling exactly. (ported)
- Wire encrypted frame read/write in `LoginClientConnection`. (ported; needs real client validation)
- Add golden tests for encrypted `SM_INIT`, checksum verification, key update timing, and decrypt failure behavior.

### 2. Login Credential Authentication

- Port `CM_LOGIN` RSA no-padding credential decrypt in 128-byte blocks. (ported)
- Preserve normal login and `-loginex` layout:
  - normal content offset 94, username 14 bytes, password 16 bytes
  - `-loginex` content offset 78, username 64 bytes, password 32 bytes
  - OTP from little-endian int immediately after username/password
- Keep Cp1252 string extraction and null termination behavior. (ported)
- Port account password hashing from Java `AccountUtils.encodePassword`. (ported)

### 3. Existing Database Schema Integration

- Add direct SQL DAO ports for:
  - `AccountDAO` (core load/insert/update fields ported)
  - `AccountTimeDAO` (ported)
  - `GameServersDAO` (ported)
  - `PremiumDAO` (ported)
  - `BannedIpDAO`
  - `BannedMacDAO`
  - `BannedHddDAO`
  - `AccountsLogDAO`
  - `PlayerTransferDAO`
- Use existing `account_data`, `account_time`, `gameservers`, and related login DB tables without schema migration.
- Preserve Java SQL strings and autocommit behavior unless a verified difference is documented.

### 4. AccountController Flow

- Port `AccountController.login` branch-for-branch:
  - banned IP check (ported)
  - optional external auth
  - account auto-create (ported)
  - password mismatch responses (ported)
  - activation check (ported)
  - account expiry and penalty checks (ported)
  - forced IP mask check (ported)
  - double-login behavior against LS and GS (ported for request-kick behavior)
  - `updateOnLogin`, last IP update, membership expiry update (ported)
- Port reconnect behavior:
  - `ReconnectingAccount` (ported)
  - `CM_UPDATE_SESSION` (ported)
  - `SM_UPDATE_SESSION` (ported)
- Port disconnect cleanup:
  - remove LS account if not joined GS (ported)
  - update account time on logout (ported)

### 5. Game-Server Bridge

- Load registered game servers from DB on startup. (ported)
- Enforce registered server ID, password, and IP mask in `CM_GS_AUTH`. (ported)
- Track online/offline game-server state and clear accounts on disconnect. (ported)
- Port ping/pong lifecycle:
  - send `SM_PING` every 5 seconds (ported)
  - close after more than 2 unanswered pings (ported)
- Port account bridge packets:
  - account auth response (ported)
  - account reconnect key (ported)
  - account disconnected (ported)
  - account list sync (ported)
  - account connection info (ported for DB update/log path)
  - GS character count response (ported)
- Port admin/control bridge packets:
  - LS control (ported)
  - ban control (ported)
  - mac ban control/list (ported)
  - HDD ban control/list (ported)
  - allowed HDD serial change (ported)
  - premium control (ported)
  - account toll info (ported)
  - player transfer control (ported; needs DB/GS integration validation)
  - request kick account (ported)

### 6. Server List And Play Flow

- Preserve Java `CM_SERVER_LIST` behavior:
  - validate session key (ported)
  - close with no-server response when no GS exists (ported)
  - request per-GS character counts (ported)
  - send `SM_SERVER_LIST` only after all counts are known (ported)
- Preserve Java `CM_PLAY` behavior:
  - validate session key (ported)
  - check GS online state (ported)
  - check min access level (ported)
  - check full server (ported)
  - mark client as joined GS (ported)
  - send exact `SM_PLAY_OK` / `SM_PLAY_FAIL` response (ported for core cases)
- Update server lists for logged-in players when GS state changes.

### 7. Startup, Shutdown, And Validation

- Match Java startup ordering: config, DB factory, game-server table, key generation, listener startup.
- Load Java `.properties` from `config/main`, `config/network`, and `config/myls.properties` using identical keys.
- Add graceful shutdown behavior equivalent to Java pending-close semantics where packet sends must complete before closing.
- Validate with:
  - packet golden tests for encrypted and unencrypted frames
  - DAO fixture tests against the current login schema
  - C# login server plus Java game server mixed mode
  - real client login, server list, and server select smoke test

## Verification

- `dotnet test AionServer.slnx`
- Result: all tests passing, 100 total.

## Optional MySQL Integration Test

Start a local login DB container after Docker Desktop is running:

```powershell
cd dotnetConversion
powershell -ExecutionPolicy Bypass -File scripts\start-login-db.ps1
```

Then run the opt-in repository integration test:

```powershell
$env:AION_LOGIN_DB_INTEGRATION = "1"
$env:AION_LOGIN_DB_PORT = "3307"
$env:AION_LOGIN_DB_PASSWORD = "aion"
dotnet test tests\Aion.LoginServer.Tests\Aion.LoginServer.Tests.csproj --filter LoginDatabaseIntegrationTests
```
