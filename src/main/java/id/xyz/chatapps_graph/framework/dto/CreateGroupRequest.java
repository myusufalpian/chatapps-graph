package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateGroupRequest(
    @NotBlank @Size(max = 255) String name,
    String description,
    @NotEmpty @Size(min = 2) List<String> participantUuids
) {}
