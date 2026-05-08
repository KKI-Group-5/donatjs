package id.ac.ui.cs.advprog.donatjs.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final Collection<GrantedAuthority> authorities;

    public CustomOAuth2User(OAuth2User delegate, boolean isAdmin) {
        this.delegate = delegate;
        var delegateAuths = delegate.getAuthorities();
        Set<GrantedAuthority> auths = delegateAuths != null ? new HashSet<>(delegateAuths) : new HashSet<>();
        auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (isAdmin) {
            auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        this.authorities = Collections.unmodifiableSet(auths);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public <A> A getAttribute(String name) {
        return delegate.getAttribute(name);
    }
}
