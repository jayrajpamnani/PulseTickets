package com.antra.user;
import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
interface UserRepository extends JpaRepository<User,Long> { Optional<User> findByUsername(String username); boolean existsByUsernameOrEmail(String username,String email); }
