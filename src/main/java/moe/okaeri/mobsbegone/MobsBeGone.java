package moe.okaeri.mobsbegone;

import moe.okaeri.mobsbegone.config.MobsBeGoneConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MobsBeGone implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("mobsbegone");
	private static long[] BLACKLIST;
	private static final ObjectArrayList<Entity> ENTITY_BUFFER = new ObjectArrayList<>(64);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
	}

	private void onServerStarting(MinecraftServer minecraftServer) {
		BLACKLIST = MobsBeGoneConfig.loadBlacklist();

		ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
		ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);

		LOGGER.info("MobsBeGone >> Up and running!");
	}

	private void onEntityLoad(Entity entity, ServerWorld world) {
		if (entity instanceof LivingEntity) {
			if (isEntityBlacklisted(entity.getType())) {
                //TODO: Remove hard-coding and add unwanted dimensions to Blacklist file
                if (entity.getEntityWorld().getRegistryKey() == World.OVERWORLD) {
                    ENTITY_BUFFER.add(entity);
                }
			}
		}
	}

	private void onEndServerTick(MinecraftServer minecraftServer) {
		for (int i = ENTITY_BUFFER.size(); i-- > 0;) {
			Entity e = ENTITY_BUFFER.get(i);
			if (e != null && !e.isRemoved()) {
                e.discard();
                // LOGGER.info("Event watcher discarded" + e.getType());
            }
		}
		ENTITY_BUFFER.clear();
	}

	public static boolean isEntityBlacklisted(EntityType<?> entityType) {
		int raw = Registries.ENTITY_TYPE.getRawId(entityType);
		return (((BLACKLIST[raw >>> 6] >>> (raw & 63)) & 1L) != 0);
	}
}
