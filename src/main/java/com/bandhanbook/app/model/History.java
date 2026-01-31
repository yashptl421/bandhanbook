package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class History {

    private ObjectId id;

    @Field("batch_id")
    private String batchId;

    @Field("total_amount")
    private double totalAmount;

    @Field("remaining_amount")
    private double remainingAmount;

    @Field("settled_amount")
    private double settledAmount;

    private String remark;

    private SettlementStatus status;

    private LocalDateTime settlementAt = null;

    @Field("created_at")
    private LocalDateTime createdAt;
}