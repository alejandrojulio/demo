package co.com.pragma.security;

import co.com.pragma.model.auth.gateways.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderImpl implements PasswordEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public PasswordEncoderImpl() {
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public String encode(String rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        System.out.println("DEBUG - Password check:");
        System.out.println("  Raw password: " + rawPassword);
        System.out.println("  Encoded password: " + encodedPassword);
        boolean matches = bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("  Matches: " + matches);
        return matches;
    }
}
