package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.*;
import lombok.*;
import org.bson.types.ObjectId;
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
    private ObjectId id;

    @Field("user_id")
    private ObjectId userId;

    private Address address;

    private ProfileStatus status = ProfileStatus.pending;

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

    @Builder.Default
    private List<ObjectId> favorites = null;

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

        @Builder.Default
        private GenderOptions gender = GenderOptions.MALE;

        private HeightOptions height;

        @Field("birth_place")
        private String birthPlace;

        @Field("blood_group")
        private BloodGroupOptions bloodGroup;

        @Field("complexion")
        private String complexion = ComplexionOptions.FAIR.name();

        @Builder.Default
        @Field("mother_tongue")
        private String motherTongue = "Hindi";

        @Field("nationality")
        @Builder.Default
        private String nationality = "Indian";

        @Builder.Default
        private String religion = "Hindu";

        @Field("gotra")
        private String gotra;

        @Field("maternal_gotra")
        private String maternalGotra;

        private String caste;

        @Builder.Default
        private ManglikOptions manglik = ManglikOptions.NO;

        @Field("marital_status")
        private MaritalStatus maritalStatus = MaritalStatus.SINGLE;

        private String kuldevi;

        @Field("is_blood_donated")
        private boolean bloodDonated;

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
        private FamilyStatus familyStatus = FamilyStatus.MIDDLE_CLASS;

        @Field("family_type")
        private FamilyType familyType = FamilyType.NUCLEAR;

        @Field("family_values")
        private FamilyValues familyValues = FamilyValues.MODERATE;

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
        @Builder.Default
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
        private DietaryHabits dietaryHabits = DietaryHabits.VEGETARIAN;
        private HabitsOptions drinkingHabits = HabitsOptions.NO;
        private HabitsOptions smokingHabits = HabitsOptions.NO;

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
        @Field("is_hide_email")
        private boolean isHideEmail;

        @Builder.Default
        @Field("is_hide_phone")
        private boolean isHidePhone = false;

        @Field("is_hide_profile")
        @Builder.Default
        private boolean isHideProfile = false;

        @Field("is_hide_profile_image")
        @Builder.Default
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
        private String drinkingHabits = HabitsOptions.NO.name();
        private String dietaryHabits = DietaryHabits.VEGETARIAN.name();
        private String smokingHabits = HabitsOptions.NO.name();
        private String manglik = ManglikOptions.NO.name();
        private String maritalStatus = MaritalStatus.SINGLE.name();

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
