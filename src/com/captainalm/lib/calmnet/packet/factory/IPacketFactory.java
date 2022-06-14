package com.captainalm.lib.calmnet.packet.factory;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.IStreamedPacket;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

/**
 * This interface provides the ability to construct {@link IPacket}s given their {@link PacketProtocolInformation}.
 *
 * @author Captain ALM
 */
public interface IPacketFactory {
    /**
     * Constructs a {@link IPacket} of the protocol specified by the passed {@link PacketProtocolInformation} instance.
     *
     * @param information The protocol information to use.
     * @throws NullPointerException The information is null.
     * @return The constructed packet or null.
     */
    IPacket getPacket(PacketProtocolInformation information);

    /**
     * Gets if {@link #getPacket(PacketProtocolInformation)} prefers returning {@link IStreamedPacket}s if possible.
     *
     * @return If streamed packets are preferred for construction.
     */
    boolean areStreamPacketsPreferred();

    /**
     * Sets if {@link #getPacket(PacketProtocolInformation)} prefers returning {@link IStreamedPacket}s if possible.
     *
     * @param preferred If streamed packets are preferred for construction.
     */
    void setStreamPacketsPreferred(boolean preferred);
}
