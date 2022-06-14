package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

/**
 * This class provides a packet for sending an optional payload with a packetID and fragmentID
 * as a response for {@link FragmentMessagePacket}.
 * <p>
 * Major ID: 254
 * Minor ID: 4
 * </p>
 *
 * @author Captain ALM
 */
public class FragmentMessageResponsePacket extends FragmentPIDMSGPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 254, (byte) 4);

    /**
     * Constructs a new FragmentMessageResponsePacket given the packet ID, fragment ID and payload.
     *
     * @param packetID   The packet ID.
     * @param fragmentID The fragment ID.
     * @param payload    The payload to store.
     * @throws IllegalArgumentException packetID or fragmentID is less than 0.
     */
    public FragmentMessageResponsePacket(Integer packetID, Integer fragmentID, byte[] payload) {
        super(packetID, fragmentID, payload);
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
}
