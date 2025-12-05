package com.bandhanbook.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class States {
    private String id;
    private String name;
    private String country_id;
    @JsonIgnore
    private String country_code;
    @JsonIgnore
    private String country_name;
}
