package org.powernukkitx.holograms.entity;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.custom.CustomEntity;
import org.powernukkitx.entity.custom.CustomEntityDefinition;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.Location;
import org.powernukkitx.level.format.IChunk;
import org.powernukkitx.level.particle.FloatingTextParticle;
import org.powernukkitx.math.Vector3;
import org.powernukkitx.nbt.tag.CompoundTag;
import org.powernukkitx.nbt.tag.ListTag;
import org.powernukkitx.nbt.tag.StringTag;
import org.powernukkitx.nbt.tag.Tag;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.jetbrains.annotations.NotNull;
import org.powernukkitx.placeholderapi.PlaceholderAPI;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class EntityHologram extends Entity implements CustomEntity {

    public static final String IDENTIFIER = "powernukkitx:hologram";

    public static final String SPACING = "spacing";
    public static final String LINES = "lines";

    private static final Field text_id;

    private final HashMap<Integer, List<String>> lineCache = new HashMap<>();

    static {
        try {
            text_id = FloatingTextParticle.class.getDeclaredField("entityId");
            text_id.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private float spacing = 0.3f;
    private List<String> lines;

    public EntityHologram(IChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        if(!this.nbt.containsFloat(SPACING)) {
            this.nbt.putFloat(SPACING, 0.3f);
        }
        this.spacing = this.nbt.getFloat(SPACING);
        if(!this.nbt.containsList(LINES)) {
            this.nbt.putList(LINES, new ListTag<>(Tag.TAG_String));
        }
        this.lines = new ArrayList<>(this.nbt.getList(LINES, StringTag.class).getAll().stream().map(StringTag::parseValue).toList());
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        this.nbt.putFloat(SPACING, spacing);
        this.nbt.putList(LINES, new ListTag<>(lines.stream().map(StringTag::new).toList()));
    }

    @Override
    public boolean teleport(Vector3 pos) {
        return super.teleport(pos);
    }

    @Override
    public @NotNull String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void spawnTo(Player player) {
        if(this.closed) return;
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.getUsedChunks().contains(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            Location loc = this.getLocation();
            List<String> lines = this.getDisplayLines(player);
            for (int i = lines.size() - 1; i >= 0; i--) {
                loc.y += spacing;
                sendFloatingText(player, i, loc, lines.get(i), false);
            }
            lineCache.put(player.getLoaderId(), new ArrayList<>(lines));
        }
    }

    @Override
    public void despawnFrom(Player player) {
        if (this.hasSpawned.containsKey(player.getLoaderId())) {
            int lineCount = this.lineCache.getOrDefault(player.getLoaderId(), this.lines).size();
            for (int i = lineCount - 1; i >= 0; i--) {
                sendFloatingText(player, i, Vector3.ZERO, "", true);
            }
            this.hasSpawned.remove(player.getLoaderId());
            this.lineCache.remove(player.getLoaderId());
        }
    }

    protected List<String> getDisplayLines(Player player) {
        if(Server.getInstance().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return lines.stream().map(s -> PlaceholderAPI.get().processPlaceholders(player, s)).toList();
        } else return lines;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if(isClosed()) return false;
        for(Player player : getViewers().values()) {
            List<String> sentLines = this.lineCache.get(player.getLoaderId());
            if (sentLines == null) {
                spawnTo(player);
                continue;
            }
            if(sentLines.size() != lines.size()) {
                despawnFrom(player);
                spawnTo(player);
                continue;
            }
            List<String> curLines = getDisplayLines(player);
            Location loc = this.getLocation();
            for (int i = sentLines.size() - 1; i >= 0; i--) {
                loc.y += spacing;
                String curLine = curLines.get(i);
                if(!sentLines.get(i).equals(curLine)) {
                    sentLines.set(i, curLine);
                    sendFloatingText(player, i, loc, curLine, false);
                }
            }
        }
        return super.onUpdate(currentTick);
    }

    @Override
    public void kill() {
    }

    public List<String> getLines() {
        return this.lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public float getSpacing() {
        return this.spacing;
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
    }

    public void removeLine(int lineIndex) {
        for (Player viewer : getViewers().values()) {
            sendFloatingText(viewer, lineIndex, Vector3.ZERO, "", true);
        }
    }

    private void sendFloatingText(Player player, int lineIndex, Vector3 position, String text, boolean invisible) {
        FloatingTextParticle particle = new FloatingTextParticle(position, text);
        try {
            text_id.setLong(particle, lineId(lineIndex));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to assign floating text entity id", e);
        }
        particle.setInvisible(invisible);
        for (BedrockPacket packet : particle.encode()) {
            player.sendPacket(packet);
        }
    }

    private long lineId(int lineIndex) {
        return (this.getId() << 32) | (lineIndex & 0xffffffffL);
    }

    public static CustomEntityDefinition definition() {
        return CustomEntityDefinition.simpleBuilder(IDENTIFIER)
                .eid(IDENTIFIER)
                .hasSpawnEgg(false)
                .isSummonable(false)
                .build();
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
