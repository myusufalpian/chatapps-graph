package id.xyz.chatapps_graph.framework.dto;

import java.util.List;

public interface ErrorData {
    String key();
    String detail();
    Integer status();
    List<ValidationData> validation();
}
