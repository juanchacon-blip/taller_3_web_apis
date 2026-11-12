package co.edu.unibague.auth.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Configura el par de llaves RSA con el que se firman los JWT (RS256).
 * <p>
 * - Si se definen las rutas de las llaves PEM (PKCS#8 la privada, X.509 la pública)
 *   se cargan desde disco (modo Kubernetes: llaves montadas desde un Secret).
 * - Si no, se genera un par efímero al arrancar (modo desarrollo).
 */
@Configuration
public class JwtConfig {

    @Bean
    RSAKey rsaKey(@Value("${app.jwt.private-key-path:}") String privatePath,
                  @Value("${app.jwt.public-key-path:}") String publicPath) throws Exception {
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;

        if (!privatePath.isBlank() && !publicPath.isBlank()) {
            try (InputStream in = Files.newInputStream(Path.of(privatePath))) {
                privateKey = RsaKeyConverters.pkcs8().convert(in);
            }
            try (InputStream in = Files.newInputStream(Path.of(publicPath))) {
                publicKey = RsaKeyConverters.x509().convert(in);
            }
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            publicKey = (RSAPublicKey) pair.getPublic();
            privateKey = (RSAPrivateKey) pair.getPrivate();
        }

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint() // mismo kid en todas las réplicas con las mismas llaves
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }
}
