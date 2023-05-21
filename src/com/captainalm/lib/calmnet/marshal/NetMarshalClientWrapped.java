package com.captainalm.lib.calmnet.marshal;

import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.function.Function;

/**
 * This class provides a managed way of networking on the client side, allows stream wrapping.
 * Wrapped streams should close the underlying stream when closed.
 * NOTE: Methods that are synchronised are used here, do NOT use instances of these classes as monitors.
 *
 * @author Captain ALM
 */
public class NetMarshalClientWrapped extends NetMarshalClient {
    protected Function<InputStream, InputStream> wrapperInputStream;
    protected Function<OutputStream, OutputStream> wrapperOutputStream;

    /**
     * Constructs a new NetMarshalClientWrapped with the specified {@link Socket}, {@link IPacketFactory},
     * {@link PacketLoader}, {@link FragmentationOptions}, {@link Function} for wrapping the input stream and the {@link Function} for wrapping the output stream.
     * Wrapped streams should close the underlying stream when closed.
     *
     * @param socketIn The socket to use.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @param inputStreamWrapper The input stream wrapper to use (Can be null).
     * @param outputStreamWrapper The output stream wrapper to use (Can be null).
     * @throws NullPointerException socketIn, factory or loader is null.
     * @throws IllegalArgumentException Fragmentation options failed validation.
     */
    public NetMarshalClientWrapped(Socket socketIn, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions, Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) {
        super(socketIn, factory, loader, fragmentationOptions);
        setupWrappers(inputStreamWrapper, outputStreamWrapper);
    }

    /**
     * Constructs a new NetMarshalClientWrapped with the specified {@link MulticastSocket}, multicast group {@link InetAddress}, multicast port, {@link IPacketFactory},
     * {@link PacketLoader}, {@link FragmentationOptions}, {@link Function} for wrapping the input stream and the {@link Function} for wrapping the output stream.
     * The {@link MulticastSocket} will join the multicast group.
     * Wrapped streams should close the underlying stream when closed.
     *
     * @param socketIn The multicast socket to use.
     * @param multicastGroupAddress The multicast group address.
     * @param multicastGroupPort The multicast group port.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @param inputStreamWrapper The input stream wrapper to use (Can be null).
     * @param outputStreamWrapper The output stream wrapper to use (Can be null).
     * @throws IOException There is an error joining or multicastGroupAddress is not a multicast address.
     * @throws NullPointerException socketIn, multicastGroupAddress, factory or loader is null.
     * @throws IllegalArgumentException multicastGroupPort is less than 0 or greater than 65535 or fragmentation options failed validation.
     */
    public NetMarshalClientWrapped(MulticastSocket socketIn, InetAddress multicastGroupAddress, int multicastGroupPort, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions, Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) throws IOException {
        super(socketIn, multicastGroupAddress, multicastGroupPort, factory, loader, fragmentationOptions);
        setupWrappers(inputStreamWrapper, outputStreamWrapper);
    }

    /**
     * Constructs a new NetMarshalClientWrapped with the specified {@link DatagramSocket}, remote {@link InetAddress}, remote port, {@link InputStream}, {@link IPacketFactory},
     * {@link PacketLoader}, {@link FragmentationOptions}, {@link Function} for wrapping the input stream and the {@link Function} for wrapping the output stream.
     * Wrapped streams should close the underlying stream when closed.
     *
     * @param socketIn The datagram socket to use.
     * @param remoteAddress The remote address to send data to.
     * @param remotePort The remote port to send data to.
     * @param inputStream The receiving input stream.
     * @param factory The packet factory to use.
     * @param loader The loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @param inputStreamWrapper The input stream wrapper to use (Can be null).
     * @param outputStreamWrapper The output stream wrapper to use (Can be null).
     * @throws NullPointerException socketIn, remoteAddress, inputStream, factory or loader is null.
     * @throws IllegalArgumentException remotePort is less than 0 or greater than 65535 or fragmentation options failed validation.
     */
    public NetMarshalClientWrapped(DatagramSocket socketIn, InetAddress remoteAddress, int remotePort, InputStream inputStream, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions, Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) {
        super(socketIn, remoteAddress, remotePort, inputStream, factory, loader, fragmentationOptions);
        setupWrappers(inputStreamWrapper, outputStreamWrapper);
    }

    /**
     * Constructs a new NetMarshalClientWrapped with the specified {@link DatagramSocket}, remote {@link InetAddress}, remote port, {@link IPacketFactory},
     * {@link PacketLoader}, {@link FragmentationOptions}, {@link Function} for wrapping the input stream and the {@link Function} for wrapping the output stream.
     * Wrapped streams should close the underlying stream when closed.
     *
     * @param socketIn The datagram socket to use.
     * @param remoteAddress The remote address to send data to.
     * @param remotePort The remote port to send data to.
     * @param factory The packet factory to use.
     * @param loader The loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @param inputStreamWrapper The input stream wrapper to use (Can be null).
     * @param outputStreamWrapper The output stream wrapper to use (Can be null).
     * @throws NullPointerException socketIn, remoteAddress, factory or loader is null.
     * @throws IllegalArgumentException remotePort is less than 0 or greater than 65535 or fragmentation options failed validation.
     */
    public NetMarshalClientWrapped(DatagramSocket socketIn, InetAddress remoteAddress, int remotePort, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions, Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) {
        super(socketIn, remoteAddress, remotePort, factory, loader, fragmentationOptions);
        setupWrappers(inputStreamWrapper, outputStreamWrapper);
    }

    protected void setupWrappers(Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) {
        wrapperInputStream = inputStreamWrapper;
        if (wrapperInputStream != null) inputStream = wrapperInputStream.apply(rootInputStream);
        if (inputStream == null) inputStream = rootInputStream;
        wrapperOutputStream = outputStreamWrapper;
        if (wrapperOutputStream != null) outputStream = wrapperOutputStream.apply(rootOutputStream);
        if (outputStream == null) outputStream = rootOutputStream;
    }

    /**
     * Gets the {@link Function} input stream wrapper function.
     *
     * @return The input stream wrapper function or null.
     */
    public Function<InputStream, InputStream> getWrapperInputStream() {
        return wrapperInputStream;
    }

    /**
     * Gets the {@link Function} output stream wrapper function.
     *
     * @return The output stream wrapper function or null.
     */
    public Function<OutputStream, OutputStream> getWrapperOutputStream() {
        return wrapperOutputStream;
    }
}
