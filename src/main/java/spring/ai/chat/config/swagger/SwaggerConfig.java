package spring.ai.chat.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAiApi openAiApi(){
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public OpenAPI springOpenAPI(){

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Spring AI API")
                                .version("1.0")
                                .description("Spring AI 활용")
                );
    }
}
