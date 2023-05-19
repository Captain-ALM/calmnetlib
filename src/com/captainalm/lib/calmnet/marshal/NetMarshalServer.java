package com.captainalm.lib.calmnet.marshal;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.calmnet.stream.NetworkInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class provides a way of networking on the server side and holds a collection of {@link NetMarshalClient}s.
 * NOTE: Methods that are synchronised are used here, do NOT use instances of these classes as monitors.
 *
 * @author Captain ALM
 */
public class NetMarshalServer implements Closeable {
    protected boolean running;

    protected ServerSocket socket;
    protected DatagramSocket dsocket;
    protected final NetworkInputStream dInputStream;
    protected final Map<NetMarshalClient, PipedOutputStream> outputs;
    protected final List<NetMarshalClient> clients = new ArrayList<>();

    protected BiConsumer<IPacket, NetMarshalClient> receiveBiConsumer;
    protected BiConsumer<Exception, NetMarshalClient> receiveExceptionBiConsumer;
    protected BiConsumer<Exception, NetMarshalServer> acceptExceptionBiConsumer;
    protected Consumer<NetMarshalClient> closedConsumer;
    protected BiConsumer<CandidateClient, NetMarshalServer> acceptanceBiConsumer;
    protected BiConsumer<Socket, NetMarshalServer> socketSetupBiConsumer;
    protected BiConsumer<DatagramSocket, NetMarshalServer> dSocketSetupBiConsumer;

    protected final Thread acceptThread;

    protected final InetAddress localAddress;
    protected final int localPort;
    protected final IPacketFactory factory;
    protected final PacketLoader loader;
    protected final FragmentationOptions fragmentationOptions;

    private NetMarshalServer(InetAddress localAddress, int localPort, IPacketFactory factory, PacketLoader loader, boolean isSocketNull, FragmentationOptions fragmentationOptions, DatagramSocket dsock) {
        if (isSocketNull) throw new NullPointerException("socketIn is null");
        if (localAddress == null) throw new NullPointerException("localAddress is null");
        this.localAddress = localAddress;
        if (localPort < 0) throw new IllegalArgumentException("localPort is less than 0");
        if (localPort > 65535) throw new IllegalArgumentException("localPort is greater than 65535");
        this.localPort = localPort;
        if (factory == null) throw new NullPointerException("factory is null");
        this.factory = factory;
        if (loader == null) throw new NullPointerException("loader is null");
        this.loader = loader;
        this.fragmentationOptions = fragmentationOptions;
        if (fragmentationOptions != null) fragmentationOptions.validate();
        if (dsock == null) {
            dInputStream = null;
            outputs = null;
            acceptThread = new Thread(() -> {
                while (running) acceptThreadExecutedSocket();
            }, "thread_accept_" + localAddress.getHostAddress() + ":" + localPort);
        } else {
            dInputStream = new NetworkInputStream(dsock);
            outputs = new HashMap<>();
            acceptThread = new Thread(() -> {
                while (running) acceptThreadExecutedDSocket();
            }, "thread_accept_" + localAddress.getHostAddress() + ":" + localPort);
        }
    }

    /**
     * Constructs a new NetMarshalServer with the specified {@link ServerSocket}, {@link IPacketFactory}, {@link PacketLoader} and {@link FragmentationOptions}.
     *
     * @param socketIn The server socket to use.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @throws NullPointerException socketIn, factory or loader is null.
     * @throws IllegalArgumentException Fragmentation options failed validation.
     */
    public NetMarshalServer(ServerSocket socketIn, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions) {
        this((socketIn == null) ? null : socketIn.getInetAddress(), (socketIn == null) ? -1 : socketIn.getLocalPort(), factory, loader, socketIn == null, fragmentationOptions, null);
        socket = socketIn;
    }

    /**
     * Constructs a new NetMarshalServer with the specified {@link DatagramSocket}, {@link IPacketFactory}, {@link PacketLoader} and {@link FragmentationOptions}.
     *
     * @param socketIn The datagram socket to use.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @throws NullPointerException socketIn, factory or loader is null.
     * @throws IllegalArgumentException Fragmentation options failed validation.
     */
    public NetMarshalServer(DatagramSocket socketIn, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions) {
        this((socketIn == null) ? null : socketIn.getInetAddress(), (socketIn == null) ? -1 : socketIn.getLocalPort(), factory, loader, socketIn == null, fragmentationOptions, socketIn);
        dsocket = socketIn;
    }

    /**
     * Opens the marshal.
     */
    public synchronized final void open() {
        if (running) return;
        running = true;
        acceptThread.start();
    }

    /**
     * Gets the packet factory in use.
     *
     * @return The packet factory.
     */
    public IPacketFactory getPacketFactory() {
        return factory;
    }

    /**
     * Gets the packet loader in use.
     *
     * @return The packet loader.
     */
    public PacketLoader getPacketLoader() {
        return loader;
    }

    /**
     * Get the local {@link InetAddress}.
     *
     * @return The local address or null.
     */
    public InetAddress localAddress() {
        return localAddress;
    }

    /**
     * Get the local port.
     *
     * @return The local port or -1.
     */
    public int localPort() {
        return localPort;
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
     * Gets the {@link FragmentationOptions} of the client.
     *
     * @return The fragmentation options or null if fragmentation is disabled.
     */
    public FragmentationOptions getFragmentationOptions() {
        return fragmentationOptions;
    }

    /**
     * Gets the current set of connected {@link NetMarshalClient}s.
     *
     * @return An array of connected clients.
     */
    public synchronized final NetMarshalClient[] getConnectedClients() {
        synchronized ((socket == null) ? dsocket : socket) {
            return clients.toArray(new NetMarshalClient[0]);
        }
    }

    /**
     * Broadcasts a {@link IPacket}.
     *
     * @param packetIn The packet to broadcast.
     * @param directSend Whether the packet should be sent directly or through the fragmentation system.
     * @throws IOException A stream exception has occurred.
     * @throws PacketException An exception has occurred.
     * @throws NullPointerException packetIn is null.
     */
    public synchronized final void broadcastPacket(IPacket packetIn, boolean directSend) throws IOException, PacketException {
        if (packetIn == null) throw new NullPointerException("packetIn is null");
        synchronized ((socket == null) ? dsocket : socket) {
            for (NetMarshalClient c : clients)
                if (c.isRunning()) c.sendPacket(packetIn, directSend);
        }
    }

    /**
     * Flushes all the output streams on all the clients.
     *
     * @throws IOException A stream exception has occurred.
     */
    public synchronized final void flush() throws IOException {
        synchronized ((socket == null) ? dsocket : socket) {
            for (NetMarshalClient c : clients)
                if (c.isRunning()) c.flush();
        }
    }

    /**
     * Disconnects all the clients (By closing them).
     *
     * @throws IOException An I/O Exception has occurred.
     */
    public synchronized final void disconnectAll() throws IOException {
        disconnectAllInternal();
    }

    private void disconnectAllInternal() throws IOException {
        synchronized ((socket == null) ? dsocket : socket) {
            for (NetMarshalClient c : clients)
                if (c.isRunning()) c.close();
        }
    }

    /**
     * Connects to a remote endpoint.
     *
     * @param remoteAddress The remote address to connect to.
     * @param remotePort The remote port to connect to.
     * @return A NetMarshalClient instance or null for non-accepted connection.
     * @throws IOException A connection error has occurred.
     */
    public synchronized final NetMarshalClient connect(InetAddress remoteAddress, int remotePort) throws IOException {
        return null;
    }

    /**
     * Closes the marshal, closing all the connected clients.
     *
     * @throws IOException An I/O Exception has occurred.
     */
    @Override
    public synchronized final void close() throws IOException {
        if (running) {
            running = false;
            if (Thread.currentThread() != acceptThread) acceptThread.interrupt();
            try {
                if (socket != null) socket.close();
                if (dInputStream != null) dInputStream.close();
            } finally {
                disconnectAllInternal();
            }
        }
    }

    protected void acceptThreadExecutedSocket() {

    }

    protected void acceptThreadExecutedDSocket() {

    }
}
