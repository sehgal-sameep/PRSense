package com.codewithsam.prsense.repository;

import com.codewithsam.prsense.entity.RepositoryRegistryEntity;
import com.codewithsam.prsense.entity.RepositoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryRegistryRepository extends JpaRepository<RepositoryRegistryEntity, UUID> {

    Optional<RepositoryRegistryEntity> findByRepositoryId(String repositoryId);

    Optional<RepositoryRegistryEntity> findByProjectNameAndRepositoryName(
            String projectName, String repositoryName);

    boolean existsByRepositoryId(String repositoryId);

    boolean existsByProjectNameAndRepositoryName(String projectName, String repositoryName);

    // Repositories eligible for scheduled sync
    List<RepositoryRegistryEntity> findAllByStatusIn(List<RepositoryStatus> statuses);
}
