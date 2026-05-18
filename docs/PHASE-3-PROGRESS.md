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

## Intentional Gaps

- Login client packet encryption is not complete. The socket scaffold currently serializes unencrypted packet frames for internal testing only.
- Java login Blowfish `CryptEngine` parity is not ported yet.
- RSA modulus scrambling is represented by the packet shape, but real RSA keypair generation and no-padding credential decrypt are not wired in yet.
- `CM_LOGIN` parses encrypted credential blocks, but account authentication is not DB-backed yet.
- Game-server IP mask validation is deferred until registered game servers are loaded from the existing login database.
- C# login server is not ready for Java game-server or real client interoperability yet.

## Verification

- `dotnet test AionServer.slnx`
- Result: all tests passing.
