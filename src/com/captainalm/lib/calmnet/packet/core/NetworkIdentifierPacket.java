package com.captainalm.lib.calmnet.packet.core;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

import java.nio.charset.StandardCharsets;

/**
 * This class provides a packet that is used to identify the type of network client is using this library.
 * <p>
 * Major ID: 255
 * Minor ID: 255
 * </p>
 *
 * @author Captain ALM
 */
public class NetworkIdentifierPacket implements IPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 255, (byte) 255);
    protected String id;

    /**
     * Constructs a new instance of NetworkIdentifierPacket with the specified ID.
     *
     * @param id The network ID of the client.
     */
    public NetworkIdentifierPacket(String id) {
        this.id = id;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return (id != null);
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

    /**
     * Saves the packet payload to a byte array.
     *
     * @return The packet payload data.
     * @throws PacketException An Exception has occurred.
     */
    @Override
    public byte[] savePayload() throws PacketException {
        if (id == null) throw new PacketException("no data");
        return id.getBytes(StandardCharsets.UTF_8);
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
        id = new String(packetData, StandardCharsets.UTF_8);
    }
}
