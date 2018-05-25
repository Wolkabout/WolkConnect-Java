/*
 * Copyright (c) 2018 WolkAbout Technology s.r.o.
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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class MqttBuilder {

    private static final String FACTORY_TYPE = "X.509";

    private WeakReference<Wolk.Builder> wolkBuilder;

    /**
     * URL of the MQTT broker. Must start with "ssl://" or "tcp://"
     */
    private String host;

    /**
     * Username for the connection.
     */
    private String deviceKey;

    /**
     * Password for the connection.
     */
    private String password;

    /**
     * Sets the "keep alive" interval.
     * This value, measured in seconds, defines the maximum time interval
     * between messages sent or received. It enables the client to
     * detect if the server is no longer available, without
     * having to wait for the TCP/IP timeout. The client will ensure
     * that at least one message travels across the network within each
     * keep alive period.  In the absence of a data-related message during
     * the time period, the client sends a very small "ping" message, which
     * the server will acknowledge.
     * A value of 0 disables keepalive processing in the client.
     */
    private int keepAlive = 60;

    /**
     * Sets whether the client and server should remember state across restarts and reconnects.
     * If set to false both the client and server will maintain state across
     * restarts of the client, the server and the connection. As state is maintained:
     * Message delivery will be reliable meeting the specified QOS even if the client,
     * server or connection are restarted. The server will treat a subscription as durable.
     */
    private boolean cleanSession = true;

    /**
     * Sets the connection timeout value.
     * This value, measured in seconds, defines the maximum time interval
     * the client will wait for the network connection to the MQTT server to be established.
     * The default timeout is 30 seconds.
     * A value of 0 disables timeout processing meaning the client will wait until the
     * network connection is made successfully or fails.
     */
    private int connectionTimeout = 30;

    /**
     * Max message inflight.
     */
    private int maxInflight = 10;


    /**
     * Persistence for inflight MQTT messages. If not set, defaults to {@link MqttDefaultFilePersistence}.
     */
    private MqttClientPersistence persistence = new MqttDefaultFilePersistence();

    /**
     * Sets the certificate authority to be used for SSL authentication.
     * Only used if the host URL starts with "ssl://"
     */
    private String certificateAuthority;

    MqttBuilder(Wolk.Builder wolkBuilder) {
        this.wolkBuilder = new WeakReference<>(wolkBuilder);
    }

    public MqttBuilder host(String host) {
        if (!host.startsWith("ssl://") && !host.startsWith("tcp://")) {
            throw new IllegalArgumentException("Host must start with ssl:// or tcp://");
        }

        this.host = host;
        return this;
    }

    public MqttBuilder deviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
        return this;
    }

    public MqttBuilder password(String password) {
        this.password = password;
        return this;
    }

    public MqttBuilder keepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public MqttBuilder cleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        return this;
    }

    public MqttBuilder connectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public MqttBuilder maxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
        return this;
    }

    public MqttBuilder persistence(MqttClientPersistence mqttClientPersistence) {
        this.persistence = mqttClientPersistence;
        return this;
    }

    public MqttBuilder sslCertification(String certificateAuthority) throws Exception {
        if (certificateAuthority == null || certificateAuthority.isEmpty()) {
            throw new IllegalArgumentException("Invalid certification authority.");
        }

        this.certificateAuthority = certificateAuthority;
        return this;
    }

    public Wolk.Builder build() {
        return wolkBuilder.get();
    }

    public MqttClient connect() throws MqttException {
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(deviceKey);
        options.setPassword(password.toCharArray());
        options.setKeepAliveInterval(keepAlive);
        options.setCleanSession(cleanSession);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(connectionTimeout);
        options.setMaxInflight(maxInflight);

        options.setWill("lastwill/" + deviceKey, "Gone offline".getBytes(), 2, false);

        if (host.startsWith("ssl") && certificateAuthority != null) {
            options.setSocketFactory(getSslSocketFactory());
        }

        final MqttClient client = new MqttClient(host, deviceKey, persistence);
        client.connect(options);
        return client;
    }

    private SSLSocketFactory getSslSocketFactory() {
        try {
            final String certificateName = certificateAuthority;
            final Certificate certificate = getCertificate(certificateName);
            final TrustManagerFactory trustManagerFactory = getTrustManagerFactory(certificate, certificateName);
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create socket factory.", e);
        }
    }

    private Certificate getCertificate(String certName) throws GeneralSecurityException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance(FACTORY_TYPE);
        try (InputStream certificateString = getClass().getClassLoader().getResourceAsStream(certName)) {
            return certificateFactory.generateCertificate(certificateString);
        }
    }

    private TrustManagerFactory getTrustManagerFactory(Certificate certificate, String ca) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
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
