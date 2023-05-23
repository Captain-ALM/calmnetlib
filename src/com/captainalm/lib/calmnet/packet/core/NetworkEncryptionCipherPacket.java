package com.captainalm.lib.calmnet.packet.core;

import com.captainalm.lib.calmnet.packet.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * This class provides the ability for supporting streams to negotiate a cipher.
 * <p>
 * Major ID: 255
 * Minor ID: 250
 * </p>
 *
 * @author Captain ALM
 */
public class NetworkEncryptionCipherPacket implements IStreamedPacket, IAcknowledgement, IInternalCache {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 255, (byte) 250);

    protected Boolean acknowledgement;
    protected String[] ciphers;

    protected byte[] cipherData;
    protected boolean useCache;

    protected final Object slock = new Object();

    /**
     * Constructs a new instance of NetworkEncryptionCipherPacket with the specified acknowledgement value and the specified ciphers.
     *
     * @param acknowledgement The acknowledgement value to use (Can be null).
     * @param cipherNames The cipher names.
     * @throws NullPointerException cipherNames is null.
     */
    public NetworkEncryptionCipherPacket(Boolean acknowledgement, String[] cipherNames) {
        if (cipherNames == null) throw new NullPointerException("cipherNames is null");
        this.acknowledgement = acknowledgement;
        this.ciphers = cipherNames;
    }

    /**
     * Gets if the class instance is an Acknowledgement.
     *
     * @return If the class instance is an Acknowledgement.
     */
    @Override
    public boolean isAcknowledgement() {
        return (acknowledgement != null && acknowledgement);
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return (acknowledgement != null && ciphers != null);
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

    protected void processCache() throws PacketException {
        if (cipherData == null) {
            if (acknowledgement == null || ciphers == null) throw new PacketException("no data");

            ByteArrayOutputStream arrayData = new ByteArrayOutputStream();
            arrayData.write((acknowledgement) ? (byte) 1 : (byte) 0);
            try {
                PacketLoader.writeInteger(arrayData, ciphers.length);
            } catch (IOException e) {
                throw new PacketException(e);
            }
            for (String c : ciphers) {
                if (c == null) throw new PacketException("no data in entry");
                try {
                    if (c.length() < 1) {
                        PacketLoader.writeInteger(arrayData, 0);
                    } else {
                        byte[] d = c.getBytes(StandardCharsets.UTF_8);
                        PacketLoader.writeInteger(arrayData, d.length);
                        arrayData.write(d);
                    }
                } catch (IOException e) {
                    throw new PacketException(e);
                }
            }
            cipherData = arrayData.toByteArray();
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
            processCache();
            if (useCache) return cipherData; else {
                byte[] toret = cipherData;
                cipherData = null;
                return toret;
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
        if (packetData.length < 5) throw new PacketException("no data");
        synchronized (slock) {
            acknowledgement = (packetData[0] == 1);
            if (!acknowledgement && packetData[0] != 0) acknowledgement = null;
            int index = 1;

            int recordCount = (packetData[index++] & 0xff) * 16777216;
            recordCount += (packetData[index++] & 0xff) * 65536;
            recordCount += (packetData[index++] & 0xff) * 256;
            recordCount += (packetData[index++] & 0xff);
            if (recordCount < 0) throw new PacketException("record count less than 0");

            if (useCache) cipherData = packetData;
            ciphers = new String[recordCount];
            for (int i = 0; i < recordCount; i++) {
                int recordLength = (packetData[index++] & 0xff) * 16777216;
                recordLength += (packetData[index++] & 0xff) * 65536;
                recordLength += (packetData[index++] & 0xff) * 256;
                recordLength += (packetData[index++] & 0xff);
                if (recordLength < 0) throw new PacketException("record length less than 0");
                byte[] currentRecord = new byte[recordLength];
                if (recordLength > 0) {
                    System.arraycopy(packetData, index, currentRecord, 0, recordLength);
                    index += recordLength;
                    ciphers[i] = new String(currentRecord, StandardCharsets.UTF_8);
                } else {
                    ciphers[i] = "";
                }
            }
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
                processCache();
                outputStream.write(cipherData);
            } else {
                outputStream.write((acknowledgement) ? (byte) 1 : (byte) 0);
                try {
                    PacketLoader.writeInteger(outputStream, ciphers.length);
                } catch (IOException e) {
                    throw new PacketException(e);
                }
                for (String c : ciphers) {
                    if (c == null) throw new PacketException("no data in entry");
                    try {
                        if (c.length() < 1) {
                            PacketLoader.writeInteger(outputStream, 0);
                        } else {
                            byte[] d = c.getBytes(StandardCharsets.UTF_8);
                            PacketLoader.writeInteger(outputStream, d.length);
                            outputStream.write(d);
                        }
                    } catch (IOException e) {
                        throw new PacketException(e);
                    }
                }
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
            if (size < 1) throw new IOException("inputStream end of stream");
            byte aknByte = PacketLoader.readByteFromInputStream(inputStream);
            acknowledgement = (aknByte == 1);
            if (!acknowledgement && aknByte != 0) acknowledgement = null;
            if (size < 5) throw new IOException("inputStream end of stream");

            int recordCount = PacketLoader.readInteger(inputStream);
            if (recordCount < 0) throw new PacketException("record count less than 0");

            cipherData = null;
            ciphers = new String[recordCount];
            for (int i = 0; i < recordCount; i++) {
                int recordLength = PacketLoader.readInteger(inputStream);
                if (recordLength < 0) throw new PacketException("record length less than 0");
                if (recordLength > 0) {
                    byte[] currentRecord = PacketLoader.readArrayFromInputStream(inputStream, recordLength);
                    ciphers[i] = new String(currentRecord, StandardCharsets.UTF_8);
                } else {
                    ciphers[i] = "";
                }
            }
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
            processCache();
            if (useCache) return cipherData.length; else {
                int toret = cipherData.length;
                cipherData = null;
                return toret;
            }
        }
    }

    /**
     * Gets the cipher names this packet contains.
     *
     * @return An array of cipher names.
     */
    public String[] getCiphers() {
        return ciphers;
    }

    /**
     * Sets the cipher names this packet contains.
     *
     * @param cipherNames The array of cipher names.
     * @throws NullPointerException cipherNames is null.
     */
    public void setCiphers(String[] cipherNames) {
        if (cipherNames == null) throw new NullPointerException("cipherNames is null");
        synchronized (slock) {
            ciphers = cipherNames;
            cipherData = null;
        }
    }

    /**
     * Gets if the cipher information is cached.
     *
     * @return If the cipher information is cached.
     */
    @Override
    public boolean isCacheUsed() {
        return useCache;
    }

    /**
     * Sets if the cipher information is cached.
     *
     * @param used If the cipher information is cached.
     */
    @Override
    public void setCacheUsed(boolean used) {
        synchronized (slock) {
            useCache = used;
            if (!useCache)
                cipherData = null;
        }
    }
}
