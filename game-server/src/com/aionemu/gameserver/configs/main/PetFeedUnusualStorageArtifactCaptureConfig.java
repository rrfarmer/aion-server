package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * Disabled-by-default config shell for pet-feed unusual-storage parity artifacts.
 */
public class PetFeedUnusualStorageArtifactCaptureConfig {

	@Property(key = "gameserver.petfeed.unusual_storage_artifacts.enabled", defaultValue = "false")
	public static boolean ENABLED;

	@Property(key = "gameserver.petfeed.unusual_storage_artifacts.output_dir", defaultValue = "./parity-artifacts/pet-feed-unusual-storage/java")
	public static String OUTPUT_DIR;

	@Property(key = "gameserver.petfeed.unusual_storage_artifacts.max_pending_contexts_per_player", defaultValue = "4")
	public static int MAX_PENDING_CONTEXTS_PER_PLAYER;

	@Property(key = "gameserver.petfeed.unusual_storage_artifacts.max_queued_artifacts", defaultValue = "32")
	public static int MAX_QUEUED_ARTIFACTS;

	@Property(key = "gameserver.petfeed.unusual_storage_artifacts.allowed_scenario", defaultValue = "pet_feed_unusual_storage")
	public static String ALLOWED_SCENARIO;
}
