package com.captainalm.lib.calmnet.packet;

/**
 * This interface provides the packet methods.
 *
 * @author Captain ALM
 */
public interface IPacket {
    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    boolean isValid();

    /**
     * Gets the protocol information.
     *
     * @return The protocol information.
     */
    PacketProtocolInformation getProtocol();

    /**
     * Saves the packet payload to a byte array.
     *
     * @return The packet payload data.
     * @throws PacketException An Exception has occurred.
     */
    byte[] savePayload() throws PacketException;

    /**
     * Loads the packet payload from save data.
     *
     * @param packetData The packet payload data.
     * @throws NullPointerException The new store data is null.
     * @throws PacketException An Exception has occurred.
     */
    void loadPayload(byte[] packetData) throws PacketException;
}
