package com.ninjaflip.androidrevenge.core.security;

import com.nimbusds.jwt.SignedJWT;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.utils.NetworkAddressUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import io.jsonwebtoken.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Solitario on 03/06/2017.
 * <p>
 * This class wraps web server security logic.
 * <p>
 * JWT: JSON-based open standard (RFC 7519) for creating access tokens that assert some number of claims.
 * For example, a server could generate a token that has the claim "logged in as admin" and provide
 * that to a client. The client could then use that token to prove that it is logged in as admin.
 * The tokens are signed by the server's key, so the client and server are both able to verify
 * that the token is legitimate. The tokens are designed to be compact,
 * URL-safe and usable especially in web browser single sign-on (SSO) context.
 * JWT claims can be typically used to pass identity of authenticated users between an
 * identity provider and a service provider, or any other type of claims as required
 * by business processes.
 * The tokens can also be authenticated and encrypted.
 * </p>
 */

public class ServerSecurityManager {
    public static final Logger LOGGER = Logger.getLogger(ServerSecurityManager.class.getName());
    private static ServerSecurityManager INSTANCE;
    private static byte[] sharedKey;
    private static Map<String, String> userToServerTokens;
    private static Map<String, String> userToDesktopTokens;


    private ServerSecurityManager() {
        // generate random shared key for JWS
        sharedKey = new byte[32];
        new SecureRandom().nextBytes(sharedKey);
        if(userToServerTokens != null) {
            userToServerTokens.clear();
            userToServerTokens = null;
        }
        userToServerTokens = new HashMap<String, String>();
        if(userToDesktopTokens != null) {
            userToDesktopTokens.clear();
            userToDesktopTokens = null;
        }
        userToDesktopTokens = new HashMap<String, String>();
    }

    /**
     * This method resets the security manager, so that the next call to getInstance will recreate the sharedKey.
     * This method is called at every service start and destroy, and also when the connectivity is lost.
     * To ensure that tokens are renewed whenever the services is restarted, and also to ensure that user's websocket connection is always alive
     */
    public static void resetInstance(){
        INSTANCE = null;
    }

    public static ServerSecurityManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ServerSecurityManager();
        }
        return INSTANCE;
    }

    /**
     * Sample method to construct a JWT security token and persist it in memory.
     * This token will authenticate all requests between the browser and the android app
     *
     * @param uuid connected user uuid
     * @return token, something like eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
     */
    public String generateNewServerToken(String uuid) {
        if(userToServerTokens.containsKey(uuid)){
            userToServerTokens.remove(uuid);
            //throw new IllegalArgumentException("You are already connected!");
        }

        if (userToServerTokens.size() > 0) {
            throw new IllegalArgumentException("No more than 1 active user could connect to the server at once!");
        }
        /*
        if (!userToServerTokens.containsKey(uuid) && userToServerTokens.size() > 0) {
            throw new IllegalArgumentException("No more than 1 active user could connect to the server at once!");
        }*/

        // We need a signing key, so we'll create one just for this example. Usually
        // the key would be read from your application configuration instead.
        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder()
                .setSubject("revenge-html-client")
                .setIssuer("www.revenge.com")
                .claim("uuid", uuid)
                .signWith(SignatureAlgorithm.HS256, sharedKey);
        //.signWith(SignatureAlgorithm.HS512, key);

        // token expires in 24 hours
        long nowMillis = System.currentTimeMillis();
        long ttlMillis = 24 * 60 * 60 * 1000;
        long expMillis = nowMillis + ttlMillis;
        Date exp = new Date(expMillis);
        builder.setExpiration(exp);

        //Builds the JWT and serializes it to a compact, URL-safe string
        String token = builder.compact();

        userToServerTokens.put(uuid, token);
        return token;
    }

    /**
     * Sample method to validate an incoming JWT token
     *
     * @param incomingToken the token to verify
     * @param userUuid        token's owner uuid
     * @return true if verified, false otherwise
     */
    public boolean verifyToken(String incomingToken, String userUuid) {
        if (userToServerTokens.size() == 0 || !userToServerTokens.containsValue(incomingToken)) {
            return false;
        }

        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .requireSubject("revenge-html-client")
                    .requireIssuer("www.revenge.com")
                    .setSigningKey(sharedKey)
                    .parseClaimsJws(incomingToken);
        } catch (SignatureException e) {
            return false;
        } catch (MissingClaimException e) {
            // we get here if the required claim is not present
            return false;
        } catch (IncorrectClaimException e) {
            // we get here if the required claim has the wrong value
            return false;
        }

        try {
            Claims claims = jws.getBody();
            /*System.out.println("ID: " + claims.getId());
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issuer: " + claims.getIssuer());
            System.out.println("Expiration: " + claims.getExpiration().toString());
            System.out.println("userId: " + claims.get("userId"));*/
            if (!(claims.getExpiration().getTime() > System.currentTimeMillis())) {
                System.err.println("server token has expired");
                return false;
            } else {
                return (claims.get("uuid").equals(userUuid));
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Remove (userUuid,token) from in-memory map.
     * When token is invalidated, all request from user will receive 401 unauthorized code, and redirected to
     * sign-in page.
     *
     * @param token the token to remove
     */
    public void invalidateToken(String token) {
        if (userToServerTokens.size() != 0 && userToServerTokens.containsValue(token)) {
            for (String key : userToServerTokens.keySet()) {
                if (userToServerTokens.get(key).equals(token)) {
                    userToServerTokens.remove(key);
                    try {
                        userToDesktopTokens.remove(key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    /**
     * Get user's uuid from JWT token claims
     * the uuid is the unique identifier of the user, it is also the name of his working folder
     * @param incomingToken the authentication token
     * @return UUID from JWT claims
     */
    public String getUserUuidFromToken(String incomingToken){
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .requireSubject("revenge-html-client")
                    .requireIssuer("www.revenge.com")
                    .setSigningKey(sharedKey)
                    .parseClaimsJws(incomingToken);
        } catch (SignatureException e) {
            return null;
        } catch (MissingClaimException e) {
            // we get here if the required claim is not present
            return null;
        } catch (IncorrectClaimException e) {
            // we get here if the required claim has the wrong value
            return null;
        }

        try {
            Claims claims = jws.getBody();
            /*System.out.println("ID: " + claims.getId());
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issuer: " + claims.getIssuer());
            System.out.println("Expiration: " + claims.getExpiration().toString());
            System.out.println("userId: " + claims.get("userId"));*/
            if (!(claims.getExpiration().getTime() > System.currentTimeMillis())) {
                System.err.println("server token has expired");
                return null;
            } else {
                SignedJWT signedJWT = SignedJWT.parse(incomingToken);
                return signedJWT.getJWTClaimsSet().getClaim("uuid").toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if system is OK, by verifying whether or not system time is after last NTP time
     * @return boolean
     */
    public boolean isClockOk(){
        Date lastNtp = PreferencesManager.getInstance().getLastNtpTime();
        if(lastNtp == null){ // ntp time not updated yet
            return true;
        }else{
            /* we add 30 minutes to user's clock, because sometimes user clock is not accurate
            then we compare it to the last recorded ntp time.
            user's clock must always be greater than last NTP time, if not, it means that user's clock is not correct
            */
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MINUTE, 30);
            Date newPlusHalfAnHour = cal.getTime();
            return newPlusHalfAnHour.after(lastNtp);
        }
    }


    /**
     * This method checks if we must restrict the sft with a license.
     * It checks if current date is after April 5th, 2018, if so it checks for a
     * certain DNS, if exist => restrict soft, else => do nothing
     */
    public void checkIfMustBlockAccess(){
        try {
            Date lastNtp = PreferencesManager.getInstance().getLastNtpTime();
            Date now = new Date();

            if (lastNtp != null) {
                Date latestDate;
                if (now.after(lastNtp)) {
                    latestDate = now;
                } else {
                    latestDate = lastNtp;
                }

                // check if last NTP date is after 1 april 2018
                String dateStopString = "05-04-2018";
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                Date dateStop;
                boolean doTest;
                try {
                    dateStop = format.parse(dateStopString);
                    doTest = latestDate.after(dateStop);
                } catch (ParseException e) {
                    doTest = true;
                }

                if (doTest) {
                    boolean mustCheckUserLicense = PreferencesManager.getInstance().mustCheckUserLicense();
                    if (!mustCheckUserLicense) {
                    /* check dns if exist => Restrict software with license
                     else => no restriction
                    */
                        try {
                            // md5(revengestudio12firecom) = b217c203b1ab1710cf357e23fd7c595f
                            InetAddress inetAddress = InetAddress.getByName("www.b217c203b1ab1710cf357e23fd7c595f.com");
                            // DNS exist => restrict software with a license
                            PreferencesManager.getInstance().setMustCheckUserLicense(true);
                        } catch (UnknownHostException e) {
                            // DNS not exist, no problem => ley users use the software without license
                        }

                    }
                }
            }
        }catch(Exception e){
            // do nothing
        }
    }
}