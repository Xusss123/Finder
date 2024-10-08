package karm.van.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({CommentMicroServiceProperties.class,ImageMicroServiceProperties.class})
@Configuration
public class AdsConfiguration {
}
