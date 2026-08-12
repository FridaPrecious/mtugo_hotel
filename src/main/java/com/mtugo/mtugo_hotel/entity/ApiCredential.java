package com.mtugo.mtugo_hotel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_key", nullable = false, length = 255)
    private String consumerKey;

    @Column(name = "consumer_secret", nullable = false, length = 255)
    private String consumerSecret;

    @Column(nullable = false, length = 255)
    private String passkey;

    @Column(nullable = false, length = 50)
    private String shortcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Environment environment = Environment.SANDBOX;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Environment {
        SANDBOX,
        PRODUCTION
    }
}