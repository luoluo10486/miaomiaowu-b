package com.personalblog.ragbackend.rag.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DailyQuestionLimitUpdateRequest {
    @NotNull(message = "每日提问上限不能为空")
    @Min(value = 1, message = "每日提问上限必须大于 0")
    private Integer limit;
}
