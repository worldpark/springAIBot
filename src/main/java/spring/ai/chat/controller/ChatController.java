package spring.ai.chat.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import spring.ai.chat.service.ChatService;

@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatService chatService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAI(
            @RequestParam String message,
            HttpSession httpSession
    ){

        return chatService.chatWithAI(message, httpSession);
    }

    @GetMapping("/image")
    public String createImage(
            @RequestParam String message
    ){

        return chatService.createImage(message);
    }

    @PostMapping("/image-analyze")
    public String imageAnalyze(
            @RequestParam("image") MultipartFile imageFile
    ){
        return chatService.imageAnalyze(imageFile);
    }

    @GetMapping("/text-to-speech")
    public ResponseEntity<byte[]> textToSpeech(
            @RequestParam String message
    ){
        return chatService.textToSpeech(message);
    }

    @GetMapping(value = "/rag-bot", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ragBot(
            @RequestParam String message
    ){

        return chatService.ragBot(message);
    }

    @GetMapping("/front-test")
    public String frontTest(
            @RequestParam String message
    ){
        return message + ", aiChat";
    }
}
