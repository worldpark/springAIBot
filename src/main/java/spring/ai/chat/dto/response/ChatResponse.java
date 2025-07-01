package spring.ai.chat.dto.response;

import java.time.LocalDateTime;

public record ChatResponse(
        String content,
        LocalDateTime createDate
) {
}
