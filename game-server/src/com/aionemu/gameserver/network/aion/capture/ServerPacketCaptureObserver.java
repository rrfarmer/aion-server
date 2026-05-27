package com.aionemu.gameserver.network.aion.capture;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * Java parity capture hook for optional server-packet runtime artifacts.
 */
public interface ServerPacketCaptureObserver {

	boolean isEnabled();

	void onPacketSerialized(AionConnection con, AionServerPacket packet, ByteBuffer clearFrame);
}
