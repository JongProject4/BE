package com.aikids.care.domain.child.controller;

import com.aikids.care.domain.child.dto.ChildResponse;
import com.aikids.care.domain.child.dto.CreateChildRequest;
import com.aikids.care.domain.child.dto.PatchChildRequest;
import com.aikids.care.domain.child.service.ChildService;
import com.aikids.care.domain.user.dto.UserActionResponse;
import com.aikids.care.domain.user.model.SocialType;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children")
public class ChildController {

	private final ChildService childService;

	@GetMapping
	public ResponseEntity<List<ChildResponse>> getChildren(@AuthenticationPrincipal OAuth2User oauth2User) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			return ResponseEntity.ok(childService.getChildren(authInfo.socialId(), authInfo.socialType()));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/{childId}")
	public ResponseEntity<ChildResponse> getChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable Long childId
	) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			return ResponseEntity.ok(childService.getChild(authInfo.socialId(), authInfo.socialType(), childId));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PostMapping
	public ResponseEntity<ChildResponse> createChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody CreateChildRequest request
	) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			ChildResponse response = childService.createChild(authInfo.socialId(), authInfo.socialType(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PatchMapping("/{childId}")
	public ResponseEntity<ChildResponse> patchChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable Long childId,
			@RequestBody PatchChildRequest request
	) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			ChildResponse response = childService.patchChild(authInfo.socialId(), authInfo.socialType(), childId, request);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@DeleteMapping("/{childId}")
	public ResponseEntity<UserActionResponse> deleteChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable Long childId
	) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			childService.deleteChild(authInfo.socialId(), authInfo.socialType(), childId);
			return ResponseEntity.ok(UserActionResponse.success("Child profile deleted successfully."));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(UserActionResponse.fail(ex.getMessage()));
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(UserActionResponse.fail(ex.getMessage()));
		}
	}

	private AuthInfo extractAuthInfo(OAuth2User oauth2User) {
		if (oauth2User == null) {
			throw new IllegalArgumentException("Unauthenticated user.");
		}
		Map<String, Object> attributes = oauth2User.getAttributes();
		String socialId = (String) attributes.get("socialId");
		String socialTypeStr = (String) attributes.get("socialType");
		if (socialId == null || socialId.isBlank() || socialTypeStr == null || socialTypeStr.isBlank()) {
			throw new IllegalStateException("OAuth2 attributes are missing social info.");
		}
		return new AuthInfo(socialId, SocialType.valueOf(socialTypeStr));
	}

	private record AuthInfo(String socialId, SocialType socialType) {
	}
}
