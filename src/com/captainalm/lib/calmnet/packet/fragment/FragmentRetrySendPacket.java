package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

/**
 * This class provides a packet for signalling that the sending end
 * should start re-sending un acknowledged fragment packets.
 * <p>
 * Major ID: 254
 * Minor ID: 6
 * </p>
 *
 * @author Captain ALM
 */
public class FragmentRetrySendPacket extends FragmentPIDAKNPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 254, (byte) 6);

    /**
     * Constructs a new FragmentRetrySendPacket given the packet ID and the acknowledgement value.
     *
     * @param packetID        The packet ID.
     * @param acknowledgement The acknowledgement value to use.
     * @throws IllegalArgumentException packetID is less than 0.
     */
    public FragmentRetrySendPacket(Integer packetID, Boolean acknowledgement) {
        super(packetID, acknowledgement);
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
