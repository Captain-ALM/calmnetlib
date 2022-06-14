package com.captainalm.lib.calmnet.packet.factory;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.IStreamedPacket;
import com.captainalm.lib.calmnet.packet.PacketLoader;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;
import com.captainalm.lib.calmnet.packet.core.*;
import com.captainalm.lib.calmnet.packet.fragment.*;
import com.captainalm.lib.stdcrypt.encryption.ICipherFactory;

/**
 * This class provides a standard extensible {@link IPacketFactory} for calmnet packets.
 *
 * @author Captain ALM
 */
public class CALMNETPacketFactory implements IPacketFactory {
    protected IPacketFactory factoryToUse;
    protected PacketLoader loaderToUse;
    protected ICipherFactory cipherToUse;
    protected boolean streamPreferred;

    /**
     * Constructs a new Instance of CALMNETPacketFactory with if {@link IStreamedPacket}s are preferred and the specified {@link PacketLoader}.
     *
     * @param preferStreamPackets If streamed packets are preferred for construction.
     * @param loader The packet loader to use.
     * @throws NullPointerException loader is null.
     */
    public CALMNETPacketFactory(boolean preferStreamPackets, PacketLoader loader) {
        this(preferStreamPackets ,loader, null);
    }

    /**
     * Constructs a new Instance of CALMNETPacketFactory with if {@link IStreamedPacket}s are preferred, the specified {@link PacketLoader} and the {@link IPacketFactory}.
     *
     * @param preferStreamPackets If streamed packets are preferred for construction.
     * @param loader The packet loader to use.
     * @param factory The packet factory to use or null (null signifies to use the same instance).
     * @throws NullPointerException loader is null.
     */
    public CALMNETPacketFactory(boolean preferStreamPackets, PacketLoader loader, IPacketFactory factory) {
        streamPreferred = preferStreamPackets;
        setPacketLoader(loader);
        setPacketFactory(factory);
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

        if (information.equals(Base64Packet.getTheProtocol())) return new Base64Packet(factoryToUse, loaderToUse);
        if (information.equals(EncryptedPacket.getTheProtocol()) && cipherToUse != null) return new EncryptedPacket(factoryToUse, loaderToUse, cipherToUse);
        if (information.equals(NetworkEncryptionUpgradePacket.getTheProtocol())) return new NetworkEncryptionUpgradePacket(null, false, false, cipherToUse);
        if (information.equals(NetworkIdentifierPacket.getTheProtocol())) return new NetworkIdentifierPacket(null);
        if (information.equals(NetworkSSLUpgradePacket.getTheProtocol())) return new NetworkSSLUpgradePacket(null);

        if (information.equals(FragmentAllocatePacket.getTheProtocol())) return new FragmentAllocatePacket(null, null);
        if (information.equals(FragmentAllocationPacket.getTheProtocol())) return new FragmentAllocationPacket(null, null, null);
        if (information.equals(FragmentMessagePacket.getTheProtocol())) return new FragmentMessagePacket(null, null, null);
        if (information.equals(FragmentMessageResponsePacket.getTheProtocol())) return new FragmentMessageResponsePacket(null, null, null);
        if (information.equals(FragmentRetrySendPacket.getTheProtocol())) return new FragmentRetrySendPacket(null, null);
        if (information.equals(FragmentSendCompletePacket.getTheProtocol())) return new FragmentSendCompletePacket(null, null);
        if (information.equals(FragmentSendStopPacket.getTheProtocol())) return new FragmentSendStopPacket(null);

        return null;
    }

    /**
     * Gets if {@link #getPacket(PacketProtocolInformation)} prefers returning {@link IStreamedPacket}s if possible.
     *
     * @return If streamed packets are preferred for construction.
     */
    @Override
    public boolean areStreamPacketsPreferred() {
        return streamPreferred;
    }

    /**
     * Sets if {@link #getPacket(PacketProtocolInformation)} prefers returning {@link IStreamedPacket}s if possible.
     *
     * @param preferred If streamed packets are preferred for construction.
     */
    @Override
    public void setStreamPacketsPreferred(boolean preferred) {
        streamPreferred = preferred;
    }

    /**
     * Gets the {@link IPacketFactory} in use (Could be the same instance).
     *
     * @return The packet factory in use.
     */
    public IPacketFactory getPacketFactory() {
        return factoryToUse;
    }

    /**
     * Sets the {@link IPacketFactory} in use (null signifies to use the same instance).
     *
     * @param factory The packet factory to use or null.
     */
    public void setPacketFactory(IPacketFactory factory) {
        if (factory == null) factoryToUse = this; else factoryToUse = factory;
    }

    /**
     * Gets the {@link PacketLoader} in use.
     *
     * @return The packet loader in use.
     */
    public PacketLoader getPacketLoader() {
        return loaderToUse;
    }

    /**
     * Sets the {@link PacketLoader} to use.
     *
     * @param loader The packet loader to use.
     * @throws NullPointerException loader is null.
     */
    public void setPacketLoader(PacketLoader loader) {
        if (loader == null) throw new NullPointerException("loader is null");
        loaderToUse = loader;
    }

    /**
     * Gets the {@link ICipherFactory} in use (Could be the same instance).
     *
     * @return The cipher factory in use.
     */
    public ICipherFactory getCipherFactory() {
        return cipherToUse;
    }

    /**
     * Sets the {@link ICipherFactory} in use.
     *
     * @param factory The cipher factory to use or null.
     */
    public void setCipherFactory(ICipherFactory factory) {
        cipherToUse = factory;
    }
}
