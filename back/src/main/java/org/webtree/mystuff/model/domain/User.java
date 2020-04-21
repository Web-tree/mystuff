package org.webtree.mystuff.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;

@NodeEntity
public class User implements UserDetails {
    private static final long serialVersionUID = -1640162851205761563L;
    @GraphId
    private Long id = null;
    private String username;
    @JsonIgnore
    private String password;
    private Date lastPasswordResetDate;

    public User enable() {
        return this;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Date getLastPasswordResetDate() {
        return lastPasswordResetDate;
    }

    public static final class Builder {
        private Long id = null;
        private String username;
        private String password;
        private Date lastPasswordResetDate;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withLastPasswordResetDate(Date lastPasswordResetDate) {
            this.lastPasswordResetDate = lastPasswordResetDate;
            return this;
        }

        public User build() {
            User user = new User();
            user.username = this.username;
            user.id = this.id;
            user.password = this.password;
            user.lastPasswordResetDate = this.lastPasswordResetDate;
            return user;
        }
    }
}
