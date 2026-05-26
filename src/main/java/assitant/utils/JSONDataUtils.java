package assitant.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class JSONDataUtils {

    private static ObjectMapper objectMapper;

    public JSONDataUtils(ObjectMapper objectMapper) {
        JSONDataUtils.objectMapper = objectMapper;
    }

    @SneakyThrows
    public static String formatParams(Object[] args) {
        if (args == null || args.length == 0) return "";
        Object[] filtered = java.util.stream.Stream.of(args)
                .filter(arg -> arg != null)
                .toArray();
        if (filtered.length == 0) return "无参数";
        Object toSerialize = filtered.length == 1 ? filtered[0] : filtered;
        return objectMapper.writeValueAsString(toSerialize);
    }

    @SneakyThrows
    public static String formatResult(Object result) {
        if (result == null) return "null";
        String json = objectMapper.writeValueAsString(result);
        if (json.length() > 2000) {
            json = json.substring(0, 2000) + "...(truncated)";
        }
        return json;
    }
}
