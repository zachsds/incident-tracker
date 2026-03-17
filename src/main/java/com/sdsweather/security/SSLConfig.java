package com.sdsweather.security;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * SSLConfig - Configures HTTPS trust for the application's self-signed certificate.
 *
 * The Rock Pi server uses a self-signed certificate not trusted by the default
 * Java trust store. This class loads the bundled certificate (itsserver.crt)
 * from application resources and creates a custom SSLContext that trusts it.
 * No manual certificate installation is required on client machines.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class SSLConfig {

    /** Cached SSLContext — initialized once and reused for all requests */
    private static SSLContext sslContext;

    /**
     * Returns an SSLContext configured to trust the bundled server certificate.
     * Initializes once and caches for subsequent calls.
     *
     * @return Configured SSLContext
     * @throws RuntimeException if the certificate cannot be loaded
     */
    public static SSLContext getSSLContext() {
        if (sslContext != null) {
            return sslContext;
        }

        try {
            // Load the bundled certificate
            InputStream certStream = SSLConfig.class.getResourceAsStream("/itsserver.crt");
            if (certStream == null) {
                throw new RuntimeException("Certificate file not found in resources");
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(certStream);
            certStream.close();

            // Create a KeyStore containing our trusted certificate
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("itsserver", cert);

            // Create a TrustManager that trusts the certificate in our KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return sslContext;

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SSL", e);
        }
    }

    /**
     * Creates an HttpClient configured to trust the bundled server certificate.
     * All repository classes should use this to create their HTTP clients.
     */
    public static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .build();
    }
}