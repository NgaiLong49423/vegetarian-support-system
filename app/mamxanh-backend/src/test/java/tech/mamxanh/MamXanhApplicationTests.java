package tech.mamxanh;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.nutrition.repository.NutritionProfileRepository;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class MamXanhApplicationTests {
    @Autowired
    private ConfigurableApplicationContext context;

    @MockitoBean
    private NutritionProfileRepository nutritionProfileRepository;

    @Test
    void mainStartsApplicationContext() {
        assertTrue(context.isActive(), "The application entry point must start a working context");
    }

}
