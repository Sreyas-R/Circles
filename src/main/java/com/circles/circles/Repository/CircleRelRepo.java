package com.circles.circles.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circles.circles.Model.circlesRelation;

public interface CircleRelRepo extends JpaRepository<circlesRelation, Long>{

    Optional<circlesRelation> findByUserIdAndCircleIdAndRole(Long userId , Long circleId , String role);
    
    Optional<circlesRelation> findByUserIdAndCircleId(Long userId , Long circleId);

    java.util.List<circlesRelation> findByUserId(Long userId);
    
}
