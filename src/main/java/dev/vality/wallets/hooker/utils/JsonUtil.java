package dev.vality.wallets.hooker.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.io.IOException;

@UtilityClass
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> String toString(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't convert object to string: ", e);
        }
    }


    public static <T> T toObject(String stringObject, Class<T> type) {
        try {
            return objectMapper.readValue(stringObject, type);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't convert json string to object: ", e);
        }
    }

    public static <T> T toObject(byte[] byteObject, Class<T> type) {
        try {
            return objectMapper.readValue(byteObject, type);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't convert byte[] to object: ", e);
        }
    }
}