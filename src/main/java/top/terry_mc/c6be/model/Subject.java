package top.terry_mc.c6be.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Subject {
    CHINESE("语"), MATHS("数"), ENGLISH("英"), PHYSICS("物"), CHEMISTRY("化"), POLITICS("政"), HISTORY("史");
    @Getter
    private final String name;
}
