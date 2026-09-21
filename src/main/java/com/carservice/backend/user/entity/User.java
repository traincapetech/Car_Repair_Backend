package com.carservice.backend.user.entity;

import com.carservice.backend.user.enums.Permission;
import com.carservice.backend.user.enums.RolePermissions;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.enums.UserStatus;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UserStatus status;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        if (status != null) {
            return status;
        }
        return Boolean.TRUE.equals(isActive) ? UserStatus.ACTIVE : UserStatus.INACTIVE;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
        this.isActive = (status == UserStatus.ACTIVE);
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        this.isActive = active;
        if (Boolean.TRUE.equals(active)) {
            if (this.status == null || this.status == UserStatus.INACTIVE) {
                this.status = UserStatus.ACTIVE;
            }
        } else {
            if (this.status == null || this.status == UserStatus.ACTIVE) {
                this.status = UserStatus.INACTIVE;
            }
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        // SUPER_ADMIN automatically satisfies all ADMIN roles
        if (role == UserRole.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_OPERATIONS_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_FINANCE_ADMIN"));
        }

        // WORKSHOP_OWNER aliases PARTNER and vice versa
        if (role == UserRole.WORKSHOP_OWNER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PARTNER"));
        } else if (role == UserRole.PARTNER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_WORKSHOP_OWNER"));
        }

        // Add granular permission authorities
        for (Permission perm : RolePermissions.getPermissionsForRole(role)) {
            authorities.add(new SimpleGrantedAuthority(perm.name()));
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return (status == null || status == UserStatus.ACTIVE) && Boolean.TRUE.equals(isActive);
    }
}