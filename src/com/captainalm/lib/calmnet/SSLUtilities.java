package com.captainalm.lib.calmnet;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This class provides SSL utilities to create {@link SSLContext} and {@link SSLSocket}
 * objects using JKS files (Or other supported formats).
 *
 * @author Captain ALM
 */
public class SSLUtilities {
    protected static final X509TrustManager jreTrustManager = getJreTrustManager();
    protected static final String defaultSSLContextProtocol = "TLSv1";

    /**
     * Loads a Keystore of a certain type from a file given the password.
     *
     * @param type The type of keystore (pass null for the default type).
     * @param file The file to load the keystore from.
     * @param password The password of the keystore (Can be null).
     * @return The keystore.
     * @throws NullPointerException file is null.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static KeyStore loadKeyStore(String type, File file, String password) throws SSLUtilityException {
        if (file == null) throw new NullPointerException("file is null");
        if (password == null || password.equals("")) password = "changeit";
        try (FileInputStream inputStream = new FileInputStream(file)) {
            KeyStore toret = KeyStore.getInstance((type == null) ? KeyStore.getDefaultType() : type);
            toret.load(inputStream, password.toCharArray());
            return toret;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new SSLUtilityException(e);
        }
    }

    /**
     * Gets the SSL context without the JRE Trust Store using a unified {@link KeyStore}.
     *
     * @param algorithmName The name of the context protocol or null for the JRE Default (TLSv1).
     * @param unifiedKeyStore The keystore for use with the private and trust stores.
     * @param keyStorePassword The password of the key store or null (Use "changeit" for JKS keystore defaults).
     * @return The SSLContext.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLContext getSSLContextNoJRETrust(String algorithmName, KeyStore unifiedKeyStore, char[] keyStorePassword) throws SSLUtilityException {
        return getSSLContextNoJRETrust(algorithmName, unifiedKeyStore, keyStorePassword, unifiedKeyStore);
    }

    /**
     * Gets the SSL context without the JRE Trust Store using separate private and trust {@link KeyStore}s.
     * @param algorithmName The name of the context protocol or null for the JRE Default (TLSv1).
     * @param privateKeyStore The keystore for use with the private store.
     * @param privateKeyStorePassword The password of the private key store or null (Use "changeit" for JKS keystore defaults).
     * @param trustKeyStore The keystore for use with the trust store.
     * @return The SSLContext.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLContext getSSLContextNoJRETrust(String algorithmName, KeyStore privateKeyStore, char[] privateKeyStorePassword, KeyStore trustKeyStore) throws SSLUtilityException {
        try {
            SSLContext toret = SSLContext.getInstance((algorithmName == null) ? defaultSSLContextProtocol : algorithmName);
            if (algorithmName == null || !algorithmName.equalsIgnoreCase("Default")) {
                KeyManagerFactory kmanagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmanagerFactory.init(privateKeyStore, privateKeyStorePassword);
                TrustManagerFactory tmanagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmanagerFactory.init(trustKeyStore);
                toret.init(kmanagerFactory.getKeyManagers(), buildTrustManagerArray(getX509TrustManager(tmanagerFactory)), null);
            }
            return toret;
        } catch (RuntimeException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
            throw new SSLUtilityException(e);
        }
    }

    /**
     * Gets the SSL context merged with the JRE Trust Store using a unified {@link KeyStore}.
     *
     * @param algorithmName The name of the context protocol or null for the JRE Default (TLSv1).
     * @param unifiedKeyStore The keystore for use with the private and trust stores.
     * @param keyStorePassword The password of the key store or null (Use "changeit" for JKS keystore defaults).
     * @return The SSLContext.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLContext getSSLContext(String algorithmName, KeyStore unifiedKeyStore, char[] keyStorePassword) throws SSLUtilityException {
        return getSSLContext(algorithmName, unifiedKeyStore, keyStorePassword, unifiedKeyStore);
    }

    /**
     * Gets the SSL context merged with the JRE Trust Store using separate private and trust {@link KeyStore}s.
     * @param algorithmName The name of the context protocol or null for the JRE Default (TLSv1).
     * @param privateKeyStore The keystore for use with the private store.
     * @param privateKeyStorePassword The password of the private key store or null (Use "changeit" for JKS keystore defaults).
     * @param trustKeyStore The keystore for use with the trust store.
     * @return The SSLContext.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLContext getSSLContext(String algorithmName, KeyStore privateKeyStore, char[] privateKeyStorePassword, KeyStore trustKeyStore) throws SSLUtilityException {
        try {
            SSLContext toret = SSLContext.getInstance((algorithmName == null) ? defaultSSLContextProtocol : algorithmName);
            if (algorithmName == null || !algorithmName.equalsIgnoreCase("Default")) {
                KeyManagerFactory kmanagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmanagerFactory.init(privateKeyStore, privateKeyStorePassword);
                toret.init(kmanagerFactory.getKeyManagers(), buildTrustManagerArray(new MergedX509TrustManager(trustKeyStore)), null);
            }
            return toret;
        } catch (RuntimeException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
            throw new SSLUtilityException(e);
        }
    }

    /**
     * Creates a new {@link SSLSocket} using the specified {@link SSLContext}, host and port.
     *
     * @param sslContext The SSL Context to create the socket from.
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return The SSLSocket.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLSocket getSSLClientSocket(SSLContext sslContext, String host, int port) throws SSLUtilityException {
        try {
            return (SSLSocket) sslContext.getSocketFactory().createSocket(host, port);
        } catch (RuntimeException | IOException e) {
            throw new SSLUtilityException(e);
        }
    }

    /**
     * Upgrades an existing {@link Socket} to an {@link SSLSocket} using the specified {@link SSLContext},
     * {@link Socket}, host, port and if the specified socket should be closed when the returned socket is closed.
     * This socket is in client mode (Upgrade for client side).
     *
     * @param sslContext The SSL Context to create the socket from.
     * @param socket The socket to wrap.
     * @param host The host to "connect" to.
     * @param port The port to "connect" to.
     * @param autoClose If the underlying socket should be closed when the returned socket is closed.
     * @return The SSLSocket.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLSocket upgradeClientSocketToSSL(SSLContext sslContext, Socket socket, String host, int port, boolean autoClose) throws SSLUtilityException {
        return upgradeClientSocketToSSL(sslContext, socket, host, port, autoClose, true);
    }

    /**
     * Upgrades an existing {@link Socket} to an {@link SSLSocket} using the specified {@link SSLContext},
     * {@link Socket}, host, port and if the specified socket should be closed when the returned socket is closed.
     *
     * @param sslContext The SSL Context to create the socket from.
     * @param socket The socket to wrap.
     * @param host The host to "connect" to.
     * @param port The port to "connect" to.
     * @param autoClose If the underlying socket should be closed when the returned socket is closed.
     * @param onClient Is this being called on the client side.
     * @return The SSLSocket.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLSocket upgradeClientSocketToSSL(SSLContext sslContext, Socket socket, String host, int port, boolean autoClose, boolean onClient) throws SSLUtilityException {
        try {
            SSLSocket toret = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
            toret.setUseClientMode(onClient);
            return toret;
        } catch (RuntimeException | IOException e) {
            throw new SSLUtilityException(e);
        }
    }

    /**
     * Gets the SSL Server socket for the specified {@link SSLContext}, port, backlog and {@link InetAddress}.
     *
     * @param sslContext The SSL Context to create the socket from.
     * @param port The port to listen on.
     * @param backlog The number of connections that can be queued.
     * @param ifAddress The network interface to listen on (null means listen on all network interfaces).
     * @return The SSLServerSocket.
     * @throws SSLUtilityException An Exception has occurred.
     */
    public static SSLServerSocket getSSLServerSocket(SSLContext sslContext, int port, int backlog, InetAddress ifAddress) throws SSLUtilityException {
        try {
            return (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port, backlog, ifAddress);
        } catch (RuntimeException | IOException e) {
            throw new SSLUtilityException(e);
        }
    }

    protected static X509TrustManager getJreTrustManager() {
        try {
            return getX509TrustManager((KeyStore) null);
        } catch (RuntimeException | KeyStoreException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    protected static X509TrustManager getX509TrustManager(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
        TrustManagerFactory tmanagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmanagerFactory.init(keyStore);
        return getX509TrustManager(tmanagerFactory);
    }

    protected static X509TrustManager getX509TrustManager(TrustManagerFactory tmanagerFactory) {
        for (TrustManager c : tmanagerFactory.getTrustManagers()) if (c instanceof X509TrustManager) return (X509TrustManager) c;
        return null;
    }

    protected static TrustManager[] buildTrustManagerArray(TrustManager trustManager) {
        if (trustManager == null) return null; else return new TrustManager[] {trustManager};
    }

    /**
     * This class provides a merging of a loaded trust manager and the JRE default trust manager.
     * (Useful for clients)
     *
     * @author Captain ALM
     */
    private static class MergedX509TrustManager implements X509TrustManager {
        protected final X509TrustManager loadedTrustManager;

        public MergedX509TrustManager(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
            loadedTrustManager = getX509TrustManager(keyStore);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                loadedTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                if (jreTrustManager == null) throw new CertificateException(new NullPointerException("jreTrustManager is null"));
                jreTrustManager.checkClientTrusted(chain, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                loadedTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                if (jreTrustManager == null) throw new CertificateException(new NullPointerException("jreTrustManager is null"));
                jreTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            if (jreTrustManager == null) {
                if (loadedTrustManager == null) return new X509Certificate[0];
                return loadedTrustManager.getAcceptedIssuers();
            } else {
                X509Certificate[] jre = jreTrustManager.getAcceptedIssuers();
                X509Certificate[] lod = loadedTrustManager.getAcceptedIssuers();
                X509Certificate[] toret = new X509Certificate[jre.length + lod.length];
                int index = 0;
                for (X509Certificate c : jre) toret[index++] = c;
                for (X509Certificate c : lod) toret[index++] = c;
                return toret;
            }
        }
    }
}
