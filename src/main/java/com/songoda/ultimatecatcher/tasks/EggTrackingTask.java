package com.songoda.ultimatecatcher.tasks;

import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.songoda.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.ultimatecatcher.UltimateCatcher;
import com.songoda.ultimatecatcher.utils.EntityUtils;
import com.songoda.ultimatecatcher.utils.OldEntityUtils;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class EggTrackingTask extends BukkitRunnable {

    private static final Set<Item> eggs = new HashSet<>();

    private static EggTrackingTask instance;
    private static UltimateCatcher plugin;

    private EggTrackingTask(UltimateCatcher plug) {
        plugin = plug;
    }

    public static EggTrackingTask startTask(UltimateCatcher plug) {
        plugin = plug;
        if (instance == null) {
            instance = new EggTrackingTask(plugin);
            instance.runTaskTimer(plugin, 0, 1);
        }

        return instance;
    }

    @Override
    public void run() {
        for (Item item : new HashSet<>(eggs)) {
            // Skip if this egg is no longer tracked by EggHandler
            if (!UltimateCatcher.getInstance().getEggHandler().getEggs().containsKey(item.getUniqueId())) {
                eggs.remove(item);
                continue;
            }

            if (!item.isValid()) {
                eggs.remove(item);
                item.remove();
                continue;
            }

            if (item.isOnGround() && item.getTicksLived() > 10 || item.getTicksLived() > 50) {

                String displayName = item.getItemStack().getItemMeta().getDisplayName();

                boolean inWater = XMaterial.matchXMaterial(item.getLocation().getBlock().getType()) == XMaterial.WATER;

                Entity entity;
                if (new NBTItem(item.getItemStack()).hasKey("serialized_entity")) {
                    entity = EntityUtils.spawnEntity(inWater ? item.getLocation().getBlock().getLocation().add(.5, .5, .5)
                            : item.getLocation(), item.getItemStack());
                } else if (!displayName.contains("~") && new NBTItem(item.getItemStack()).hasKey("UCI")) {
                    entity = OldEntityUtils.spawnEntity(item.getLocation(), item.getItemStack());
                } else {
                    String[] split = item.getItemStack().getItemMeta().getDisplayName().split("~");
                    String json = split[0].replace(String.valueOf(ChatColor.COLOR_CHAR), "");
                    entity = OldEntityUtils.spawnEntity(item.getLocation(), json);
                }

                eggs.remove(item);

                // Couldn't spawn
                if (entity == null) {
                    plugin.getEggHandler().getEggs().remove(item.getUniqueId());

                    item.getItemStack().removeEnchantment(Enchantment.ARROW_KNOCKBACK);
                    item.setPickupDelay(1);

                    NBTItem newItem = new NBTItem(item.getItemStack());
                    newItem.removeKey("UCI");
                    item.setItemStack(newItem.getItem());
                    continue;
                }

                CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.SMOKE_NORMAL, entity.getLocation(), 100, .5, .5, .5);
                XSound.ITEM_FIRECHARGE_USE.play(entity.getLocation(), 1L, 1L);

                item.remove();
            }
        }
    }

    public static void addEgg(Item item) {
        eggs.add(item);
    }

}
