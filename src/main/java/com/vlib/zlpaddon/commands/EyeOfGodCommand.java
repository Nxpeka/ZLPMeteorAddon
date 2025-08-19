package com.vlib.zlpaddon.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.vlib.zlpaddon.dto.request.ZlpMapPlayersDTO;
import com.vlib.zlpaddon.exceptions.FetchException;
import com.vlib.zlpaddon.exceptions.PlayerAlreadyInListException;
import com.vlib.zlpaddon.exceptions.PlayerNotFoundException;
import com.vlib.zlpaddon.models.MinecraftPlayerModel;
import com.vlib.zlpaddon.modules.EyeOfGodModule;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

import java.util.List;

public class EyeOfGodCommand extends Command {

    public EyeOfGodCommand() {
        super("eyeofgod", "commands for eyeofgod", "eye");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        Modules modules = Modules.get();
        EyeOfGodModule eyeOfGodModule = modules.get(EyeOfGodModule.class);

        builder.then(literal("locate")
            .then(argument("player", PlayerListEntryArgumentType.create())
                .executes(context -> {
                    String nickname = PlayerListEntryArgumentType.get(context).getProfile().getName();
                    try {
                        ZlpMapPlayersDTO.PositionDTO positionDTO = eyeOfGodModule.locatePlayer(nickname);
                        info("Position of (highlight)%s(default) is %s",
                            nickname,
                            String.format("%.0f, %.0f, %.0f", positionDTO.getX(), positionDTO.getY(), positionDTO.getZ())
                        );
                    } catch (PlayerNotFoundException e) {
                        error("Failed to find player " + nickname);
                    } catch (FetchException e) {
                        error("Failed to fetch players positions т-т");
                    }
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("near")
            .then(argument("radius", DoubleArgumentType.doubleArg(0, 10000))
                .executes(context -> {
                    double radius = DoubleArgumentType.getDouble(context, "radius");

                    try {
                        List<MinecraftPlayerModel> playersInRadius = eyeOfGodModule.spyNearPlayer(radius);
                        info("Players added to spying: " + listOfNamesToString(playersInRadius));
                    } catch (PlayerNotFoundException e) {
                        error("No player found in radius %s", radius);
                    }
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("spy")
            .then(literal("add")
                .then(argument("player", PlayerListEntryArgumentType.create())
                    .executes(context -> {
                        String nickname = PlayerListEntryArgumentType.get(context).getProfile().getName();
                        try {
                            eyeOfGodModule.addPlayerToSpying(nickname);
                            info("Added player (highlight)%s(default)", nickname);
                        } catch (PlayerAlreadyInListException e) {
                            error("Player (highlight)%s(default) already in list", nickname);
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );

        builder.then(literal("spy")
            .then(literal("remove")
                .then(argument("player", PlayerListEntryArgumentType.create())
                    .executes(context -> {
                        String nickname = PlayerListEntryArgumentType.get(context).getProfile().getName();
                        try {
                            eyeOfGodModule.removePlayerFromSpying(nickname);
                            info("Removed player (highlight)%s(default)", nickname);
                        } catch (PlayerNotFoundException e) {
                            error("Player (highlight)%s(default) not found in list", nickname);
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }

    private String listOfNamesToString(List<MinecraftPlayerModel> players) {
        StringBuilder builder = new StringBuilder();
        for (MinecraftPlayerModel player : players) {
            builder.append(String.format("(highlight)%s(default)", player.getName())).append(", ");
        }

        return builder.toString();
    }
}
