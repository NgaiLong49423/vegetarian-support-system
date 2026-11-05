package tech.mamxanh.admin.event;

import java.util.List;

public record AiConfigurationEmailEvent(List<String> changes) {
    public AiConfigurationEmailEvent { changes = List.copyOf(changes); }
}
