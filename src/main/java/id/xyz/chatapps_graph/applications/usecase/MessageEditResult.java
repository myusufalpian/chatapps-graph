package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Message;

public record MessageEditResult(Message message, boolean changed) {}
