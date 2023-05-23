package com.captainalm.lib.calmnet.packet;

/**
 * This interface allows getting and setting if the
 * internal cache should be used within a class instance.
 *
 * @author Captain ALM
 */
public interface IInternalCache {
    /**
     * Gets if the internal cache is used.
     *
     * @return If the internal cache is used.
     */
    boolean isCacheUsed();

    /**
     * Sets if the internal cache is used.
     *
     * @param used If the internal cache is used.
     */
    void setCacheUsed(boolean used);
}
