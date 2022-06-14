package com.captainalm.lib.calmnet.packet.factory;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.IStreamedPacket;
import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;
import com.captainalm.lib.calmnet.packet.core.*;

/**
 * This class provides a standard extensible {@link IPacketFactory} for calmnet packets
 * with the ability to set the {@link IPacket} of supporting packets.
 *
 * @author Captain ALM
 */
public class CALMNETPacketFactoryWithPacket extends CALMNETPacketFactory {
    protected IPacket packetToUse;

    /**
     * Constructs a new Instance of CALMNETPacketFactoryWithPacket with if {@link IStreamedPacket}s are preferred and the specified {@link PacketLoader}.
     *
     * @param preferStreamPackets If streamed packets are preferred for construction.
     * @param loader The packet loader to use.
     * @throws NullPointerException loader is null.
     */
    public CALMNETPacketFactoryWithPacket(boolean preferStreamPackets, PacketLoader loader) {
        this(preferStreamPackets, loader, null);
    }

    /**
     * Constructs a new Instance of CALMNETPacketFactoryWithPacket with if {@link IStreamedPacket}s are preferred, the specified {@link PacketLoader} and the {@link IPacketFactory}.
     *
     * @param preferStreamPackets If streamed packets are preferred for construction.
     * @param loader  The packet loader to use.
     * @param factory The packet factory to use or null (null signifies to use the same instance).
     * @throws NullPointerException loader is null.
     */
    public CALMNETPacketFactoryWithPacket(boolean preferStreamPackets, PacketLoader loader, IPacketFactory factory) {
        super(preferStreamPackets, loader, factory);
    }

    /**
     * Constructs a {@link IPacket} of the protocol specified by the passed {@link PacketProtocolInformation} instance.
     *
     * @param information The protocol information to use.
     * @return The constructed packet or null.
     * @throws NullPointerException The information is null.
     */
    @Override
    public IPacket getPacket(PacketProtocolInformation information) {
        if (information == null) throw new NullPointerException("information is null");

        if (information.equals(Base64Packet.getTheProtocol())) return new Base64Packet(factoryToUse, loaderToUse, packetToUse);
        if (information.equals(EncryptedPacket.getTheProtocol()) && cipherToUse != null) return new EncryptedPacket(factoryToUse, loaderToUse, cipherToUse, packetToUse);

        return super.getPacket(information);
    }

    /**
     * Gets the {@link IPacket} in use (Could be the same instance).
     *
     * @return The packet in use.
     */
    public IPacket getPacket() {
        return packetToUse;
    }

    /**
     * Sets the {@link IPacket} in use.
     *
     * @param packet The packet to use or null.
     */
    public void setPacket(IPacket packet) {
        packetToUse = packet;
    }
}
