package com.captainalm.lib.calmnet.packet.core;

import com.captainalm.lib.calmnet.packet.IAcknowledgement;
import com.captainalm.lib.stdcrypt.encryption.CipherException;
import com.captainalm.lib.stdcrypt.encryption.ICipherFactory;
import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketProtocolInformation;

/**
 * This class provides the ability for supporting streams to upgrade to using password encrypted and / or base64 connections.
 * This class can also signal the use changes for {@link Base64Packet}s or {@link EncryptedPacket}s.
 * <p>
 * Major ID: 255
 * Minor ID: 253
 * </p>
 *
 * @author Captain ALM
 */
public class NetworkEncryptionUpgradePacket implements IPacket, IAcknowledgement {
    private static final PacketProtocolInformation protocol = new PacketProtocolInformation((byte) 255, (byte) 253);
    
    protected Boolean acknowledgement;
    protected boolean upgrade;
    protected boolean base64ed;
    protected ICipherFactory cipherFactory;
    protected boolean sendSecrets;

    protected final Object slock = new Object();

    /**
     * Constructs a new NetworkEncryptionUpgradePacket with the specified acknowledgement value, upgrade value, base 64 value and {@link ICipherFactory}.
     *
     * @param acknowledgement The acknowledgement value to use (Can be null).
     * @param upgrade Is the packet treated as a stream upgrade, See: {@link #isUpgrade()}.
     * @param base64ed Is the packet signalling base 64 to be used.
     * @param cipherFactory The cipherFactory to signal for use.
     */
    public NetworkEncryptionUpgradePacket(Boolean acknowledgement, boolean upgrade, boolean base64ed, ICipherFactory cipherFactory) {
        this.acknowledgement = acknowledgement;
        this.upgrade = upgrade;
        this.base64ed = base64ed;
        this.cipherFactory = cipherFactory;
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
        synchronized (slock) {
            if (acknowledgement == null) throw new PacketException("no data");

            byte[] cipherBytes = (cipherFactory == null) ? null : (sendSecrets) ? cipherFactory.getSettings() : cipherFactory.getSettingsNoSecrets();
            byte[] toret = new byte[2 + ((cipherBytes == null) ? 0 : cipherBytes.length)];
            toret[0] = (acknowledgement) ? (byte) 1 : (byte) 0;
            toret[1] = (byte) (((upgrade) ? 1 : 0) + ((base64ed) ? 2 : 0));

            if (cipherBytes != null) System.arraycopy(cipherBytes, 0, toret, 2, cipherBytes.length);

            return toret;
        }
    }

    /**
     * Loads the packet payload from save data.
     *
     * @param packetData The packet payload data.
     * @throws NullPointerException The new store data is null.
     * @throws PacketException An Exception has occurred.
     */
    @Override
    public void loadPayload(byte[] packetData) throws PacketException {
        if (packetData == null) throw new NullPointerException("packetData is null");
        if (packetData.length < 2) throw new PacketException("no data");
        synchronized (slock) {
            try {
                acknowledgement = (packetData[0] == 1);
                if (!acknowledgement && packetData[0] != 0) acknowledgement = null;

                upgrade = ((packetData[1] & 1) == 1);
                base64ed = ((packetData[1] & 2) == 2);

                if (cipherFactory != null && packetData.length > 2) {
                    byte[] cipherBytes = new byte[packetData.length - 2];
                    System.arraycopy(packetData, 2, cipherBytes, 0, cipherBytes.length);
                    try {
                        cipherFactory.setSettings(cipherBytes);
                    } catch (CipherException e) {
                        throw new PacketException(e);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                throw new PacketException(e);
            }
        }
    }

    /**
     * Gets if the packet is treated as a stream upgrade or
     * a change in packet use for {@link EncryptedPacket} and {@link Base64Packet}.
     *
     * @return If the packet is a stream upgrade.
     */
    public boolean isUpgrade() {
        return upgrade;
    }

    /**
     * Sets if the packet is treated as a stream upgrade or
     * a change in packet use for {@link EncryptedPacket} and {@link Base64Packet}.
     *
     * @param upgrade If the packet is a stream upgrade.
     */
    public void setUpgrade(boolean upgrade) {
        synchronized (slock) {
            this.upgrade = upgrade;
        }
    }

    /**
     * Gets if base 64 is used.
     * (This is not for this packet, it is part of the upgrade attributes.)
     *
     * @return If base 64 is used.
     */
    public boolean isBase64ed() {
        return base64ed;
    }

    /**
     * Sets if base64 is used.
     * (This is not for this packet, it is part of the upgrade attributes.)
     *
     * @param base64ed If base 64 is to be used.
     */
    public void setBase64ed(boolean base64ed) {
        synchronized (slock) {
            this.base64ed = base64ed;
        }
    }

    /**
     * Gets the {@link ICipherFactory} being used or null.
     *
     * @return The Cipher Factory or null.
     */
    public ICipherFactory getCipherFactory() {
        return cipherFactory;
    }

    /**
     * Sets the {@link ICipherFactory} being used.
     *
     * @param cipherFactory The Cipher Factory or null.
     */
    public void setCipherFactory(ICipherFactory cipherFactory) {
        synchronized (slock) {
            this.cipherFactory = cipherFactory;
        }
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

    /**
     * Gets if secrets are sent as part of cipher settings.
     *
     * @return If the secrets are part of the cipher settings.
     */
    public boolean areSecretsSent() {
        return sendSecrets;
    }

    /**
     * Sets if secrets should be sent as part of cipher settings.
     *
     * @param sendSecrets If secrets are part of the cipher settings.
     */
    public void setIfSecretsSent(boolean sendSecrets) {
        synchronized (slock) {
            this.sendSecrets = sendSecrets;
        }
    }
}
