package com.captainalm.lib.calmnet.marshal;

import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

/**
 * This class provides a way of networking on the server side and holds a collection of {@link NetMarshalClient}s, allows stream wrapping.
 * Wrapped streams should close the underlying stream when closed.
 * NOTE: Methods that are synchronised are used here, do NOT use instances of these classes as monitors.
 *
 * @author Captain ALM
 */
public class NetMarshalServerWrapped extends NetMarshalServer {
    protected Function<InputStream, InputStream> wrapperInputStream;
    protected Function<OutputStream, OutputStream> wrapperOutputStream;

    /**
     * Constructs a new NetMarshalServerWrapped with the specified {@link ServerSocket}, {@link IPacketFactory}, {@link PacketLoader}, {@link FragmentationOptions},
     * {@link Function} for wrapping the input stream and the {@link Function} for wrapping the output stream.
     *
     * @param socketIn The server socket to use.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @param inputStreamWrapper The input stream wrapper to use (Can be null).
     * @param outputStreamWrapper The output stream wrapper to use (Can be null).
     * @throws NullPointerException socketIn, factory or loader is null.
     * @throws IllegalArgumentException Fragmentation options failed validation.
     */
    public NetMarshalServerWrapped(ServerSocket socketIn, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions, Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) {
        super(socketIn, factory, loader, fragmentationOptions);
        wrapperInputStream = inputStreamWrapper;
        wrapperOutputStream = outputStreamWrapper;
    }

    /**
     * Constructs a new NetMarshalServerWrapped with the specified {@link DatagramSocket}, {@link IPacketFactory}, {@link PacketLoader}, {@link FragmentationOptions},
     * {@link Function} for wrapping the input stream and the {@link Function} for wrapping the output stream.
     *
     * @param socketIn The datagram socket to use.
     * @param factory The packet factory to use.
     * @param loader The packet loader to use.
     * @param fragmentationOptions The fragmentation options, null to disable fragmentation.
     * @param inputStreamWrapper The input stream wrapper to use (Can be null).
     * @param outputStreamWrapper The output stream wrapper to use (Can be null).
     * @throws NullPointerException socketIn, factory or loader is null.
     * @throws IllegalArgumentException Fragmentation options failed validation.
     */
    public NetMarshalServerWrapped(DatagramSocket socketIn, IPacketFactory factory, PacketLoader loader, FragmentationOptions fragmentationOptions, Function<InputStream, InputStream> inputStreamWrapper, Function<OutputStream, OutputStream> outputStreamWrapper) {
        super(socketIn, factory, loader, fragmentationOptions);
        wrapperInputStream = inputStreamWrapper;
        wrapperOutputStream = outputStreamWrapper;
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

    @Override
    protected NetMarshalClient generateClientSocket(Socket socketIn) {
        return new NetMarshalClientWrapped(socketIn, factory, loader, fragmentationOptions, wrapperInputStream, wrapperOutputStream);
    }

    @Override
    protected NetMarshalClient generateClientDSocket(CandidateClient candidate, PipedInputStream inputStream) {
        return new NetMarshalClientWrapped(dsocket, candidate.address, candidate.port, inputStream, factory, loader, fragmentationOptions, wrapperInputStream, wrapperOutputStream);
    }
}
