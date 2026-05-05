package com.aikids.care.domain.user.model;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findBySocialIdAndSocialType(String socialId, SocialType socialType);
}

