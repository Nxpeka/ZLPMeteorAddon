package com.vlib.zlpaddon.models;

import com.vlib.zlpaddon.dto.request.ZlpMapPlayersDTO;
import lombok.Value;
import meteordevelopment.meteorclient.utils.world.Dimension;

@Value
public class MinecraftPlayerModel {
    String uuid;
    String name;
    ZlpMapPlayersDTO.PositionDTO position;
    ZlpMapPlayersDTO.RotationDTO rotation;
    Dimension dimension;

    public MinecraftPlayerModel(String uuid,
                           String name,
                           ZlpMapPlayersDTO.PositionDTO position,
                           ZlpMapPlayersDTO.RotationDTO rotation,
                           Dimension dimension) {
        this.uuid = uuid;
        this.name = name;
        this.position = position;
        this.rotation = rotation;
        this.dimension = dimension;
    }


}
