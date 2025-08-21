package com.dlyk.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** copy by ShigureYukina,from 2025/8/20-下午10:16 */
public class CacheUtils {
    public static <T> T getCacheData(Supplier<T> cacheSelector, Supplier<T> databaseSelector, Consumer<T> cacheSave) {
        T data = cacheSelector.get();
        if (Objects.isNull(data)) {
            data = databaseSelector.get();
            if (!Objects.isNull(data)) {
                cacheSave.accept(data);
            }
        }
        return data;
    }
}
