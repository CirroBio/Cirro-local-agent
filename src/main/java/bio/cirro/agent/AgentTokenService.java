package bio.cirro.agent;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for generating and validating tokens given to the running jobs
 */
@Singleton
public class AgentTokenService {
    private final Algorithm algorithm;
    private final String issuer;
    private final Duration jwtExpiration;

    public AgentTokenService(AgentConfig agentConfig) {
        this.algorithm = Algorithm.HMAC256(agentConfig.getJwtSecret());
        this.issuer = agentConfig.getId();
        this.jwtExpiration = Duration.ofDays(agentConfig.getJwtExpiryDays());
    }

    /**
     * Generate a token for the given execution ID
     */
    public String generateForExecution(String executionId) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(executionId)
                .withExpiresAt(Instant.now().plus(jwtExpiration))
                .sign(algorithm);
    }

    /**
     * Validate the token and return the execution ID
     */
    public void validate(String token, String executionId) {
        try {
            token = token.replace("Bearer ", "");
            var resp = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token);
            var executionIdFromToken = resp.getSubject();
            if (!executionId.equals(executionIdFromToken)) {
                throw new SecurityException("Not authorized to access this execution");
            }
        } catch (JWTVerificationException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }
}

