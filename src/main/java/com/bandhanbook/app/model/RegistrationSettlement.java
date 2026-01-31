package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "registration_settlement")
public class RegistrationSettlement {

    @Id
    private ObjectId id;

    @Field("agent_id")
    private ObjectId agentId;

    @Field("event_id")
    private ObjectId eventId;

    @Field("organization_id")
    private ObjectId organizationId;

    @Field("registration_fee")
    private double registrationFee;

    @Field("registrations")
    private int registrations;

    @Field("total_amount")
    private double totalAmount;

    @Field("total_remaining_amount")
    private double totalRemainingAmount;

    @Field("total_settled_amount")
    private double totalSettledAmount;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    private LocalDateTime deletedAt = null;

    private List<History> settlementHistory;
}
