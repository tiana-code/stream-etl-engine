package com.streametl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stream-etl")
public record EtlProperties(
        ViolationProperties violation,
        WindowProperties window,
        DataLakeProperties datalake
) {
    public record ViolationProperties(String defaultSeverity) {
    }

    public record WindowProperties(int defaultSizeSeconds) {
    }

    public record DataLakeProperties(boolean autoPromote) {
    }
}
