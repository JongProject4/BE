package com.aikids.care.domain.child.dto;

import com.aikids.care.domain.child.entity.Child.Gender;
import java.time.LocalDateTime;

public record CreateChildRequest(
		String name,
		LocalDateTime birthdate,
		Gender gender,
		Float height,
		Float weight,
		String medicalHistory,
		String allergies
) {
}
