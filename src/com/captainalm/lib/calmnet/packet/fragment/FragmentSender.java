package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketLoader;

import java.util.*;

/**
 * This class provides the ability to create packets for {@link com.captainalm.lib.calmnet.packet.fragment}.
 *
 * @author Captain ALM
 */
public final class FragmentSender {
    private final Queue<IPacket> inputQueue = new LinkedList<>();
    private final Queue<Integer> finishedIDs = new LinkedList<>();
    private final HashMap<UUID, byte[]> allocationInputs = new HashMap<>();
    private final HashMap<Integer, FragmentOutput> registry = new HashMap<>();
    private final Object slock = new Object();
    private final Object slockfinish = new Object();
    private int splitSize = 496;
    private PacketLoader packetLoader;
    private boolean verifyResponses = false;
    private boolean makeSureSendDataVerified = false;

    /**
     * Constructs a new FragmentSender with the specified {@link PacketLoader}.
     *
     * @param loader The packet loader to use.
     * @throws NullPointerException loader is null.
     */
    public FragmentSender(PacketLoader loader) {
        setPacketLoader(loader);
    }

    /**
     * Constructs a new FragmentSender with the specified {@link PacketLoader}
     * and packet split size in bytes.
     *
     * @param loader The packet loader to use.
     * @param newSize The new split size.
     * @throws NullPointerException loader is null.
     * @throws IllegalArgumentException newSize is less than 1.
     */
    public FragmentSender(PacketLoader loader, int newSize) {
        this(loader);
        setSplitSize(newSize);
    }

    /**
     * Sends a {@link IPacket} using this FragmentSender.
     *
     * @param packetIn The packet to fragment and send.
     */
    public void sendPacket(IPacket packetIn) {
        if (packetIn == null) throw new NullPointerException("packetIn is null");
        synchronized (slock) {
            inputQueue.add(packetIn);
        }
    }

    /**
     * Sends the current {@link IPacket}s from the FragmentSender.
     *
     * @return The packets to send.
     * @throws PacketException A Packet Exception has occurred.
     */
    public IPacket[] sendPacket() throws PacketException {
        synchronized (slock) {
            ArrayList<IPacket> toret = new ArrayList<>();
            while (inputQueue.size() > 0) allocationInputs.put(UUID.randomUUID(), packetLoader.writePacketNoDigest(inputQueue.poll(), true));
            for (UUID c : allocationInputs.keySet()) {
                toret.add(new FragmentAllocatePacket(getNumberOfFragments(allocationInputs.get(c)), c));
            }
            for (int key : registry.keySet()) {
                IPacket packet = registry.get(key).getSendPacket();
                if (packet != null) toret.add(packet);
            }
            return toret.toArray(new IPacket[0]);
        }
    }

    /**
     * Receives a {@link IPacket} into the FragmentSender.
     *
     * @param packetIn The packet to receive.
     * @return If the received packet was a fragment packet.
     * @throws PacketException A Packet Exception has occurred.
     */
    public boolean receivePacket(IPacket packetIn) throws PacketException {
        if (packetIn == null || !packetIn.isValid()) return false;
        if (packetIn instanceof FragmentPIDPacket) {
            int currentID = ((FragmentPIDPacket) packetIn).getPacketID();
            synchronized (slock) {
                if (packetIn instanceof FragmentAllocationPacket && allocationInputs.containsKey(((FragmentAllocationPacket) packetIn).getAllocationID()) && ((FragmentAllocationPacket) packetIn).allocationSuccessful()) {
                    registry.put(currentID, new FragmentOutput(currentID, allocationInputs.get(((FragmentAllocationPacket) packetIn).getAllocationID())));
                    allocationInputs.remove(((FragmentAllocationPacket) packetIn).getAllocationID());
                } else {
                    FragmentOutput fragmentOutput = registry.get(currentID);
                    if (fragmentOutput != null && fragmentOutput.shouldBeRemovedReceivePacket(packetIn)) {
                        registry.remove(currentID);
                        synchronized (slockfinish) {
                            finishedIDs.add(currentID);
                            slockfinish.notify();
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Gets whether packets are waiting for allocation.
     *
     * @return If packets are waiting for allocation.
     */
    public boolean arePacketsWaiting() {
        synchronized (slock) {
            return inputQueue.size() > 0;
        }
    }

    /**
     * Clears the currently waiting packets.
     */
    public void clearWaitingPackets() {
        synchronized (slock) {
            inputQueue.clear();
            allocationInputs.clear();
        }
    }

    /**
     * Deletes a packet from the registry.
     *
     * @param id The ID of the packet to remove.
     */
    public void deletePacketFromRegistry(int id) {
        synchronized (slock) {
            registry.remove(id);
        }
    }

    /**
     * Clears the registry.
     * NOTE: Do NOT do this unless you are finished with the FragmentSender.
     */
    public void clearRegistry() {
        synchronized (slock) {
            registry.clear();
        }
    }

    /**
     * Gets whether finished IDs are waiting for obtaining.
     *
     * @return If finished IDs are waiting for obtaining.
     */
    public boolean areFinishedIDsWaiting() {
        synchronized (slockfinish) {
            return finishedIDs.size() > 0;
        }
    }

    /**
     * Gets the last finished packet ID.
     * This method blocks until a packet finishes processing.
     *
     * @return The last finished packet ID.
     * @throws InterruptedException The Thread was Interrupted.
     */
    public int getLastIDFinished() throws InterruptedException {
        synchronized (slockfinish) {
            while (finishedIDs.size() < 1) slockfinish.wait();
            Integer polled = finishedIDs.poll();
            return (polled == null) ? -1 : polled;
        }
    }

    /**
     * Polls the last finished packet ID.
     *
     * @return The last finished packet ID.
     */
    public Integer pollLastIDFinished() {
        synchronized (slockfinish) {
            Integer polled = finishedIDs.poll();
            return (polled == null) ? -1 : polled;
        }
    }

    /**
     * Clears all the last finished packet IDs.
     */
    public void clearLastIDFinished() {
        synchronized (slockfinish) {
            finishedIDs.clear();
        }
    }

    private int getNumberOfFragments(byte[] toSplit) {
        return (int) Math.ceil((double) toSplit.length / (double) splitSize);
    }

    /**
     * Gets the current packet split size in bytes.
     *
     * @return The current packet split size.
     */
    public int getSplitSize() {
        return splitSize;
    }

    /**
     * Sets the packet split size in bytes.
     *
     * @param newSize The new packet split size.
     * @throws IllegalArgumentException newSize is less than 1.
     */
    public void setSplitSize(int newSize) {
        if (newSize < 1) throw new IllegalArgumentException("newSize is less than 1");
        synchronized (slock) {
            splitSize = newSize;
        }
    }

    /**
     * Gets the {@link PacketLoader} in use.
     *
     * @return The PacketLoader in use.
     */
    public PacketLoader getPacketLoader() {
        return packetLoader;
    }

    /**
     * Sets the {@link PacketLoader} to use.
     *
     * @param loader The packet loader to use.
     * @throws NullPointerException loader is null.
     */
    public void setPacketLoader(PacketLoader loader) {
        if (loader == null) throw new NullPointerException("loader is null");
        synchronized (slock) {
            packetLoader = loader;
        }
    }

    /**
     * Gets whether responses should be verified.
     *
     * @return Should responses be verified.
     */
    public boolean shouldVerifyResponses() {
        return verifyResponses;
    }

    /**
     * Sets whether responses should be verified.
     * If set to false, {@link #setSentDataWillBeAllVerified(boolean)} will be set to false too.
     *
     * @param state If responses should be verified.
     */
    public void setResponseVerification(boolean state) {
        synchronized (slock) {
            verifyResponses = state;
            if (makeSureSendDataVerified && !state) makeSureSendDataVerified = false;
        }
    }

    /**
     * Gets whether all sent fragments are verified to be equal.
     *
     * @return If all sent fragments will be verified to be equal.
     */
    public boolean shouldSentDataBeAllVerified() {
        return makeSureSendDataVerified;
    }

    /**
     * Gets whether all sent fragments are verified to be equal.
     * Requires {@link #setResponseVerification(boolean)} set to true.
     *
     * @param state If all sent fragments will be verified to be equal.
     */
    public void setSentDataWillBeAllVerified(boolean state) {
        synchronized (slock) {
            if (!verifyResponses) return;
            makeSureSendDataVerified = state;
        }
    }

    /**
     * Stops data verification for the specified Packet ID when {@link #shouldSentDataBeAllVerified()} is true.
     *
     * @param id The PacketID to act on.
     */
    public void stopDataVerificationAndCompleteSend(int id) {
        synchronized (slock) {
            if (!makeSureSendDataVerified) return;
            FragmentOutput output = registry.get(id);
            if (output != null) output.forceDataVerifiedSendStop = true;
        }
    }

    /**
     * Stops data verification for all packets being sent when {@link #shouldSentDataBeAllVerified()} is true.
     */
    public void stopAllDataVerificationAndCompleteSend() {
        synchronized (slock) {
            for (int c : registry.keySet()) registry.get(c).forceDataVerifiedSendStop = true;
        }
    }

    /**
     * This class provides the ability to get the next fragment packet information for sending.
     * The next packet for sending will change as signalling packets come through.
     *
     * @author Captain ALM
     */
    private final class FragmentOutput {
        public final int packetID;
        private final FragmentMessagePacket[] messagePackets;
        private final ArrayList<Integer> msgToResend = new ArrayList<>();
        private final ArrayList<Integer> msgToResendCurrent = new ArrayList<>();
        private int msgPacketIndex = 0;
        public boolean isResending = false;
        public boolean forceDataVerifiedSendStop = false;

        public FragmentOutput(int id, byte[] toSplit) {
            packetID = id;
            messagePackets = new FragmentMessagePacket[getNumberOfFragments(toSplit)];
            int index = 0;
            for (int i = 0; i < messagePackets.length; i++) {
                byte[] toInput = new byte[(index + splitSize > toSplit.length) ? toSplit.length - index : splitSize];
                System.arraycopy(toSplit, index, toInput, 0, toInput.length);
                messagePackets[i] = new FragmentMessagePacket(packetID, i, toInput);
                msgToResend.add(i);
                index += toInput.length;
            }
        }

        public IPacket getSendPacket() {
            if (msgPacketIndex < 0) {
                msgPacketIndex = 0;
                return new FragmentRetrySendPacket(packetID, true);
            }
            if (!isResending && makeSureSendDataVerified && msgPacketIndex >= messagePackets.length && !forceDataVerifiedSendStop) setResendingOn(true);
            if (isResending) {
                if (makeSureSendDataVerified && msgPacketIndex >= msgToResendCurrent.size() && !forceDataVerifiedSendStop) setResendingOn(true);
                if (msgPacketIndex < msgToResendCurrent.size()) return messagePackets[msgToResendCurrent.get(msgPacketIndex++)];
            } else {
                if (msgPacketIndex < messagePackets.length) return messagePackets[msgPacketIndex++];
            }
            return (makeSureSendDataVerified && (msgToResend.size() < 1 || forceDataVerifiedSendStop)) ? new FragmentSendVerifyCompletePacket(packetID) : new FragmentSendCompletePacket(packetID, false);
        }

        public boolean shouldBeRemovedReceivePacket(IPacket packetIn) {
            if (packetIn instanceof FragmentSendStopPacket || (packetIn instanceof FragmentSendCompletePacket && ((FragmentSendCompletePacket) packetIn).isAcknowledgement())) {
                msgPacketIndex = messagePackets.length;
                return true;
            }
            if (packetIn instanceof FragmentMessageResponsePacket) {
                FragmentMessageResponsePacket responsePacket = (FragmentMessageResponsePacket)packetIn;
                if (!verifyResponses || compareData(responsePacket.getFragmentMessage(), messagePackets[responsePacket.getFragmentID()].getFragmentMessage()))
                    msgToResend.remove(responsePacket.getFragmentID());
            }
            if (packetIn instanceof FragmentRetrySendPacket && !((FragmentRetrySendPacket) packetIn).isAcknowledgement()) setResendingOn(false);
            return false;
        }

        private void setResendingOn(boolean zeroIndex) {
            msgPacketIndex = (zeroIndex) ? 0 : -1;
            isResending = true;
            msgToResendCurrent.clear();
            msgToResendCurrent.addAll(msgToResend);
        }

        private boolean compareData(byte[] data1, byte[] data2) {
            if ((data1 == null && data2 != null) || (data1 != null && data2 == null)) return false;
            if (data1 == data2) return true;
            if (data1.length != data2.length) return false;
            for (int i = 0; i < data1.length; i++) if (data1[i] != data2[i]) return false;
            return true;
        }
    }
}
