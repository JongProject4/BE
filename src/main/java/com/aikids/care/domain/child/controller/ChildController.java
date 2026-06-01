package com.aikids.care.domain.child.controller;

import com.aikids.care.domain.chat.dto.ChatListResponse;
import com.aikids.care.domain.chat.service.ChatService;
import com.aikids.care.domain.child.dto.ChildResponse;
import com.aikids.care.domain.child.dto.CreateChildRequest;
import com.aikids.care.domain.child.dto.PatchChildRequest;
import com.aikids.care.domain.child.service.ChildService;
import com.aikids.care.domain.user.dto.UserActionResponse;
import com.aikids.care.global.security.OAuth2Utils;
import com.aikids.care.global.security.OAuth2Utils.AuthInfo;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children")
public class ChildController {

	private final ChildService childService;
	private final ChatService chatService;

	@GetMapping
	public ResponseEntity<List<ChildResponse>> getChildren(@AuthenticationPrincipal OAuth2User oauth2User) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		return ResponseEntity.ok(childService.getChildren(auth.socialId(), auth.socialType()));
	}

	@GetMapping("/{childId}")
	public ResponseEntity<ChildResponse> getChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable Long childId
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		return ResponseEntity.ok(childService.getChild(auth.socialId(), auth.socialType(), childId));
	}

	@PostMapping
	public ResponseEntity<ChildResponse> createChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody CreateChildRequest request
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(childService.createChild(auth.socialId(), auth.socialType(), request));
	}

	@PatchMapping("/{childId}")
	public ResponseEntity<ChildResponse> patchChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable Long childId,
			@RequestBody PatchChildRequest request
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		return ResponseEntity.ok(childService.patchChild(auth.socialId(), auth.socialType(), childId, request));
	}

	@DeleteMapping("/{childId}")
	public ResponseEntity<UserActionResponse> deleteChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable Long childId
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		childService.deleteChild(auth.socialId(), auth.socialType(), childId);
		return ResponseEntity.ok(UserActionResponse.success("Child profile deleted successfully."));
	}

	@GetMapping("/{child_id}/chat")
	public ResponseEntity<List<ChatListResponse>> getChatRoomsByChild(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable("child_id") Long childId) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		// 권한 검증 로직은 getChatList 내부 혹은 ChildService 에서 처리하는 것이 좋으나,
		// 현재는 기존 코드 흐름을 유지합니다.
		return ResponseEntity.ok(chatService.getChatList(childId));
	}

	@GetMapping("/{childId}/chats/exists")
	public ResponseEntity<Boolean> hasChatHistoryForDate(
			@PathVariable Long childId,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		boolean hasHistory = chatService.hasChatHistoryForDate(childId, date);
		return ResponseEntity.ok(hasHistory);
	}
}