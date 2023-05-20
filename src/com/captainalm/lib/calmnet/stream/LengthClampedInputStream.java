package com.captainalm.lib.calmnet.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides the ability to limit the number of bytes read from the underlying stream.
 * When the limit is reached, this class considers that state as end of stream.
 *
 * @author Captain ALM
 */
public class LengthClampedInputStream extends FilterInputStream {
    protected boolean closed;
    protected int clampedLength;
    protected int markReadLimit;
    protected int markResetLength;

    /**
     * Creates a LengthClampedInputStream with the specified {@link InputStream}
     * and the maximum number of bytes that can be read from the stream.
     *
     * @param inputStream The input stream to clamp.
     * @param length The maximum number of bytes that can be read before end of stream is reached.
     * @throws NullPointerException inputStream is null.
     * @throws IllegalArgumentException length is less than 0.
     */
    public LengthClampedInputStream(InputStream inputStream, int length) {
        super(inputStream);
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (length < 0) throw new IllegalArgumentException("length is less than 0");
        clampedLength = length;
    }

    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned. This method blocks until input data
     * is available, the end of the stream is detected, or an exception
     * is thrown.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("stream closed");
        if (clampedLength > 0) clampedLength--; else return -1;
        decrementMarkResetLength();
        return super.read();
    }

    protected synchronized void decrementMarkResetLength() {
        if (markResetLength >= 0) markResetLength--;
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * caller of a method for this input stream. The next caller might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        if (closed) throw new IOException("stream closed");
        return Math.min(super.available(), clampedLength);
    }

    /**
     * Marks the current position in this input stream. A subsequent
     * call to the <code>reset</code> method repositions this stream at
     * the last marked position so that subsequent reads re-read the same bytes.
     * <p>
     * The <code>readlimit</code> argument tells this input stream to
     * allow that many bytes to be read before the mark position gets
     * invalidated.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     */
    @Override
    public synchronized void mark(int readlimit) {
        if (super.markSupported()) {
            super.mark(readlimit);
            markReadLimit = readlimit;
            markResetLength = readlimit;
        }
    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     * <p>
     * Stream marks are intended to be used in
     * situations where you need to read ahead a little to see what's in
     * the stream. Often this is most easily done by invoking some
     * general parser. If the stream is of the type handled by the
     * parse, it just chugs along happily. If the stream is not of
     * that type, the parser should toss an exception when it fails.
     * If this happens within readlimit bytes, it allows the outer
     * code to reset the stream and try another parser.
     *
     * @exception  IOException  if the stream has not been marked, if the
     *               mark has been invalidated or marking is not supported.
     */
    @Override
    public synchronized void reset() throws IOException {
        if (closed) return;
        super.reset();
        if (markResetLength >= 0) clampedLength += (markReadLimit - markResetLength);
        markResetLength = -1;
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (!closed) closed = true;
        super.close();
    }

    /**
     * Sets a new clamped length value.
     * This is the maximum number of bytes that can be read from the stream.
     *
     * @param clampedLength The new clamped length value.
     * @throws IllegalArgumentException clampedLength is less than 0.
     */
    public synchronized void setClampedLength(int clampedLength) {
        if (clampedLength < 0) throw new IllegalArgumentException("clampedLength is less than 0");
        if (closed) return;
        this.clampedLength = clampedLength;
    }
}
