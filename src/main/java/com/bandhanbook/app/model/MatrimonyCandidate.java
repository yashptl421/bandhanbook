package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "matrimonyprofiles")
public class MatrimonyCandidate {

    @Id
    private String id;
    private String userId; // Assuming ObjectId as String

    private Address address;

    @Field("contact_details")
    private ContactDetails contactDetails;

    @Field("personal_details")
    private PersonalDetails personalDetails;

    @Field("profileImage")
    private Image profileImage;

    @Field("images")
    private Image images;

    @Field("family_details")
    private FamilyDetails familyDetails;

    @Field("education_details")
    private EducationDetails educationDetails;

    @Field("occupation_details")
    private OccupationDetails occupationDetails;

    @Field("lifestyle_interests")
    private LifestyleInterests lifestyleInterests;

    @Field("privacy_settings")
    private PrivacySettings privacySettings;

    private PartnerPreferences partnerPreferences;

    private List<String> favorites;

    private ProfileStatus status;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("profile_completed")
    private boolean profileCompleted;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PersonalDetails {

        @Field("birth_time")
        private String birthTime;

        @Field("date_of_birth")
        private Date dob;
        private String gender = GenderOptions.MALE.name();

        private String height;

        @Field("birth_place")
        private String birthPlace;

        @Field("blood_group")
        private String bloodGroup;

        @Field("complexion")
        private ComplexionOptions complexion;

        @Field("mother_tongue")
        private String motherTongue = "Hindi";

        @Field("nationality")
        private String nationality = "Indian";

        @Field("religion")
        private String religion = "Hindu";

        @Field("gotra")
        private String gotra;

        @Field("maternal_gotra")
        private String maternalGotra;

        private String caste;

        private ManglikOptions manglik;

        @Field("marital_status")
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

        @Field("father_name")
        private String fatherName;

        @Field("father_occupation")
        private String fatherOccupation;

        @Field("mather_name")
        private String motherName;

        @Field("mother_occupation")
        private String motherOccupation;

        @Field("siblings")
        private String siblings;

        @Field("family_status")
        private FamilyStatus familyStatus;

        @Field("family_type")
        private FamilyType familyType;

        @Field("family_values")
        private FamilyValues familyValues;

        @Field("native_place")
        private String nativePlace;

        @Field("krashi_bhumi")
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

        @Field("highest_qualification")
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
        @Field("sector_type")
        private SectorType sectorType;
        @Field("company_name")
        private String companyName;
        @Field("annual_income")
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
