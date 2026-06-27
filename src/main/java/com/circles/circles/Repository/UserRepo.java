package com.circles.circles.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.circles.circles.Model.User;


@Repository
public interface UserRepo extends JpaRepository<User , Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);


    boolean existsByEmail(String email);

    boolean existsByUsername(String username);




    

    
}
