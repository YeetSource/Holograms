package org.powernukkitx.holograms;

import org.powernukkitx.entity.Entity;
import org.powernukkitx.level.Position;
import org.powernukkitx.nbt.tag.CompoundTag;
import org.powernukkitx.nbt.tag.ListTag;
import org.powernukkitx.nbt.tag.StringTag;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.registry.RegisterException;
import org.powernukkitx.registry.Registries;
import org.powernukkitx.holograms.commands.HologramCommand;
import org.powernukkitx.holograms.entity.EntityHologram;

import java.util.List;

public class Holograms extends PluginBase {

    private static Holograms INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.getServer().getCommandMap().register("hologram", new HologramCommand());
        try {
            Registries.ENTITY.registerCustomEntity(get(), EntityHologram.class);
        } catch (RegisterException e) {
            throw new RuntimeException(e);
        }
    }

    public static Holograms get() {
        return INSTANCE;
    }

    public EntityHologram createHologram(String identifier, Position position, List<String> lines) {
        CompoundTag tag = Entity.getDefaultNBT(position);
        ListTag<StringTag> linesTag = new ListTag<>(lines.stream().map(StringTag::new).toList());
        tag.putList(EntityHologram.LINES, linesTag);
        EntityHologram hologram = new EntityHologram(position.getChunk(), tag);
        hologram.setNameTag(identifier);
        hologram.spawnToAll();
        return hologram;
    }
}
