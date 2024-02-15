package uk.ac.ebi.eva.submission;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubmissionApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SubmissionApplication.class, args);
    }

    @Bean
    public OpenAPI contentApi() {
        return new OpenAPI().info(new Info().title("EVA Submission Webservices").version("1.0"));
    }

}
