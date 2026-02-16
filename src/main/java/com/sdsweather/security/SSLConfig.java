package com.sdsweather.security;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class SSLConfig {

    private static SSLContext sslContext;

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

    public static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .sslContext(getSSLContext())
                .build();
    }
}