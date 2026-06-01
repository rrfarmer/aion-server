package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
import com.aionemu.gameserver.services.RepurchaseService;

import sun.misc.Unsafe;

public class SM_REPURCHASE_GoldenTest {

	@Test
	public void writeImpl_writesEmptyRepurchaseListPayload() throws Exception {
		SM_REPURCHASE packet = allocatePacket();
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);

		assertEquals("29230000010000000000", toHex(payload));
		assertArrayEquals(new byte[] { 0x29, 0x23, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 }, payload);
	}

	@Test
	public void writeImpl_writesSimpleRepurchaseItemWithGeneralInfoBlobAndPrice() throws Exception {
		ItemRestrictionCleanupData originalCleanup = DataManager.ITEM_CLEAN_UP;
		try {
			DataManager.ITEM_CLEAN_UP = emptyCleanupData();

			SM_REPURCHASE packet = allocatePacket(Collections.singletonList(simpleItem()));
			ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);

			packet.writeImpl(null);

			byte[] payload = new byte[buffer.position()];
			buffer.flip();
			buffer.get(payload);

			assertEquals(
				"29230000010000000100591B000001E1F50524008138010000002200000100010000000000000000000000000000000000000000000000000000000012003930000000000000",
				toHex(payload));
		} finally {
			DataManager.ITEM_CLEAN_UP = originalCleanup;
		}
	}

	@Test
	public void writeImpl_writesSimpleEquipmentRepurchaseItemWithEquipmentBlobAndPrice() throws Exception {
		ItemRestrictionCleanupData originalCleanup = DataManager.ITEM_CLEAN_UP;
		try {
			DataManager.ITEM_CLEAN_UP = emptyCleanupData();

			SM_REPURCHASE packet = allocatePacket(Collections.singletonList(simpleSwordItem()));
			ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);

			packet.writeImpl(null);

			byte[] payload = new byte[buffer.position()];
			buffer.flip();
			buffer.get(payload);

			assertEquals(
				"292300000100000001005A1B000001E1F5052400813801000000CB0006000000000000000001010000000000000002000000000000000B000301E1F50500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000100010000000000000000000000000000000000000000000000000000000012003930000000000000",
				toHex(payload));
		} finally {
			DataManager.ITEM_CLEAN_UP = originalCleanup;
		}
	}

	@Test
	public void constructor_usesRepurchaseServiceSetIterationOrderForItems() throws Exception {
		ItemRestrictionCleanupData originalCleanup = DataManager.ITEM_CLEAN_UP;
		Player player = simplePlayer();
		RepurchaseService repurchaseService = RepurchaseService.getInstance();
		try {
			DataManager.ITEM_CLEAN_UP = emptyCleanupData();
			repurchaseService.removeRepurchaseItems(player);
			repurchaseService.addRepurchaseItems(player, Arrays.asList(simpleItem(7002), simpleItem(7001)));

			List<Integer> expectedObjectIds = new ArrayList<>();
			for (Item item : repurchaseService.getRepurchaseItems(player.getObjectId()))
				expectedObjectIds.add(item.getObjectId());

			SM_REPURCHASE packet = new SM_REPURCHASE(player, 9001);
			ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);

			packet.writeImpl(null);

			byte[] payload = new byte[buffer.position()];
			buffer.flip();
			buffer.get(payload);

			assertEquals(expectedObjectIds, readRepurchaseItemObjectIds(payload));
		} finally {
			repurchaseService.removeRepurchaseItems(player);
			DataManager.ITEM_CLEAN_UP = originalCleanup;
		}
	}

	private static SM_REPURCHASE allocatePacket() throws Exception {
		return allocatePacket(Collections.emptyList());
	}

	private static SM_REPURCHASE allocatePacket(Iterable<Item> items) throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		SM_REPURCHASE packet = (SM_REPURCHASE) unsafe.allocateInstance(SM_REPURCHASE.class);
		setField(packet, "targetObjectId", 9001);
		setField(packet, "items", items);
		setField(packet, "player", null);
		return packet;
	}

	private static Item simpleItem() throws Exception {
		return simpleItem(7001);
	}

	private static Item simpleItem(int objectId) throws Exception {
		Item item = (Item) unsafe().allocateInstance(Item.class);
		setAionObjectId(item, objectId);
		setField(item, "itemCount", 1L);
		setField(item, "itemTemplate", simpleTemplate());
		setField(item, "repurchasePrice", 12345L);
		return item;
	}

	private static Item simpleSwordItem() throws Exception {
		Item item = (Item) unsafe().allocateInstance(Item.class);
		setAionObjectId(item, 7002);
		setField(item, "itemCount", 1L);
		setField(item, "itemTemplate", simpleTemplate(ItemGroup.SWORD));
		setField(item, "enchantLevel", 3);
		setField(item, "repurchasePrice", 12345L);
		return item;
	}

	private static ItemTemplate simpleTemplate() throws Exception {
		return simpleTemplate(ItemGroup.NONE);
	}

	private static ItemTemplate simpleTemplate(ItemGroup itemGroup) throws Exception {
		ItemTemplate template = (ItemTemplate) unsafe().allocateInstance(ItemTemplate.class);
		setField(template, "itemId", 100000001);
		setField(template, "mask", 1);
		setField(template, "description", 40000);
		setField(template, "itemGroup", itemGroup);
		return template;
	}

	private static Player simplePlayer() throws Exception {
		Player player = (Player) unsafe().allocateInstance(Player.class);
		setAionObjectId(player, 1001);
		return player;
	}

	private static ItemRestrictionCleanupData emptyCleanupData() throws Exception {
		ItemRestrictionCleanupData data = (ItemRestrictionCleanupData) unsafe().allocateInstance(ItemRestrictionCleanupData.class);
		setField(data, "bplist", Collections.emptyList());
		return data;
	}

	private static void setAionObjectId(AionObject object, int objectId) throws Exception {
		Field field = AionObject.class.getDeclaredField("objectId");
		long offset = unsafe().objectFieldOffset(field);
		unsafe().putInt(object, offset, objectId);
	}

	private static Unsafe unsafe() throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		return (Unsafe) unsafeField.get(null);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte value : bytes)
			sb.append(String.format("%02X", value));
		return sb.toString();
	}

	private static List<Integer> readRepurchaseItemObjectIds(byte[] payload) {
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
		buffer.getInt(); // targetObjectId
		buffer.getInt(); // constant 1
		int itemCount = Short.toUnsignedInt(buffer.getShort());
		List<Integer> objectIds = new ArrayList<>();
		for (int i = 0; i < itemCount; i++) {
			objectIds.add(buffer.getInt());
			buffer.getInt(); // template id
			readS(buffer);
			int blobLength = Short.toUnsignedInt(buffer.getShort());
			buffer.position(buffer.position() + blobLength);
			buffer.getLong(); // repurchase price
		}
		return objectIds;
	}

	private static void readS(ByteBuffer buffer) {
		while (buffer.remaining() >= 2 && buffer.getChar() != 0) {
			// Skip UTF-16LE characters until the null terminator written by writeS.
		}
	}
}
