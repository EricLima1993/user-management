package br.com.eric.usermanagement.config;

import br.com.eric.usermanagement.client.ViaCepProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties({ViaCepProperties.class, CorsProperties.class})
public class ApplicationConfig {}
