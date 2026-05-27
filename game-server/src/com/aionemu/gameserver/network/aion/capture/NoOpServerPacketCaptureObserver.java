package com.aionemu.gameserver.network.aion.capture;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * Disabled default packet capture observer.
 */
public enum NoOpServerPacketCaptureObserver implements ServerPacketCaptureObserver {
	INSTANCE;

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public void onPacketSerialized(AionConnection con, AionServerPacket packet, ByteBuffer clearFrame) {
	}
}
