package com.aikids.care.domain.child.dto;

import com.aikids.care.domain.child.entity.Child.Gender;
import java.time.LocalDateTime;

public record PatchChildRequest(
		String name,
		LocalDateTime birthdate,
		Gender gender,
		Float height,
		Float weight,
		String medicalHistory,
		String allergies
) {
	public boolean isEmpty() {
		return name == null
				&& birthdate == null
				&& gender == null
				&& height == null
				&& weight == null
				&& medicalHistory == null
				&& allergies == null;
	}
}
