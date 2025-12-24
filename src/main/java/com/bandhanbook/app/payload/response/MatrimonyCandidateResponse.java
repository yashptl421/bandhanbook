package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.constants.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatrimonyCandidateResponse {
    private String id;

    private String userId; // Assuming ObjectId as String

    private Address address;

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

    private List<String> favorites;

    private ProfileStatus status;

    private LocalDateTime createdAt;

    private boolean profileCompleted;

    private LocalDateTime updatedAt;

    private int profileCompletion;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PersonalDetails {

        private String birthTime;

        private Date dob;
        private GenderOptions gender = GenderOptions.MALE;

        private String height;

        private String birthPlace;

        private String bloodGroup;

        private ComplexionOptions complexion;

        private String motherTongue = "Hindi";

        private String nationality = "Indian";

        private String religion = "Hindu";

        private String gotra;

        private String maternalGotra;

        private String caste;

        private ManglikOptions manglik;

        private MaritalStatus maritalStatus;

        private String kuldevi;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Image {
        private String url;
        private String id; // cloudinary unique ID for the image
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
    public static class Address {
        private String address;
        private int country = 101; // India
        private int state = 4039; // Madhya Pradesh
        private int city;
        private String zip;

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
        private boolean isHideEmail = false;
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
        private PartnerPreferences.AgeRange ageRange;
        private PartnerPreferences.HeightRange heightRange;
        private PartnerPreferences.SalaryRange salaryRange;
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
