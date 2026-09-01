package com.huace.trace.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;

    @JsonCreator
    public PageResult(@JsonProperty("list") List<T> list, @JsonProperty("total") long total) {
        this.list = list;
        this.total = total;
    }
}
