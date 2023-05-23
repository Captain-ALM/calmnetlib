package com.captainalm.lib.calmnet.packet.core;

import com.captainalm.lib.calmnet.packet.*;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.calmnet.stream.LengthClampedInputStream;
import com.captainalm.lib.stdcrypt.encryption.CipherException;
import com.captainalm.lib.stdcrypt.encryption.ICipherFactory;

import javax.crypto.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static com.captainalm.lib.calmnet.packet.PacketLoader.readByteFromInputStream;

/**
 * This class provides an encrypted packet that can hold an {@link IPacket}.
 * <p>
 * Major ID: 255
 * Minor ID: 252
 * </p>
 *
 * @author Captain ALM
 */
public class EncryptedPacket implements IStreamedPacket, IInternalCache {
    /*
     * Packet Format:
     *
     * Sections are seperated by spaces.
     * {} is condition followed by () containing the sections caused by the condition.
     * [] contains the length of the section.
     *
     * payload = trailerFlag[1] cypherSettingsLen[4] cypherSettings[cypherSettingsLen] {trailerFlag & 1}(trailerLength[4]) encrypted[*]
     * encrypted = encrypt<toEncrypt>
     * toEncrypt = data[*] {trailerFlag & 1}(trailer[trailerLength])
     */
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 255, (byte) 252);

    protected final Object slock = new Object();
    protected PacketLoader loader;
    protected IPacketFactory factory;
    protected IPacket held;
    protected byte[] encryptedCache;
    protected int trailingArrayLengthCache;
    protected boolean useCache;

    protected Cipher cipher;
    protected ICipherFactory cipherFactory;

    protected String trailingPassword;

    /**
     * Constructs a new EncryptedPacket with the specified {@link IPacketFactory}, {@link PacketLoader} and {@link ICipherFactory}.
     * The encrypted data will not be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param cipherFactory The cipher factory to use.
     * @throws NullPointerException factory, loader or cipherFactory is null.
     */
    public EncryptedPacket(IPacketFactory factory, PacketLoader loader, ICipherFactory cipherFactory) {
        this(factory, loader, cipherFactory, false);
    }

    /**
     * Constructs a new EncryptedPacket with the specified {@link IPacketFactory}, {@link PacketLoader}, {@link ICipherFactory}
     * and if the encrypted data should be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param cipherFactory The cipher factory to use.
     * @param useCache If the encrypted data should be cached.
     * @throws NullPointerException factory, loader or cipherFactory is null.
     */
    public EncryptedPacket(IPacketFactory factory, PacketLoader loader, ICipherFactory cipherFactory, boolean useCache) {
        this(factory, loader, cipherFactory, null, useCache);
    }

    /**
     * Constructs a new EncryptedPacket with the specified {@link IPacketFactory}, {@link PacketLoader}, {@link ICipherFactory} and {@link IPacket}.
     * The encrypted data will not be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param cipherFactory The cipher factory to use.
     * @throws NullPointerException factory, loader or cipherFactory is null.
     */
    public EncryptedPacket(IPacketFactory factory, PacketLoader loader, ICipherFactory cipherFactory, IPacket packet) {
        this(factory, loader, cipherFactory, packet, false);
    }

    /**
     * Constructs a new EncryptedPacket with the specified {@link IPacketFactory}, {@link PacketLoader}, {@link ICipherFactory},
     * {@link IPacket} and if the encrypted data should be cached.
     *
     * @param factory The packet factory to use.
     * @param loader The Packet Loader to use.
     * @param cipherFactory The cipher factory to use.
     * @param useCache If the encrypted data should be cached.
     * @throws NullPointerException factory, loader or cipherFactory is null.
     */
    public EncryptedPacket(IPacketFactory factory, PacketLoader loader, ICipherFactory cipherFactory, IPacket packet, boolean useCache) {
        if (factory == null) throw new NullPointerException("factory is null");
        if (loader == null) throw new NullPointerException("loader is null");
        if (cipherFactory == null) throw new NullPointerException("cipherFactory is null");
        this.factory = factory;
        this.loader = loader;
        this.cipher = null;
        held = packet;
        this.useCache = useCache;
    }

    protected void generateCipher(int opmode) throws PacketException {
        if (cipher != null) {
            try {
                cipher.doFinal();
            } catch (BadPaddingException | IllegalBlockSizeException e) {
            }
            cipher = null;
        }
        try {
            cipher = cipherFactory.getCipher(opmode);
        } catch (CipherException e) {
            throw new PacketException(e);
        }
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
        if (encryptedCache == null || cipherFactory.cipherAttributesModified()) {
            if (held == null) throw new PacketException("no data");

            generateCipher(Cipher.ENCRYPT_MODE);

            byte[] savedArray = loader.writePacketNoDigest(held, true);
            byte[] trailingArray = (trailingPassword == null || trailingPassword.length() < 1) ? new byte[0] : trailingPassword.getBytes(StandardCharsets.UTF_8);
            trailingArrayLengthCache = trailingArray.length;
            byte[] toEncrypt = new byte[savedArray.length + trailingArray.length];

            System.arraycopy(savedArray, 0, toEncrypt, 0, savedArray.length);

            if (trailingArray.length > 0) System.arraycopy(trailingArray, 0, toEncrypt, savedArray.length, trailingArray.length);
            try {
                encryptedCache = cipher.doFinal(toEncrypt);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                throw new PacketException(e);
            }
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
            processEncryptedCache();

            int cipherLen = cipherFactory.getSettingsNoSecretsLength();
            byte[] toret = new byte[5 + cipherLen + ((trailingArrayLengthCache == 0) ? 0 : 4) + encryptedCache.length];

            int index = 0;
            toret[index++] = (byte) ((trailingPassword != null && trailingPassword.length() > 0) ? 1 : 0);

            int length = cipherLen;

            toret[index++] = (byte) (length / 16777216);
            length %= 16777216;
            toret[index++] = (byte) (length / 65536);
            length %= 65536;
            toret[index++] = (byte) (length / 256);
            length %= 256;
            toret[index++] = (byte) (length);

            System.arraycopy(cipherFactory.getSettingsNoSecrets(), 0, toret, index, cipherLen); index += cipherLen;

            if (trailingArrayLengthCache > 0) {
                length = trailingArrayLengthCache;

                toret[index++] = (byte) (length / 16777216);
                length %= 16777216;
                toret[index++] = (byte) (length / 65536);
                length %= 65536;
                toret[index++] = (byte) (length / 256);
                length %= 256;
                toret[index++] = (byte) (length);
            }

            System.arraycopy(encryptedCache, 0, toret, index, encryptedCache.length);

            if (!useCache) encryptedCache = null;

            return toret;
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
            int index = 1;

            int cipherLenCache = (packetData[index++] & 0xff) * 16777216;
            cipherLenCache += (packetData[index++] & 0xff) * 65536;
            cipherLenCache += (packetData[index++] & 0xff) * 256;
            cipherLenCache += (packetData[index++] & 0xff);
            if (cipherLenCache < 1) throw new PacketException("cipher length less than 1");

            byte[] cipherSettingsCache = new byte[cipherLenCache];
            System.arraycopy(packetData, index, cipherSettingsCache, 0, cipherLenCache); index += cipherLenCache;
            try {
                cipherFactory.setSettings(cipherSettingsCache);
            } catch (CipherException e) {
                throw new PacketException(e);
            }

            generateCipher(Cipher.DECRYPT_MODE);

            trailingArrayLengthCache = 0;
            if ((packetData[0] & 1) == 1) {
                trailingArrayLengthCache = (packetData[index++] & 0xff) * 16777216;
                trailingArrayLengthCache += (packetData[index++] & 0xff) * 65536;
                trailingArrayLengthCache += (packetData[index++] & 0xff) * 256;
                trailingArrayLengthCache += (packetData[index++] & 0xff);
                if (trailingArrayLengthCache < 1) throw new PacketException("trailer length less than 1");
            }

            encryptedCache = new byte[packetData.length - index];
            System.arraycopy(packetData, index, encryptedCache, 0, encryptedCache.length);

            try {
                byte[] decrypted = cipher.doFinal(encryptedCache);
                byte[] thePacket = new byte[decrypted.length - trailingArrayLengthCache];

                System.arraycopy(decrypted, 0, thePacket, 0, thePacket.length);

                if (trailingArrayLengthCache > 0) {
                    byte[] theTrailer = new byte[trailingArrayLengthCache];
                    System.arraycopy(decrypted, thePacket.length, theTrailer, 0, trailingArrayLengthCache);
                    trailingPassword = new String(theTrailer, StandardCharsets.UTF_8);
                }

                held = loader.readPacketNoDigest(thePacket, factory, null);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                throw new PacketException(e);
            } finally {
                if (!useCache) encryptedCache = null;
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
            byte[] trailingArray;
            if (useCache) {
                processEncryptedCache();
                trailingArray = null;
            } else {
                if (held == null) throw new PacketException("no data");
                generateCipher(Cipher.ENCRYPT_MODE);
                trailingArray = (trailingPassword == null || trailingPassword.length() < 1) ? new byte[0] : trailingPassword.getBytes(StandardCharsets.UTF_8);
                trailingArrayLengthCache = trailingArray.length;
            }

            outputStream.write((trailingPassword != null && trailingPassword.length() > 0) ? 1 : 0);

            int length = cipherFactory.getSettingsNoSecretsLength();

            PacketLoader.writeInteger(outputStream, length);

            outputStream.write(cipherFactory.getSettingsNoSecrets());

            if (trailingArrayLengthCache > 0) PacketLoader.writeInteger(outputStream, trailingArrayLengthCache);

            if (useCache) {
                outputStream.write(encryptedCache);
            } else {
                CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
                loader.writePacketNoDigest(cipherOutputStream, held, true);
                if (trailingArrayLengthCache > 0)
                    cipherOutputStream.write(trailingArray);
                try {
                    outputStream.write(cipher.doFinal());
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    throw new PacketException(e);
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
            int flag = readByteFromInputStream(inputStream) & 0xff;

            if (size < 5) throw new IOException("inputStream end of stream");
            int cipherLenCache = PacketLoader.readInteger(inputStream);
            if (cipherLenCache < 1) throw new PacketException("cipher length less than 1");

            if (size < 5 + cipherLenCache) throw new IOException("inputStream end of stream");
            byte[] cipherSettingsCache = new byte[cipherLenCache];
            int offset = 0;
            int slen;
            while (offset < cipherLenCache) if ((slen = inputStream.read(cipherSettingsCache, offset, size - cipherLenCache)) != -1) offset += slen; else throw new IOException("inputStream end of stream");
            try {
                cipherFactory.setSettings(cipherSettingsCache);
            } catch (CipherException e) {
                throw new PacketException(e);
            }

            generateCipher(Cipher.DECRYPT_MODE);

            trailingArrayLengthCache = 0;
            if ((flag & 1) == 1) {
                if (size < 9 + cipherLenCache) throw new IOException("inputStream end of stream");
                trailingArrayLengthCache = PacketLoader.readByteFromInputStream(inputStream);
                if (trailingArrayLengthCache < 1) throw new PacketException("trailer length less than 1");
            }

            encryptedCache = null;
            CipherInputStream cipherInputStream = new CipherInputStream(new LengthClampedInputStream(inputStream, size - 5 - cipherLenCache - (((flag & 1) == 1) ? 4 + trailingArrayLengthCache : 0)), cipher);

            held = loader.readStreamedPacketNoDigest(cipherInputStream, factory, null);

            if (trailingArrayLengthCache > 0) {
                byte[] theTrailer = PacketLoader.readArrayFromInputStream(cipherInputStream, trailingArrayLengthCache);
                trailingPassword = new String(theTrailer, StandardCharsets.UTF_8);
            }

            try {
                cipher.doFinal();
            }
            catch (BadPaddingException | IllegalBlockSizeException e) {
                throw new PacketException(e);
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
            if (useCache) {
                processEncryptedCache();
                return encryptedCache.length + 5 + cipherFactory.getSettingsNoSecretsLength() + ((trailingArrayLengthCache == 0) ? 0 : 4);
            } else {
                if (held == null) throw new PacketException("no data");
                generateCipher(Cipher.ENCRYPT_MODE);
                trailingArrayLengthCache = (trailingPassword == null || trailingPassword.length() < 1) ? 0 : trailingPassword.getBytes(StandardCharsets.UTF_8).length;
                return 5 + cipherFactory.getSettingsNoSecretsLength() + ((trailingArrayLengthCache == 0) ? 0 : 4) + cipher.getOutputSize(loader.getPacketSize(held, true, true) + trailingArrayLengthCache);
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
     * Gets the {@link ICipherFactory} being used.
     *
     * @return The Cipher Factory.
     */
    public ICipherFactory getCipherFactory() {
        return cipherFactory;
    }

    /**
     * Sets the {@link ICipherFactory} being used.
     *
     * @param cipherFactory The Cipher Factory.
     * @throws NullPointerException cipherFactory is null.
     */
    public void setCipherFactory(ICipherFactory cipherFactory) {
        if (cipherFactory == null) throw new NullPointerException("cipherFactory is null");
        synchronized (slock) {
            this.cipherFactory = cipherFactory;
        }
    }

    /**
     * Gets the trailing password (Or null if no trailing password).
     *
     * @return The trailing password or null.
     */
    public String getTrailingPassword() {
        return trailingPassword;
    }

    /**
     * Sets the trailing password (Use null for no trailing password).
     *
     * @param trailingPassword The new trailing password or null.
     */
    public void setTrailingPassword(String trailingPassword) {
        synchronized (slock) {
            encryptedCache = null;
            this.trailingPassword = trailingPassword;
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
    @Override
    public boolean isCacheUsed() {
        return useCache;
    }

    /**
     * Sets if the encrypted data is cached.
     *
     * @param used If the encrypted data should be cached.
     */
    @Override
    public void setCacheUsed(boolean used) {
        synchronized (slock) {
            useCache = used;
            if (!useCache)
                encryptedCache = null;
        }
    }
}
