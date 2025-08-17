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
    private final static String FOUND_MSG_PATTERN = "Player '%s' in %s on: %s";
    private final static String NOTFOUND_MSG_PATTERN = "Player '%s' not found";
    private final ConcurrentHashMap<String, MinecraftPlayerModel> players = new ConcurrentHashMap<>();
    private Long lastRequestTime = -1L;
    Semaphore semaphore = new Semaphore(1);


    public EyeOfGodModule() {
        super(ZlpAddon.CATEGORY, "eyeofgod", "module which spying of players");
    }
    private final Setting<List<String>> nicknamesSg = sgGeneral.add(new StringListSetting.Builder()
        .name("nicknames")
        .description("list of nicknames")
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
        players.clear();
        ChatUtils.sendMsg(Text.of("EyeOfGod disabled"));
    }


    private void spyingPlayers() {
        ChatUtils.sendMsg(Text.of("Trying to spy players"));
        ZlpAddon.LOG.debug("semaphore.acquire");

        while (this.isActive()) {
            List<String> nicknamesList = this.nicknamesSg.get();
            if (nicknamesList.isEmpty()) {
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
                ZlpAddon.LOG.debug("Send requests");
                Future<String> worldReq = httpClient.sendGet("https://zlp.onl/map/maps/world/live/players.json");
                Future<String> netherReq = httpClient.sendGet("https://zlp.onl/map/maps/world_nether/live/players.json");

                ZlpMapPlayersDTO worldZlpMapPlayersDto = new ObjectMapper().readValue(worldReq.get(), ZlpMapPlayersDTO.class);
                ZlpMapPlayersDTO netherZlpMapPlayersDto = new ObjectMapper().readValue(netherReq.get(), ZlpMapPlayersDTO.class);

                for (int i = 0; i < nicknamesList.size(); i++) {
                    if(nicknamesList.isEmpty()){
                        return;
                    }
                    String nickname = nicknamesList.get(i);
                    Predicate<ZlpMapPlayersDTO.ZlpMapPlayerDTO> equalsName = player -> player.getName().toLowerCase().equals(nickname.toLowerCase());
                    Predicate<ZlpMapPlayersDTO.ZlpMapPlayerDTO> isInDimension = player -> !player.isForeign();

                    Optional<ZlpMapPlayersDTO.ZlpMapPlayerDTO> worldPlayer = worldZlpMapPlayersDto.getPlayers().stream()
                        .filter(equalsName)
                        .filter(isInDimension)
                        .findFirst();
                    Optional<ZlpMapPlayersDTO.ZlpMapPlayerDTO> netherPlayer = netherZlpMapPlayersDto.getPlayers().stream()
                        .filter(equalsName)
                        .filter(isInDimension)
                        .findFirst();

                    if (worldPlayer.isPresent()) {
                        var pl = worldPlayer.get();
                        players.put(nickname, new MinecraftPlayerModel(pl.getUuid(), pl.getName(), pl.getPosition(), pl.getRotation(), Dimension.Overworld));
                    } else if (netherPlayer.isPresent()) {
                        var pl = netherPlayer.get();
                        players.put(nickname, new MinecraftPlayerModel(pl.getUuid(), pl.getName(), pl.getPosition(), pl.getRotation(), Dimension.Nether));
                    } else {
                        ChatUtils.error("Player '%s' not found", nickname);
                        nicknamesList.remove(nickname);
                    }
                }
            } catch (Exception e) {
                ZlpAddon.LOG.error(e.getMessage());
                e.printStackTrace();
                this.toggle();
                throw new RuntimeException(e);
            }
        }
    }

    public ConcurrentHashMap<String, MinecraftPlayerModel> getPlayers() {
        return players;
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
