package com.captainalm.lib.calmnet.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface provides the streaming packet methods.
 *
 * @author Captain ALM
 */
public interface IStreamedPacket extends IPacket {
    /**
     * Reads payload data to an {@link OutputStream}.
     *
     * @param outputStream The output stream to read data to.
     * @throws NullPointerException outputStream is null.
     * @throws IOException An IO Exception has occurred.
     * @throws PacketException An Exception has occurred.
     */
    void readData(OutputStream outputStream) throws IOException, PacketException;

    /**
     * Writes payload data from an {@link InputStream}.
     *
     * @param inputStream The input stream to write data from.
     * @param size The size of the input payload in bytes.
     * @throws NullPointerException inputStream is null.
     * @throws IllegalArgumentException size is less than 0.
     * @throws IOException An IO Exception has occurred.
     * @throws PacketException An Exception has occurred.
     */
    void writeData(InputStream inputStream, int size) throws IOException, PacketException;

    /**
     * Gets the size of the output data.
     *
     * @throws PacketException An Exception has occurred.
     * @return The size of the output data in bytes.
     */
    int getSize() throws PacketException;
}
