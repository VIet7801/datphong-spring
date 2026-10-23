package com.vn.datphong;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatphongApplicationTests.NoJobs.class)
class DatphongApplicationTests {
    @Value("${local.server.port}")
    int port;

    @TestConfiguration
    static class NoJobs {
        // Validate khởi động trên DB thật nhưng không chạy job làm thay đổi dữ liệu demo.
        @Bean
        TaskScheduler taskScheduler() { return mock(TaskScheduler.class); }
    }

    @Test
    void applicationStartsAndServesSwaggerWithoutAuthentication() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        var swagger = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/swagger-ui/index.html")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, swagger.statusCode());
        assertTrue(swagger.body().contains("swagger-ui"));
        var spec = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/v3/api-docs")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, spec.statusCode());
        assertTrue(spec.body().contains("\"openapi\""));
    }

}
