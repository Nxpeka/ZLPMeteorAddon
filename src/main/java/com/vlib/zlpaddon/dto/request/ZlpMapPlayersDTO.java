package com.vlib.zlpaddon.dto.request;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
public class ZlpMapPlayersDTO {
    List<ZlpMapPlayerDTO> players;

    @JsonCreator
    public ZlpMapPlayersDTO(@JsonProperty("players") List<ZlpMapPlayerDTO> players) {
        this.players = players;
    }

    @Value
    @EqualsAndHashCode(callSuper=false, exclude = {"foreign", "position", "rotation"} )
    public static class ZlpMapPlayerDTO {
        String uuid;
        String name;
        boolean foreign;
        PositionDTO position;
        RotationDTO rotation;

        @JsonCreator
        public ZlpMapPlayerDTO(@JsonProperty("uuid") String uuid,
                               @JsonProperty("name") String name,
                               @JsonProperty("foreign") boolean foreign,
                               @JsonProperty("position") PositionDTO position,
                               @JsonProperty("rotation") RotationDTO rotation) {
            this.uuid = uuid;
            this.name = name;
            this.foreign = foreign;
            this.position = position;
            this.rotation = rotation;
        }


    }

    @Value
    public static class PositionDTO {
        double x;
        double y;
        double z;

        @JsonCreator
        public PositionDTO(@JsonProperty("x") double x,
                           @JsonProperty("y") double y,
                           @JsonProperty("z") double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return "{x=" + x +
                ", y=" + y +
                ", z=" + z + "}";
        }
    }

    @Value
    public static class RotationDTO {
        double pitch;
        double yaw;
        double roll;

        @JsonCreator
        public RotationDTO(@JsonProperty("pitch") double pitch,
                           @JsonProperty("yaw") double yaw,
                           @JsonProperty("roll") double roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }
    }
}


