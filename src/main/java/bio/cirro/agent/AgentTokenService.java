package bio.cirro.agent;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.inject.Singleton;

/**
 * Service for generating and validating tokens given to the running jobs
 */
@Singleton
public class AgentTokenService {
    private final Algorithm algorithm;
    private final String issuer;

    public AgentTokenService(AgentConfig agentConfig) {
        this.algorithm = Algorithm.HMAC256(agentConfig.getJwtSecret());
        this.issuer = agentConfig.getId();
    }

    /**
     * Generate a token for the given execution ID
     */
    public String generateForExecution(String executionId) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(executionId)
                .sign(algorithm);
    }

    /**
     * Validate the token and return the execution ID
     */
    public void validate(String token, String executionId) {
        token = token.replace("Bearer ", "");
        var resp = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token);
        var executionIdFromToken = resp.getSubject();
        if (!executionId.equals(executionIdFromToken)) {
            throw new IllegalArgumentException("Invalid token");
        }
    }
}

