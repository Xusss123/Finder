package karm.van.model;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "card_index")
public class CardDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String title;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String text;

    @Field(type = FieldType.Date)
    private LocalDate createTime;
}
