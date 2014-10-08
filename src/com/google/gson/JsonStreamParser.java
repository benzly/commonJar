package com.google.gson;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

/**
 * A streaming parser that allows reading of multiple {@link JsonElement}s from the specified reader
 * asynchronously.
 * 
 * <p>
 * This class is conditionally thread-safe (see Item 70, Effective Java second edition). To properly
 * use this class across multiple threads, you will need to add some external synchronization. For
 * example:
 * 
 * <pre>
 * JsonStreamParser parser = new JsonStreamParser("['first'] {'second':10} 'third'");
 * JsonElement element;
 * synchronized (parser) {  // synchronize on an object shared by threads
 *   if (parser.hasNext()) {
 *     element = parser.next();
 *   }
 * }
 * </pre>
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @since 1.4
 */
public final class JsonStreamParser implements Iterator<JsonElement> {
    private final JsonReader parser;
    private final Object lock;

    /**
     * @param json The string containing JSON elements concatenated to each other.
     * @since 1.4
     */
    public JsonStreamParser(String json) {
        this(new StringReader(json));
    }

    /**
     * @param reader The data stream containing JSON elements concatenated to each other.
     * @since 1.4
     */
    public JsonStreamParser(Reader reader) {
        parser = new JsonReader(reader);
        parser.setLenient(true);
        lock = new Object();
    }

    /**
     * Returns the next available {@link JsonElement} on the reader. Null if none available.
     * 
     * @return the next available {@link JsonElement} on the reader. Null if none available.
     * @throws JsonParseException if the incoming stream is malformed JSON.
     * @since 1.4
     */
    @Override
    public JsonElement next() throws JsonParseException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            return Streams.parse(parser);
        } catch (StackOverflowError e) {
            throw new JsonParseException("Failed parsing JSON source to Json", e);
        } catch (OutOfMemoryError e) {
            throw new JsonParseException("Failed parsing JSON source to Json", e);
        } catch (JsonParseException e) {
            throw e.getCause() instanceof EOFException ? new NoSuchElementException() : e;
        }
    }

    /**
     * Returns true if a {@link JsonElement} is available on the input for consumption
     * 
     * @return true if a {@link JsonElement} is available on the input, false otherwise
     * @since 1.4
     */
    @Override
    public boolean hasNext() {
        synchronized (lock) {
            try {
                return parser.peek() != JsonToken.END_DOCUMENT;
            } catch (MalformedJsonException e) {
                throw new JsonSyntaxException(e);
            } catch (IOException e) {
                throw new JsonIOException(e);
            }
        }
    }

    /**
     * This optional {@link Iterator} method is not relevant for stream parsing and hence is not
     * implemented.
     * 
     * @since 1.4
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
