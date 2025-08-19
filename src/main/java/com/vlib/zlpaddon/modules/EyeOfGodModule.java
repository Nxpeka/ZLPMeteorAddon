package com.vlib.zlpaddon.modules;

import com.vlib.zlpaddon.ZlpAddon;
import com.vlib.zlpaddon.dto.request.ZlpMapPlayersDTO;
import com.vlib.zlpaddon.exceptions.FetchException;
import com.vlib.zlpaddon.exceptions.PlayerAlreadyInListException;
import com.vlib.zlpaddon.exceptions.PlayerNotFoundException;
import com.vlib.zlpaddon.hud.EyeOfGodHud;
import com.vlib.zlpaddon.models.MinecraftPlayerModel;
import com.vlib.zlpaddon.services.EyeOfGodFactory;
import lombok.Getter;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EyeOfGodModule extends Module {

    private final SettingGroup sgGeneral = this.settings.createGroup("General");
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    @Getter
    private final ConcurrentHashMap<String, MinecraftPlayerModel> onlinePlayers = new ConcurrentHashMap<>();
    private final Set<String> offlinePlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> spiedPlayers = ConcurrentHashMap.newKeySet();
    private volatile List<MinecraftPlayerModel> playersList = List.of();
    private ScheduledFuture<?> spyingTask;
    private final Supplier<Text> MODULE_PREFIX = () -> Text.empty()
        .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
        .append("[")
        .append(Text.literal("EyeOfGod")
            .setStyle(Style.EMPTY.withColor(Formatting.DARK_RED)))
        .append("] ");

    public EyeOfGodModule() {
        super(ZlpAddon.CATEGORY, "eyeofgod", "module which spying of players");
        ChatUtils.registerCustomPrefix(EyeOfGodModule.class.getPackageName(), MODULE_PREFIX);
    }
    private final Setting<List<String>> nicknamesSg = sgGeneral.add(new StringListSetting.Builder()
        .name("nicknames")
        .description("list of nicknames")
        .onChanged(resultedList -> {
            Set<String> newSet = new HashSet<>(resultedList);

            spiedPlayers.removeIf(nick -> !newSet.contains(nick));

            spiedPlayers.addAll(newSet);

            onlinePlayers.keySet().removeIf(nick -> !newSet.contains(nick));
            offlinePlayers.removeIf(nick -> !newSet.contains(nick));
        })
        .build()
    );

    private final Setting<Boolean> soundNotification = sgGeneral.add(new BoolSetting.Builder()
        .name("sound")
        .description("Sound notification")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> delaySg = sgGeneral.add(new DoubleSetting.Builder()
        .name("delay")
        .description("delay in seconds below 0.5 may cause exception т-т")
        .max(60)
        .min(0.5)
        .defaultValue(1)
        .build()
    );


    @Override
    public void onActivate() {
        ChatUtils.sendMsg(Text.of("Trying to spy players"));
        if (!EyeOfGodFactory.getINSTANCE().getEyeOfGodHud().isActive()){
            EyeOfGodFactory.getINSTANCE().getEyeOfGodHud().toggle();
        }
        spyingTask = executor.scheduleWithFixedDelay(() -> {
            try {
                collectPlayers();
                spyingPlayers();
            }
            catch (Exception e) {
                ZlpAddon.LOG.error("Error in spying task", e);
                this.toggle();
            }
        }, 0, (long) (delaySg.get() * 1000L), TimeUnit.MILLISECONDS);
    }

    @Override
    public void onDeactivate() {
        if (EyeOfGodFactory.getINSTANCE().getEyeOfGodHud().isActive()){
            EyeOfGodFactory.getINSTANCE().getEyeOfGodHud().toggle();
        }
        if (spyingTask != null && !spyingTask.isCancelled()) {
            spyingTask.cancel(true);
        }
        onlinePlayers.clear();
        ChatUtils.sendMsg(Text.of("EyeOfGod disabled"));
    }

    private void collectPlayers() throws FetchException{
        if (!this.isActive()){
            return;
        }
        try {
            ZlpMapPlayersDTO overworldPlayers = fetchPlayers("https://zlp.onl/map/maps/world/live/players.json");
            ZlpMapPlayersDTO netherPlayers = fetchPlayers("https://zlp.onl/map/maps/world/live/players.json");

            if(this.isActive()){
                if (overworldPlayers == null || netherPlayers == null) {
                    throw new FetchException("Could not fetch overworld players");
                }
            } else {
                return;
            }

            Map<String, MinecraftPlayerModel> endPlayers = Stream.concat(
                overworldPlayers.getPlayers().stream().filter(ZlpMapPlayersDTO.ZlpMapPlayerDTO::isForeign),
                netherPlayers.getPlayers().stream().filter(ZlpMapPlayersDTO.ZlpMapPlayerDTO::isForeign)
            ).collect(Collectors.toMap(
                ZlpMapPlayersDTO.ZlpMapPlayerDTO::getName,
                dto -> toModel(dto, Dimension.End),
                (a, b) -> a)
            );

            Stream<MinecraftPlayerModel> otherPlayers = overworldPlayers.getPlayers().stream()
                .filter(dto -> !endPlayers.containsKey(dto.getName()))
                .map(dto -> toModel(dto, dto.isForeign() ? Dimension.Nether : Dimension.Overworld));

            this.playersList = Stream.concat(otherPlayers, endPlayers.values().stream()).toList();
        } catch (Exception e) {
            ZlpAddon.LOG.error("Error in collecting players", e);
        }
    }

    private void spyingPlayers() {
        if (spiedPlayers.isEmpty()) {
            this.toggle();
            return;
        }
        try {
            spiedPlayers.forEach(this::processPlayer);
        } catch (Exception e) {
            ZlpAddon.LOG.error("Error spying players", e);
            this.toggle();
        }
    }

    private void processPlayer(String nickname) {
        if (!this.isActive()){
            return;
        }
        if (nickname.trim().isEmpty()) {
            return;
        }
        Predicate<ZlpMapPlayersDTO.ZlpMapPlayerDTO> equalsName = player -> player.getName().equalsIgnoreCase(nickname);

        Optional<MinecraftPlayerModel> first = playersList.stream()
            .filter(player -> player.getName().equalsIgnoreCase(nickname))
            .findFirst();

        if (!first.isPresent()) {
            if (onlinePlayers.containsKey(nickname)) {
                onlinePlayers.remove(nickname);
                offlinePlayers.add(nickname);
                this.logoutNotification(nickname);
            }
            else if (!offlinePlayers.contains(nickname) && !onlinePlayers.containsKey(nickname)) {
                offlinePlayers.add(nickname);
                this.notFoundNotification(nickname);
            }
            return;
        }

        MinecraftPlayerModel minecraftPlayerModel = first.get();

        if (offlinePlayers.contains(minecraftPlayerModel.getName())) {
            offlinePlayers.remove(minecraftPlayerModel.getName());
            this.joinNotification(nickname);
        }

        onlinePlayers.put(nickname, minecraftPlayerModel);
    }

    private void logoutNotification(String nickname) {
        ChatUtils.warning("Player (highlight)%s(default) logout", nickname);
        if (soundNotification.get()) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F));
        }
    }

    private void notFoundNotification(String nickname) {
        ChatUtils.error("Player (highlight)%s(default) not found", nickname);
        if (soundNotification.get()) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1.0F));
        }
    }

    private void joinNotification(String nickname) {
        ChatUtils.info("Player (highlight)%s(default) joined to the server", nickname);
        if (soundNotification.get()){
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
        }
    }

    private ZlpMapPlayersDTO fetchPlayers(String url){
        ZlpAddon.LOG.debug("Send requests");
        return Http.get(url).sendJson(ZlpMapPlayersDTO.class);
    }

    public void addPlayerToSpying(String nickname) throws PlayerAlreadyInListException {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (!nicknamesSg.get().contains(nickname)) {
            nicknamesSg.get().add(nickname);
            nicknamesSg.onChanged();
            return;
        }
        throw new PlayerAlreadyInListException("Player with this nickname already in list");
    }

    public void removePlayerFromSpying(String nickname) throws PlayerNotFoundException {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (nicknamesSg.get().contains(nickname)) {
            nicknamesSg.get().remove(nickname);
            nicknamesSg.onChanged();
            return;
        }
        throw new PlayerNotFoundException("Nickname not found in list");
    }

    public ZlpMapPlayersDTO.PositionDTO locatePlayer(String nickname) throws PlayerNotFoundException, FetchException {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (!this.isActive()){
            this.collectPlayers();
        }

        return playersList.stream()
            .filter(player -> player.getName().equalsIgnoreCase(nickname))
            .findFirst()
            .orElseThrow(() -> new PlayerNotFoundException("Player not found"))
            .getPosition();
    }

    private MinecraftPlayerModel toModel(ZlpMapPlayersDTO.ZlpMapPlayerDTO dto, Dimension dimension) {
        return new MinecraftPlayerModel(
            dto.getUuid(),
            dto.getName(),
            dto.getPosition(),
            dto.getRotation(),
            dimension
        );
    }
}
