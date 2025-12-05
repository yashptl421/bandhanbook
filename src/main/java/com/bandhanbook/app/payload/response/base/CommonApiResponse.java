package com.bandhanbook.app.payload.response.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private String error;
    private int totalRecords;

    public CommonApiResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }
}
