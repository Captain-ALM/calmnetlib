package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.captainalm.lib.calmnet.packet.PacketLoader.getByteArrayFromInteger;
import static com.captainalm.lib.calmnet.packet.PacketLoader.getIntegerFromByteArray;

/**
 * This class provides a packet for giving the allocated packetID
 * as a response for {@link FragmentAllocatePacket}.
 * <p>
 * Major ID: 254
 * Minor ID: 2
 * </p>
 *
 * @author Captain ALM
 */
public class FragmentAllocationPacket extends FragmentPIDPacket {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 254, (byte) 2);

    protected Boolean success;
    protected UUID allocationID;

    /**
     * Constructs a new FragmentAllocationPacket given the packet ID, allocation ID and if it's successful.
     *
     * @param packetID The packet ID.
     * @param allocationID The allocation ID.
     * @param success The allocation was successful.
     * @throws IllegalArgumentException packetID is less than 0.
     */
    public FragmentAllocationPacket(Integer packetID, UUID allocationID, Boolean success) {
        super(packetID);
        this.success = success;
        this.allocationID = allocationID;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return super.isValid() && (success != null) && (allocationID != null);
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
        if (packetID == null || success == null || allocationID == null) throw new PacketException("no data");
        ByteBuffer buffer = ByteBuffer.wrap(new byte[21]);
        buffer.put(getByteArrayFromInteger(packetID));
        buffer.put((success) ? (byte) 1 : (byte) 0);
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
        if (packetData.length != 21) throw new PacketException("packet length is not 21");
        ByteBuffer buffer = ByteBuffer.wrap(packetData);
        byte[] toProcess = new byte[4];
        buffer.get(toProcess);
        packetID = getIntegerFromByteArray(toProcess);
        success = (buffer.get() == 1);
        if (!success && packetData[4] != 0) success = null;
        long mostSig = buffer.getLong();
        allocationID = new UUID(mostSig, buffer.getLong());
    }

    /**
     * Checks if the allocation is successful.
     *
     * @return If the allocation was successful.
     */
    public boolean allocationSuccessful() {
        return (success != null && success);
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
