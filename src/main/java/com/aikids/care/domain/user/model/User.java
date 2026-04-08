package com.aikids.care.domain.user.model;

import com.aikids.care.domain.child.entity.Child;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "users",
	uniqueConstraints = @UniqueConstraint(columnNames = { "social_id", "social_type" })
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "social_id", nullable = false)
	private String socialId;

	@Enumerated(EnumType.STRING)
	@Column(name = "social_type", nullable = false)
	private SocialType socialType;

	@Column(nullable = false)
	private String name;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "fcm_token")
	private String fcmToken;

	// User(부모) 1명은 Child(아이 프로필) 여러 명을 가질 수 있다.
	@OneToMany(mappedBy = "user")
	private List<Child> children = new ArrayList<>();

	@Builder
	public User(String socialId, SocialType socialType, String name) {
		this.socialId = socialId;
		this.socialType = socialType;
		this.name = name;
	}

	public void updateName(String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		this.name = name;
	}

	public void updateAdditionalInfo(String phoneNumber, String fcmToken) {
		if (phoneNumber != null) {
			String trimmedPhoneNumber = phoneNumber.trim();
			this.phoneNumber = trimmedPhoneNumber.isBlank() ? null : trimmedPhoneNumber;
		}
		if (fcmToken != null) {
			String trimmedFcmToken = fcmToken.trim();
			this.fcmToken = trimmedFcmToken.isBlank() ? null : trimmedFcmToken;
		}
	}

	public void updateFcmToken(String fcmToken) {
		if (fcmToken == null) {
			return;
		}
		String trimmedFcmToken = fcmToken.trim();
		this.fcmToken = trimmedFcmToken.isBlank() ? null : trimmedFcmToken;
	}
}
