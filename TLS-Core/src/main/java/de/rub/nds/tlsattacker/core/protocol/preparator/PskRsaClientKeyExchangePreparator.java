/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.modifiablevariable.util.RandomHelper;
import de.rub.nds.tlsattacker.core.protocol.message.PskRsaClientKeyExchangeMessage;
import static de.rub.nds.tlsattacker.core.protocol.preparator.Preparator.LOGGER;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import java.math.BigInteger;
import java.util.Arrays;

/**
 *
 * @author Florian Linsner - florian.linsner@rub.de
 */
public class PskRsaClientKeyExchangePreparator extends ClientKeyExchangePreparator<PskRsaClientKeyExchangeMessage> {

    private byte[] premasterSecret;
    private byte[] clientRandom;
    private byte[] padding;
    private byte[] randomValue;
    private byte[] encryptedPremasterSecret;
    private final PskRsaClientKeyExchangeMessage msg;
    private ByteArrayOutputStream outputStream;

    public PskRsaClientKeyExchangePreparator(Chooser chooser, PskRsaClientKeyExchangeMessage message) {
        super(chooser, message);
        this.msg = message;
    }

    @Override
    public void prepareHandshakeMessageContents() {
        msg.setIdentity(msg.getIdentity());
        msg.setIdentityLength(msg.getIdentityLength());
        msg.prepareComputations();

        prepareClientRandom(msg);
        int keyByteLength = chooser.getRsaModulus().bitLength() / 8;
        // the number of random bytes in the pkcs1 message
        int randomByteLength = keyByteLength - HandshakeByteLength.PREMASTER_SECRET - 3;
        padding = new byte[randomByteLength];
        RandomHelper.getRandom().nextBytes(padding);
        ArrayConverter.makeArrayNonZero(padding);
        preparePadding(msg);
        randomValue = generateRandomValue();
        premasterSecret = generatePremasterSecret(randomValue);
        preparePremasterSecret(msg);
        encryptedPremasterSecret = generateEncryptedPremasterSecret(randomValue);
        prepareEncryptedPremasterSecret(msg);
        prepareEncryptedPremasterSecretLength(msg);
        prepareSerializedPublicKey(msg);
        prepareSerializedPublicKeyLength(msg);
    }

    private byte[] generatePremasterSecret(byte[] randomValue) {
        outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(ArrayConverter.intToBytes(HandshakeByteLength.PREMASTER_SECRET,
                    HandshakeByteLength.ENCRYPTED_PREMASTER_SECRET_LENGTH));
            outputStream.write(randomValue);
            outputStream.write(ArrayConverter.intToBytes(chooser.getConfig().getDefaultPSKKey().length,
                    HandshakeByteLength.PSK_LENGTH));
            outputStream.write(chooser.getConfig().getDefaultPSKKey());
        } catch (IOException ex) {
            LOGGER.warn("Encountered exception while writing to ByteArrayOutputStream.");
            LOGGER.debug(ex);
        }
        byte[] tempPremasterSecret = outputStream.toByteArray();
        return tempPremasterSecret;
    }

    private byte[] generateRandomValue() {
        byte[] tempPremasterSecret = new byte[HandshakeByteLength.PREMASTER_SECRET];
        RandomHelper.getRandom().nextBytes(tempPremasterSecret);
        tempPremasterSecret[0] = chooser.getSelectedProtocolVersion().getMajor();
        tempPremasterSecret[1] = chooser.getSelectedProtocolVersion().getMinor();
        return tempPremasterSecret;
    }

    private byte[] generateEncryptedPremasterSecret(byte[] randomValue) {
        byte[] paddedPremasterSecret = ArrayConverter.concatenate(new byte[] { 0x00, 0x02 }, padding,
                new byte[] { 0x00 }, randomValue);
        if (paddedPremasterSecret.length == 0) {
            paddedPremasterSecret = new byte[] { 0 };
        }
        BigInteger biPaddedPremasterSecret = new BigInteger(1, paddedPremasterSecret);
        BigInteger biEncrypted = biPaddedPremasterSecret.modPow(chooser.getServerRSAPublicKey(),
                chooser.getRsaModulus());
        byte[] encrypted = ArrayConverter.bigIntegerToByteArray(biEncrypted, chooser.getRsaModulus().bitLength() / 8,
                true);
        return encrypted;
    }

    private void prepareEncryptedPremasterSecret(PskRsaClientKeyExchangeMessage msg) {
        msg.getComputations().setEncryptedPremasterSecret(encryptedPremasterSecret);
    }

    private void prepareEncryptedPremasterSecretLength(PskRsaClientKeyExchangeMessage msg) {
        msg.getComputations().setEncryptedPremasterSecretLength(
                encryptedPremasterSecret.length);
    }

    private void preparePremasterSecret(PskRsaClientKeyExchangeMessage msg) {
        msg.getComputations().setPremasterSecret(premasterSecret);
        LOGGER.debug("PremasterSecret: "
                + ArrayConverter.bytesToHexString(msg.getComputations().getPremasterSecret().getValue()));
    }

    private void prepareClientRandom(PskRsaClientKeyExchangeMessage msg) {
        // TODO spooky
        clientRandom = ArrayConverter.concatenate(chooser.getClientRandom(), chooser.getServerRandom());
        msg.getComputations().setClientRandom(clientRandom);
        LOGGER.debug("ClientRandom: "
                + ArrayConverter.bytesToHexString(msg.getComputations().getClientRandom().getValue()));
    }

    private void preparePadding(PskRsaClientKeyExchangeMessage msg) {
        msg.getComputations().setPadding(padding);
        LOGGER.debug("Padding: " + ArrayConverter.bytesToHexString(msg.getComputations().getPadding().getValue()));
    }

    private void prepareSerializedPublicKey(PskRsaClientKeyExchangeMessage msg) {
        msg.setPublicKey(encryptedPremasterSecret);
        LOGGER.debug("SerializedPublicKey: " + Arrays.toString(msg.getPublicKey().getValue()));
    }

    private void prepareSerializedPublicKeyLength(PskRsaClientKeyExchangeMessage msg) {
        msg.setPublicKeyLength(msg.getPublicKey().getValue().length);
    }

    private byte[] decryptPremasterSecret() {
        BigInteger bigIntegerEncryptedPremasterSecret = new BigInteger(1, msg.getPublicKey().getValue());
        BigInteger serverPrivateKey = chooser.getConfig().getDefaultServerRSAPrivateKey();
        BigInteger decrypted = bigIntegerEncryptedPremasterSecret.modPow(serverPrivateKey, chooser.getRsaModulus());
        return decrypted.toByteArray();
    }

    @Override
    public void prepareAfterParse() {
        // Decrypt premaster secret
        msg.prepareComputations();
        byte[] paddedPremasterSecret = decryptPremasterSecret();
        LOGGER.debug("PaddedPremaster:" + ArrayConverter.bytesToHexString(paddedPremasterSecret));

        int keyByteLength = chooser.getRsaModulus().bitLength() / 8;
        // the number of random bytes in the pkcs1 message
        int randomByteLength = keyByteLength - HandshakeByteLength.PREMASTER_SECRET - 1;
        premasterSecret = generatePremasterSecret(Arrays.copyOfRange(paddedPremasterSecret, randomByteLength,
                paddedPremasterSecret.length));
        LOGGER.debug("PaddedPremaster:" + ArrayConverter.bytesToHexString(paddedPremasterSecret));
        preparePremasterSecret(msg);
        prepareClientRandom(msg);
    }
}
