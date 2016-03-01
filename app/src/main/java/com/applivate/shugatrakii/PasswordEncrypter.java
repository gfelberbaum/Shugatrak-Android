package com.applivate.shugatrakii;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/**
 *
 *
 */
public class PasswordEncrypter {

    public static final String SALT_SEP = "::";

    public static final String CIPHER_TYPE_DEFAULT = "RSA/ECB/NoPadding";

    /*
    Find the public RSA key modulus

    $ openssl rsa -pubin -in pubkey.pem -modulus -noout
    Modulus=F56D...
    Find the public RSA key Exponent

    $ openssl rsa -pubin -in pubkey.pem -text -noout
    ...
    Exponent: 65537 (0x10001)
     */

    BigInteger modulus = new BigInteger("A275DCBE58A8754C48F169963C8E71951CE43445EB1C3070F9015F6A8D23906C218545175D5B8F66C8BD2A731544632C7845C6FD39A1FFB9A0A91AAA114DF50302ED066C6C5637CE3BDEA04BACA8E14D4E786280CF0403055234031F7E19F395363E13441CCB946ED47E4BCFB3B17701278D7AAFDC3C215C8DF882FDD1C89CC1A639BABA98D591475212447344B0F536ABA5B2383468217B9679105E185EB7BD5365441613B5E1006D0C123C1A06BFED807BDA48E6B9E5D09637C25189B01B51511DF6A4785FE2C9C153EFEB75F5A303AE127360FED56CED7DE475A2E4E7E17E442EF6E8EB51A0D806DDE0D4114A434045C84685A1A0AABC82DF1A3B99DECEE3", 16);
    BigInteger pubExp = new BigInteger("65537", 10);

    /** encrypted password */
    byte[] cipherData = null;

    public static PasswordEncrypter getInstance(String unencrypted){
        return new PasswordEncrypter(unencrypted, CIPHER_TYPE_DEFAULT);
    }

    public static PasswordEncrypter getInstance(String unencrypted, String cipherType){
        return new PasswordEncrypter(unencrypted, cipherType);
    }

    private PasswordEncrypter(String unencrypted, String cipherType){
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, pubExp);
        RSAPublicKey key = null;
        try {
            key = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return;
        }

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(cipherType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return;
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return;
        }

        String saltStr = SALT_SEP + Math.random() + Math.random() + Math.random();
        String toEncrypt = unencrypted + saltStr;
        cipherData = new byte[0];
        try {
            cipherData = cipher.doFinal(toEncrypt.getBytes());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return;
        }

    }

    /**
     *
     * @return encrypted password + SALT_SEP + salt ( [0-9.]+ )
     */
    public String getEncryptedBase64(){
        if (cipherData == null){
            return "";
        }
        // Base64 not included till android 8, currently this app supports android 7
        return Base64.encodeToString(cipherData, false);
    }

}
