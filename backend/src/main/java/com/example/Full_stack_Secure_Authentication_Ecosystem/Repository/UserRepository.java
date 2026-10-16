package com.example.Full_stack_Secure_Authentication_Ecosystem.Repository;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

}
