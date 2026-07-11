package com.circles.circles.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.circles.circles.DTO.fileDTO;
import com.circles.circles.Model.fileMetadata;

public interface FileRepo extends JpaRepository<fileMetadata,Long> {

    @Query("SELECT f FROM fileMetadata f WHERE f.id = :id AND f.circle_id = :circleId")
    Optional<fileMetadata> findByIdAndCircleId(@Param("id") Long id, @Param("circleId") Long circleId);
    
    @Query("SELECT new com.circles.circles.DTO.fileDTO(f.id, u.username, f.file_name, f.file_type, f.uploaded_at, f.file_size , f.s3_key) " +
           "FROM fileMetadata f JOIN User u ON f.uploaded_by = u.id " +
           "WHERE f.circle_id = :circleId")
    List<fileDTO> getAllDocs(@Param("circleId") Long circleId , Pageable pageable);
}

