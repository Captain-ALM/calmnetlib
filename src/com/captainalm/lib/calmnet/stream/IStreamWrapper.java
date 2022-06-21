package com.captainalm.lib.calmnet.stream;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface provides the ability to wrap passed streams.
 *
 * @author Captain ALM
 */
public interface IStreamWrapper {
    /**
     * Wraps an {@link InputStream} returning a wrapped stream.
     *
     * @param toWrap The stream to wrap.
     * @return The wrapped stream.
     */
    InputStream wrapInputStream(InputStream toWrap);

    /**
     * Wraps an {@link OutputStream} returning the wrapped stream.
     *
     * @param toWrap The stream to wrap.
     * @return The wrapped stream.
     */
    OutputStream wrapOutputStream(OutputStream toWrap);
}
