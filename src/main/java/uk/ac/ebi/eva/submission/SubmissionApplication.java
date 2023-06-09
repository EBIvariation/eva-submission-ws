package uk.ac.ebi.eva.submission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubmissionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubmissionApplication.class, args);
    }

}
