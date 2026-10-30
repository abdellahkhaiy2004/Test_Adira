package com.example.account.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publishes {@link AccountTools} as MCP tools. */
@Configuration
public class McpServerConfiguration {

    @Bean
    public ToolCallbackProvider accountToolCallbackProvider(AccountTools accountTools) {
        return MethodToolCallbackProvider.builder().toolObjects(accountTools).build();
    }
}
