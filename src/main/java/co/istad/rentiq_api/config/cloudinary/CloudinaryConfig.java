package co.istad.rentiq_api.config.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        validateProperties(properties);

        return new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", properties.cloudName(),
                        "api_key", properties.apiKey(),
                        "api_secret", properties.apiSecret(),
                        "secure", true
                )
        );
    }

    private void validateProperties(
            CloudinaryProperties properties
    ) {
        if (isBlank(properties.cloudName())) {
            throw new IllegalStateException(
                    "CLOUDINARY_CLOUD_NAME is not configured"
            );
        }

        if (isBlank(properties.apiKey())) {
            throw new IllegalStateException(
                    "CLOUDINARY_API_KEY is not configured"
            );
        }

        if (isBlank(properties.apiSecret())) {
            throw new IllegalStateException(
                    "CLOUDINARY_API_SECRET is not configured"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}