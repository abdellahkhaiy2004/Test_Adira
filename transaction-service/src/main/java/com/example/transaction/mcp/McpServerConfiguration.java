package com.example.transaction.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publishes {@link TransactionTools} as MCP tools. */
@Configuration
public class McpServerConfiguration {

    @Bean
    public ToolCallbackProvider transactionToolCallbackProvider(TransactionTools transactionTools) {
        return MethodToolCallbackProvider.builder().toolObjects(transactionTools).build();
    }
}
