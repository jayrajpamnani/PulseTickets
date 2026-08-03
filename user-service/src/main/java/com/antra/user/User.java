package com.antra.user;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="users") public class User { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(unique=true,nullable=false) String username; @Column(unique=true,nullable=false) String email; @Column(name="password_hash",nullable=false) String passwordHash; @Column(nullable=false) String role="USER"; @Column(name="created_at",nullable=false) Instant createdAt=Instant.now(); protected User(){} User(String u,String e,String p){username=u;email=e;passwordHash=p;} }
