package com.teamtask.security;

import com.teamtask.model.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Set;

/**
 * Builds and signs the JWT (JSON Web Token) we hand out at login.
 *
 * A JWT is three base64 parts joined by dots:  header.payload.signature
 *  - header    : which algorithm signed it
 *  - payload   : the "claims" - who you are (subject), your role (groups),
 *                when it expires (exp), who issued it (iss)
 *  - signature : created with our PRIVATE key. Anyone can READ the payload,
 *                but nobody can CHANGE it without breaking the signature,
 *                which the server checks with the PUBLIC key on every request.
 *
 * This is what makes the auth STATELESS: the server stores no session -
 * everything needed to trust the request travels inside the token itself.
 */
@ApplicationScoped
public class TokenService {

    /** How long a token stays valid. After this the user must log in again. */
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    public String generate(User user) {
        return Jwt.issuer("teamflow")                          // must match mp.jwt.verify.issuer
                .subject(String.valueOf(user.getId()))         // whose token this is
                .upn(user.getEmail())                          // "user principal name"
                .groups(Set.of(user.getRole().name()))         // becomes the role for @RolesAllowed
                .claim("displayName", user.getDisplayName())
                .expiresIn(TOKEN_LIFETIME)
                .sign();                                       // signed with jwt/privateKey.pem
    }
}
