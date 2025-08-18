package com.vlib.zlpaddon.modules;

import com.vlib.zlpaddon.ZlpAddon;
import com.vlib.zlpaddon.dto.request.ZlpMapPlayersDTO;
import com.vlib.zlpaddon.http.HttpClient;
import com.vlib.zlpaddon.models.MinecraftPlayerModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class EyeOfGodModule extends Module {

    private final SettingGroup sgGeneral = this.settings.createGroup("General");
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final HttpClient httpClient = HttpClient.getInstance();
    private final ConcurrentHashMap<String, MinecraftPlayerModel> onlinePlayers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> offlinePlayers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> spiedPlayers = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<MinecraftPlayerModel> playersList = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> spyingTask;
    Semaphore semaphore = new Semaphore(1);


    public EyeOfGodModule() {
        super(ZlpAddon.CATEGORY, "eyeofgod", "module which spying of players");
    }
    private final Setting<List<String>> nicknamesSg = sgGeneral.add(new StringListSetting.Builder()
        .name("nicknames")
        .description("list of nicknames")
        .onChanged(resultedList -> {
            spiedPlayers.removeAll(spiedPlayers.stream().filter(s -> !resultedList.contains(s)).toList());
            spiedPlayers.addAll(resultedList.stream().filter(s -> !spiedPlayers.contains(s)).toList());
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
        .description("delay in seconds")
        .max(60)
        .min(0.25)
        .defaultValue(1)
        .build()
    );


    @Override
    public void onActivate() {
        ChatUtils.sendMsg(Text.of("Trying to spy players"));
        spyingTask = executor.scheduleWithFixedDelay(() -> {
            try {
                collectPlayers();
                spyingPlayers();
            } catch (Exception e) {
                ZlpAddon.LOG.error("Error in spying task", e);
                this.toggle();
            }
        }, 0, (long) (delaySg.get() * 1000L), TimeUnit.MILLISECONDS);
    }

    @Override
    public void onDeactivate() {
        if (spyingTask != null && !spyingTask.isCancelled()) {
            spyingTask.cancel(true);
        }
        onlinePlayers.clear();
        ChatUtils.sendMsg(Text.of("EyeOfGod disabled"));
    }

    private void collectPlayers() {
        if (!this.isActive()){
            return;
        }
        try {
            ZlpMapPlayersDTO overworldPlayers = fetchPlayers("https://zlp.onl/map/maps/world/live/players.json");

            List<MinecraftPlayerModel> tmpPlayersList = overworldPlayers.getPlayers().stream()
                .map(dto -> new MinecraftPlayerModel(dto.getUuid(),
                    dto.getName(),
                    dto.getPosition(),
                    dto.getRotation(),
                    dto.isForeign() ? Dimension.Nether : Dimension.Overworld))
                .toList();

            this.playersList = new CopyOnWriteArrayList<>(tmpPlayersList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void spyingPlayers() {
        ZlpAddon.LOG.debug("semaphore.acquire");

        if (spiedPlayers.isEmpty()) {
            this.toggle();
            return;
        }
        try {
            spiedPlayers.forEach(this::processPlayer);
        } catch (Exception e) {
            ZlpAddon.LOG.error("Error spying players", e);
            this.toggle();
            throw new RuntimeException(e);
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

    private ZlpMapPlayersDTO fetchPlayers(String url) throws Exception {
        ZlpAddon.LOG.debug("Send requests");
        String json = httpClient.sendGet(url).get(); // Future<String>
        return new ObjectMapper().readValue(json, ZlpMapPlayersDTO.class);
    }

    public ConcurrentHashMap<String, MinecraftPlayerModel> getOnlinePlayers() {
        return onlinePlayers;
    }
}
