package uk.ac.ebi.eva.submission.controller.swagger;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Autowired
    private SwaggerInterceptAdapter interceptAdapter;

    /**
     * Advertise a relative server URL so Swagger UI calls the API using the scheme and host the page was loaded from.
     */
    @Bean
    public OpenAPI evaSubmissionOpenAPI(@Value("${server.servlet.context-path:/}") String contextPath) {
        return new OpenAPI()
                .servers(List.of(new Server().url(contextPath)))
                .info(new Info().title("EVA Submission Webservices")
                              .description("A Service that allows users to submit to the EVA")
                              .version("v1.0")
                              .license(new License().name("Apache-2.0").url("https://raw.githubusercontent.com/EBIvariation/eva-submission-ws/main/LICENSE"))
                              .contact(new Contact().name("GitHub Repository").url("https://github.com/EBIvariation/eva-submission-ws").email(null)));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptAdapter);
    }
}
