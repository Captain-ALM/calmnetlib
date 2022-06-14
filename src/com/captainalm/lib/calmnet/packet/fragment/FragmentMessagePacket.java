package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

/**
 * This class provides a packet for sending a payload with a packetID and fragmentID.
 * The response packet is: {@link FragmentMessageResponsePacket}.
 * <p>
 * Major ID: 254
 * Minor ID: 3
 * </p>
 *
 * @author Captain ALM
 */
public class FragmentMessagePacket extends FragmentPIDMSGPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 254, (byte) 3);

    /**
     * Constructs a new FragmentMessagePacket given the packet ID, fragment ID and payload.
     *
     * @param packetID   The packet ID.
     * @param fragmentID The fragment ID.
     * @param payload    The payload to store.
     * @throws IllegalArgumentException packetID or fragmentID is less than 0.
     */
    public FragmentMessagePacket(Integer packetID, Integer fragmentID, byte[] payload) {
        super(packetID, fragmentID, payload);
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return super.isValid() && (payload != null);
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
        if (payload == null) throw new PacketException("no data");
        return super.savePayload();
    }
}
