package com.circles.circles.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circles.circles.Model.Circles;

public interface CircleRepo extends  JpaRepository<Circles, Long>{
    
    java.util.List<Circles> findByIdIn(java.util.List<Long> ids);
}
