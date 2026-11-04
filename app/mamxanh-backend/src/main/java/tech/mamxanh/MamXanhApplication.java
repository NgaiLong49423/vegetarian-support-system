package tech.mamxanh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MamXanhApplication {

    public static void main(String[] args) {
        if (System.getProperty("ADK_CAPTURE_MESSAGE_CONTENT_IN_SPANS") == null
                && System.getenv("ADK_CAPTURE_MESSAGE_CONTENT_IN_SPANS") == null) {
            System.setProperty("ADK_CAPTURE_MESSAGE_CONTENT_IN_SPANS", "false");
        }
        SpringApplication.run(MamXanhApplication.class, args);
    }

}
