package tech.mamxanh.common.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the version-pinned Scalar browser entry point at a stable Backend URL. */
@RestController
public class ScalarUiController {

    @GetMapping(value = "/scalar", produces = MediaType.TEXT_HTML_VALUE)
    public Resource scalarReference() {
        return new ClassPathResource("static/scalar/index.html");
    }
}
