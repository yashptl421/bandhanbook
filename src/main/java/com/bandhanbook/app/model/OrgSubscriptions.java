package com.bandhanbook.app.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "orgsubscriptions")
public class OrgSubscriptions {

    @Id
    private String id;

    @Field("org_id")
    private String orgId;

    @Field("plan_id")
    private String planId;

    @Field("registration_period")
    private String registrationPeriod;

    @Field("start_date")
    private String startDate;

    @Field("end_date")
    private String endDate;

    @Field("is_active")
    private boolean active;

    @Field("max_agents")
    private int maxAgents;

    @Field("max_users")
    private int maxUsers;


    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    private LocalDateTime deletedAt = null;

}
