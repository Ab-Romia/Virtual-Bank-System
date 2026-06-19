package com.virtualbank.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/** Base beans shared by every service that depends on vbank-common. */
@AutoConfiguration
public class VbankCommonAutoConfiguration {

    /** A single clock so time can be controlled in tests. */
    @Bean
    @ConditionalOnMissingBean
    public Clock vbankClock() {
        return Clock.systemUTC();
    }
}
