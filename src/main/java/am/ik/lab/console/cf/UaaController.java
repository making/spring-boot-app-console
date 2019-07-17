package am.ik.lab.console.cf;

import am.ik.lab.console.security.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@RestController
public class UaaController {

    private final JwtService jwtService;

    public UaaController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("token_keys")
    public Object tokenKeys() {
        return Collections.singletonMap("keys", Collections.singleton(this.jwtService.getKey()));
    }

    @PostMapping("oauth/token")
    public Object token(UriComponentsBuilder builder, @RequestParam("username") String username) {
        final String baseUrl = builder.replacePath("").build().toUriString();
        return Collections.singletonMap("access_token", this.jwtService.generateToken(baseUrl, username).serialize());
    }
}
