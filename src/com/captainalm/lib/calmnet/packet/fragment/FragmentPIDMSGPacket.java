package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.PacketException;

import static com.captainalm.lib.calmnet.packet.PacketLoader.getByteArrayFromInteger;
import static com.captainalm.lib.calmnet.packet.PacketLoader.getIntegerFromByteArray;

/**
 * This abstract base class provides the ability for packets to contain an ID, a Fragment ID and a payload.
 *
 * @author Captain ALM
 */
public abstract class FragmentPIDMSGPacket extends FragmentPIDPacket {
    protected Integer fragmentID;
    protected byte[] payload;

    /**
     * Constructs a new FragmentPIDMSGPacket given the packet ID, fragment ID and payload.
     *
     * @param packetID The packet ID.
     * @param fragmentID The fragment ID.
     * @param payload The payload to store.
     * @throws IllegalArgumentException packetID or fragmentID is less than 0.
     */
    public FragmentPIDMSGPacket(Integer packetID, Integer fragmentID, byte[] payload) {
        super(packetID);
        if (fragmentID != null && fragmentID < 0) throw new IllegalArgumentException("fragmentID is less than 0");
        this.fragmentID = fragmentID;
        this.payload = payload;
    }

    /**
     * Gets if the packet is valid.
     *
     * @return Is the packet valid?
     */
    @Override
    public boolean isValid() {
        return super.isValid() && (fragmentID != null);
    }

    /**
     * Saves the packet payload to a byte array.
     *
     * @return The packet payload data.
     * @throws PacketException An Exception has occurred.
     */
    @Override
    public byte[] savePayload() throws PacketException {
        if (packetID == null || fragmentID == null) throw new PacketException("no data");
        byte[] localPayload = (payload == null) ? new byte[0] : payload;
        byte[] toret = new byte[8 + localPayload.length];
        System.arraycopy(getByteArrayFromInteger(packetID), 0, toret, 0, 4);
        System.arraycopy(getByteArrayFromInteger(fragmentID), 0, toret, 4, 4);
        System.arraycopy(localPayload, 0, toret, 8, localPayload.length);
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
        if (packetData.length < 8) throw new PacketException("packet length is less than 8");
        byte[] toProcess = new byte[4];
        System.arraycopy(packetData, 0, toProcess, 0, 4);
        packetID = getIntegerFromByteArray(toProcess);
        System.arraycopy(packetData, 4, toProcess, 0, 4);
        fragmentID = getIntegerFromByteArray(toProcess);
        payload = new byte[packetData.length - 8];
        System.arraycopy(packetData, 8, payload, 0, payload.length);
    }

    /**
     * Gets the fragment message byte array or null.
     *
     * @return The byte array or null.
     */
    public byte[] getFragmentMessage() {
        return payload;
    }

    /**
     * Gets the fragment ID or null.
     *
     * @return The fragment ID or null.
     */
    public Integer getFragmentID() {
        return fragmentID;
    }
}
