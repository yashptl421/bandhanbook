package com.bandhanbook.app.security.userprinciple;

import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


@Getter
@Setter
public class UserPrinciple implements UserDetails {

    private Users users;
    private RoleNames activeRole;

    public UserPrinciple(Users users, RoleNames activeRole) {
        this.users = users;
        this.activeRole = activeRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + activeRole));
    }

    @Override
    public String getPassword() {
        return users.getPassword();
    }

    @Override
    public String getUsername() {
        return users.getId().toHexString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !users.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return users.getDeletedAt() == null;
    }
}
