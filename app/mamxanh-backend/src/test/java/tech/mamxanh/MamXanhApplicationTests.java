package tech.mamxanh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.nutrition.repository.NutritionProfileRepository;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class MamXanhApplicationTests {

    @MockitoBean
    private NutritionProfileRepository nutritionProfileRepository;

    @Test
    void contextLoads() {
    }

}
