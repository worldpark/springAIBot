package spring.ai.chat.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ChatService {

    private final ChatClient chatClient;
    private final OpenAiApi openAiApi;

    private final OpenAiImageModel openAiImageModel;

    private final  OpenAiAudioSpeechModel openAiAudioSpeechModel;

    private final VectorStore vectorStore;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            OpenAiApi openAiApi,
            OpenAiImageModel openAiImageModel,
            OpenAiAudioSpeechModel openAiAudioSpeechModel,
            VectorStore vectorStore
    ) {
        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        chatOptions.setModel("gpt-4o-mini");

        this.chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();
        this.openAiApi = openAiApi;
        this.openAiImageModel = openAiImageModel;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
        this.vectorStore = vectorStore;
    }

    public Flux<String> chatWithAI(String message, HttpSession httpSession){

        MessageWindowChatMemory chatMemory = (MessageWindowChatMemory) httpSession.getAttribute("chatMemory");

        if(chatMemory == null){
            chatMemory = MessageWindowChatMemory.builder()
                    .maxMessages(5)
                    .build();

            httpSession.setAttribute("chatMemory", chatMemory);
        }

        Flux<String> content = chatClient.prompt()
                .advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .user(message)
                .stream()
                .content()
                .map(str -> str.replace(" ", "\u00A0"));

        return Flux.concat(
                content,
                Flux.just("__END__")
        );
    }

    public String createImage(String message) {

        String systemText = """
                    You are a professional English translator. 
                    Translate all user messages into natural, fluent English, and respond only with the translation.
                """;

        String translatedMessage = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(systemText))
                .user(message)
                .call()
                .content();

        ImageResponse imageResponse = openAiImageModel
                .call(new ImagePrompt(translatedMessage,
                        OpenAiImageOptions.builder()
                                .model("dall-e-2")
                                .N(1)
                                .height(256)
                                .width(256)
                                .build())
                );

        return imageResponse.getResult().getOutput().getUrl();
    }

    public String imageAnalyze(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!MimeTypeUtils.IMAGE_PNG_VALUE.equals(contentType) &&
                !MimeTypeUtils.IMAGE_JPEG_VALUE.equals(contentType)) {

            throw new IllegalArgumentException("지원되지 않는 이미지 형식입니다.");
        }

        try {
            Media media = new Media(MimeType.valueOf(contentType), imageFile.getResource());

            String systemText = """
                    Explain what's in the image sent by the user, and answer in Korean.
                    Do not answer except information related to the image.
                """;

            return chatClient.prompt()
                    .user(promptUserSpec -> promptUserSpec
                            .text("What is this")
                            .media(media))
                    .system(systemText)
                    .call()
                    .content();


        } catch (Exception e) {
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }

    public ResponseEntity<byte[]> textToSpeech(String message) {

        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                .voice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
                .speed(1.0f)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .build();

        SpeechPrompt speechPrompt = new SpeechPrompt(message, options);

        byte[] response = openAiAudioSpeechModel.call(speechPrompt).getResult().getOutput();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .body(response);
    }

    public Flux<String> ragBot(String message) {

        String prompt = """
                You are an assistant for question-answering tasks.
                Use the following pieces of retrieved context to answer the question.
                If you don't know the answer, just say that you don.t know.
                Answer in Korean.
            
                #Question:
                {message}
            
                #Context :
                {documents}
                """;

        Flux<String> content = chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text(prompt)
                        .param("message", message)
                        .param("documents", findSimilarData(message)))
                .stream()
                .content()
                .map(str -> str.replace(" ", "\u00A0"));

        return Flux.concat(
                content,
                Flux.just("__END__")
        );
    }

    private String findSimilarData(String question) {
        List<Document> documents =
                vectorStore.similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(1)
                        .build());

        return documents
                .stream()
                .map(document -> document.getText())
                .collect(Collectors.joining());
    }
}
