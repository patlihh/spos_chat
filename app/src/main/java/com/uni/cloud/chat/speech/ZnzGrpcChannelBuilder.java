package com.uni.cloud.chat.speech;

import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.support.annotation.Nullable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.okhttp.NegotiationType;
import io.grpc.okhttp.OkHttpChannelBuilder;

/**
 * A helper class to create a OkHttp based channel.
 */
public class ZnzGrpcChannelBuilder {
    /**
     * 用来生成ManagedChannel的
     * @param host ip地址
     * @param port 端口号
     * @param serverHostOverride 这个不知道是啥，是服务器端给我的
     * @param useTls 是否使用tls加密 ，ture代表使用https加密，false代表不使用，
     * @param testCa 证书的流
     * @param androidSocketFactoryTls 不知道是啥，传null
     * @return
     */
    public static ManagedChannel build(String host, int port, @Nullable String serverHostOverride,
                                       boolean useTls, @Nullable InputStream testCa, @Nullable String androidSocketFactoryTls) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        if (serverHostOverride != null) {
            // Force the hostname to match the cert the server uses.
            channelBuilder.overrideAuthority(serverHostOverride);
        }
        if (useTls) {
            try {
                SSLSocketFactory factory;
                if (androidSocketFactoryTls != null) {
                    factory = getSslCertificateSocketFactory(testCa, androidSocketFactoryTls);
                } else {
                    factory = getSslSocketFactory(testCa);
                }
                ((OkHttpChannelBuilder) channelBuilder).negotiationType(NegotiationType.TLS);
                ((OkHttpChannelBuilder) channelBuilder).sslSocketFactory(factory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // 是否使用tls加密，true不使用
            channelBuilder.usePlaintext(true);
        }
        return channelBuilder.build();
    }

    private static SSLSocketFactory getSslSocketFactory(@Nullable InputStream testCa)
            throws Exception {
        if (testCa == null) {
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, getTrustManagers(testCa), null);
        return context.getSocketFactory();
    }

    @TargetApi(14)
    private static SSLCertificateSocketFactory getSslCertificateSocketFactory(
            @Nullable InputStream testCa, String androidSocketFatoryTls) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH /* API level 14 */) {
            throw new RuntimeException(
                    "android_socket_factory_tls doesn't work with API level less than 14.");
        }
        SSLCertificateSocketFactory factory = (SSLCertificateSocketFactory)
                SSLCertificateSocketFactory.getDefault(5000 /* Timeout in ms*/);
        // Use HTTP/2.0
        byte[] h2 = "h2".getBytes();
        byte[][] protocols = new byte[][]{h2};
        if (androidSocketFatoryTls.equals("alpn")) {
            Method setAlpnProtocols =
                    factory.getClass().getDeclaredMethod("setAlpnProtocols", byte[][].class);
            setAlpnProtocols.invoke(factory, new Object[]{protocols});
        } else if (androidSocketFatoryTls.equals("npn")) {
            Method setNpnProtocols =
                    factory.getClass().getDeclaredMethod("setNpnProtocols", byte[][].class);
            setNpnProtocols.invoke(factory, new Object[]{protocols});
        } else {
            throw new RuntimeException("Unknown protocol: " + androidSocketFatoryTls);
        }

        if (testCa != null) {
            factory.setTrustManagers(getTrustManagers(testCa));
        }

        return factory;
    }

    private static TrustManager[] getTrustManagers(InputStream testCa) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(testCa);
        X500Principal principal = cert.getSubjectX500Principal();
        ks.setCertificateEntry(principal.getName("RFC2253"), cert);
        // Set up trust manager factory to use our key store.
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ks);
        return trustManagerFactory.getTrustManagers();
    }
}
