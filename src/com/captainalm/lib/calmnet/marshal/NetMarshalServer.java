package com.captainalm.lib.calmnet.marshal;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.calmnet.stream.NetworkInputStream;

import java.io.*;
import java.net.*;
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

    protected final Object slocksock = new Object();
    protected ServerSocket socket;
    protected DatagramSocket dsocket;
    protected final NetworkInputStream dInputStream;
    protected final Map<CandidateClient, PipedOutputStream> outputs;
    protected final Object slockOutputs;
    protected final List<NetMarshalClient> clients = new ArrayList<>();

    protected BiConsumer<IPacket, NetMarshalClient> receiveBiConsumer;
    protected BiConsumer<Exception, NetMarshalClient> receiveExceptionBiConsumer;
    protected BiConsumer<Exception, NetMarshalServer> acceptExceptionBiConsumer;
    protected Consumer<NetMarshalClient> openedConsumer;
    protected Consumer<NetMarshalClient> closedConsumer;
    protected BiConsumer<CandidateClient, NetMarshalServer> acceptanceBiConsumer;
    protected BiConsumer<Socket, NetMarshalServer> socketSetupBiConsumer;

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
        if (fragmentationOptions == null) {
            this.fragmentationOptions = null;
        } else {
            this.fragmentationOptions = new FragmentationOptions(fragmentationOptions);
            this.fragmentationOptions.validate();
        }
        if (dsock == null) {
            dInputStream = null;
            outputs = null;
            slockOutputs = null;
            acceptThread = new Thread(() -> {
                while (running) acceptThreadExecutedSocket();
            }, "thread_accept_" + localAddress.getHostAddress() + ":" + localPort);
        } else {
            dInputStream = new NetworkInputStream(dsock);
            outputs = new HashMap<>();
            slockOutputs = new Object();
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
        synchronized (slocksock) {
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
        synchronized (slocksock) {
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
        synchronized (slocksock) {
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
     * Gets the {@link BiConsumer} accept exception consumer.
     *
     * @return The exception consumer or null.
     */
    public BiConsumer<Exception, NetMarshalServer> getAcceptExceptionBiConsumer() {
        return acceptExceptionBiConsumer;
    }

    /**
     * Sets the {@link BiConsumer} accept exception consumer.
     *
     * @param consumer The new exception consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setAcceptExceptionBiConsumer(BiConsumer<Exception, NetMarshalServer> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        acceptExceptionBiConsumer = consumer;
    }

    /**
     * Gets the {@link Consumer} closed consumer.
     *
     * @return The closed consumer or null.
     */
    public Consumer<NetMarshalClient> getClosedConsumer() {
        return closedConsumer;
    }

    /**
     * Sets the {@link Consumer} closed consumer.
     *
     * @param consumer The new closed consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setClosedConsumer(Consumer<NetMarshalClient> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        closedConsumer = consumer;
    }

    /**
     * Gets the {@link Consumer} opened consumer.
     *
     * @return The opened consumer or null.
     */
    public Consumer<NetMarshalClient> getOpenedConsumer() {
        return openedConsumer;
    }

    /**
     * Sets the {@link Consumer} opened consumer.
     *
     * @param consumer The new opened consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setOpenedConsumer(Consumer<NetMarshalClient> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        openedConsumer = consumer;
    }

    /**
     * Gets the {@link BiConsumer} client acceptance consumer.
     * Use {@link CandidateClient#accept} to declare whether the candidate should be accepted.
     *
     * @return The acceptance consumer or null.
     */
    public BiConsumer<CandidateClient, NetMarshalServer> getClientAcceptanceBiConsumer() {
        return acceptanceBiConsumer;
    }

    /**
     * Sets the {@link BiConsumer} client acceptance consumer.
     * Use {@link CandidateClient#accept} to declare whether the candidate should be accepted.
     *
     * @param consumer The new acceptance consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setClientAcceptanceBiConsumer(BiConsumer<CandidateClient, NetMarshalServer> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        acceptanceBiConsumer = consumer;
    }

    /**
     * Gets the {@link BiConsumer} socket setup consumer.
     *
     * @return The setup consumer or null.
     */
    public BiConsumer<Socket, NetMarshalServer> getSocketSetupBiConsumer() {
        return socketSetupBiConsumer;
    }

    /**
     * Sets the {@link BiConsumer} socket setup consumer.
     *
     * @param consumer The new setup consumer.
     * @throws NullPointerException consumer is null.
     */
    public void setSocketSetupBiConsumer(BiConsumer<Socket, NetMarshalServer> consumer) {
        if (consumer == null) throw new NullPointerException("consumer is null");
        socketSetupBiConsumer = consumer;
    }

    private void disconnectAllInternal() throws IOException {
        synchronized (slocksock) {
            for (NetMarshalClient c : clients)
                if (c.isRunning()) c.close();
        }
    }

    protected NetMarshalClient generateClientSocket(Socket socketIn) {
        return new NetMarshalClient(socketIn, factory, loader, fragmentationOptions);
    }

    protected NetMarshalClient generateClientDSocket(CandidateClient candidate,PipedInputStream inputStream) {
        return new NetMarshalClient(dsocket, candidate.address, candidate.port, inputStream, factory, loader, fragmentationOptions);
    }

    protected void applyClientEvents(NetMarshalClient client) {
        client.setReceiveBiConsumer(this::onClientReceive);
        client.setReceiveExceptionBiConsumer(this::onClientReceiveException);
        client.setClosedConsumer(this::onClientClose);
    }

    /**
     * Connects to a remote endpoint.
     *
     * @param remoteAddress The remote address to connect to.
     * @param remotePort The remote port to connect to.
     * @param timeout The timeout of the connection attempt (0 for infinite timeout).
     * @return A NetMarshalClient instance or null for non-accepted connection.
     * @throws IOException A connection error has occurred.
     */
    public synchronized final NetMarshalClient connect(InetAddress remoteAddress, int remotePort, int timeout) throws IOException {
        if (remoteAddress == null) throw new NullPointerException("remoteAddress is null");
        if (remotePort < 0) throw new IllegalArgumentException("remotePort is less than 0");
        if (remotePort > 65535) throw new IllegalArgumentException("remotePort is greater than 65535");
        CandidateClient candidateClient = new CandidateClient(remoteAddress, remotePort);
        if (acceptanceBiConsumer != null) acceptanceBiConsumer.accept(candidateClient, this);
        if (candidateClient.accept) {
            NetMarshalClient found = null;
            synchronized (slocksock) {
                for (NetMarshalClient c : clients)
                    if (candidateClient.matchesNetMarshalClient(c)) {
                        found = c;
                        break;
                    }
                if (found == null) {
                    if (socket == null) {
                        PipedInputStream inputPipe = new PipedInputStream(65535);
                        PipedOutputStream outputPipe = new PipedOutputStream(inputPipe);
                        found = generateClientDSocket(candidateClient, inputPipe);
                        synchronized (slockOutputs) {
                            outputs.put(candidateClient, outputPipe);
                        }
                    } else {
                        Socket clientSocket = new Socket();
                        clientSocket.connect(new InetSocketAddress(remoteAddress, remotePort), timeout);
                        if (socketSetupBiConsumer != null) socketSetupBiConsumer.accept(clientSocket, this);
                        found = generateClientSocket(clientSocket);
                    }
                    try {
                        applyClientEvents(found);
                        clients.add(found);
                    } catch (Exception e) {
                        clients.remove(found);
                        if (socket == null) {
                            synchronized (slockOutputs) {
                                outputs.remove(new CandidateClient(found.remoteAddress(), found.remotePort()));
                            }
                        }
                        throw e;
                    }
                }
            }
            found.open();
            if (openedConsumer != null) openedConsumer.accept(found);
            return found;
        }
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

    protected void onClientReceive(IPacket packet, NetMarshalClient client) {
        if (receiveBiConsumer != null) receiveBiConsumer.accept(packet, client);
    }

    protected void onClientReceiveException(Exception e, NetMarshalClient client) {
        if (receiveExceptionBiConsumer != null) receiveExceptionBiConsumer.accept(e, client);
    }

    protected void onClientClose(NetMarshalClient closed) {
        synchronized (slocksock) {
            clients.remove(closed);
            if (socket == null) {
                CandidateClient candidate = new CandidateClient(closed.remoteAddress(), closed.remotePort());
                synchronized (slockOutputs) {
                    PipedOutputStream outputPipe = outputs.get(candidate);
                    if (outputPipe != null) {
                        try {
                            outputPipe.close();
                        } catch (IOException e) {
                            onClientReceiveException(e, closed);
                        }
                    }
                    outputs.remove(candidate);
                }
            }
        }
        if (closedConsumer != null) closedConsumer.accept(closed);
    }

    protected void acceptThreadExecutedSocket() {
        try {
            Socket clientSocket = socket.accept();
            if (socketSetupBiConsumer != null) socketSetupBiConsumer.accept(clientSocket, this);
            CandidateClient candidateClient = new CandidateClient(clientSocket.getInetAddress(), clientSocket.getPort());
            try {
                if (acceptanceBiConsumer != null) acceptanceBiConsumer.accept(candidateClient, this);
                if (candidateClient.accept) {
                    NetMarshalClient client = generateClientSocket(clientSocket);
                    applyClientEvents(client);
                    synchronized (slocksock) {
                        clients.add(client);
                    }
                    client.open();
                    if (openedConsumer != null) openedConsumer.accept(client);
                } else {
                    clientSocket.close();
                }
            } catch (Exception e) {
                clientSocket.close();
                throw e;
            }
        } catch (InterruptedIOException e) {
        } catch (Exception e) {
            if (acceptExceptionBiConsumer != null) acceptExceptionBiConsumer.accept(e, this);
        }
    }

    protected void acceptThreadExecutedDSocket() {
        try {
            byte[] dPacket = dInputStream.readPacket();
            CandidateClient candidateClient = new CandidateClient(dInputStream.getAddress(), dInputStream.getPort());
            PipedOutputStream outputPipe;
            synchronized (slockOutputs) {
                outputPipe = outputs.get(candidateClient);
            }
            if (outputPipe == null) {
                synchronized (slocksock) {
                    NetMarshalClient found = null;
                    for (NetMarshalClient c : clients)
                        if (candidateClient.matchesNetMarshalClient(c)) {
                            found = c;
                            break;
                        }
                    if (found == null) {
                        try {
                            if (acceptanceBiConsumer != null) acceptanceBiConsumer.accept(candidateClient, this);
                            if (candidateClient.accept) {
                                PipedInputStream inputPipe = new PipedInputStream(65535);
                                outputPipe = new PipedOutputStream(inputPipe);
                                synchronized (slockOutputs) {
                                    outputs.put(candidateClient, outputPipe);
                                }
                                NetMarshalClient client = generateClientDSocket(candidateClient, inputPipe);
                                applyClientEvents(client);
                                clients.add(client);
                                client.open();
                                if (openedConsumer != null) openedConsumer.accept(client);
                            }
                        } catch (Exception e) {
                            synchronized (slockOutputs) {
                                outputs.remove(candidateClient);
                            }
                            throw e;
                        }
                    } else {
                        synchronized (slockOutputs) {
                            outputPipe = outputs.get(candidateClient);
                        }
                    }
                }
            }
            if (outputPipe != null) outputPipe.write(dPacket);
        } catch (InterruptedIOException e) {
        } catch (Exception e) {
            if (acceptExceptionBiConsumer != null) acceptExceptionBiConsumer.accept(e, this);
        }
    }
}
