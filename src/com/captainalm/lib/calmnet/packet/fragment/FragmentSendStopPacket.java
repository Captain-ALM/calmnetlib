package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

import static com.captainalm.lib.calmnet.packet.PacketLoader.getByteArrayFromInteger;
import static com.captainalm.lib.calmnet.packet.PacketLoader.getIntegerFromByteArray;

/**
 * This class provides a packet for stopping the remote {@link FragmentSender}.
 * <p>
 * Major ID: 254
 * Minor ID: 7
 * </p>
 *
 * @author Captain ALM
 */
public class FragmentSendStopPacket extends FragmentPIDPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 254, (byte) 7);

    /**
     * Constructs a new FragmentSendStopPacket given the packet ID.
     *
     * @param packetID The packet ID.
     * @throws IllegalArgumentException packetID is less than 0.
     */
    public FragmentSendStopPacket(Integer packetID) {
        super(packetID);
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
        if (packetID == null) throw new PacketException("no data");
        return getByteArrayFromInteger(packetID);
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
        if (packetData.length != 4) throw new PacketException("packet length is not 4");
        packetID = getIntegerFromByteArray(packetData);
    }
}
