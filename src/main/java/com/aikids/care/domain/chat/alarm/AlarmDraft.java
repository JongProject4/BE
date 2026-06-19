package com.aikids.care.domain.chat.alarm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmDraft {

    private AlarmIntent intent;

    private String medicineName;
    private String dosage;
    private Integer intervalHour;

    private String hospitalName;
    private LocalDateTime visitDate;
    private String memo;

    public static AlarmDraft none() {
        return AlarmDraft.builder().intent(AlarmIntent.NONE).build();
    }

    public boolean isComplete() {
        if (intent == null || intent == AlarmIntent.NONE) return false;
        return missingFields().isEmpty();
    }

    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (intent == AlarmIntent.MEDICATION) {
            if (isBlank(medicineName)) missing.add("medicineName");
            if (isBlank(dosage)) missing.add("dosage");
            if (intervalHour == null) missing.add("intervalHour");
        } else if (intent == AlarmIntent.HOSPITAL) {
            if (isBlank(hospitalName)) missing.add("hospitalName");
            if (visitDate == null) missing.add("visitDate");
        }
        return missing;
    }

    public AlarmDraft mergeWith(AlarmDraft incoming) {
        if (incoming == null) return this;
        AlarmIntent resolvedIntent = (this.intent != null && this.intent != AlarmIntent.NONE)
                ? this.intent : incoming.intent;
        return AlarmDraft.builder()
                .intent(resolvedIntent)
                .medicineName(firstNonBlank(incoming.medicineName, this.medicineName))
                .dosage(firstNonBlank(incoming.dosage, this.dosage))
                .intervalHour(incoming.intervalHour != null ? incoming.intervalHour : this.intervalHour)
                .hospitalName(firstNonBlank(incoming.hospitalName, this.hospitalName))
                .visitDate(incoming.visitDate != null ? incoming.visitDate : this.visitDate)
                .memo(firstNonBlank(incoming.memo, this.memo))
                .build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return !isBlank(preferred) ? preferred : fallback;
    }
}
