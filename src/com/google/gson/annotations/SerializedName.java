package com.google.gson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicates this member should be serialized to JSON with the provided name
 * value as its field name.
 *
 * <p>
 * This annotation will override any {@link com.google.gson.FieldNamingPolicy}, including the
 * default field naming policy, that may have been set on the {@link com.google.gson.Gson} instance.
 * A different naming policy can set using the {@code GsonBuilder} class. See
 * {@link com.google.gson.GsonBuilder#setFieldNamingPolicy(com.google.gson.FieldNamingPolicy)} for
 * more information.
 * </p>
 *
 * <p>
 * Here is an example of how this annotation is meant to be used:
 * </p>
 * 
 * <pre>
 * public class SomeClassWithFields {
 *   @SerializedName("name") private final String someField;
 *   private final String someOtherField;
 *
 *   public SomeClassWithFields(String a, String b) {
 *     this.someField = a;
 *     this.someOtherField = b;
 *   }
 * }
 * </pre>
 *
 * <p>
 * The following shows the output that is generated when serializing an instance of the above
 * example class:
 * </p>
 * 
 * <pre>
 * SomeClassWithFields objectToSerialize = new SomeClassWithFields("a", "b");
 * Gson gson = new Gson();
 * String jsonRepresentation = gson.toJson(objectToSerialize);
 * System.out.println(jsonRepresentation);
 *
 * ===== OUTPUT =====
 * {"name":"a","someOtherField":"b"}
 * </pre>
 *
 * <p>
 * NOTE: The value you specify in this annotation must be a valid JSON field name.
 * </p>
 *
 * @see com.google.gson.FieldNamingPolicy
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SerializedName {

    /**
     * @return the desired name of the field when it is serialized
     */
    String value();
}
