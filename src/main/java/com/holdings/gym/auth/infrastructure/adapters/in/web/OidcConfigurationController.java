package com.holdings.gym.auth.infrastructure.adapters.in.web;

import com.holdings.gym.auth.infrastructure.security.RsaKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OidcConfigurationController {

    private final RsaKeyProvider rsaKeyProvider;

    @Value("${app.issuer-uri:http://localhost:8081}")
    private String issuerUri;

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> getOpenIdConfiguration() {
        return Map.of(
                "issuer", issuerUri,
                "jwks_uri", issuerUri + "/oauth2/jwks"
        );
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> getJwks() {
        return Map.of(
                "keys", List.of(rsaKeyProvider.getJwkMap())
        );
    }

}
