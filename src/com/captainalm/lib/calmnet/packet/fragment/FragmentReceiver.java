package com.captainalm.lib.calmnet.packet.fragment;

import com.captainalm.lib.calmnet.packet.IPacket;
import com.captainalm.lib.calmnet.packet.factory.IPacketFactory;
import com.captainalm.lib.calmnet.packet.PacketException;
import com.captainalm.lib.calmnet.packet.PacketLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * This class provides the ability to re-construct packets from {@link com.captainalm.lib.calmnet.packet.fragment}.
 *
 * @author Captain ALM
 */
public final class FragmentReceiver {
    private final Queue<IPacket> outputQueue = new LinkedList<>();
    private final Queue<Integer> finishedIDs = new LinkedList<>();
    private final Queue<AllocationOutput> allocated = new LinkedList<>();
    private final Queue<Integer> forceStopIDs = new LinkedList<>();
    private final HashMap<Integer, FragmentInput> registry = new HashMap<>();
    private final Object slock = new Object();
    private final Object slockqueue = new Object();
    private final Object slockfinish = new Object();
    private int numberOfEmptySendsTillForced = 2;
    private int IID = 0;
    private PacketLoader packetLoader;
    private IPacketFactory packetFactory;
    private boolean verifyResponses = false;
    private boolean makeSureSendDataVerified = false;

    /**
     * Constructs a new FragmentReceiver with the specified {@link PacketLoader} and {@link IPacketFactory}.
     *
     * @param loader The packet loader to use.
     * @param factory The packet factory to use.
     * @throws NullPointerException loader or factory is null.
     */
    public FragmentReceiver(PacketLoader loader, IPacketFactory factory) {
        setPacketLoader(loader);
        setPacketFactory(factory);
    }

    /**
     * Receives a {@link IPacket} from the FragmentReceiver.
     * This method blocks until a packet can be received.
     *
     * @return The received packet.
     * @throws InterruptedException The Thread was Interrupted.
     */
    public IPacket receivePacket() throws InterruptedException {
        synchronized (slockqueue) {
            while (outputQueue.size() < 1) slockqueue.wait();
            return outputQueue.poll();
        }
    }

    /**
     * Sends the current {@link IPacket}s from the FragmentReceiver.
     *
     * @return The packets to send.
     * @throws PacketException A Packet Exception has occurred.
     */
    public IPacket[] sendPacket() throws PacketException {
        synchronized (slock) {
            ArrayList<IPacket> toret = new ArrayList<>();
            while (allocated.size() > 0) toret.add(allocated.poll().getAllocation());
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (int key : registry.keySet()) {
                IPacket packet = registry.get(key).getSendPacket();
                if (packet != null) {
                    toret.add(packet);
                    if (packet instanceof FragmentSendCompletePacket) {
                        toRemove.add(key);
                        synchronized (slockfinish) {
                            finishedIDs.add(key);
                            slockfinish.notify();
                        }
                    }
                }
                IPacket wasConsumed = registry.get(key).consume();
                if (wasConsumed != null) {
                    synchronized (slockqueue) {
                        outputQueue.add(wasConsumed);
                        slockqueue.notify();
                    }
                }
            }
            for (int i : toRemove) registry.remove(i);
            while (forceStopIDs.size() > 0) toret.add(new FragmentSendStopPacket(forceStopIDs.poll()));
            return toret.toArray(new IPacket[0]);
        }
    }

    /**
     * Receives a {@link IPacket} into the FragmentReceiver.
     *
     * @param packetIn The packet to receive.
     * @throws PacketException A Packet Exception has occurred.
     */
    public void receivePacket(IPacket packetIn) throws PacketException {
        if (packetIn == null || !packetIn.isValid()) return;
        if (packetIn instanceof FragmentPIDPacket) {
            synchronized (slock) {
                FragmentInput fragmentInput = registry.get(((FragmentPIDPacket) packetIn).getPacketID());
                if (fragmentInput != null) fragmentInput.receivePacket(packetIn);
            }
        } else if (packetIn instanceof FragmentAllocatePacket && ((FragmentAllocatePacket)packetIn).getFragmentCount() > 0) {
            synchronized (slock) {
                if (!containsAllocationID(((FragmentAllocatePacket) packetIn).getAllocationID())) {
                    int currentID = getCurrentID();
                    UUID aid = ((FragmentAllocatePacket) packetIn).getAllocationID();
                    if (currentID >= 0) {
                        registry.put(currentID, new FragmentInput(currentID, ((FragmentAllocatePacket) packetIn).getFragmentCount(), aid));
                        allocated.add(new AllocationOutput(currentID, aid, true));
                    } else {
                        allocated.add(new AllocationOutput(0, aid, false));
                    }
                }
            }
        }
    }

    private int getCurrentID() {
        if (registry.containsKey(IID)) while (IID >= 0 && registry.containsKey(IID)) IID++;
        if (IID < 0) IID = -1;
        return IID;
    }

    private boolean containsAllocationID(UUID aid) {
        for (int c : registry.keySet()) if (registry.get(c).allocationID.equals(aid)) return true;
        return false;
    }

    /**
     * Gets whether packets are waiting to be received.
     *
     * @return If packets are waiting to be received.
     */
    public boolean arePacketsWaiting() {
        synchronized (slockqueue) {
            return outputQueue.size() > 0;
        }
    }

    /**
     * Clears the currently waiting packets.
     */
    public void clearWaitingPackets() {
        synchronized (slockqueue) {
            outputQueue.clear();
        }
    }

    /**
     * Deletes a packet from the registry and requests the sender to stop sending.
     *
     * @param id The ID of the packet to remove.
     */
    public void deletePacketFromRegistry(int id) {
        synchronized (slock) {
            forceStopIDs.add(id);
            registry.remove(id);
        }
    }

    /**
     * Clears the registry (And requests the sender to stop sending).
     * NOTE: Do NOT do this unless you are finished with the FragmentReceiver.
     */
    public void clearRegistry() {
        synchronized (slock) {
            forceStopIDs.addAll(registry.keySet());
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
     * Clears all the last finished packet IDs.
     */
    public void clearLastIDFinished() {
        synchronized (slockfinish) {
            finishedIDs.clear();
        }
    }

    /**
     * Gets the {@link IPacketFactory} in use.
     *
     * @return The PacketFactory in use.
     */
    public IPacketFactory getPacketFactory() {
        return packetFactory;
    }

    /**
     * Sets the {@link IPacketFactory} to use.
     *
     * @param factory The packet factory to use.
     * @throws NullPointerException factory is null.
     */
    public void setPacketFactory(IPacketFactory factory) {
        if (factory == null) throw new NullPointerException("factory is null");
        synchronized (slock) {
            packetFactory = factory;
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
     * Gets the number of {@link #sendPacket()} calls, that return null, to a registry entry are made before
     * the {@link FragmentSendCompletePacket} or {@link FragmentRetrySendPacket} packets are sent.
     * A {@link FragmentSendCompletePacket} is sent if completely received and a
     * {@link FragmentRetrySendPacket} is sent if not completely received.
     * This excludes empty packets due to {@link #shouldSentDataBeAllVerified()}.
     *
     * @return The number of send packet calls before a completion or restart is forced.
     */
    public int getNumberOfEmptySendsTillForcedCompleteOrResend() {
        return numberOfEmptySendsTillForced;
    }

    /**
     * Sets the number of {@link #sendPacket()} calls, that return null, to a registry entry are made before
     * the {@link FragmentSendCompletePacket} or {@link FragmentRetrySendPacket} packets are sent.
     * A {@link FragmentSendCompletePacket} is sent if completely received and a
     * {@link FragmentRetrySendPacket} is sent if not completely received.
     * This excludes empty packets due to {@link #shouldSentDataBeAllVerified()}.
     *
     * @param numberOfEmptySends The number of empty sends to allow.
     * @throws IllegalArgumentException numberOfEmptySends is less than 1.
     */
    public void setNumberOfEmptySendsTillForcedCompleteOrResend(int numberOfEmptySends) {
        if (numberOfEmptySends < 1) throw new IllegalArgumentException("numberOfEmptySends is less than 1");
        numberOfEmptySendsTillForced = numberOfEmptySends;
    }

    /**
     * Stops data verification for the specified Packet ID when {@link #shouldSentDataBeAllVerified()} is true.
     *
     * @param id The PacketID to act on.
     */
    public void stopDataVerificationAndCompleteReceive(int id) {
        synchronized (slock) {
            if (!makeSureSendDataVerified) return;
            FragmentInput input = registry.get(id);
            if (input != null) input.verifyReceived = true;
        }
    }

    /**
     * Stops data verification for all packets being received when {@link #shouldSentDataBeAllVerified()} is true.
     */
    public void stopAllDataVerificationAndCompleteReceive() {
        synchronized (slock) {
            for (int c : registry.keySet()) registry.get(c).verifyReceived = true;
        }
    }

    /**
     * This class provides the ability to store allocated responses to be sent back.
     *
     * @author Captain ALM
     */
    private static final class AllocationOutput {
        public final int packetID;
        public final UUID allocationID;
        public final boolean success;

        public AllocationOutput(int packetID, UUID allocationID, boolean success) {
            this.packetID = packetID;
            this.allocationID = allocationID;
            this.success = success;
        }

        public FragmentAllocationPacket getAllocation() {
            return new FragmentAllocationPacket(packetID, allocationID, success);
        }
    }

    /**
     * This class provides the ability to store the received fragments and get the next packet to send on the protocol.
     * The next packet for sending will change as signalling packets come through.
     *
     * @author Captain ALM
     */
    private final class FragmentInput {
        public final int packetID;
        public final UUID allocationID;
        private final ArrayList<Integer> idsToReceive = new ArrayList<>();
        private final ArrayList<Integer> idsToAKN = new ArrayList<>();
        private int msgPacketIndex = 0;
        private boolean consumeDone = false;
        private int sendsTillCompleteForced;
        private boolean fsendActive = false;
        private final FragmentMessagePacket[] messagePackets;
        public boolean verifyReceived = false;

        public FragmentInput(int id, int count, UUID aid) {
            packetID = id;
            messagePackets = new FragmentMessagePacket[count];
            allocationID = aid;
            sendsTillCompleteForced = numberOfEmptySendsTillForced + 1;
            for (int i = 0; i < count; i++) idsToReceive.add(i);
        }

        public IPacket getSendPacket() {
            if (msgPacketIndex < idsToAKN.size() && msgPacketIndex >= 0) {
                int pindex = idsToAKN.get(msgPacketIndex++);
                if (msgPacketIndex >= idsToAKN.size()) {
                    idsToAKN.clear();
                    msgPacketIndex = 0;
                }
                return new FragmentMessageResponsePacket(packetID, pindex, (verifyResponses) ? messagePackets[pindex].getFragmentMessage() : null);
            }
            if (fsendActive) {
                if (sendsTillCompleteForced > 0) sendsTillCompleteForced--;
            } else fsendActive = true;
            if (sendsTillCompleteForced == 0 && !(makeSureSendDataVerified && !verifyReceived)) return (idsToReceive.size() < 1) ? new FragmentSendCompletePacket(packetID, true) : new FragmentRetrySendPacket(packetID, false);
            return null;
        }

        public void receivePacket(IPacket packetIn) {
            if ((packetIn instanceof FragmentSendCompletePacket && !((FragmentSendCompletePacket) packetIn).isAcknowledgement())) sendsTillCompleteForced = 0;
            if (packetIn instanceof FragmentSendVerifyCompletePacket) {
                sendsTillCompleteForced = 0;
                verifyReceived = true;
            }
            if ((packetIn instanceof FragmentRetrySendPacket && ((FragmentRetrySendPacket) packetIn).isAcknowledgement())) sendsTillCompleteForced = numberOfEmptySendsTillForced + 1;
            if (packetIn instanceof FragmentMessagePacket) {
                FragmentMessagePacket messagePacket = (FragmentMessagePacket) packetIn;
                messagePackets[messagePacket.getFragmentID()] = messagePacket;
                idsToReceive.remove(messagePacket.getFragmentID());
                idsToAKN.add(messagePacket.getFragmentID());
            }
        }

        public IPacket consume() throws PacketException {
            if (consumeDone || idsToReceive.size() > 0 || messagePackets.length < 1 || (makeSureSendDataVerified && !verifyReceived)) return null;
            ByteArrayOutputStream packetStream = new ByteArrayOutputStream(messagePackets[0].getFragmentMessage().length);
            for (FragmentMessagePacket messagePacket : messagePackets) {
                try {
                    packetStream.write(messagePacket.getFragmentMessage());
                } catch (IOException e) {
                    throw new PacketException(e);
                }
            }
            consumeDone = true;
            return packetLoader.readPacketNoDigest(packetStream.toByteArray(), packetFactory, null);
        }
    }
}
