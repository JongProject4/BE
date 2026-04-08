package com.aikids.care.domain.child.dto;

import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.entity.Child.Gender;
import java.time.LocalDateTime;

public record ChildResponse(
		Long id,
		Long userId,
		String name,
		LocalDateTime birthdate,
		Gender gender,
		Float height,
		Float weight,
		String medicalHistory,
		String allergies
) {
	public static ChildResponse from(Child child) {
		return new ChildResponse(
				child.getId(),
				child.getUser().getId(),
				child.getName(),
				child.getBirthdate(),
				child.getGender(),
				child.getHeight(),
				child.getWeight(),
				child.getMedicalHistory(),
				child.getAllergies()
		);
	}
}
