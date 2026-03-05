package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.DonationStatus;
import com.bandhanbook.app.model.constants.DonorType;
import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.PaymentMode;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "donations")
@CompoundIndexes({

        @CompoundIndex(
                name = "org_event_agent_created_idx",
                def = "{'organization_id':1,'event_id':1,'agent_id':1,'deleted_at':1,'created_at':-1}"
        ),

        @CompoundIndex(
                name = "org_status_created_idx",
                def = "{'organization_id':1,'status':1,'created_at':-1}"
        ),

        @CompoundIndex(
                name = "agent_created_idx",
                def = "{'agent_id':1,'created_at':-1}"
        )

})
public class Donations {
    @Id
    private ObjectId id;

    @Field("event_id")
    @Indexed
    private ObjectId eventId;

    @Field("organization_id")
    @Indexed
    private ObjectId organizationId;

    @Field("agent_id")
    @Indexed
    private ObjectId agentId;

    @Field("event_type")
    private EventType eventType;

    @Field("amount")
    private double amount;
    @Field("remark")
    private String remark;
    @Field("donor_type")
    private DonorType donorType;
    @Field("status")
    private DonationStatus status;
    @Field("payment_mode")
    private PaymentMode paymentMode;
    @Field("donor_name")
    private String donorName;
    @Field("phone_number")
    private String phoneNumber;
    private String address;
    private String email;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime deletedAt = null;

}
