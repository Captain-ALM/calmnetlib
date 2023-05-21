package com.captainalm.lib.calmnet.packet.core;

import com.captainalm.lib.calmnet.packet.*;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.calmnet.stream.LengthClampedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

/**
 * This class provides a base64 encrypted packet that can hold an {@link IPacket}.
 * <p>
 * Major ID: 255
 * Minor ID: 251
 * </p>
 *
 * @author Captain ALM
 */
public class Base64Packet implements IStreamedPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 255, (byte) 251);

    protected final Object slock = new Object();
    protected PacketLoader loader;
    protected IPacketFactory factory;
    protected IPacket held;
    protected byte[] encryptedCache;
    protected boolean useCache;

    /**
     * Constructs a new Base64Packet with the specified {@link IPacketFactory} and {@link PacketLoader}.
     * The encrypted data will not be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @throws NullPointerException factory or loader is null.
     */
    public Base64Packet(IPacketFactory factory, PacketLoader loader) {
        this(factory, loader, false);
    }

    /**
     * Constructs a new Base64Packet with the specified {@link IPacketFactory}, {@link PacketLoader}
     * and if the encrypted data should be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param useCache If the encrypted data should be cached.
     * @throws NullPointerException factory or loader is null.
     */
    public Base64Packet(IPacketFactory factory, PacketLoader loader, boolean useCache) {
        this(factory, loader, null, useCache);
    }

    /**
     * Constructs a new Base64Packet with the specified {@link IPacketFactory}, {@link PacketLoader} and {@link IPacket}.
     * The encrypted data will not be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param packet The packet to store or null.
     * @throws NullPointerException factory or loader is null.
     */
    public Base64Packet(IPacketFactory factory, PacketLoader loader, IPacket packet) {
        this(factory, loader, packet, false);
    }

    /**
     * Constructs a new Base64Packet with the specified {@link IPacketFactory}, {@link PacketLoader},
     * {@link IPacket} and if the encrypted data should be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param packet The packet to store or null.
     * @param useCache If the encrypted data should be cached.
     * @throws NullPointerException factory or loader is null.
     */
    public Base64Packet(IPacketFactory factory, PacketLoader loader, IPacket packet, boolean useCache) {
        if (factory == null) throw new NullPointerException("factory is null");
        if (loader == null) throw new NullPointerException("loader is null");
        this.factory = factory;
        held = packet;
        this.loader = loader;
        this.useCache = useCache;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return (held != null);
    }

    /**
     * Gets the protocol information.
     *
     * @return The protocol information.
     */
    @Override
    public PacketProtocolInformation getProtocol() {
        return protocol;
    }

    /**
     * Gets the protocol information statically.
     *
     * @return The protocol information.
     */
    public static PacketProtocolInformation getTheProtocol() {
        return protocol;
    }

    protected void processEncryptedCache() throws PacketException {
        if (encryptedCache == null) {
            if (held == null) throw new PacketException("no data");
            encryptedCache = Base64.getEncoder().encode(loader.writePacketNoDigest(held, true));
        }
    }

    /**
     * Saves the packet payload to a byte array.
     *
     * @return The packet payload data.
     * @throws PacketException An Exception has occurred.
     */
    @Override
    public byte[] savePayload() throws PacketException {
        synchronized (slock) {
            if (useCache) {
                processEncryptedCache();
                return encryptedCache;
            } else {
                if (held == null) throw new PacketException("no data");
                return Base64.getEncoder().encode(loader.writePacketNoDigest(held, true));
            }
        }
    }

    /**
     * Loads the packet payload from save data.
     *
     * @param packetData The packet payload data.
     * @throws NullPointerException The new store data is null.
     * @throws PacketException      An Exception has occurred.
     */
    @Override
    public void loadPayload(byte[] packetData) throws PacketException {
        if (packetData == null) throw new NullPointerException("packetData is null");
        synchronized (slock) {
            try {
                byte[] payload = Base64.getDecoder().decode(packetData);
                held = loader.readPacketNoDigest(payload, factory, null);
            } catch (IllegalArgumentException e) {
                throw new PacketException(e);
            }
            if (useCache) encryptedCache = packetData;
        }
    }

    /**
     * Reads payload data to an {@link OutputStream}.
     *
     * @param outputStream The output stream to read data to.
     * @throws NullPointerException outputStream is null.
     * @throws IOException          An IO Exception has occurred.
     * @throws PacketException      An Exception has occurred.
     */
    @Override
    public void readData(OutputStream outputStream) throws IOException, PacketException {
        if (outputStream == null) throw new NullPointerException("outputStream is null");
        synchronized (slock) {
            if (useCache) {
                processEncryptedCache();
                outputStream.write(encryptedCache);
            } else {
                if (held == null) throw new PacketException("no data");
                loader.writePacketNoDigest(Base64.getEncoder().wrap(outputStream), held, true);
            }
        }
    }

    /**
     * Writes payload data from an {@link InputStream}.
     *
     * @param inputStream The input stream to write data from.
     * @param size        The size of the input payload in bytes.
     * @throws NullPointerException     inputStream is null.
     * @throws IllegalArgumentException size is less than 0.
     * @throws IOException              An IO Exception has occurred.
     * @throws PacketException          An Exception has occurred.
     */
    @Override
    public void writeData(InputStream inputStream, int size) throws IOException, PacketException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (size < 0) throw new IllegalArgumentException("size is less than 0");
        synchronized (slock) {
            encryptedCache = null;
            held = loader.readStreamedPacketNoDigest(Base64.getDecoder().wrap(new LengthClampedInputStream(inputStream, size)), factory, null);
        }
    }

    /**
     * Gets the size of the output data.
     *
     * @return The size of the output data in bytes.
     * @throws PacketException An Exception has occurred.
     */
    @Override
    public int getSize() throws PacketException {
        synchronized (slock) {
            if (useCache) {
                processEncryptedCache();
                return encryptedCache.length;
            } else {
                if (held == null) throw new PacketException("no data");
                return 4 * (int) Math.ceil((double) loader.getPacketSize(held, true, true) / 3);
            }
        }
    }

    /**
     * Gets the {@link PacketLoader} in use.
     *
     * @return The Packet Loader in use.
     */
    public PacketLoader getPacketLoader() {
        return loader;
    }

    /**
     * Sets the {@link PacketLoader} to use.
     *
     * @param loader The Packet Loader to use.
     * @throws NullPointerException loader is null.
     */
    public void setPacketLoader(PacketLoader loader) {
        if (loader == null) throw new NullPointerException("loader is null");
        synchronized (slock) {
            this.loader = loader;
        }
    }

    /**
     * Gets the {@link IPacketFactory} in use.
     *
     * @return The Packet Factory in use.
     */
    public IPacketFactory getFactory() {
        return factory;
    }

    /**
     * Sets the {@link IPacketFactory} to use.
     *
     * @param factory The Packet Factory to use.
     * @throws NullPointerException factory is null.
     */
    public void setFactory(IPacketFactory factory) {
        if (factory == null) throw new NullPointerException("factory is null");
        synchronized (slock) {
            this.factory = factory;
        }
    }

    /**
     * Gets the held packet or null.
     *
     * @return The packet or null.
     */
    public IPacket getHeldPacket() {
        return held;
    }

    /**
     * Sets the held packet.
     *
     * @param packet The new packet or null.
     */
    public void setHeldPacket(IPacket packet) {
        synchronized (slock) {
            encryptedCache = null;
            held = packet;
        }
    }

    /**
     * Gets if the encrypted data is cached.
     *
     * @return If the encrypted data is cached.
     */
    public boolean isCacheUsed() {
        return useCache;
    }

    /**
     * Sets if the encrypted data is cached.
     *
     * @param used If the encrypted data should be cached.
     */
    public void setCacheUsed(boolean used) {
        synchronized (slock) {
            useCache = used;
            if (!useCache)
                encryptedCache = null;
        }
    }
}
