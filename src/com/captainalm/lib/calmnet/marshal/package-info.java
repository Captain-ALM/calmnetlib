/**
 * This package contains the network marshals for managed sending of {@link com.captainalm.lib.calmnet.packet.IPacket}s on sockets.
 *
 * @author Captain ALM
 */
package com.captainalm.lib.calmnet.marshal;
/*TODO:
NetMarshalClientWrapped - Stream wrapping support
NetMarshalServer - Has a thread for UDP receiving and has a dictionary of input streams (final, created in constructor)
NetMarshalServerWrapped - Constructs NetMarshalClientWrapped instead of NetMarshalClient, stream wrapping support
 */