package am.ik.lab.console.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Base64Utils;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "jwt")
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private JWSSigner signer;

    private JWSVerifier verifier;

    private String verifierKey;

    private String signingKey;

    public void setKeyPair(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        signer = new RSASSASigner(privateKey);
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        verifier = new RSASSAVerifier(publicKey);
        verifierKey = "-----BEGIN PUBLIC KEY-----\n"
            + Base64Utils.encodeToString(publicKey.getEncoded())
            + "\n-----END PUBLIC KEY-----";
    }

    public void setSigningKey(String key) throws Exception {
        this.signingKey = key;
        key = key.trim();

        key = key
            .replace("fake", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----\n", "")
            .replace("-----END RSA PRIVATE KEY-----", "").trim().replace("\n", "");
        byte[] encoded = Base64Utils.decodeFromString(key);
        DerInputStream derInputStream = new DerInputStream(encoded);
        DerValue[] seq = derInputStream.getSequence(0);

        BigInteger modulus = seq[1].getBigInteger();
        BigInteger publicExp = seq[2].getBigInteger();
        BigInteger privateExp = seq[3].getBigInteger();
        BigInteger prime1 = seq[4].getBigInteger();
        BigInteger prime2 = seq[5].getBigInteger();
        BigInteger exp1 = seq[6].getBigInteger();
        BigInteger exp2 = seq[7].getBigInteger();
        BigInteger crtCoef = seq[8].getBigInteger();

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp,
            privateExp, prime1, prime2, exp1, exp2, crtCoef);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.signer = new RSASSASigner(kf.generatePrivate(keySpec));
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setVerifierKey(String key) {
        this.verifierKey = key;
    }

    public String getVerifierKey() {
        return verifierKey;
    }

    SignedJWT sign(JWTClaimsSet claimsSet) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256) //
            .keyID(getKey().get("kid"))//
            .type(JOSEObjectType.JWT) //
            .build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return signedJWT;
    }

    public boolean isValid(SignedJWT jwt) {
        try {
            return jwt.verify(this.verifier);
        } catch (JOSEException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public SignedJWT generateToken(String baseUrl, String applicationId) {
        Instant now = Instant.now();
        Instant exp = now.plus(10, ChronoUnit.SECONDS);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(baseUrl + "/oauth/token") //
            .expirationTime(Date.from(exp)) //
            .issueTime(Date.from(now)) //
            .claim("scope", Collections.singletonList("actuator.read")) //
            .audience(applicationId) //
            .build();
        return this.sign(claimsSet);
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        if (this.verifier != null) {
            return;
        }

        String key = this.verifierKey.replace("-----BEGIN PUBLIC KEY-----\n", "")
            .replace("-----END PUBLIC KEY-----", "").trim().replace("\n", "");

        byte[] decode = Base64Utils.decodeFromString(key);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(keySpec);
        JWSVerifier verifier = new RSASSAVerifier(publicKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("test").build();
        SignedJWT signedJWT = sign(claimsSet);

        if (!signedJWT.verify(verifier)) {
            throw new IllegalStateException(
                "The pair of verifierKey and signingKey is wrong.");
        }

        this.verifier = verifier;
    }

    public Map<String, String> getKey() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("kid", "init");
        result.put("alg", "RS256");
        result.put("value", verifierKey);
        return result;
    }
}