package karm.van.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(AuthenticationMicroServiceProperties.class)
@Configuration
public class CommentConfiguration {
}
