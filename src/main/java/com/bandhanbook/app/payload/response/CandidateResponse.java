package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.constants.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateResponse {
    private String id;
    @JsonProperty("phoneNumber")
    private String phone_number;
    @JsonProperty("fullName")
    private String full_name;
    private String email;
    private List<String> role;
    private MatrimonyCandidate matrimony_data;
    private AgentResponse.OrganizationDetails organization_details;
    private Boolean isFavorite;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MatrimonyCandidate {

        @JsonProperty("id")
        private String _id;

        @JsonProperty("userId")
        private String user_id;

        @JsonIgnore
        private Address address;

        @JsonProperty("address")
        private com.bandhanbook.app.model.Address localAddress;

        @JsonProperty("contactDetails")
        private ContactDetails contact_details;

        @JsonProperty("personalDetails")
        private PersonalDetails personal_details;

        @JsonProperty("profileImage")
        private Image profile_image;

        @JsonProperty("images")
        private Image images;

        @JsonProperty("familyDetails")
        private FamilyDetails family_details;

        @JsonProperty("educationDetails")
        private EducationDetails education_details;

        @JsonProperty("occupationDetails")
        private OccupationDetails occupation_details;

        @JsonProperty("lifestyleInterests")
        private LifestyleInterests lifestyle_interests;

        @JsonProperty("privacySettings")
        private PrivacySettings privacy_settings;

        @JsonProperty("partnerPreferences")
        private PartnerPreferences partner_preferences;

        @JsonProperty("favorites")
        private List<ObjectId> favorites;

        @JsonProperty("status")
        private ProfileStatus status;

        @JsonProperty("bloodDonated")
        private boolean blood_donated;

        @JsonProperty("createdAt")
        private LocalDateTime created_at;

        @JsonProperty("profileCompleted")
        private boolean profile_completed;

        @JsonProperty("updatedAt")
        private LocalDateTime updated_at;

        @JsonProperty("eventParticipant")
        private List<EventParticipant> event_participant;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class EventParticipant {
            @JsonProperty("id")
            private String _id;

            @JsonProperty("agentDetails")
            private AgentResponse agent_details;

            @JsonProperty("candidateId")
            private String candidate_id;

            @JsonProperty("eventId")
            private String event_id;

            @JsonProperty("addedBy")
            private String added_by;
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PersonalDetails {

            @JsonProperty("birthTime")
            private String birth_time;

            @JsonProperty("dob")
            private Date date_of_birth;

            private GenderOptions gender = GenderOptions.MALE;

            private String height;

            @JsonProperty("birthPlace")
            private String birth_place;

            @JsonProperty("bloodGroup")
            private BloodGroupOptions blood_group;

            @JsonProperty("complexion")
            private ComplexionOptions complexion;

            @JsonProperty("motherTongue")
            private String mother_tongue = "Hindi";

            @JsonProperty("nationality")
            private String nationality = "Indian";

            @JsonProperty("religion")
            private String religion = "Hindu";

            @JsonProperty("gotra")
            private String gotra;

            @JsonProperty("maternalGotra")
            private String maternal_gotra;

            private String caste;

            private ManglikOptions manglik;

            @JsonProperty("maritalStatus")
            private MaritalStatus marital_status;

            private String kuldevi;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Image {
            private String url;
            private String id; // cloudinary unique ID for the image
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class FamilyDetails {

            @JsonProperty("fatherName")
            private String father_name;

            @JsonProperty("fatherOccupation")
            private String father_occupation;

            @JsonProperty("motherName")
            private String mather_name;

            @JsonProperty("motherOccupation")
            private String mother_occupation;

            @JsonProperty("siblings")
            private String siblings;

            @JsonProperty("familyStatus")
            private FamilyStatus family_status;

            @JsonProperty("familyType")
            private FamilyType family_type;

            @JsonProperty("familyValues")
            private FamilyValues family_values;

            @JsonProperty("nativePlace")
            private String native_place;

            @JsonProperty("krashiBhumi")
            private String krashi_bhumi;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class EducationDetails {

            @JsonProperty("highestQualification")
            private String highest_qualification;
            private String institution;

            // Getters and setters omitted for brevity
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class OccupationDetails {
            private String designation;
            @JsonProperty("sectorType")
            private SectorType sector_type;
            @JsonProperty("companyName")
            private String company_name;
            @JsonProperty("annualIncome")
            private String annual_income;
            private String location;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class LifestyleInterests {
            @JsonProperty("dietaryHabits")
            private DietaryHabits dietary_habits;
            @JsonProperty("drinkingHabits")
            private HabitsOptions drinking_habits;
            @JsonProperty("smokingHabits")
            private HabitsOptions smoking_habits;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PrivacySettings {
            @JsonProperty("isHideEmail")
            private boolean hide_email;
            @JsonProperty("isHidePhone")
            private boolean hide_phone;
            @JsonProperty("isHideProfile")
            private boolean hide_profile;
            @JsonProperty("isHideProfileImage")
            private boolean hide_profile_image;
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PartnerPreferences {
            @JsonProperty("ageRange")
            private AgeRange age_range;
            @JsonProperty("heightRange")
            private HeightRange height_range;
            @JsonProperty("salaryRange")
            private SalaryRange salary_range;
            @JsonProperty("drinkingHabits")
            private String drinking_habits;
            @JsonProperty("dietaryHabits")
            private String dietary_habits;
            @JsonProperty("smokingHabits")
            private String smoking_habits;
            private String manglik;
            @JsonProperty("maritalStatus")
            private String marital_status;

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class AgeRange {
                private int min;
                private int max;

            }

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class HeightRange {
                private String min;
                private String max;

            }

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class SalaryRange {
                private String min;
                private String max;

            }
        }

    }
}
