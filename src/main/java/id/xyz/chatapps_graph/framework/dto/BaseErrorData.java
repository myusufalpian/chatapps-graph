package id.xyz.chatapps_graph.framework.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public record BaseErrorData(String key, String detail, Integer status,
                            List<ValidationData> validation) implements ErrorData {

}
