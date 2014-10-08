package com.google.gson;

import java.lang.reflect.Type;

/**
 * Context for deserialization that is passed to a custom deserializer during invocation of its
 * {@link JsonDeserializer#deserialize(JsonElement, Type, JsonDeserializationContext)} method.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public interface JsonDeserializationContext {

    /**
     * Invokes default deserialization on the specified object. It should never be invoked on the
     * element received as a parameter of the
     * {@link JsonDeserializer#deserialize(JsonElement, Type, JsonDeserializationContext)} method.
     * Doing so will result in an infinite loop since Gson will in-turn call the custom deserializer
     * again.
     *
     * @param json the parse tree.
     * @param typeOfT type of the expected return value.
     * @param <T> The type of the deserialized object.
     * @return An object of type typeOfT.
     * @throws JsonParseException if the parse tree does not contain expected data.
     */
    public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException;
}
