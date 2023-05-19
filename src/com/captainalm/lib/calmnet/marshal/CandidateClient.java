package com.captainalm.lib.calmnet.marshal;

import java.net.InetAddress;

/**
 * This class provides a candidate client for {@link NetMarshalServer}s.
 *
 * @author Captain ALM
 */
public final class CandidateClient {
    /**
     * The remote address of the candidate.
     */
    public final InetAddress address;
    /**
     * The remote port of the candidate.
     */
    public final int port;
    /**
     * Whether the candidate should be accepted.
     */
    public boolean accept = true;

    /**
     * Constructs a new instance of CandidateClient with an address and port.
     *
     * @param address The remote address of the candidate.
     * @param port The remote port of the candidate.
     */
    public CandidateClient(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }
}
