package com.captainalm.lib.calmnet.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class provides a Network Input stream for either {@link Socket}s or {@link DatagramSocket}s.
 *
 * @author Captain ALM
 */
public class NetworkInputStream extends InputStream {
    protected boolean closed = false;
    protected Socket socket;
    protected InputStream socketStream;
    protected DatagramSocket dsocket;
    protected DatagramPacket dsocketPacket;
    protected int dsocketPacketIndex = 0;
    protected InetAddress dAddress;
    protected int dPort = -1;
    protected int dlen = 0;

    /**
     * Constructs a new NetworkInputStream with the specified {@link Socket}.
     *
     * @param socketIn The socket to use.
     * @throws NullPointerException socketIn is null.
     */
    public NetworkInputStream(Socket socketIn) {
        if (socketIn == null) throw new NullPointerException("socketIn is null");
        socket = socketIn;
    }

    /**
     * Constructs a new NetworkInputStream with the specified {@link DatagramSocket}.
     *
     * @param socketIn The datagram socket to use.
     * @throws NullPointerException socketIn is null.
     */
    public NetworkInputStream(DatagramSocket socketIn) {
        if (socketIn == null) throw new NullPointerException("socketIn is null");
        dsocket = socketIn;
    }

    protected void assureDSocketPacket() throws IOException {
        if (dsocketPacket == null) {
            dsocketPacket = new DatagramPacket(new byte[65535], 65535);
            dsocket.receive(dsocketPacket);
            dAddress = dsocketPacket.getAddress();
            dPort = dsocketPacket.getPort();
            dsocketPacketIndex = 0;
            dlen = dsocketPacket.getLength();
            if (dlen < 1) dsocketPacket = null;
        }
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("stream closed");
        if (socket == null) {
            int toret = -1;
            assureDSocketPacket();
            if (dsocketPacket != null) {
                toret = dsocketPacket.getData()[dsocketPacketIndex++] & 0xff;
                if (dsocketPacketIndex >= dlen) dsocketPacket = null;
            }
            return toret;
        } else {
            if (socketStream == null) socketStream = socket.getInputStream();
            return socketStream.read();
        }
    }

    /**
     * Gets the current {@link InetAddress} of the stream.
     * Can be null.
     *
     * @return The address.
     */
    public InetAddress getAddress() {
        if (dsocket == null) {
            return socket.getInetAddress();
        } else {
            return (dAddress == null) ? dsocket.getInetAddress() : dAddress;
        }
    }

    /**
     * Gets the current port of the stream.
     * Can be -1.
     *
     * @return The current port.
     */
    public Integer getPort() {
        if (dsocket == null) {
            return socket.getPort();
        } else {
            return (dPort == -1) ? dsocket.getPort() : dPort;
        }
    }

    /**
     * Gets the local {@link InetAddress} of the stream.
     * Can be null.
     *
     * @return The local address.
     */
    public InetAddress getLocalAddress() {
        if (dsocket == null) {
            return socket.getLocalAddress();
        } else {
            return dsocket.getLocalAddress();
        }
    }

    /**
     * Gets the local port of the stream.
     * Can be -1.
     *
     * @return The local port.
     */
    public Integer getLocalPort() {
        if (dsocket == null) {
            return socket.getPort();
        } else {
            return dsocket.getPort();
        }
    }

    /**
     * Gets the socket in use or null.
     *
     * @return The socket in use or null.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Sets the socket in use.
     *
     * @param socketIn The socket to now use.
     * @throws NullPointerException socketIn is null.
     * @throws IOException stream closed or not using a socket.
     */
    public void setSocket(Socket socketIn) throws IOException {
        if (closed) throw new IOException("stream closed");
        if (socket == null) throw new IOException("not using a socket");
        if (socketIn == null) throw new NullPointerException("socketIn is null");
        socket = socketIn;
        socketStream = null;
    }

    /**
     * Gets the datagram socket in use or null.
     *
     * @return The datagram socket in use or null.
     */
    public DatagramSocket getDatagramSocket() {
        return dsocket;
    }

    /**
     * Sets the datagram socket in use.
     *
     * @param socketIn The datagram socket to now use.
     * @throws NullPointerException socketIn is null.
     * @throws IOException stream closed or not using a datagram socket.
     */
    public void setDatagramSocket(DatagramSocket socketIn) throws IOException {
        if (closed) throw new IOException("stream closed");
        if (dsocket == null) throw new IOException("not using a datagram socket");
        if (socketIn == null) throw new NullPointerException("socketIn is null");
        dsocket = socketIn;
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation
     * might be the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking or {@code 0} when
     *             it reaches the end of the input stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        if (closed) throw new IOException("stream closed");
        if (dsocket == null) {
            if (socketStream == null) socketStream = socket.getInputStream();
            return socketStream.available();
        } else {
            assureDSocketPacket();
            return (dsocketPacket == null) ? 0 : dlen - dsocketPacketIndex;
        }
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream. The underlying socket is closed.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if (socket == null) {
                dsocketPacket = null;
                dAddress = null;
                dPort = -1;
                dsocket.close();
                dsocket = null;
            } else {
                socketStream = null;
                socket.close();
                socket = null;
            }
        }
    }
}
