package com.captainalm.lib.calmnet.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class provides a Network Output stream for either {@link Socket}s or {@link DatagramSocket}s.
 *
 * @author Captain ALM
 */
public class NetworkOutputStream extends OutputStream {
    protected boolean closed = false;
    protected Socket socket;
    protected OutputStream socketStream;
    protected DatagramSocket dsocket;
    protected byte[] dsocketBuffer;
    protected int dsocketBufferIndex = 0;
    protected InetAddress dAddress;
    protected int dPort = -1;

    /**
     * Constructs a new NetworkOutputStream with the specified {@link Socket}.
     *
     * @param socketIn The socket to use.
     * @throws NullPointerException socketIn is null.
     */
    public NetworkOutputStream(Socket socketIn) {
        if (socketIn == null) throw new NullPointerException("socketIn is null");
        socket = socketIn;
    }

    /**
     * Constructs a new NetworkOutputStream with the specified {@link DatagramSocket}.
     *
     * @param socketIn The datagram socket to use.
     * @throws NullPointerException socketIn is null.
     */
    public NetworkOutputStream(DatagramSocket socketIn) {
        if (socketIn == null) throw new NullPointerException("socketIn is null");
        dsocket = socketIn;
    }

    /**
     * Constructs a new NetworkOutputStream with the specified {@link DatagramSocket} and datagram buffer size.
     *
     * @param socketIn The datagram socket to use.
     * @param size The size of the buffer.
     * @throws NullPointerException socketIn is null.
     * @throws IllegalArgumentException size is less than 1 or greater than 65535.
     */
    public NetworkOutputStream(DatagramSocket socketIn, int size) {
        this(socketIn);
        try {
            setDatagramBufferSize(size);
        } catch (IOException e) {
        }
    }

    /**
     * Constructs a new NetworkOutputStream with the specified {@link DatagramSocket}, datagram buffer size, {@link InetAddress} target and port target.
     *
     * @param socketIn The datagram socket to use.
     * @param size The size of the buffer.
     * @param address The target address to set to.
     * @param port The target port to set to.
     * @throws NullPointerException socketIn or address is null.
     * @throws IllegalArgumentException size is less than 1 or greater than 65535 or port is less than 0 or greater than 65535.
     */
    public NetworkOutputStream(DatagramSocket socketIn, int size, InetAddress address, int port) {
        this(socketIn, size);
        try {
            setDatagramTarget(address, port);
        } catch (IOException e) {
        }
    }

    /**
     * Gets if the {@link #setDatagramBufferSize(int)} can be used.
     *
     * @return If the datagram buffer size can be set.
     */
    public boolean canDatagramBufferBeSet() {
        return dsocket != null && dsocketBufferIndex == 0;
    }

    /**
     * Sets the buffer size for sending datagrams.
     *
     * @param size The size to set to.
     * @throws IllegalArgumentException size is less than 1 or greater than 65535.
     * @throws IOException a datagram socket is not in use, buffer index is not null or the stream is closed.
     */
    public void setDatagramBufferSize(int size) throws IOException {
        if (closed) throw new IOException("stream closed");
        if (dsocket == null) throw new IOException("not using a datagram socket");
        if (size < 1) throw new IllegalArgumentException("size is less than 1");
        if (size > 65535) throw new IllegalArgumentException("size is greater than 65535");
        if (dsocketBufferIndex != 0) throw new IOException("buffer index is not 0");
        dsocketBuffer = new byte[size];
    }

    /**
     * Sets the datagram target {@link InetAddress} and port.
     *
     * @param address The address to set to.
     * @param port The port to set to.
     * @throws NullPointerException address is null.
     * @throws IllegalArgumentException port is less than 0 or greater than 65535.
     * @throws IOException a datagram socket is not in use or the stream is closed.
     */
    public void setDatagramTarget(InetAddress address, int port) throws IOException {
        if (closed) throw new IOException("stream closed");
        if (dsocket == null) throw new IOException("not using a datagram socket");
        if (address == null) throw new NullPointerException("address is null");
        if (port < 0) throw new IllegalArgumentException("port is less than 0");
        if (port > 65535) throw new IllegalArgumentException("port is greater than 65535");
        dAddress = address;
        dPort = port;
    }

    /**
     * Writes the specified byte to this output stream. The general
     * contract for <code>write</code> is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument <code>b</code>. The 24
     * high-order bits of <code>b</code> are ignored.
     *
     * @param b the <code>byte</code>.
     * @throws IOException if an I/O error occurs. In particular,
     *                     an <code>IOException</code> will be thrown if the
     *                     output stream has been closed.
     */
    @Override
    public void write(int b) throws IOException {
        if (closed) throw new IOException("stream closed");
        if (socket == null) {
            if (dsocketBuffer != null) {
                dsocketBuffer[dsocketBufferIndex++] = (byte) b;
                if (dsocketBufferIndex >= dsocketBuffer.length) {
                    sendDatagramData(dsocketBuffer, dsocketBuffer.length);
                    dsocketBufferIndex = 0;
                    dsocketBuffer = new byte[dsocketBuffer.length];
                }
            } else {
                throw new IOException("null datagram buffer");
            }
        } else {
            if (socketStream == null) socketStream = socket.getOutputStream();
            socketStream.write(b);
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
     * Flushes this output stream and forces any buffered output bytes
     * to be written out. The general contract of <code>flush</code> is
     * that calling it is an indication that, if any bytes previously
     * written have been buffered by the implementation of the output
     * stream, such bytes should immediately be written to their
     * intended destination.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        if (closed) throw new IOException("stream closed");
        if (dsocket == null) {
            if (socketStream == null) socketStream = socket.getOutputStream();
            socketStream.flush();
        } else {
            sendDatagramData(dsocketBuffer, dsocketBufferIndex);
            dsocketBufferIndex = 0;
            dsocketBuffer = (dsocketBuffer == null) ? null : new byte[dsocketBuffer.length];
        }
    }

    protected void sendDatagramData(byte[] data, int length) throws IOException {
        if (data == null || length < 1) return;
        if (dAddress == null || dPort < 0 || dPort > 65535) throw new IOException("no datagram target parameters set");
        DatagramPacket packet = new DatagramPacket(data, length, dAddress, dPort);
        dsocket.send(packet);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of <code>close</code>
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened. The underlying socket is closed.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if (socket == null) {
                dsocketBuffer = null;
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
