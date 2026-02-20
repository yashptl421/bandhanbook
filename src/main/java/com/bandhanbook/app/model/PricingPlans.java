package com.bandhanbook.app.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pricing_plans")
public class PricingPlans {
    @Id
    private ObjectId id;

    private String name;
    private double price;

    @Field("limits")
    private Limits limits;

    private List<String> features;

    @Field("is_active")
    private boolean isActive;

    @Field("billing_cycle")
    private String billingCycle; // MONTHLY, YEARLY

    @Field("registration_period")
    private int registrationPeriod;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    private LocalDateTime deletedAt = null;

    @Getter
    @Setter
    public static class Limits {
        @Field("max_agents")
        private int maxAgents;

        @Field("max_users")
        private int maxUsers;

        @Field("max_banners")
        private int maxBanners;

        @Field("max_advertisements")
        private int maxAdvertisements;
    }
}
