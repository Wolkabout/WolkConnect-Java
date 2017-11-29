/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.wolkabout.wolk;

import org.fusesource.mqtt.client.MQTT;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

class MqttFactory {
    private static final String FACTORY_TYPE = "X.509";

    private final MQTT mqtt;

    public MqttFactory() {
        mqtt = new MQTT();
        mqtt.setCleanSession(true);
    }

    public MqttFactory host(String host) throws URISyntaxException {
        host(new URI(host));
        return this;
    }

    public MqttFactory host(URI host) {
        mqtt.setHost(host);
        return this;
    }

    public MqttFactory deviceKey(String deviceKey) {
        mqtt.setUserName(deviceKey);
        mqtt.setClientId(deviceKey);
        return this;
    }

    public MqttFactory password(String password) {
        mqtt.setPassword(password);
        return this;
    }

    public MqttFactory cleanSession(boolean clean) {
        mqtt.setCleanSession(clean);
        return this;
    }

    public MQTT noSslClient() {
        if (mqtt.getHost() == null) {
            throw new IllegalStateException("No host configured.");
        }

        if (mqtt.getUserName() == null) {
            throw new IllegalStateException("No device key provided.");
        }

        return mqtt;
    }

    public MQTT sslClient(String... ca) throws Exception {
        if (mqtt.getHost() == null) {
            throw new IllegalStateException("No host configured.");
        }

        if (mqtt.getUserName() == null) {
            throw new IllegalStateException("No device key provided.");
        }

        if (ca != null && ca.length == 1 && ca[0] != null) {
            final Certificate certificate = getCertificate(ca[0]);
            final TrustManagerFactory trustManagerFactory = getTrustManagerFactory(certificate, ca[0]);
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            mqtt.setSslContext(sslContext);
        }

        mqtt.setConnectAttemptsMax(-1);        // No limit on number of connection attempts

        mqtt.setReconnectDelay(5000);          // Delay in milliseconds
        mqtt.setReconnectAttemptsMax(-1);      // No limit on number of reconnection attempts
        mqtt.setReconnectBackOffMultiplier(2); // Do not use exponential back off for reconnect

        return mqtt;
    }

    private Certificate getCertificate(String certName) throws GeneralSecurityException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance(FACTORY_TYPE);
        try (InputStream certificateString = getClass().getClassLoader().getResourceAsStream(certName)) {
            return certificateFactory.generateCertificate(certificateString);
        }
    }

    private TrustManagerFactory getTrustManagerFactory(final Certificate certificate, final String ca) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        // Creating a KeyStore containing our trusted CAs
        final String keyStoreType = KeyStore.getDefaultType();
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry(ca, certificate);

        // Creating a TrustManager that trusts the CAs in our KeyStore
        final String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
        trustManagerFactory.init(keyStore);
        return trustManagerFactory;
    }

}
