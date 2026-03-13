package com.admitcard.repository;

import com.admitcard.model.AdmitCardSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdmitCardSessionRepository extends JpaRepository<AdmitCardSession, Long> {
}
