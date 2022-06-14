package com.captainalm.lib.calmnet.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * This class provides the packet protocol information for {@link IPacket}.
 *
 * @author Captain ALM
 */
public class PacketProtocolInformation {

    /**
     * Constructs a new instance of PacketProtocolInformation.
     * This is set to 0 Major, 0 Minor.
     */
    public PacketProtocolInformation(){
        major = 0;
        minor = 0;
    }

    /**
     * Constructs a new instance of PacketProtocolInformation.
     *
     * @param major The major version.
     * @param minor The minor version.
     */
    public PacketProtocolInformation(byte major, byte minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * The major protocol of the packet.
     */
    protected final byte major;

    /**
     * The minor protocol of the packet.
     */
    protected final byte minor;

    /**
     * Gets the major protocol for the packet.
     *
     * @return The major protocol.
     */
    public int getMajor() {
        return major & 0xFF;
    }

    /**
     * Gets the minor protocol for the packet.
     *
     * @return The minor protocol.
     */
    public int getMinor() {
        return minor & 0xFF;
    }

    /**
     * Gets the {@link PacketProtocolInformation} of the packet.
     *
     * @param inputStream The input stream for reading.
     * @return The protocol information.
     * @throws NullPointerException The inputStream is null.
     * @throws IOException A stream exception occurs.
     */
    public static PacketProtocolInformation getProtocolInformation(InputStream inputStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("inputStream is null");
        byte major = PacketLoader.readByteFromInputStream(inputStream);
        byte minor = PacketLoader.readByteFromInputStream(inputStream);
        return new PacketProtocolInformation(major, minor);
    }

    /**
     * Saves the {@link PacketProtocolInformation} of the packet.
     *
     * @param outputStream The output stream for writing.
     * @param information The protocol information.
     * @throws NullPointerException A parameter is null.
     * @throws IOException A stream exception occurs.
     */
    public static void savePacketProtocolInformation(OutputStream outputStream, PacketProtocolInformation information) throws IOException {
        if (outputStream == null) throw new NullPointerException("outputStream is null");
        if (information == null) throw new NullPointerException("information is null");
        outputStream.write(information.getMajor());
        outputStream.write(information.getMinor());
    }

    /**
     * Gets whether this object equals the passed object.
     *
     * @param o The object to check.
     * @return If the objects are equivalent.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PacketProtocolInformation)) return false;
        PacketProtocolInformation that = (PacketProtocolInformation) o;
        return major == that.major && minor == that.minor;
    }

    /**
     * Gets the hash code of the object.
     *
     * @return The hash code of the object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }
}
