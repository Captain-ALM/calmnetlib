package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.IPacket;

/**
 * This abstract base class provides the ability for packets to return an ID.
 *
 * @author Captain ALM
 */
public abstract class FragmentPIDPacket implements IPacket {
    protected Integer packetID;

    /**
     * Constructs a new FragmentPIDPacket given the packet ID.
     *
     * @param packetID The packet ID.
     * @throws IllegalArgumentException packetID is less than 0.
     */
    public FragmentPIDPacket(Integer packetID) {
        if (packetID != null && packetID < 0) throw new IllegalArgumentException("packetID is less than 0");
        this.packetID = packetID;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return (packetID != null);
    }

    /**
     * Gets the packet ID or null.
     *
     * @return The packet ID or null.
     */
    public Integer getPacketID() {
        return packetID;
    }
}
