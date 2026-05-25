package com.aionemu.gameserver.network.aion;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.commons.network.AcceptReadWriteDispatcherImpl;
import com.aionemu.commons.network.Dispatcher;
import com.aionemu.gameserver.network.aion.serverpackets.SM_VERSION_CHECK;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Proof utility for Phase 6 selectable-decompose Java runtime capture.
 * <p>
 * Java source breadcrumbs:
 * <ul>
 *   <li>{@code com.aionemu.commons.network.Acceptor#accept}</li>
 *   <li>{@code com.aionemu.gameserver.network.aion.AionConnection#initialized}</li>
 *   <li>{@code com.aionemu.gameserver.network.Crypt}</li>
 *   <li>{@code com.aionemu.gameserver.network.EncryptionKeyPair}</li>
 *   <li>{@code com.aionemu.gameserver.network.aion.AionClientPacketFactory}</li>
 * </ul>
 *
 * This is intentionally a standalone opt-in utility rather than a default JUnit test. The Java dispatcher and
 * packet processor do not expose a full test shutdown API, so the process exits after the proof run.
 */
public final class LoopbackCaptureProof {

	private static final byte[] STATIC_KEY = "nKO/WctQ0AVLbpzfBkS6NevDYT8ourG5CRlmdjyJ72aswx4EPq1UgZhFMXH?3iI9".getBytes();
	private static final int STATIC_CLIENT_PACKET_CODE = 0x65;
	private static final int STATIC_SERVER_PACKET_CODE = 0x44;
	private static final int SM_KEY_OPCODE = 72;
	private static final int CM_SELECT_DECOMPOSABLE_OPCODE = 236;

	private LoopbackCaptureProof() {
	}

	public static void main(String[] args) {
		int exitCode = 0;
		try {
			new LoopbackCaptureProof().run();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			exitCode = 1;
		} finally {
			try {
				ThreadPoolManager.getInstance().shutdown();
			} catch (Throwable ignored) {
			}
			System.exit(exitCode);
		}
	}

	private void run() throws Exception {
		int port = Integer.getInteger("aion.capture.port", 21077);
		Executor directDisconnectExecutor = Runnable::run;
		AcceptReadWriteDispatcherImpl dispatcher = new AcceptReadWriteDispatcherImpl("LoopbackCaptureProof Dispatcher", directDisconnectExecutor);
		dispatcher.setDaemon(true);
		dispatcher.start();

		AtomicReference<CaptureConnection> accepted = new AtomicReference<>();
		CountDownLatch acceptedLatch = new CountDownLatch(1);

		try (ServerSocketChannel server = ServerSocketChannel.open()) {
			server.configureBlocking(true);
			server.socket().bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));

			Thread.ofPlatform()
				.name("LoopbackCaptureProof Acceptor")
				.daemon(true)
				.start(() -> acceptOnce(server, dispatcher, accepted, acceptedLatch));

			try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
				client.setSoTimeout(3000);

				if (!acceptedLatch.await(3, TimeUnit.SECONDS))
					throw new IllegalStateException("Timed out waiting for Java AionConnection accept");

				CaptureConnection connection = accepted.get();
				connection.setState(AionConnection.State.IN_GAME);

				byte[] smKeyFrame = readFrame(client.getInputStream());
				DecodedServerFrame smKey = decodeUnencryptedServerFrame(smKeyFrame);
				if (smKey.opcode != SM_KEY_OPCODE)
					throw new IllegalStateException("Expected SM_KEY opcode " + SM_KEY_OPCODE + " but got " + smKey.opcode);

				int falseKey = readIntLE(smKeyFrame, 7);
				int baseKey = (falseKey - 0x3FF2CCCF) ^ 0xCD92E4DF;
				byte[] encryptedSelectFrame = createEncryptedSelectDecomposableFrame(baseKey, 5001, 0, 1);
				client.getOutputStream().write(encryptedSelectFrame);
				client.getOutputStream().flush();

				System.out.println("Loopback proof delivered encrypted CM_SELECT_DECOMPOSABLE frame");
				System.out.println("acceptedConnectionState=" + connection.getState());
				System.out.println("smKeyFrameHex=" + toHex(smKeyFrame));
				System.out.println("encryptedClientFrameHex=" + toHex(encryptedSelectFrame));
			}
		}
	}

	private static void acceptOnce(ServerSocketChannel server, Dispatcher dispatcher, AtomicReference<CaptureConnection> accepted,
		CountDownLatch acceptedLatch) {
		try {
			SocketChannel socketChannel = server.accept();
			socketChannel.configureBlocking(false);
			socketChannel.socket().setSoLinger(true, 10);
			socketChannel.socket().setTcpNoDelay(true);

			CaptureConnection connection = new CaptureConnection(socketChannel, dispatcher);
			dispatcher.register(socketChannel, SelectionKey.OP_READ, connection);
			accepted.set(connection);
			acceptedLatch.countDown();
			connection.initializeForCapture();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] readFrame(InputStream input) throws Exception {
		int b0 = input.read();
		int b1 = input.read();
		if (b0 < 0 || b1 < 0)
			throw new IllegalStateException("Socket closed before frame length");
		int length = b0 | (b1 << 8);
		byte[] frame = new byte[length];
		frame[0] = (byte) b0;
		frame[1] = (byte) b1;
		int offset = 2;
		while (offset < length) {
			int read = input.read(frame, offset, length - offset);
			if (read < 0)
				throw new IllegalStateException("Socket closed before full frame");
			offset += read;
		}
		return frame;
	}

	private static DecodedServerFrame decodeUnencryptedServerFrame(byte[] frame) {
		int encodedOpcode = readUnsignedShortLE(frame, 2);
		int staticCode = frame[4] & 0xFF;
		int complement = readUnsignedShortLE(frame, 5);
		if (staticCode != STATIC_SERVER_PACKET_CODE)
			throw new IllegalStateException("Invalid server static packet code: " + staticCode);
		if (((short) encodedOpcode) != (short) ~complement)
			throw new IllegalStateException("Invalid server opcode complement");
		int opcode = (encodedOpcode ^ 0xDF) - SM_VERSION_CHECK.INTERNAL_VERSION;
		return new DecodedServerFrame(opcode);
	}

	private static byte[] createEncryptedSelectDecomposableFrame(int baseKey, int objectId, int unknownDword, int index) {
		byte[] frame = new byte[16];
		writeShortLE(frame, 0, frame.length);
		int encodedOpcode = encodeClientOpcode(CM_SELECT_DECOMPOSABLE_OPCODE);
		writeShortLE(frame, 2, encodedOpcode);
		frame[4] = (byte) STATIC_CLIENT_PACKET_CODE;
		writeShortLE(frame, 5, ~encodedOpcode);
		writeIntLE(frame, 7, objectId);
		writeIntLE(frame, 11, unknownDword);
		frame[15] = (byte) index;

		byte[] clientKey = createInitialKey(baseKey);
		encrypt(frame, 2, frame.length - 2, clientKey);
		return frame;
	}

	private static int encodeClientOpcode(int opcode) {
		return (((opcode + SM_VERSION_CHECK.INTERNAL_VERSION) ^ 0xEF) + 0x0C) ^ 0xEF;
	}

	private static byte[] createInitialKey(int baseKey) {
		return new byte[] {
			(byte) (baseKey & 0xFF),
			(byte) ((baseKey >> 8) & 0xFF),
			(byte) ((baseKey >> 16) & 0xFF),
			(byte) ((baseKey >> 24) & 0xFF),
			(byte) 0xA1,
			(byte) 0x6C,
			(byte) 0x54,
			(byte) 0x87
		};
	}

	private static void encrypt(byte[] data, int offset, int size, byte[] key) {
		data[offset] ^= key[0];
		int previousEncrypted = data[offset++] & 0xFF;
		for (int i = 1; i < size; i++, offset++) {
			data[offset] ^= STATIC_KEY[i & 63] ^ key[i & 7] ^ previousEncrypted;
			previousEncrypted = data[offset] & 0xFF;
		}
		advanceKey(key, size);
	}

	private static void advanceKey(byte[] key, int size) {
		long oldKey = 0;
		for (int i = 0; i < key.length; i++)
			oldKey |= (long) (key[i] & 0xFF) << (i * 8);
		oldKey += size;
		for (int i = 0; i < key.length; i++)
			key[i] = (byte) (oldKey >> (i * 8));
	}

	private static int readUnsignedShortLE(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
	}

	private static int readIntLE(byte[] data, int offset) {
		return (data[offset] & 0xFF) |
			((data[offset + 1] & 0xFF) << 8) |
			((data[offset + 2] & 0xFF) << 16) |
			(data[offset + 3] << 24);
	}

	private static void writeShortLE(byte[] data, int offset, int value) {
		data[offset] = (byte) value;
		data[offset + 1] = (byte) (value >> 8);
	}

	private static void writeIntLE(byte[] data, int offset, int value) {
		data[offset] = (byte) value;
		data[offset + 1] = (byte) (value >> 8);
		data[offset + 2] = (byte) (value >> 16);
		data[offset + 3] = (byte) (value >> 24);
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes)
			sb.append(String.format("%02X", b));
		return sb.toString();
	}

	private record DecodedServerFrame(int opcode) {
	}

	private static final class CaptureConnection extends AionConnection {

		private CaptureConnection(SocketChannel socketChannel, Dispatcher dispatcher) throws Exception {
			super(socketChannel, dispatcher);
		}

		private void initializeForCapture() {
			initialized();
		}
	}
}
