package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EventThemeIdGoldenTest {

	@Test
	public void getId_returnsJavaConstructorIds() {
		assertEquals(0, EventTheme.NONE.getId());
		assertEquals(1, EventTheme.CHRISTMAS.getId());
		assertEquals(2, EventTheme.HALLOWEEN.getId());
		assertEquals(4, EventTheme.VALENTINE.getId());
		assertEquals(8, EventTheme.BRAXCAFE.getId());
		assertEquals(16, EventTheme.TEST_BASIC_1.getId());
		assertEquals(32, EventTheme.TEST_BASIC_2.getId());
		assertEquals(64, EventTheme.TEST_BASIC_3.getId());
		assertEquals(128, EventTheme.TEST_BASIC_4.getId());
	}
}
