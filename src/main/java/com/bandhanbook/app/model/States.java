package com.bandhanbook.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class States {
    private int id;
    private String name;
    private int country_id;
    @JsonIgnore
    private String country_code;
    @JsonIgnore
    private String country_name;
}
