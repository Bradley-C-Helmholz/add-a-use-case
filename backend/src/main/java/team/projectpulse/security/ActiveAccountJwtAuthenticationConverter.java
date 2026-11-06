package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * Converts a verified JWT into the caller's authentication, refusing a token whose account is no longer enabled.
 *
 * <p>The JWT is self-contained and lives two hours, so on its own it would keep a deactivated account working
 * until it expires. Looking the account up by the token's {@code userId} claim on every request closes that
 * window (FR-SEC-active-account, BR-student-lifecycle, BR-instructor-lifecycle) for every role. The
 * {@code DisabledException} is an {@code AccountStatusException}, so the bearer entry point hands it to
 * {@code ExceptionHandlerAdvice}, which answers {@code 401}. A token whose account no longer exists is refused
 * the same way.
 *
 * <p>{@code JwtAuthenticationConverter.convert} is final, so this wraps one instead of subclassing it. It is a
 * class of its own, wired into the resource server explicitly in {@code SecurityConfiguration}, because the
 * resource server's DSL only discovers a converter <em>bean</em> typed exactly {@code JwtAuthenticationConverter}.
 * A bean typed as this wrapper, or as a lambda, is silently ignored and the default converter is used instead,
 * which skips this check and drops the {@code authorities} claim mapping.
 */
@Component
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    private final JwtAuthenticationConverter delegate;


    public ActiveAccountJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;

        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // The token carries the roles in a custom claim called "authorities", space-delimited.
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");

        // The default prefix is "SCOPE_". The roles already carry their own "ROLE_" prefix, so use none.
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        this.delegate = new JwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = readUserId(jwt);
        if (userId == null || !this.userRepository.isEnabled(userId).orElse(false)) {
            throw new DisabledException("User account is disabled.");
        }
        return this.delegate.convert(jwt);
    }

    private static Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) {
            return number.intValue();
        }
        if (claim instanceof String text) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
