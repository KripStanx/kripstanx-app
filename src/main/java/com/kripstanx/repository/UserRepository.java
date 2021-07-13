package com.kripstanx.repository;

import com.kripstanx.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    String USERS_BY_USERNAME_CACHE = "usersByUsername";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActiveIndicatorIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneByUsername(String username);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesById(Long id);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_USERNAME_CACHE)
    Optional<User> findOneWithAuthoritiesByUsername(String username);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
    Optional<User> findOneWithAuthoritiesByEmail(String email);

    @EntityGraph(attributePaths = "authorities")
    List<User> findAllWithAuthoritiesByJwtTokenNotNull();

    Page<User> findAllByIdNotNullAndActiveIndicatorIsTrue(Pageable pageable);

    @Modifying
    @Query(
        nativeQuery = true,
        value = "UPDATE user set jwt_token2 = NULL, jwt_token2_expires_at = NULL WHERE jwt_token2_expires_at IS NOT NULL AND jwt_token2_expires_at <= :now"
    )
    void clearTemporalJwtTokens(@Param("now") Instant now);
}
