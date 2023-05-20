package com.captainalm.lib.calmnet.marshal;

import java.net.InetAddress;
import java.util.Objects;

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
     * @throws NullPointerException address is null.
     */
    public CandidateClient(InetAddress address, int port) {
        if (address == null) throw new NullPointerException("address is null");
        this.address = address;
        this.port = port;
    }

    /**
     * Checks if this candidate matches an existing {@link NetMarshalClient}.
     *
     * @param toCheck The client to check against.
     * @return If the candidate matches the passed client.
     */
    public boolean matchesNetMarshalClient(NetMarshalClient toCheck) {
        return toCheck.remoteAddress().equals(address) && toCheck.remotePort() == port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateClient)) return false;
        CandidateClient that = (CandidateClient) o;
        return port == that.port && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}
