/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.*;

import java.io.*;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;


@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
public class PgpUtils {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PgpUtils.class);

    /**
     * This is supose to be a static access only class. Therefor, a private constructor was defined.
     */
    private PgpUtils() {
    }

    /**
     * A simple routine that opens a key ring file and loads the first available key suitable for
     * encryption.
     *
     * @param in inputstream for the public key
     * @return A PGPPublicKey object based on the given file
     * @throws IOException  if file could not be read
     * @throws PGPException if keyring cannot be generated
     */
    private static PGPPublicKey readPublicKey(InputStream in) throws IOException, PGPException {
        in = PGPUtil.getDecoderStream(in);

        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in);

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //
        PGPPublicKey key = null;

        //
        // iterate through the key rings.
        //
        Iterator rIt = pgpPub.getKeyRings();

        while (key == null && rIt.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
            Iterator kIt = kRing.getPublicKeys();

            while (key == null && kIt.hasNext()) {
                PGPPublicKey k = (PGPPublicKey) kIt.next();

                if (k.isEncryptionKey()) {
                    key = k;
                }
            }
        }

        if (key == null) {
            throw new IllegalArgumentException("Can't find encryption key in key ring.");
        }
        return key;
    }

    /**
     * Load a secret key ring collection from keyIn and find the secret key corresponding to
     * keyID if it exists.
     *
     * @param keyIn input stream representing a key ring collection.
     * @param keyID keyID we want.
     * @param pass  passphrase to decrypt secret key with.
     * @return PrivateKey Object from the keyring given.
     * @throws IOException             files could not be read
     * @throws NoSuchProviderException if private key cannot be extracted from keyring (Possible encoding
     *                                 files mismatch)
     * @throws PGPException            if keyring cannot be generated
     */
    private static PGPPrivateKey findSecretKey(InputStream keyIn, long keyID, char[] pass) throws IOException,
            PGPException, NoSuchProviderException {
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));
        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

        if (pgpSecKey == null) {
            return null;
        }

        return pgpSecKey.extractPrivateKey(pass, "BC");
    }

    /**
     * Generates a RSA PGPPublicKey/PGPSecretKey pair.
     *
     * @param secretOut  Output Stream for the secret key file
     * @param publicOut  Output Stream for the public key file
     * @param publicKey  Public Key object
     * @param privateKey Private Key object
     * @param identity   Identifier for the key Ring
     * @param passPhrase Passphrase for secret key decryption
     * @param armor      Should the generated key be armored? If false the output won't be readable(binary file) if false
     *                   it will be readable (text file).
     * @throws IOException             files could not be written
     * @throws NoSuchProviderException if keyring cannot be generated
     * @throws PGPException            if keyring cannot be generated
     */
    public static void exportKeyPair(OutputStream secretOut, OutputStream publicOut,
            PublicKey publicKey, PrivateKey privateKey,
            String identity, char[] passPhrase,
            boolean armor) throws IOException, NoSuchProviderException, PGPException {
        logger.debug("Exporting Key Files...");
        if (armor) {
            secretOut = new ArmoredOutputStream(secretOut);
        }

        PGPSecretKey secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION,
                PGPPublicKey.RSA_GENERAL, publicKey, privateKey,
                new Date(), identity, PGPEncryptedData.CAST5,
                passPhrase, null, null, new SecureRandom(), "BC");

        secretKey.encode(secretOut);

        secretOut.close();

        if (armor) {
            publicOut = new ArmoredOutputStream(publicOut);
        }

        PGPPublicKey key = secretKey.getPublicKey();

        key.encode(publicOut);

        publicOut.close();
        logger.debug("Key Files Exported.");
    }


    /**
     * Decrypts a pgp encrypted file (in) using a private key file (keyIn) and the a passphrase (passwd - used to
     * decrypt the private key). The decrypted file is placed in destPath and it's full path is returned.
     *
     * @param in       Input Stream for the encrypted file
     * @param destPath Path for the destination folder (where the output file will be placed)
     * @param keyIn    Input Stream for the private key file
     * @param passwd   Password used in private key encryption (it can be an empty String)
     * @param defaultOutputFilename filename to be used in case the pgp file does not have the information about the file
     * @return Returns the path for the output file
     * @throws Exception if it fails to decrypt the file
     */
    public static String decryptFile(InputStream in, String destPath, InputStream keyIn, char[] passwd, String defaultOutputFilename) throws Exception {
        logger.debug("Decrypting File...");
        String destFileName = null;
        in = PGPUtil.getDecoderStream(in);
        //Stream Declarations
        BufferedOutputStream bOut = null;
        InputStream compressedStream = null;

        try {
            PGPObjectFactory pgpF = new PGPObjectFactory(in);
            PGPEncryptedDataList enc;

            Object o = pgpF.nextObject();
            //
            // the first object might be a PGP marker packet.
            //
            if (o instanceof PGPEncryptedDataList) {
                enc = (PGPEncryptedDataList) o;
            }
            else {
                enc = (PGPEncryptedDataList) pgpF.nextObject();
            }

            //
            // find the secret key
            //
            Iterator it = enc.getEncryptedDataObjects();
            PGPPrivateKey sKey = null;
            PGPPublicKeyEncryptedData pbe = null;

            while (sKey == null && it.hasNext()) {
                pbe = (PGPPublicKeyEncryptedData) it.next();
                sKey = findSecretKey(keyIn, pbe.getKeyID(), passwd);
            }

            if (sKey == null) {
                throw new IllegalArgumentException("secret key for message not found.");
            }

            InputStream clear = pbe.getDataStream(sKey, "BC");

            PGPObjectFactory plainFact = new PGPObjectFactory(clear);

            PGPCompressedData cData = (PGPCompressedData) plainFact.nextObject();

            compressedStream = new BufferedInputStream(cData.getDataStream());
            PGPObjectFactory pgpFact = new PGPObjectFactory(compressedStream);

            Object message = pgpFact.nextObject();

            if (message instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData) message;

                if (Common.isBlank(ld.getFileName())) {
                    destFileName = destPath.replaceFirst("(/|\\\\)$", "") + '/' + defaultOutputFilename;
                } else {
                    destFileName = destPath + ld.getFileName();
                }


                FileOutputStream fOut = new FileOutputStream(destFileName);
                bOut = new BufferedOutputStream(fOut);

                InputStream unc = ld.getInputStream();
                int ch;

                while ((ch = unc.read()) >= 0) {
                    bOut.write(ch);
                }
            }
            else if (message instanceof PGPOnePassSignatureList) {
                throw new PGPException("encrypted message contains a signed message - not literal data.");
            }
            else {
                throw new PGPException("message is not a simple encrypted file - type unknown.");
            }

            if (pbe.isIntegrityProtected()) {
                if (!pbe.verify()) {
                    logger.error("message failed integrity check");
                }
                else {
                    logger.debug("message integrity check passed");
                }
            }
            else {
                logger.debug("no message integrity check");
            }
        }
        catch (PGPException e) {
            logger.error(e.getMessage(), e);
            if (e.getUnderlyingException() != null) {
                logger.debug("Underlying Exception - " + e.getUnderlyingException() + '\n', e.getUnderlyingException());
            }
        }
        finally {
            Common.close(compressedStream, bOut);
        }
        logger.debug("Decryption done. Output file created in " + destFileName);
        return destFileName;
    }

    /**
     * Encrypts a file (filename) using a public key file (keyInStream). A given OutputStream is used for the output
     * file(keyInStream).
     *
     * @param out                Output Stream for the encrypted file.
     * @param fileName           Path for the file that will be encrypted
     * @param keyInStream        Input Stream for the public Key
     * @param armor              Is the private key armored? If false the private key won't be readable (binary file) if
     *                           false it will be readable (text file).
     * @param withIntegrityCheck Should the encryption be followed by an integrity check?
     * @throws IOException             if the input file can't be found
     * @throws NoSuchProviderException if public key couldn't be read properly
     */
    public static void encryptFile(OutputStream out, String fileName,
            InputStream keyInStream, boolean armor,
            boolean withIntegrityCheck) throws IOException,
            NoSuchProviderException {
        logger.debug("Encripting file " + fileName + "...");

        if (armor) {
            out = new ArmoredOutputStream(out);
        }

        OutputStream cOut = null;
        PGPCompressedDataGenerator comData = null;

        try {
            PGPPublicKey encKey = readPublicKey(keyInStream);
            PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
                    PGPEncryptedData.CAST5, withIntegrityCheck,
                    new SecureRandom(), "BC");

            cPk.addMethod(encKey);

            cOut = cPk.open(out, new byte[1 << 16]);

            comData = new PGPCompressedDataGenerator(
                    PGPCompressedData.ZIP);

            PGPUtil.writeFileToLiteralData(comData.open(cOut),
                    PGPLiteralData.BINARY, new File(fileName),
                    new byte[1 << 16]);
        }
        catch (PGPException e) {
            logger.error(e.getMessage(), e);
            if (e.getUnderlyingException() != null) {
                logger.debug("Underlying Exception - " + e.getUnderlyingException() + '\n', e.getUnderlyingException());
            }
        }
        finally {
            Common.close(comData, cOut, out);
        }
        logger.debug("Encryption done.");
    }


}
