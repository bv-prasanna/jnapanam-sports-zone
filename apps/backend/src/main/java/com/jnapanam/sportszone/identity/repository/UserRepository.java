package com.jnapanam.sportszone.identity.repository;

import com.jnapanam.sportszone.identity.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jnapanam.sportszone.identity.model.*;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobile(String mobile);
    Optional<User> findByMobile(String mobile);
    @Query("select distinct u from User u left join u.roles r where (:q is null or lower(u.fullName) like lower(concat('%',:q,'%')) or lower(u.email) like lower(concat('%',:q,'%')) or u.mobile like concat('%',:q,'%')) and (:status is null or u.status=:status) and (:role is null or r.name=:role)")
    Page<User> search(@Param("q")String q,@Param("status")UserStatus status,@Param("role")RoleName role,Pageable pageable);
    @Query("select count(distinct u) from User u join u.roles r where r.name=:role")long countByRole(@Param("role")RoleName role);
}
