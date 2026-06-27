package com.circles.circles.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circles.circles.Model.fileMetadata;

public interface FileRepo extends JpaRepository<fileMetadata,Long> {

}
