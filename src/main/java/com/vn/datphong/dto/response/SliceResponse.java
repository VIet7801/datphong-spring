package com.vn.datphong.dto.response;

import org.springframework.data.domain.Slice;
import java.util.List;
import java.util.function.Function;

public record SliceResponse<T>(int page, int size, boolean hasNext, List<T> items) {
    public static <E, T> SliceResponse<T> from(Slice<E> result, Function<E, T> mapper) {
        return new SliceResponse<>(result.getNumber(), result.getSize(), result.hasNext(),
                result.getContent().stream().map(mapper).toList());
    }
}
