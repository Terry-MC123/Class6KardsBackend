package top.terry_mc.c6be.dto;

import top.terry_mc.c6be.model.Subject;

import java.util.List;

public record StartBroadcast(List<Subject> subjects) implements Packet {
}
