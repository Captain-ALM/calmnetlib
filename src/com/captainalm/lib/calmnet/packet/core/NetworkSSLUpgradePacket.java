package com.captainalm.lib.calmnet.packet.core;

import com.captainalm.lib.calmnet.packet.IAcknowledgement;
import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

/**
 * This class provides the ability for supporting streams to upgrade to using SSL connections.
 * <p>
 * Major ID: 255
 * Minor ID: 254
 * </p>
 *
 * @author Captain ALM
 */
public class NetworkSSLUpgradePacket implements IPacket, IAcknowledgement {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 255, (byte) 254);

    protected Boolean acknowledgement;

    /**
     * Constructs a new NetworkSSLUpgrade packet with the specified acknowledgement value.
     *
     * @param acknowledgement The acknowledgement value to use (Can be null).
     */
    public NetworkSSLUpgradePacket(Boolean acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return (acknowledgement != null);
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
        if (acknowledgement == null) throw new PacketException("no data");
        return new byte[] {((acknowledgement) ? (byte) 1 : (byte) 0)};
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
        if (packetData.length != 1) throw new PacketException("no data");
        acknowledgement = (packetData[0] == 1);
        if (!acknowledgement && packetData[0] != 0) acknowledgement = null;
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
}
