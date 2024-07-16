package uk.ac.ebi.eva.submission.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public JavaMailSender javaMailService() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost("smtp.ebi.ac.uk");
        javaMailSender.setPort(25);
        return javaMailSender;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
