package com.captainalm.lib.calmnet.packet;

import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.stdcrypt.digest.DigestComparer;
import com.captainalm.lib.stdcrypt.digest.DigestProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;

import static com.captainalm.lib.calmnet.packet.PacketProtocolInformation.getProtocolInformation;
import static com.captainalm.lib.calmnet.packet.PacketProtocolInformation.savePacketProtocolInformation;

/**
 * This class provides the ability to load and save {@link IPacket}
 * to {@link java.io.InputStream} and {@link java.io.OutputStream}.
 * Packets can have contents checking support using {@link DigestProvider}.
 *
 * @author Captain ALM
 */
public class PacketLoader {
    protected boolean allowInvalidPackets;

    protected boolean oldPacketFormat;

    /**
     * Constructs a new Packet loader instance.
     * If using a digest provider, use {@link #PacketLoader(DigestProvider)}
     */
    public PacketLoader() {
        this(null, false);
    }

    /**
     * Constructs a new Packet loader instance with the specified {@link DigestProvider}.
     * If using a digest provider, make sure all endpoints use the same algorithm;
     * if null, no trailer is created;
     * this is ignored if saving / loading packets from byte arrays.
     *
     * @param provider The digest provider or null.
     */
    public PacketLoader(DigestProvider provider) {
        this(provider, false);
    }

    /**
     * Constructs a new Packet loader instance with the specified {@link DigestProvider}
     * and if the old packet format should be used.
     * If using a digest provider, make sure all endpoints use the same algorithm;
     * if null, no trailer is created;
     * this is ignored if saving / loading packets from byte arrays.
     *
     * @param provider The digest provider or null.
     * @param oldPacketFormat If the old packet format should be used (No explicit hash indication nor length).
     */
    public PacketLoader(DigestProvider provider, boolean oldPacketFormat) {
        hashProvider = provider;
        this.oldPacketFormat = oldPacketFormat;
    }

    protected DigestProvider hashProvider;

    /**
     * This field provides the {@link DigestProvider} to use for the payload of the packets on the trailer.
     *
     * @return The digest provider in use or null.
     */
    public DigestProvider getHashProvider() {
        return hashProvider;
    }

    /**
     * Gets whether invalid packets are allowed to be read and written.
     *
     * @return If invalid packets can be processed.
     */
    public boolean areInvalidPacketsAllowed() {
        return allowInvalidPackets;
    }

    /**
     * This sets whether invalid packets are allowed to be read and written.
     *
     * @param allowInvalidPackets If invalid packets can be processed.
     */
    public void setAllowInvalidPackets(boolean allowInvalidPackets) {
        this.allowInvalidPackets = allowInvalidPackets;
    }

    /**
     * Is the old packet format in use (No explicit hash indication nor length).
     *
     * @return If the old packet format is in use.
     */
    public boolean isOldPacketFormatInUse() {
        return oldPacketFormat;
    }

    /**
     * Sets if the old packet format should be used (No explicit hash indication nor length).
     * @param useOldFormat If the old packet format should be used.
     */
    public void setOldPacketFormatUsage(boolean useOldFormat) {
        oldPacketFormat = useOldFormat;
    }

    protected boolean isPacketInvalid(IPacket packetIn) {
        return (packetIn == null || !packetIn.isValid()) && !allowInvalidPackets;
    }

    /**
     * Adds the most significant flag to the given integer.
     *
     * @param value The integer to add the flag to.
     * @return The integer with the flag added.
     */
    public static int addMostSignificantFlag(int value) {
        value += 1;
        value += Integer.MAX_VALUE;
        return value;
    }

    /**
     * Subtracts the most significant flag from the given integer.
     *
     * @param value The integer to subtract the flag from.
     * @return The integer with the flag subtracted.
     */
    public static int subtractMostSignificantFlag(int value) {
        value -= 1;
        value -= Integer.MAX_VALUE;
        return value;
    }

    /**
     * Reads a {@link IPacket} from a byte array (No digest support).
     * If the information parameter is null, this is obtained as part of the reading.
     * NOTE: The {@link #getHashProvider()} for digests is NOT supported and no digest is expected for these packets.
     *
     * @param arrayIn The byte array for reading.
     * @param factory The {@link IPacketFactory} to use to generate packets.
     * @param information The protocol information or null.
     * @return The loaded packet or null.
     * @throws NullPointerException The arrayIn or the factory is null.
     * @throws PacketException An Exception has occurred.
     */
    public IPacket readPacketNoDigest(byte[] arrayIn, IPacketFactory factory, PacketProtocolInformation information) throws PacketException {
        if (arrayIn == null) throw new NullPointerException("arrayIn is null");
        if (factory == null) throw new NullPointerException("factory is null");

        if (information == null) {
            if (arrayIn.length < 2) throw new PacketException("arrayIn does not have an information header.");
            information = new PacketProtocolInformation(arrayIn[0], arrayIn[1]);
        }

        IPacket toret = factory.getPacket(information);

        if (toret != null) {
            if (arrayIn.length < 6) throw new PacketException("arrayIn does not have a length header.");
            int length = (arrayIn[2] & 0xff) * 16777216 + (arrayIn[3] & 0xff) * 65536 + (arrayIn[4] & 0xff) * 256 + (arrayIn[5] & 0xff);
            if (length < 0) length = subtractMostSignificantFlag(length);
            byte[] loadArray = new byte[length];
            System.arraycopy(arrayIn, 6, loadArray, 0, Math.min(arrayIn.length - 6, length));
            toret.loadPayload(loadArray);
            if (isPacketInvalid(toret)) toret = null;
        }
        return toret;
    }

    /**
     * Reads a {@link IPacket} from an input stream.
     * If the information parameter is null, this is obtained as part of the reading.
     *
     * @param inputStream The input stream for reading.
     * @param factory The {@link IPacketFactory} to use to generate packets.
     * @param information The protocol information or null.
     * @return The loaded packet or null.
     * @throws NullPointerException The inputStream or the factory is null.
     * @throws IOException A stream exception occurs.
     * @throws PacketException An Exception has occurred.
     */
    public IPacket readPacket(InputStream inputStream, IPacketFactory factory, PacketProtocolInformation information) throws IOException, PacketException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (factory == null) throw new NullPointerException("factory is null");

        if (information == null) information = getProtocolInformation(inputStream);

        IPacket toret = factory.getPacket(information);

        if (toret != null) {
            int length = readInteger(inputStream);
            boolean hasHash = length < 0;
            if (hasHash) length = subtractMostSignificantFlag(length);
            InputStream lIS = (hashProvider == null || !hasHash) ? inputStream : hashProvider.getDigestInputStream(inputStream);
            byte[] loadArray = readArrayFromInputStream(lIS, length);
            int hashLength;
            if (hasHash) {
                hashLength = readByteIntegerFromInputStream(inputStream);
                if (hashProvider != null && hashProvider.getLength() != hashLength) {
                    readArrayFromInputStream(inputStream, hashLength);
                    return null;
                }
            } else hashLength = 0;
            if ((!hasHash && !oldPacketFormat) || hashProvider == null) {
                readArrayFromInputStream(inputStream, hashLength);
                toret.loadPayload(loadArray);
            } else if (DigestComparer.compareDigests(inputStream, ((DigestInputStream) lIS).getMessageDigest().digest())) toret.loadPayload(loadArray);
            if (isPacketInvalid(toret)) toret = null;
        }
        return toret;
    }

    /**
     * Reads a {@link IPacket} from an input stream (No digest support).
     * If the information parameter is null, this is obtained as part of the reading.
     * NOTE: The {@link #getHashProvider()} for digests is NOT supported and no digest is expected for these packets.
     *
     * @param inputStream The input stream for reading.
     * @param factory The {@link IPacketFactory} to use to generate packets.
     * @param information The protocol information or null.
     * @return The loaded packet or null.
     * @throws NullPointerException The inputStream or the factory is null.
     * @throws IOException A stream exception occurs.
     * @throws PacketException An Exception has occurred.
     */
    public IPacket readPacketNoDigest(InputStream inputStream, IPacketFactory factory, PacketProtocolInformation information) throws IOException, PacketException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (factory == null) throw new NullPointerException("factory is null");

        if (information == null) information = getProtocolInformation(inputStream);

        IPacket toret = factory.getPacket(information);

        if (toret != null) {
            int length = readInteger(inputStream);
            if (length < 0) length = subtractMostSignificantFlag(length);
            byte[] loadArray = readArrayFromInputStream(inputStream, length);
            toret.loadPayload(loadArray);
            if (isPacketInvalid(toret)) toret = null;
        }
        return toret;
    }

    /**
     * Reads a {@link IStreamedPacket} from an input stream.
     * If the information parameter is null, this is obtained as part of the reading.
     * NOTE: The packet may be an {@link IPacket} if no stream packet is available for that protocol.
     *
     * @param inputStream The input stream for reading.
     * @param factory The {@link IPacketFactory} to use to generate packets.
     * @param information The protocol information or null.
     * @return The loaded packet or null.
     * @throws NullPointerException The inputStream or the factory is null.
     * @throws IOException A stream exception occurs.
     * @throws PacketException An Exception has occurred.
     */
    public IPacket readStreamedPacket(InputStream inputStream, IPacketFactory factory, PacketProtocolInformation information) throws IOException, PacketException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (factory == null) throw new NullPointerException("factory is null");

        if (information == null) information = getProtocolInformation(inputStream);

        IPacket toret = factory.getPacket(information);

        if (toret instanceof IStreamedPacket) {
            int length = readInteger(inputStream);
            boolean hasHash = length < 0;
            if (hasHash) length = subtractMostSignificantFlag(length);
            InputStream lIS = (hashProvider == null || !hasHash) ? inputStream : hashProvider.getDigestInputStream(inputStream);
            ((IStreamedPacket) toret).writeData(lIS, length);
            int hashLength;
            if (hasHash) {
                hashLength = readByteIntegerFromInputStream(inputStream);
                if (hashProvider != null && hashProvider.getLength() != hashLength) {
                    readArrayFromInputStream(inputStream, hashLength);
                    return null;
                }
            } else hashLength = 0;
            if ((hasHash || oldPacketFormat) && hashProvider != null) {
                if (!DigestComparer.compareDigests(inputStream, ((DigestInputStream) lIS).getMessageDigest().digest())) toret = null;
            } else readArrayFromInputStream(inputStream, hashLength);
            if (isPacketInvalid(toret)) toret = null;
        } else if (toret != null) {
            return readPacket(inputStream, factory, information);
        }
        return toret;
    }

    /**
     * Reads a {@link IStreamedPacket} from an input stream (No digest support).
     * If the information parameter is null, this is obtained as part of the reading.
     * NOTE: The packet may be an {@link IPacket} if no stream packet is available for that protocol.
     * NOTE: The {@link #getHashProvider()} for digests is NOT supported and no digest is expected for these packets.
     *
     * @param inputStream The input stream for reading.
     * @param factory The {@link IPacketFactory} to use to generate packets.
     * @param information The protocol information or null.
     * @return The loaded packet or null.
     * @throws NullPointerException The inputStream or the factory is null.
     * @throws IOException A stream exception occurs.
     * @throws PacketException An Exception has occurred.
     */
    public IPacket readStreamedPacketNoDigest(InputStream inputStream, IPacketFactory factory, PacketProtocolInformation information) throws IOException, PacketException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (factory == null) throw new NullPointerException("factory is null");

        if (information == null) information = getProtocolInformation(inputStream);

        IPacket toret = factory.getPacket(information);

        if (toret instanceof IStreamedPacket) {
            int length = readInteger(inputStream);
            if (length < 0) length = subtractMostSignificantFlag(length);
            ((IStreamedPacket) toret).writeData(inputStream, length);
            if (isPacketInvalid(toret)) toret = null;
        } else if (toret != null) {
            return readPacketNoDigest(inputStream, factory, information);
        }
        return toret;
    }

    /**
     * Returns a {@link IPacket} to a byte array (No digest support).
     * NOTE: The {@link #getHashProvider()} for digests is NOT supported and no digest is expected for these packets.
     *
     * @param packet The packet to save.
     * @param writeInformation Write the {@link PacketProtocolInformation} to the beginning of the array.
     * @return The written packet data as a byte array.
     * @throws NullPointerException A parameter is null.
     * @throws PacketException An Exception has occurred.
     */
    public byte[] writePacketNoDigest(IPacket packet, boolean writeInformation) throws PacketException {
        if (packet == null) throw new NullPointerException("packet is null");
        if (isPacketInvalid(packet)) throw new PacketException("packet is invalid");

        byte[] header = (writeInformation) ? new byte[6] : new byte[4];
        if (writeInformation) {
            header[0] = (byte) packet.getProtocol().getMajor();
            header[1] = (byte) packet.getProtocol().getMinor();
        }

        byte[] saveArray = packet.savePayload();
        int length = saveArray.length;
        header[((writeInformation) ? 2 : 0)] = (byte) (length / 16777216);
        length %= 16777216;
        header[((writeInformation) ? 3 : 1)] = (byte) (length / 65536);
        length %= 65536;
        header[((writeInformation) ? 4 : 2)] = (byte) (length / 256);
        length %= 256;
        header[((writeInformation) ? 5 : 3)] = (byte) (length);

        byte[] toret = new byte[header.length + saveArray.length];
        System.arraycopy(header, 0, toret, 0, header.length);
        System.arraycopy(saveArray, 0, toret, header.length, saveArray.length);
        return toret;
    }

    /**
     * Writes a {@link IPacket} to an output stream (No digest support).
     *
     * @param outputStream The output stream for writing.
     * @param packet The packet to save.
     * @param writeInformation Write the {@link PacketProtocolInformation} to the stream.
     * @throws NullPointerException A parameter is null.
     * @throws IOException A stream exception occurs.
     * @throws PacketException An Exception has occurred.
     */
    public void writePacket(OutputStream outputStream, IPacket packet, boolean writeInformation) throws IOException, PacketException {
        if (outputStream == null) throw new NullPointerException("outputStream is null");
        if (packet == null) throw new NullPointerException("packet is null");
        if (isPacketInvalid(packet)) throw new PacketException("packet is invalid");

        if (writeInformation) savePacketProtocolInformation(outputStream, packet.getProtocol());

        if (packet instanceof IStreamedPacket) {
            int pLength = ((IStreamedPacket) packet).getSize();
            if (hashProvider != null && !oldPacketFormat) pLength = addMostSignificantFlag(pLength);
            writeInteger(outputStream, pLength);
            OutputStream lOS = (hashProvider == null) ? outputStream : hashProvider.getDigestOutputStream(outputStream);
            ((IStreamedPacket) packet).readData(lOS);
            if (hashProvider != null) {
                if (!oldPacketFormat) outputStream.write(hashProvider.getLength());
                outputStream.write(((DigestOutputStream) lOS).getMessageDigest().digest());
            }
        } else {
            byte[] saveArray = packet.savePayload();
            int pLength = saveArray.length;
            if (hashProvider != null && !oldPacketFormat) pLength = addMostSignificantFlag(pLength);
            writeInteger(outputStream, pLength);
            outputStream.write(saveArray);
            if (hashProvider != null) {
                if (!oldPacketFormat) outputStream.write(hashProvider.getLength());
                outputStream.write(hashProvider.getDigestOf(saveArray));
            }
        }
        outputStream.flush();
    }

    /**
     * Writes a {@link IPacket} to an output stream.
     * NOTE: The {@link #getHashProvider()} for digests is NOT supported and no digest is expected for these packets.
     *
     * @param outputStream The output stream for writing.
     * @param packet The packet to save.
     * @param writeInformation Write the {@link PacketProtocolInformation} to the stream.
     * @throws NullPointerException A parameter is null.
     * @throws IOException A stream exception occurs.
     * @throws PacketException An Exception has occurred.
     */
    public void writePacketNoDigest(OutputStream outputStream, IPacket packet, boolean writeInformation) throws IOException, PacketException {
        if (outputStream == null) throw new NullPointerException("outputStream is null");
        if (packet == null) throw new NullPointerException("packet is null");
        if (isPacketInvalid(packet)) throw new PacketException("packet is invalid");

        if (writeInformation) savePacketProtocolInformation(outputStream, packet.getProtocol());

        if (packet instanceof IStreamedPacket) {
            writeInteger(outputStream, ((IStreamedPacket) packet).getSize());
            ((IStreamedPacket) packet).readData(outputStream);
        } else {
            byte[] saveArray = packet.savePayload();
            writeInteger(outputStream, saveArray.length);
            outputStream.write(saveArray);
        }
        outputStream.flush();
    }

    /**
     * Reads an Integer from an {@link InputStream}.
     *
     * @param inputStream The input stream to use.
     * @return The integer that was stored.
     * @throws NullPointerException inputStream is null.
     * @throws IOException An I/O error has occurred.
     */
    public static int readInteger(InputStream inputStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        int length = readByteIntegerFromInputStream(inputStream)* 16777216;
        length += readByteIntegerFromInputStream(inputStream) * 65536;
        length += readByteIntegerFromInputStream(inputStream) * 256;
        length += readByteIntegerFromInputStream(inputStream);
        return length;
    }

    /**
     * Writes an Integer to the {@link OutputStream} using 4 bytes.
     *
     * @param outputStream The output stream to use.
     * @param i The integer to store.
     * @throws NullPointerException outputStream is null.
     * @throws IOException An I/O error has occurred.
     */
    public static void writeInteger(OutputStream outputStream, int i) throws IOException {
        if (outputStream == null) throw new NullPointerException("outputStream is null");
        boolean neg = i < 0;
        if (i < 0) i = -(Integer.MIN_VALUE - i);
        outputStream.write((i / 16777216) + ((neg) ? 128 : 0));
        i %= 16777216;
        outputStream.write(i / 65536);
        i %= 65536;
        outputStream.write(i / 256);
        i %= 256;
        outputStream.write(i);
    }

    /**
     * Reads a byte from an {@link InputStream}.
     * See also: {@link #readByteIntegerFromInputStream(InputStream)}.
     *
     * @param inputStream The input stream to read from.
     * @return The byte read.
     * @throws NullPointerException inputStream is null.
     * @throws IOException An I/O error has occurred or end of stream has been reached.
     */
    public static byte readByteFromInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        int toret;
        if ((toret = inputStream.read()) == -1) throw new IOException("inputStream end of stream");
        return (byte) toret;
    }

    /**
     * Reads a byte (In int form) from an {@link InputStream}.
     * See also: {@link #readByteFromInputStream(InputStream)}.
     *
     * @param inputStream The input stream to read from.
     * @return The byte read (As an int).
     * @throws NullPointerException inputStream is null.
     * @throws IOException An I/O error has occurred or end of stream has been reached.
     */
    public static int readByteIntegerFromInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        int toret;
        if ((toret = inputStream.read()) == -1) throw new IOException("inputStream end of stream");
        return toret;
    }

    /**
     * Reads in a byte array of a specified length from an {@link InputStream}.
     *
     * @param inputStream The input stream to read from.
     * @param length The length of the stream.
     * @return The array of read bytes.
     * @throws NullPointerException inputStream is null.
     * @throws IllegalArgumentException length is less than 0.
     * @throws IOException An I/O error occurs or end of stream has been reached.
     */
    public static byte[] readArrayFromInputStream(InputStream inputStream, int length) throws IOException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        if (length < 0) throw new IllegalArgumentException("length is less than 0");
        byte[] toret = new byte[length];
        int offset = 0;
        int slen;
        while (offset < length) if ((slen = inputStream.read(toret, offset, length - offset)) != -1) offset += slen; else throw new IOException("inputStream end of stream");
        return toret;
    }

    /**
     * Saves an Integer into a byte array.
     *
     * @param i The integer to save.
     * @return The byte array.
     */
    public static byte[] getByteArrayFromInteger(int i) {
        boolean neg = i < 0;
        if (i < 0) i = -(Integer.MIN_VALUE - i);
        byte[] toret = new byte[4];
        toret[0] = (byte) ((i / 16777216) + ((neg) ? 128 : 0));
        i %= 16777216;
        toret[1] = (byte) (i / 65536);
        i %= 65536;
        toret[2] = (byte) (i / 256);
        i %= 256;
        toret[3] = (byte) (i);
        return toret;
    }

    /**
     * Loads an Integer from a byte array.
     *
     * @param bytes The byte array.
     * @return The integer.
     * @throws NullPointerException bytes is null.
     * @throws IllegalArgumentException bytes length is not 4.
     */
    public static int getIntegerFromByteArray(byte[] bytes) {
        if (bytes == null) throw new NullPointerException("bytes is null");
        if (bytes.length != 4) throw new IllegalArgumentException("bytes length is not 4");
        return (bytes[0] & 0xff) * 16777216 + (bytes[1] & 0xff) * 65536 + (bytes[2] & 0xff) * 256 + (bytes[3] & 0xff);
    }

    /**
     * Gets the total size of a written packet in bytes.
     *
     * @param packet The packet to check.
     * @param includeInformation If the 2 byte information header is included.
     * @param ignoreDigest If the digest length should be ignored if available.
     * @return The size of the packet in bytes.
     * @throws NullPointerException packet is null.
     * @throws PacketException A Packet Exception has occurred.
     */
    public int getPacketSize(IPacket packet, boolean includeInformation, boolean ignoreDigest) throws PacketException {
        if (packet == null) throw new NullPointerException("packet is null");
        return 4 + ((includeInformation) ? 2 : 0) + ((packet instanceof IStreamedPacket) ? ((IStreamedPacket) packet).getSize() : packet.savePayload().length)
                + ((ignoreDigest || hashProvider == null) ? 0 : hashProvider.getLength());
    }
}
