/*
 * Copyright (c) 2023 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.transport.ssh;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.netconf.shaded.sshd.client.ClientFactoryManager;
import org.opendaylight.netconf.shaded.sshd.common.FactoryManager;
import org.opendaylight.netconf.shaded.sshd.common.kex.KeyExchangeFactory;
import org.opendaylight.netconf.shaded.sshd.common.session.SessionHeartbeatController;
import org.opendaylight.netconf.shaded.sshd.server.ServerFactoryManager;
import org.opendaylight.netconf.transport.api.UnsupportedConfigurationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev221212.AsymmetricKeyPairGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev221212.EcPrivateKeyFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev221212.RsaPrivateKeyFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev221212.SshPublicKeyFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev221212.SubjectPublicKeyInfoFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev221212.asymmetric.key.pair.grouping._private.key.type.CleartextPrivateKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.keystore.rev221212.LocalOrKeystoreEndEntityCertWithKeyGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.client.rev221212.ssh.client.grouping.server.authentication.SshHostKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.common.rev221212.TransportParamsGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.common.rev221212.transport.params.grouping.KeyExchange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.truststore.rev221212.LocalOrTruststoreCertsGrouping;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

final class ConfigUtils {

    private static final int KEEP_ALIVE_DEFAULT_MAX_WAIT = 30; // seconds
    private static final int KEEP_ALIVE_DEFAULT_ATTEMPTS = 3;

    private ConfigUtils() {
        // utility class
    }

    static void setTransportParams(final @NonNull ClientFactoryManager factoryMgr,
            final @Nullable TransportParamsGrouping params) throws UnsupportedConfigurationException {
        setTransportParams(factoryMgr, params, TransportUtils::getClientKexFactories);
    }

    static void setTransportParams(final @NonNull ServerFactoryManager factoryMgr,
            final @Nullable TransportParamsGrouping params) throws UnsupportedConfigurationException {
        setTransportParams(factoryMgr, params, TransportUtils::getServerKexFactories);
    }

    static void setTransportParams(final @NonNull FactoryManager factoryMgr,
            final @Nullable TransportParamsGrouping params, final @NonNull KexFactoryProvider kexProvider)
            throws UnsupportedConfigurationException {

        factoryMgr.setCipherFactories(
                TransportUtils.getCipherFactories(params == null ? null : params.getEncryption()));
        factoryMgr.setSignatureFactories(
                TransportUtils.getSignatureFactories(params == null ? null : params.getHostKey()));
        factoryMgr.setKeyExchangeFactories(
                kexProvider.getKexFactories(params == null ? null : params.getKeyExchange()));
        factoryMgr.setMacFactories(
                TransportUtils.getMacFactories(params == null ? null : params.getMac()));
    }

    static void setKeepAlives(final @NonNull ServerFactoryManager factoryMgr,
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.server.rev221212
                    .ssh.server.grouping.Keepalives keepAlives) {
        setKeepAlives(factoryMgr,
                keepAlives == null ? null : keepAlives.getMaxWait(),
                keepAlives == null ? null : keepAlives.getMaxAttempts());
    }

    static void setKeepAlives(final @NonNull ClientFactoryManager factoryMgr,
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.client.rev221212
                    .ssh.client.grouping.Keepalives keepAlives) {
        setKeepAlives(factoryMgr,
                keepAlives == null ? null : keepAlives.getMaxWait(),
                keepAlives == null ? null : keepAlives.getMaxAttempts());
    }

    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "maxAttempts usage need clarification")
    private static void setKeepAlives(final @NonNull FactoryManager factoryMgr, final @Nullable Uint16 cfgMaxWait,
            final @Nullable Uint8 cfgMaxAttempts) {
        // FIXME utilize max attempts
        final var maxAttempts = cfgMaxAttempts == null ? KEEP_ALIVE_DEFAULT_ATTEMPTS : cfgMaxAttempts.intValue();
        final var maxWait = cfgMaxWait == null ? KEEP_ALIVE_DEFAULT_MAX_WAIT : cfgMaxWait.intValue();
        factoryMgr.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.RESERVED, Duration.ofSeconds(maxWait));
    }

    static List<KeyPair> extractServerHostKeys(
            final List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.server.rev221212
                    .ssh.server.grouping.server.identity.HostKey> serverHostKeys)
            throws UnsupportedConfigurationException {
        var listBuilder = ImmutableList.<KeyPair>builder();
        for (var hostKey : serverHostKeys) {
            if (hostKey.getHostKeyType()
                    instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.server.rev221212
                    .ssh.server.grouping.server.identity.host.key.host.key.type.PublicKey publicKey
                    && publicKey.getPublicKey() != null) {
                listBuilder.add(extractKeyPair(publicKey.getPublicKey().getLocalOrKeystore()));
            } else if (hostKey.getHostKeyType()
                    instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.server.rev221212
                    .ssh.server.grouping.server.identity.host.key.host.key.type.Certificate certificate
                    && certificate.getCertificate() != null) {
                listBuilder.add(extractCertificateEntry(certificate.getCertificate()).getKey());
            }
        }
        return listBuilder.build();
    }

    static KeyPair extractKeyPair(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.keystore.rev221212
                    .local.or.keystore.asymmetric.key.grouping.LocalOrKeystore input)
            throws UnsupportedConfigurationException {
        final var local = ofType(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.keystore.rev221212
                .local.or.keystore.asymmetric.key.grouping.local.or.keystore.Local.class, input);
        final var localDef = local.getLocalDefinition();
        if (localDef == null) {
            throw new UnsupportedConfigurationException("Missing local definition in " + local);
        }
        return extractKeyPair(localDef);
    }

    private static KeyPair extractKeyPair(final AsymmetricKeyPairGrouping input)
            throws UnsupportedConfigurationException {
        final var keyFormat = input.getPrivateKeyFormat();
        final String privateKeyAlgorithm;
        if (EcPrivateKeyFormat.VALUE.equals(keyFormat)) {
            privateKeyAlgorithm = KeyUtils.EC_ALGORITHM;
        } else if (RsaPrivateKeyFormat.VALUE.equals(input.getPrivateKeyFormat())) {
            privateKeyAlgorithm = KeyUtils.RSA_ALGORITHM;
        } else {
            throw new UnsupportedConfigurationException("Unsupported private key format " + keyFormat);
        }
        final byte[] privateKeyBytes;
        if (input.getPrivateKeyType() instanceof CleartextPrivateKey clearText) {
            privateKeyBytes = clearText.requireCleartextPrivateKey();
        } else {
            throw new UnsupportedConfigurationException("Unsupported private key type " + input.getPrivateKeyType());
        }

        final var publicKeyFormat = input.getPublicKeyFormat();
        final var publicKeyBytes = input.getPublicKey();
        final boolean isSshPublicKey;
        if (SubjectPublicKeyInfoFormat.VALUE.equals(publicKeyFormat)) {
            isSshPublicKey = false;
        } else if (SshPublicKeyFormat.VALUE.equals(publicKeyFormat)) {
            isSshPublicKey = true;
        } else {
            throw new UnsupportedConfigurationException("Unsupported public key format " + publicKeyFormat);
        }

        final var privateKey = KeyUtils.buildPrivateKey(privateKeyAlgorithm, privateKeyBytes);
        final var publicKey = isSshPublicKey ? KeyUtils.buildPublicKeyFromSshEncoding(publicKeyBytes)
                : KeyUtils.buildX509PublicKey(privateKeyAlgorithm, publicKeyBytes);
        /*
            ietf-crypto-types:grouping asymmetric-key-pair-grouping
            "A private key and its associated public key.  Implementations
            SHOULD ensure that the two keys are a matching pair."
         */
        KeyUtils.validateKeyPair(publicKey, privateKey);
        return new KeyPair(publicKey, privateKey);
    }

    static List<Certificate> extractCertificates(@Nullable final LocalOrTruststoreCertsGrouping input)
            throws UnsupportedConfigurationException {
        if (input == null) {
            return List.of();
        }
        final var local = ofType(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.truststore
                        .rev221212.local.or.truststore.certs.grouping.local.or.truststore.Local.class,
                input.getLocalOrTruststore());
        final var localDef = local.getLocalDefinition();
        if (localDef == null) {
            throw new UnsupportedConfigurationException("Missing local definition in " + local);
        }
        final var listBuilder = ImmutableList.<Certificate>builder();
        for (var cert : localDef.nonnullCertificate().values()) {
            listBuilder.add(KeyUtils.buildX509Certificate(cert.requireCertData().getValue()));
        }
        return listBuilder.build();
    }

    private static Map.Entry<KeyPair, List<X509Certificate>> extractCertificateEntry(
            final LocalOrKeystoreEndEntityCertWithKeyGrouping input) throws UnsupportedConfigurationException {
        final var local = ofType(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.keystore.rev221212
                        .local.or.keystore.end.entity.cert.with.key.grouping.local.or.keystore.Local.class,
                input.getLocalOrKeystore());
        final var localDef = local.getLocalDefinition();
        if (localDef == null) {
            throw new UnsupportedConfigurationException("Missing local definition in " + local);
        }
        final var keyPair = extractKeyPair(localDef);
        final var certificate = KeyUtils.buildX509Certificate(localDef.requireCertData().getValue());
        /*
          ietf-crypto-types:asymmetric-key-pair-with-cert-grouping
          "A private/public key pair and an associated certificate.
          Implementations SHOULD assert that certificates contain the matching public key."
         */
        KeyUtils.validatePublicKey(keyPair.getPublic(), certificate);
        return new SimpleImmutableEntry<>(keyPair, List.of(certificate));
    }

    private static <T> T ofType(final Class<T> expectedType, final Object obj)
            throws UnsupportedConfigurationException {
        if (!expectedType.isInstance(obj)) {
            throw new UnsupportedConfigurationException("Expected type: " + expectedType
                    + " actual: " + obj.getClass());
        }
        return expectedType.cast(obj);
    }

    static List<PublicKey> extractPublicKeys(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.truststore.rev221212
                    .local.or.truststore._public.keys.grouping.LocalOrTruststore input)
            throws UnsupportedConfigurationException {
        final var local = ofType(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.truststore.rev221212
                .local.or.truststore._public.keys.grouping.local.or.truststore.Local.class, input);
        final var localDef = local.getLocalDefinition();
        if (localDef == null) {
            throw new UnsupportedConfigurationException("Missing local definition in " + local);
        }
        final var listBuilder = ImmutableList.<PublicKey>builder();
        if (localDef.getPublicKey() != null && localDef.getPublicKey().entrySet() != null) {
            for (var entry : localDef.getPublicKey().entrySet()) {
                if (!SshPublicKeyFormat.VALUE.equals(entry.getValue().getPublicKeyFormat())) {
                    throw new UnsupportedConfigurationException("ssh public key format is expected");
                }
                listBuilder.add(KeyUtils.buildPublicKeyFromSshEncoding(entry.getValue().getPublicKey()));
            }
        }
        return listBuilder.build();
    }

    static List<PublicKey> extractPublicKeys(final @Nullable SshHostKeys sshHostKeys)
            throws UnsupportedConfigurationException {
        return sshHostKeys == null ? List.of() : extractPublicKeys(sshHostKeys.getLocalOrTruststore());
    }

    @FunctionalInterface
    private interface KexFactoryProvider {
        List<KeyExchangeFactory> getKexFactories(KeyExchange input) throws UnsupportedConfigurationException;
    }
}
