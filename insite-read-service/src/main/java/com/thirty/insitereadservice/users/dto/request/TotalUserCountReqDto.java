package com.thirty.insitereadservice.users.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalUserCountReqDto {
    @NotNull(message = "시작시간을 기입해 주세요")
    private LocalDateTime startDate;

    @NotNull(message = "끝 시간을 기입해 주세요")
    private LocalDateTime endDate;

    @NotNull(message = "앱 토큰을 기입해 주세요")
    private String applicationToken;
}
