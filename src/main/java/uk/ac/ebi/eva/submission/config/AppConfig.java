package uk.ac.ebi.eva.submission.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${eva.email.server}")
    private String emailServer;

    @Value("${eva.email.port}")
    private int emailPort;

    @Bean
    public JavaMailSender javaMailService() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(emailServer);
        javaMailSender.setPort(emailPort);
        return javaMailSender;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
