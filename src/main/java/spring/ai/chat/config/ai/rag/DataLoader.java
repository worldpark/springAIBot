package spring.ai.chat.config.ai.rag;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final VectorStore vectorStore;
    private final JdbcClient jdbcClient;

    @Value("classpath:/[보도자료]+2025년+5월+이후+국제금융외환시장+동향_f.pdf")
    private Resource resource;

    @PostConstruct
    public void init(){
        Integer dataCount = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class)
                .single();

        if(dataCount == 0){

            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(resource, config);
            List<Document> documents = pdfDocumentReader.get();

            TokenTextSplitter splitter =
                    new TokenTextSplitter(1000, 400, 10, 5000, true);

            List<Document> spliterDocuments = splitter.apply(documents);

            vectorStore.accept(spliterDocuments);
        }
    }
}
