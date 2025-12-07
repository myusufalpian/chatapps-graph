package id.xyz.chatapps_graph.framework.dto;

public record BaseResponse<T> (T data, Metadata metadata) {
}
