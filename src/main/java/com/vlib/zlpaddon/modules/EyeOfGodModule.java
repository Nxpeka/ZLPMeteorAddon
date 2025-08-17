package com.vlib.zlpaddon.modules;

import com.vlib.zlpaddon.ZlpAddon;
import com.vlib.zlpaddon.dto.request.ZlpMapPlayersDTO;
import com.vlib.zlpaddon.http.HttpClient;
import com.vlib.zlpaddon.models.MinecraftPlayerModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class EyeOfGodModule extends Module {

    private final SettingGroup sgGeneral = this.settings.createGroup("General");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient httpClient = HttpClient.getInstance();
    private final ConcurrentHashMap<String, MinecraftPlayerModel> onlinePlayers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> offlinePlayers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> spiedPlayers = new CopyOnWriteArrayList<>();
    private Long lastRequestTime = -1L;
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

    private final Setting<Double> delaySg = sgGeneral.add(new DoubleSetting.Builder()
        .name("delay")
        .description("delay in seconds")
        .max(60)
        .min(1)
        .defaultValue(1)
        .build()
    );


    @Override
    public void onActivate() {
        try {
            semaphore.acquire();
        }
        catch (InterruptedException e) {
            ZlpAddon.LOG.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        executor.execute(this::spyingPlayers);
    }

    @Override
    public void onDeactivate() {
        semaphore.release();
        onlinePlayers.clear();
        offlinePlayers.clear();
        ChatUtils.sendMsg(Text.of("EyeOfGod disabled"));
    }


    private void spyingPlayers() {
        ChatUtils.sendMsg(Text.of("Trying to spy players"));
        ZlpAddon.LOG.debug("semaphore.acquire");

        while (this.isActive()) {
            if (spiedPlayers.isEmpty()) {
                this.toggle();
                return;
            }
            try {
                if (canMakeRequest()) {
                    updateLastRequestTime();
                } else {
                    ZlpAddon.LOG.debug("Waiting delay");
                    Thread.sleep((long) (delaySg.get() * 1000L));
                }

                ZlpMapPlayersDTO overworldPlayers = fetchPlayers("https://zlp.onl/map/maps/world/live/players.json");
                ZlpMapPlayersDTO netherPlayers = fetchPlayers("https://zlp.onl/map/maps/world_nether/live/players.json");

                spiedPlayers.forEach(nickname -> processPlayer(nickname, overworldPlayers, netherPlayers));

            } catch (Exception e) {
                ZlpAddon.LOG.error("Error spying players", e);
                this.toggle();
                throw new RuntimeException(e);
            }
        }
    }

    private void processPlayer(String nickname, ZlpMapPlayersDTO overworldPlayers,ZlpMapPlayersDTO netherPlayers) {
        if (nickname.trim().isEmpty()) {
            return;
        }
        Predicate<ZlpMapPlayersDTO.ZlpMapPlayerDTO> equalsName = player -> player.getName().equalsIgnoreCase(nickname);

        Optional<ZlpMapPlayersDTO.ZlpMapPlayerDTO> overworldPlayer = overworldPlayers.getPlayers().stream()
            .filter(equalsName)
            .filter(player -> !player.isForeign())
            .findFirst();
        Optional<ZlpMapPlayersDTO.ZlpMapPlayerDTO> netherPlayer = netherPlayers.getPlayers().stream()
            .filter(equalsName)
            .filter(player -> !player.isForeign())
            .findFirst();

        if (overworldPlayer.isPresent()) {
            if (offlinePlayers.contains(nickname)) {
                offlinePlayers.remove(nickname);
                ChatUtils.info("Player (highlight)%s(default) joined to the server", nickname);
            }
            var pl = overworldPlayer.get();
            onlinePlayers.put(nickname, new MinecraftPlayerModel(
                pl.getUuid(),
                pl.getName(),
                pl.getPosition(),
                pl.getRotation(),
                Dimension.Overworld)
            );
        }
        else if (netherPlayer.isPresent()) {
            if (offlinePlayers.contains(nickname)) {
                offlinePlayers.remove(nickname);
                ChatUtils.info("Player (highlight)%s(default) joined to the server", nickname);
            }
            var pl = netherPlayer.get();
            onlinePlayers.put(nickname, new MinecraftPlayerModel(
                pl.getUuid(),
                pl.getName(),
                pl.getPosition(),
                pl.getRotation(),
                Dimension.Nether)
            );
        }
        else {
            if (!offlinePlayers.contains(nickname)) {
                offlinePlayers.add(nickname);
                if (onlinePlayers.containsKey(nickname)) {
                    ChatUtils.warning("Player (highlight)%s(default) logout", nickname);
                    onlinePlayers.remove(nickname);
                    return;
                }
                ChatUtils.error("Player (highlight)%s(default) not found", nickname);
            }
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

    private boolean canMakeRequest() {
        long currentTime = System.currentTimeMillis();
        long diffMillis = currentTime - lastRequestTime;
        long delayMillis = (long) (delaySg.get() * 1000L);

        return diffMillis >= delayMillis;
    }
    private void updateLastRequestTime() {
        this.lastRequestTime = System.currentTimeMillis();
    }
}
