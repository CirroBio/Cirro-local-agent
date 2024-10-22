package bio.cirro.agent;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.inject.Singleton;

@Singleton
public class AgentTokenService {
    private final Algorithm algorithm;
    private final String issuer;

    public AgentTokenService(AgentConfig agentConfig) {
        this.algorithm = agentConfig.getJwtSigner();
        this.issuer = agentConfig.getId();
    }

    /**
     * Generate a token for the given session ID
     */
    public String generateForSession(String sessionId) {
        return JWT.create()
                .withIssuer(issuer)
                .withClaim("sub", sessionId)
                .sign(algorithm);
    }

    /**
     * Validate the token and return the session ID
     */
    public String validate(String token) {
        var resp = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token);
        return resp.getClaim("sub").asString();
    }
}

