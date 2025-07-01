package spring.ai.chat.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomListResponse(
        UUID roomId,
        String roomTitle,
        LocalDateTime createDate
) {
}
