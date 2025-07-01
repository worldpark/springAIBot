package spring.ai.chat.dto.response;

import java.util.List;

public record ChatListResponse(
        List<ChatResponse> chatResponses
) {
}
