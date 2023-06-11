package com.captainalm.lib.calmnet.marshal;

import com.captainalm.lib.calmnet.packet.fragment.FragmentReceiver;
import com.captainalm.lib.calmnet.packet.fragment.FragmentSender;

/**
 * This class provides fragmentation options for using {@link FragmentSender}s and
 * {@link FragmentReceiver}s in this package.
 *
 * @author Captain ALM
 */
public final class FragmentationOptions {
    /**
     * The maximum age of fragments for a specified packet in seconds before those fragments are purged.
     */
    public int maximumFragmentAge = 30;
    /**
     * See:
     * {@link FragmentSender#setSplitSize(int)}
     */
    public int fragmentationSplitSize = 448;
    /**
     * See:
     * {@link FragmentReceiver#setNumberOfEmptySendsTillForcedCompleteOrResend(int)}
     */
    public int emptySendsTillForced = 2;
    /**
     * See:
     * {@link FragmentSender#setResponseVerification(boolean)} ,
     * {@link FragmentReceiver#setResponseVerification(boolean)}
     */
    public boolean verifyFragments = false;
    /**
     * See:
     * {@link FragmentSender#setSentDataWillBeAllVerified(boolean)} ,
     * {@link FragmentReceiver#setSentDataWillBeAllVerified(boolean)}
     */
    public boolean equalityVerifyFragments = false;

    public FragmentationOptions() {}

    /**
     * Creates a copy of the provided FragmentationOptions.
     *
     * @param toCopy The options to copy.
     * @throws NullPointerException toCopy is null.
     */
    public FragmentationOptions(FragmentationOptions toCopy) {
        if (toCopy == null) throw new NullPointerException("toCopy is null");
        maximumFragmentAge = toCopy.maximumFragmentAge;
        fragmentationSplitSize = toCopy.fragmentationSplitSize;
        emptySendsTillForced = toCopy.emptySendsTillForced;
        verifyFragments = toCopy.verifyFragments;
        equalityVerifyFragments = toCopy.equalityVerifyFragments;
    }

    /**
     * Validates the parameters within this structure.
     *
     * @throws IllegalArgumentException maximumFragmentAge is less than 2, fragmentationSplitSize is less than 1 or emptySendsTillForced is less than 1.
     */
    public void validate() {
        if (maximumFragmentAge < 2) throw new IllegalArgumentException("maximumFragmentAge is less than 2");
        if (fragmentationSplitSize < 1) throw new IllegalArgumentException("fragmentationSplitSize is less than 1");
        if (emptySendsTillForced < 1) throw new IllegalArgumentException("emptySendsTillForced is less than 1");
    }

    /**
     * Sets-up the provided {@link FragmentSender} with parameters.
     *
     * @param sender The sender to set up.
     * @throws IllegalArgumentException A parameter is incorrect.
     */
    public void setupSender(FragmentSender sender) {
        sender.setSplitSize(fragmentationSplitSize);
        sender.setResponseVerification(verifyFragments);
        sender.setSentDataWillBeAllVerified(equalityVerifyFragments);
    }

    /**
     * Sets-up the provided {@link FragmentReceiver} with parameters.
     *
     * @param receiver The receiver to set up.
     * @throws IllegalArgumentException A parameter is incorrect.
     */
    public void setupReceiver(FragmentReceiver receiver) {
        receiver.setNumberOfEmptySendsTillForcedCompleteOrResend(emptySendsTillForced);
        receiver.setResponseVerification(verifyFragments);
        receiver.setSentDataWillBeAllVerified(equalityVerifyFragments);
    }
}
