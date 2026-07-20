package com.cheat.exam.service.model;

import com.cheat.exam.common.api.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 统一模型路由器。
 *
 * 业务层只告诉它“本会话选择了哪个模型”，路由器负责找到对应 ModelClient。
 * 当前只有 StubModelClient，后续新增 OpenAI/Anthropic/Gemini Client 后无需改业务层。
 */
@Component
public class ModelClientRouter {

    private final List<ModelClient> clients;

    public ModelClientRouter(List<ModelClient> clients) {
        this.clients = clients;
    }

    public ModelClient route(ModelClientSelection selection) {
        return clients.stream()
            .filter(client -> !"STUB".equals(client.providerCode()) || "STUB".equalsIgnoreCase(selection.providerCode()))
            .filter(client -> client.supports(selection))
            .findFirst()
            .orElseThrow(() -> new ApiException(
                "MODEL_NOT_AVAILABLE",
                "No model client available for " + selection.modelCode(),
                HttpStatus.BAD_REQUEST
            ));
    }
}
