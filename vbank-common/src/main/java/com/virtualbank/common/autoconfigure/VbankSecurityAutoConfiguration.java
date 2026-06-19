package com.virtualbank.common.autoconfigure;

import com.virtualbank.common.security.ResourceServerSecurity;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Applies the default zero-trust resource-server security to servlet services.
 * A service that declares its own SecurityFilterChain (for example user-service,
 * which needs public register/login endpoints) overrides the default.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
@Import(ResourceServerSecurity.class)
public class VbankSecurityAutoConfiguration {
}
