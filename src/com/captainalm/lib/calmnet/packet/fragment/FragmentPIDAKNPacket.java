package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.IAcknowledgement;
import com.captainalm.lib.calmnet.packet.PacketException;

import static com.captainalm.lib.calmnet.packet.PacketLoader.getByteArrayFromInteger;
import static com.captainalm.lib.calmnet.packet.PacketLoader.getIntegerFromByteArray;

/**
 * This abstract base class provides the ability for packets to contain an ID and if it is an Acknowledgement.
 *
 * @author Captain ALM
 */
public abstract class FragmentPIDAKNPacket extends FragmentPIDPacket implements IAcknowledgement {
    protected Boolean acknowledgement;

    /**
     * Constructs a new FragmentPIDPacket given the packet ID and the acknowledgement value.
     *
     * @param packetID The packet ID.
     * @param acknowledgement The acknowledgement value to use.
     * @throws IllegalArgumentException packetID is less than 0.
     */
    public FragmentPIDAKNPacket(Integer packetID, Boolean acknowledgement) {
        super(packetID);
        this.acknowledgement = acknowledgement;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return super.isValid() && (acknowledgement != null);
    }

    /**
     * Saves the packet payload to a byte array.
     *
     * @return The packet payload data.
     * @throws PacketException An Exception has occurred.
     */
    @Override
    public byte[] savePayload() throws PacketException {
        if (packetID == null || acknowledgement == null) throw new PacketException("no data");
        byte[] toret = new byte[5];
        System.arraycopy(getByteArrayFromInteger(packetID), 0, toret, 0, 4);
        toret[4] = (acknowledgement) ? (byte) 1 : (byte) 0;
        return toret;
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
        if (packetData.length != 5) throw new PacketException("packet length is not 5");
        byte[] toProcess = new byte[4];
        System.arraycopy(packetData, 0, toProcess, 0, 4);
        packetID = getIntegerFromByteArray(toProcess);
        acknowledgement = (packetData[4] == 1);
        if (!acknowledgement && packetData[4] != 0) acknowledgement = null;
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
