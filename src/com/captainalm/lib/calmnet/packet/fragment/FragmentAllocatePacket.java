package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.captainalm.lib.calmnet.packet.PacketLoader.getByteArrayFromInteger;
import static com.captainalm.lib.calmnet.packet.PacketLoader.getIntegerFromByteArray;

/**
 * This class provides a packet for fragment allocation requesting.
 * The response packet is: {@link FragmentAllocationPacket}.
 * <p>
 * Major ID: 254
 * Minor ID: 1
 * </p>
 *
 * @author Captain ALM
 */
public class FragmentAllocatePacket implements IPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 254, (byte) 1);

    protected Integer fragmentCount;
    protected UUID allocationID;

    /**
     * Constructs a new FragmentAllocatePacket given the fragment count and allocation UUID.
     *
     * @param fragmentCount The number of fragments to allocate.
     * @param allocationID The allocation ID.
     * @throws IllegalArgumentException fragmentCount is less than 1.
     */
    public FragmentAllocatePacket(Integer fragmentCount, UUID allocationID) {
        if (fragmentCount != null && fragmentCount < 1) throw new IllegalArgumentException("fragmentCount is less than 1");
        this.fragmentCount = fragmentCount;
        this.allocationID = (allocationID == null) ? UUID.randomUUID() : allocationID;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return (fragmentCount != null) && (allocationID != null);
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
        if (fragmentCount == null || allocationID == null) throw new PacketException("no data");
        ByteBuffer buffer = ByteBuffer.wrap(new byte[20]);
        buffer.put(getByteArrayFromInteger(fragmentCount));
        buffer.putLong(allocationID.getMostSignificantBits());
        buffer.putLong(allocationID.getLeastSignificantBits());
        return buffer.array();
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
        if (packetData.length != 20) throw new PacketException("packet length is not 20");
        ByteBuffer buffer = ByteBuffer.wrap(packetData);
        byte[] toProcess = new byte[4];
        buffer.get(toProcess);
        fragmentCount = getIntegerFromByteArray(toProcess);
        long mostSig = buffer.getLong();
        allocationID = new UUID(mostSig, buffer.getLong());
    }

    /**
     * Gets the number of fragments or null.
     *
     * @return The number of fragments or null.
     */
    public Integer getFragmentCount() {
        return fragmentCount;
    }

    /**
     * Gets the allocation ID or null.
     *
     * @return The allocation ID or null.
     */
    public UUID getAllocationID() {
        return allocationID;
    }
}
