package com.cheat.exam.service.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cheat.exam.common.api.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelClientRouterTests {

    @Test
    void rejectsUnavailableRealProviderInsteadOfSilentlyFallingBackToStub() {
        ModelClientRouter router = new ModelClientRouter(List.of(new StubModelClient()));

        assertThatThrownBy(() -> router.route(new ModelClientSelection(
                "openai-default",
                "OPENAI",
                true,
                false,
                "{}"
            )))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("No model client available");
    }

    @Test
    void prefersRealProviderClientBeforeStubFallback() {
        ModelClient realClient = new ModelClient() {
            @Override
            public String providerCode() {
                return "OPENAI";
            }

            @Override
            public boolean supports(ModelClientSelection selection) {
                return "OPENAI".equals(selection.providerCode());
            }

            @Override
            public ModelChatResponse chat(ModelChatRequest request) {
                throw new UnsupportedOperationException("测试只验证路由，不调用真实模型");
            }
        };
        ModelClientRouter router = new ModelClientRouter(List.of(new StubModelClient(), realClient));

        ModelClient client = router.route(new ModelClientSelection(
            "openai-default",
            "OPENAI",
            true,
            false,
            "{}"
        ));

        assertThat(client.providerCode()).isEqualTo("OPENAI");
    }
}
