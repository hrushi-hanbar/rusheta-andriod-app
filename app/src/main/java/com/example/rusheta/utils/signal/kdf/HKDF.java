package com.example.rusheta.utils.signal.kdf;

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class HKDF {
    private static final int HASH_OUTPUT_SIZE  = 32;

    public byte[] deriveSecrets(byte[] inputKeyMaterial, byte[] info, int outputLength) {
        byte[] salt = new byte[HASH_OUTPUT_SIZE];
        return deriveSecrets(inputKeyMaterial, salt, info, outputLength);
    }

    public byte[] deriveSecrets(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputLength) {
        byte[] prk = extract(salt, inputKeyMaterial);
        return expand(prk, info, outputLength);
    }

    private byte[] extract(byte[] salt, byte[] inputKeyMaterial) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            return mac.doFinal(inputKeyMaterial);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AssertionError(e);
        }
    }

    private byte[] expand(byte[] prk, byte[] info, int outputSize) {
        try {
            int                   iterations     = (int) Math.ceil((double) outputSize / (double) HASH_OUTPUT_SIZE);
            byte[]                mixin          = new byte[0];
            ByteArrayOutputStream results        = new ByteArrayOutputStream();
            int                   remainingBytes = outputSize;

            for (int i= getIterationStartOffset();i<iterations + getIterationStartOffset();i++) {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(prk, "HmacSHA256"));

                mac.update(mixin);
                if (info != null) {
                    mac.update(info);
                }
                mac.update((byte)i);

                byte[] stepResult = mac.doFinal();
                int    stepSize   = Math.min(remainingBytes, stepResult.length);

                results.write(stepResult, 0, stepSize);

                mixin          = stepResult;
                remainingBytes -= stepSize;
            }

            return results.toByteArray();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AssertionError(e);
        }
    }
    protected abstract int getIterationStartOffset();

}
