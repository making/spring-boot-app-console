package am.ik.lab.console.cf;

import am.ik.lab.console.security.JwtService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Locale;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
public class CapiController {

    private final JwtService jwtService;

    public CapiController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("info")
    public Object info(UriComponentsBuilder builder) {
        return Collections.singletonMap("token_endpoint", builder.replacePath("").build().toUriString());
    }

    @GetMapping("/v2/apps/{applicationId}/permissions")
    public Object permission(@PathVariable("applicationId") String applicationId, @RequestHeader("Authorization") String authorization) throws Exception {
        String bearerPrefix = "bearer ";
        if (authorization == null || !authorization.toLowerCase(Locale.ENGLISH).startsWith(bearerPrefix)) {
            return ResponseEntity.badRequest().body("Authorization header is missing or invalid");
        }
        final String token = authorization.substring(bearerPrefix.length());
        final SignedJWT jwt = SignedJWT.parse(token);
        if (!this.jwtService.isValid(jwt)) {
            return ResponseEntity.status(FORBIDDEN).body("The JWT is invalid.");
        }
        final JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
        if (!claimsSet.getAudience().contains(applicationId)) {
            return ResponseEntity.status(FORBIDDEN).body(String.format("'%s' is not included in 'audience' claim.", applicationId));
        }
        return Collections.singletonMap("read_sensitive_data", true);
    }
}
