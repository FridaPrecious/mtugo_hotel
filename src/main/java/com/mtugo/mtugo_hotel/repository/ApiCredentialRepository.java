package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.ApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiCredentialRepository extends JpaRepository<ApiCredential, Long> {

    Optional<ApiCredential> findFirstByIsActiveTrue();
}
