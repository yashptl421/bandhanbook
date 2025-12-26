package com.bandhanbook.app.payload.request;

import com.bandhanbook.app.model.constants.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateRequest {
    private MatrimonyCandidate matrimonyData;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MatrimonyCandidate {

        private ContactDetails contactDetails;

        private PersonalDetails personalDetails;

        private Image profileImage;

        private Image images;

        private FamilyDetails familyDetails;

        private EducationDetails educationDetails;

        private OccupationDetails occupationDetails;

        private LifestyleInterests lifestyleInterests;

        private PrivacySettings privacySettings;

        private PartnerPreferences partnerPreferences;

        private List<ObjectId> favorites ;

        private Boolean bloodDonated;

        private Boolean profileCompleted;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class PersonalDetails {

            private HeightOptions height;

            private BloodGroupOptions bloodGroup;

            private ComplexionOptions complexion;

            private MaritalStatus maritalStatus = MaritalStatus.SINGLE;

            private String gotra;

            private String maternalGotra;

            private ManglikOptions manglik = ManglikOptions.NO;

            private String birthTime;

            private String kuldevi;

            private String birthPlace;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Image {
            private String url;
            private String id;
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class FamilyDetails {

            private String fatherName;

            private String fatherOccupation;

            private String motherName;

            private String motherOccupation;

            private String siblings;

            private FamilyStatus familyStatus;

            private FamilyType familyType;

            private FamilyValues familyValues;

            private String nativePlace;

            private String krashiBhumi;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class EducationDetails {

            private String highestQualification;
            private String institution;

            // Getters and setters omitted for brevity
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class OccupationDetails {
            private String designation;
            private SectorType sectorType;
            private String companyName;
            private String annualIncome;
            private String location;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class LifestyleInterests {
            private DietaryHabits dietaryHabits;
            private HabitsOptions drinkingHabits;
            private HabitsOptions smokingHabits;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class ContactDetails {
            private String name;
            private String relation;
            private String mobile;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class PrivacySettings {
            private boolean isHideEmail;

            private boolean isHidePhone = false;

            private boolean isHideProfile = false;

            private boolean isHideProfileImage = false;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class PartnerPreferences {
            private AgeRange ageRange;
            private HeightRange heightRange;
            private SalaryRange salaryRange;
            private String drinkingHabits;
            private String dietaryHabits;
            private String smokingHabits;
            private String manglik;
            private String maritalStatus;

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            public static class AgeRange {
                private int min;
                private int max;

            }

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            public static class HeightRange {
                private String min;
                private String max;

            }

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            public static class SalaryRange {
                private String min;
                private String max;

            }
        }
    }
}
