package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.RoleNames;
import lombok.*;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@With
@Accessors(fluent = true)
@Document(collation = "role")
public class Role {

    @Id
    private ObjectId id;

    //@Indexed(unique = true)
    private String name;

}
