package com.vn.datphong.security;

import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class JsonErrorWriter {
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    private JsonErrorWriter() { }

    public static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON.writeValue(response.getOutputStream(), ApiResponse.error(message, new ApiError(code, null)));
    }
}
