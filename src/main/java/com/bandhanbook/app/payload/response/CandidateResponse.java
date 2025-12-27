package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Address;
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
        private Image profileImage;

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
        private PartnerPreferences partnerPreferences;

        @JsonProperty("favorites")
        private List<ObjectId> favorites;

        @JsonProperty("status")
        private ProfileStatus status;

        @JsonProperty("bloodDonated")
        private boolean is_blood_donated;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

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

            @JsonProperty("family_type")
            private FamilyType familyType;

            @JsonProperty("family_values")
            private FamilyValues familyValues;

            @JsonProperty("native_place")
            private String nativePlace;

            @JsonProperty("krashi_bhumi")
            private String krashiBhumi;

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

            @JsonProperty("highest_qualification")
            private String highestQualification;
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
            @JsonProperty("sector_type")
            private SectorType sectorType;
            @JsonProperty("company_name")
            private String companyName;
            @JsonProperty("annual_income")
            private String annualIncome;
            private String location;

        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
