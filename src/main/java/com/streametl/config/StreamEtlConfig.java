package com.streametl.config;

import com.streametl.delta.DeltaAnalyzer;
import com.streametl.datalake.LayerProcessor;
import com.streametl.violation.ViolationDetector;
import com.streametl.window.WindowAggregator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EtlProperties.class)
public class StreamEtlConfig {

    @Bean
    public ViolationDetector violationDetector() {
        return new ViolationDetector();
    }

    @Bean
    public DeltaAnalyzer deltaAnalyzer() {
        return new DeltaAnalyzer();
    }

    @Bean
    public LayerProcessor layerProcessor() {
        return new LayerProcessor();
    }

    @Bean
    public WindowAggregator windowAggregator() {
        return new WindowAggregator();
    }
}
