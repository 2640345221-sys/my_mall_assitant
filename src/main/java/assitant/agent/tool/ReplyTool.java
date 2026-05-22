package assitant.agent.tool;

import assitant.annotation.OperationLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class ReplyTool {

    @Tool(description = "默认工具。当其它所有工具都不符合用户的请求时，调用此工具自由回复。例如用户闲聊、打招呼、问'你好'等情况。将回复内容作为参数传入")
    @OperationLog(module = "Agent", type = "回复", description = "默认回复")
    public String reply(
            @ToolParam(description = "你要回复用户的内容") String text) {
        return text;
    }
}
