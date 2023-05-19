package com.captainalm.lib.calmnet.marshal;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.calmnet.ssl.SSLUtilities;
import com.captainalm.lib.calmnet.ssl.SSLUtilityException;
import com.captainalm.lib.calmnet.stream.NetworkInputStream;
import com.captainalm.lib.calmnet.stream.NetworkOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;

/**
 * This class provides a managed way of networking on the client side.
 * NOTE: Methods that are synchronised are used here, do NOT use instances of these classes as monitors.
 *
 * @author Captain ALM
 */
public class NetMarshalClient implements Closeable {
    protected boolean running = true;

    protected Socket socket;
    protected DatagramSocket dsocket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected InputStream rootInputStream;
    protected OutputStream rootOutputStream;
    protected BiConsumer<IPacket, NetMarshalClient> receiveBiConsumer;
    protected BiConsumer<Exception, NetMarshalClient> receiveExceptionBiConsumer;
    protected final Object slockPacketRead = new Object();
    protected boolean disablePacketReading;

    protected final InetAddress remoteAddress;
    protected final int remotePort;

    protected final IPacketFactory factory;
    protected final PacketLoader loader;

    protected final Thread receiveThread;
    protected final Queue<IPacket> receivedPackets = new LinkedList<>();
    protected final Object slockReceive = new Object();

    private NetMarshalClient(InetAddress remoteAddress, int remotePort, IPacketFactory factory, PacketLoader loader, boolean isMulticast, boolean isSocketNull) {
        if (isSocketNull) throw new NullPointerException("socketIn is null");
        if (remoteAddress == null) throw new NullPointerException(((isMulticast) ? "multicastGroupAddress" : "remoteAddress") + " is null");
        this.remoteAddress = remoteAddress;
        if (remotePort < 0) throw new IllegalArgumentException(((isMulticast) ? "multicastGroupPort" : "remotePort") + " is less than 0");
        if (remotePort > 65535) throw new IllegalArgumentException(((isMulticast) ? "multicastGroupPort" : "remotePort") + " is greater than 65535");
        this.remotePort = remotePort;
        if (factory == null) throw new NullPointerException("factory is null");
        this.factory = factory;
        if (loader == null) throw new NullPointerException("loader is null");
        this.loader = loader;
        receiveThread = new Thread(() -> {
            while (running) receiveThreadExecuted();
        }, "thread_receive_" + remoteAddress.getHostAddress() + ":" + remotePort);
    }

    /**
     * Constructs a new NetMarshalClient with the specified {@link Socket}, {@link IPacketFactory} and {@link PacketLoader}.
     *
     * @param socketIn The socket to use.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @throws NullPointerException socketIn, factory or loader is null.
     */
    public NetMarshalClient(Socket socketIn, IPacketFactory factory, PacketLoader loader) {
        this((socketIn == null) ? null : socketIn.getInetAddress(), (socketIn == null) ? -1 : socketIn.getPort(), factory, loader, false, socketIn == null);
        socket = socketIn;
        setStreams(new NetworkInputStream(socketIn), new NetworkOutputStream(socketIn));
    }

    /**
     * Constructs a new NetMarshalClient with the specified {@link MulticastSocket}, multicast group {@link InetAddress}, multicast port, {@link IPacketFactory} and {@link PacketLoader}.
     * The {@link MulticastSocket} will join the multicast group.
     *
     * @param socketIn The multicast socket to use.
     * @param multicastGroupAddress The multicast group address.
     * @param multicastGroupPort The multicast group port.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @throws IOException There is an error joining or multicastGroupAddress is not a multicast address.
     * @throws NullPointerException socketIn, multicastGroupAddress, factory or loader is null.
     * @throws IllegalArgumentException multicastGroupPort is less than 0 or greater than 65535.
     */
    public NetMarshalClient(MulticastSocket socketIn, InetAddress multicastGroupAddress, int multicastGroupPort, IPacketFactory factory, PacketLoader loader) throws IOException {
        this(multicastGroupAddress, multicastGroupPort, factory, loader, true, socketIn == null);
        socketIn.joinGroup(multicastGroupAddress);
        NetworkOutputStream netOut = new NetworkOutputStream(socketIn, 65535);
        netOut.setDatagramTarget(multicastGroupAddress, multicastGroupPort);
        setStreams(new NetworkInputStream(socketIn), netOut);
    }

    /**
     * Constructs a new NetMarshalClient with the specified {@link DatagramSocket}, remote {@link InetAddress}, remote port, {@link IPacketFactory} and {@link PacketLoader}.
     *
     * @param socketIn The datagram socket to use.
     * @param remoteAddress The remote address to send data to.
     * @param remotePort The remote port to send data to.
     * @param inputStream The receiving input stream.
     * @param factory The packet factory to use.
     * @param loader The loader to use.
     * @throws NullPointerException socketIn, remoteAddress, inputStream, factory or loader is null.
     * @throws IllegalArgumentException remotePort is less than 0 or greater than 65535.
     */
    public NetMarshalClient(DatagramSocket socketIn, InetAddress remoteAddress, int remotePort, InputStream inputStream, IPacketFactory factory, PacketLoader loader) {
        this(remoteAddress, remotePort, factory, loader, false, socketIn == null);
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        setStreams(null, new NetworkOutputStream(socketIn, 65535));
        rootInputStream = inputStream;
        this.inputStream = inputStream;
    }

    protected void setStreams(InputStream inputStream, OutputStream outputStream) {
        if (inputStream != null) rootInputStream = inputStream;
        this.inputStream = rootInputStream;
        if (outputStream != null) rootOutputStream = outputStream;
        this.outputStream = rootOutputStream;
    }

    /**
     * Get the input stream.
     *
     * @return Gets the input stream.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Get the root input stream.
     *
     * @return Gets the root input stream.
     */
    public InputStream getRootInputStream() {
        return rootInputStream;
    }

    /**
     * Get the output stream.
     *
     * @return Gets the output stream.
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Get the root output stream.
     *
     * @return Gets the root output stream.
     */
    public OutputStream getRootOutputStream() {
        return rootOutputStream;
    }

    /**
     * Get the local {@link InetAddress}.
     *
     * @return The local address or null.
     */
    public InetAddress localAddress() {
        return (socket == null) ? ((dsocket == null) ? null : dsocket.getLocalAddress()) : socket.getLocalAddress();
    }

    /**
     * Get the local port.
     *
     * @return The local port or -1.
     */
    public int localPort() {
        return (socket == null) ? ((dsocket == null) ? -1 : dsocket.getLocalPort()) : socket.getLocalPort();
    }

    /**
     * Get the remote {@link InetAddress}.
     *
     * @return The remote address.
     */
    public InetAddress remoteAddress() {
        return remoteAddress;
    }

    /**
     * Get the remote port.
     *
     * @return The remote port or -1.
     */
    public int remotePort() {
        return remotePort;
    }

    /**
     * Gets if the marshal is running.
     *
     * @return If the marshal is running.
     */
    public synchronized final boolean isRunning() {
        return running;
    }

    /**
     * Gets if the marshal is ssl upgraded.
     *
     * @return Is the marshal ssl upgraded.
     */
    public synchronized final boolean isSSLUpgraded() {
        if (!running) return false;
        return socket instanceof SSLSocket;
    }

    /**
     * Sends a {@link IPacket}.
     *
     * @param packetIn The packet to send.
     * @throws IOException A stream exception has occurred.
     * @throws PacketException An exception has occurred.
     * @throws NullPointerException packetIn is null.
     */
    public synchronized final void sendPacket(IPacket packetIn) throws IOException, PacketException {
        if (packetIn == null) throw new NullPointerException("packetIn is null");
        synchronized ((socket == null) ? dsocket : socket) {
            loader.writePacket(outputStream, packetIn, true);
        }
    }

    /**
     * Gets if there are received {@link IPacket}s.
     *
     * @return If there are received packets.
     */
    public boolean areReceivedPacketsWaiting() {
        synchronized (slockReceive) {
            return receivedPackets.size() > 0;
        }
    }

    /**
     * Receives a {@link IPacket}.
     *
     * @return The received packet.
     * @throws InterruptedException A thread interruption has occurred.
     */
    public IPacket receivePacket() throws InterruptedException {
        synchronized (slockReceive) {
            while (receivedPackets.size() < 1) slockReceive.wait();
            return receivedPackets.poll();
        }
    }

    /**
     * Are {@link IPacket}s being read.
     *
     * @return Are packets being read.
     */
    public boolean arePacketsBeingRead() {
        if (!running) return false;
        return !disablePacketReading;
    }

    /**
     * Set if {@link IPacket}s should be read.
     *
     * @param shouldRead Should packets be read.
     */
    public void setPacketsShouldBeRead(boolean shouldRead) {
        synchronized (slockPacketRead) {
            if (receiveThread.isAlive()) receiveThread.interrupt();
            disablePacketReading = !shouldRead;
            if (!disablePacketReading) slockPacketRead.notify();
        }
    }

    /**
     * Performs SSL socket upgrades on server side (Use for accepted sockets).
     * Can only be called when {@link #setPacketsShouldBeRead(boolean)} is set to false or in a {@link BiConsumer}
     * consumer for {@link #setReceiveBiConsumer(BiConsumer)}.
     *
     * @param context The SSL context to use.
     * @throws SSLUtilityException An SSL Utility exception has occurred.
     * @throws IOException An I/O Exception has occurred.
     * @throws NullPointerException context is null.
     * @throws IllegalStateException sslUpgrade methods should be called in a BiConsumer (for setReceiveBiConsumer)
     * within the target NetMarshalClient or when reading packets (arePacketsBeingRead) is disabled on the NetMarshalClient.
     */
    public void sslUpgradeServerSide(SSLContext context) throws SSLUtilityException, IOException {
        sslUpgrade(context, null);
    }

    /**
     * Performs SSL socket upgrades on client side (Use for constructed sockets).
     * Can only be called when {@link #setPacketsShouldBeRead(boolean)} is set to false or in a {@link BiConsumer}
     * consumer for {@link #setReceiveBiConsumer(BiConsumer)}.
     *
     * @param context The SSL context to use.
     * @param remoteHostName The hostname of the remote server.
     * @throws SSLUtilityException An SSL Utility exception has occurred.
     * @throws IOException An I/O Exception has occurred.
     * @throws NullPointerException context or remoteHostName is null.
     * @throws IllegalStateException sslUpgrade methods should be called in a BiConsumer (for setReceiveBiConsumer)
     * within the target NetMarshalClient or when reading packets (arePacketsBeingRead) is disabled on the NetMarshalClient.
     */
    public void sslUpgradeClientSide(SSLContext context, String remoteHostName) throws SSLUtilityException, IOException {
        if (remoteHostName == null) throw new NullPointerException("remoteHostName is null");
        sslUpgrade(context, remoteHostName);
    }

    protected synchronized final void sslUpgrade(SSLContext context, String remoteHostName) throws SSLUtilityException, IOException {
        if (!running || socket == null || socket instanceof SSLSocket) return;
        if (context == null) throw new NullPointerException("context is null");
        if (!disablePacketReading && Thread.currentThread() != receiveThread) throw new IllegalStateException("sslUpgrade methods should be called in a BiConsumer (for setReceiveBiConsumer) within the target NetMarshalClient" +
                " or when reading packets (arePacketsBeingRead) is disabled on the NetMarshalClient");
        Socket originalSocket = socket;
        synchronized (originalSocket) {
            try {
                socket = SSLUtilities.upgradeClientSocketToSSL(context, socket, remoteHostName, socket.getPort(), true, remoteHostName != null);
                if (rootInputStream instanceof NetworkInputStream) ((NetworkInputStream) rootInputStream).setSocket(socket);
                if (rootOutputStream instanceof NetworkOutputStream) ((NetworkOutputStream) rootOutputStream).setSocket(socket);
            } catch (SSLUtilityException | IOException e) {
                socket = originalSocket;
                try {
                    if (rootInputStream instanceof NetworkInputStream) ((NetworkInputStream) rootInputStream).setSocket(socket);
                } catch (IOException ex) {
                }
                try {
                    if (rootOutputStream instanceof NetworkOutputStream) ((NetworkOutputStream) rootOutputStream).setSocket(socket);
                } catch (IOException ex) {
                }
                throw e;
            }
        }
    }

    /**
     * Gets the {@link BiConsumer} receiver consumer.
     *
     * @return The receiver consumer or null.
     */
    public BiConsumer<IPacket, NetMarshalClient> getReceiveBiConsumer() {
        return receiveBiConsumer;
    }

    /**
     * Sets the {@link BiConsumer} receiver consumer.
     *
     * @param consumer The new receiver consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setReceiveBiConsumer(BiConsumer<IPacket, NetMarshalClient> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        receiveBiConsumer = consumer;
    }

    /**
     * Gets the {@link BiConsumer} receive exception consumer.
     *
     * @return The exception consumer or null.
     */
    public BiConsumer<Exception, NetMarshalClient> getReceiveExceptionBiConsumer() {
        return receiveExceptionBiConsumer;
    }

    /**
     * Sets the {@link BiConsumer} receive exception consumer.
     *
     * @param consumer The new exception consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setReceiveExceptionBiConsumer(BiConsumer<Exception, NetMarshalClient> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        receiveExceptionBiConsumer = consumer;
    }

    /**
     * Closes the marshal, closing all its streams.
     *
     * @throws IOException An I/O Exception has occurred.
     */
    @Override
    public synchronized final void close() throws IOException {
        if (running) {
            running = false;
            if (Thread.currentThread() != receiveThread) receiveThread.interrupt();
            receivedPackets.clear();
            inputStream.close();
            outputStream.close();
            socket = null;
            dsocket = null;
        }
    }

    protected void receiveThreadExecuted() {
        try {
            synchronized (slockPacketRead) {
                while (disablePacketReading) slockPacketRead.wait();
            }
            IPacket packet = loader.readStreamedPacket(inputStream, factory, null);
            synchronized (slockPacketRead) {
                if (packet == null || !packet.isValid()) return;
                if (receiveBiConsumer != null) receiveBiConsumer.accept(packet, this);
                synchronized (slockReceive) {
                    receivedPackets.add(packet);
                    slockReceive.notify();
                }
            }
        } catch (InterruptedException | InterruptedIOException e) {
        } catch (PacketException | IOException e) {
            if (receiveExceptionBiConsumer != null) receiveExceptionBiConsumer.accept(e, this);
            try {
                close();
            } catch (IOException ex) {
                if (receiveExceptionBiConsumer != null) receiveExceptionBiConsumer.accept(ex, this);
            }
        }
    }
}
