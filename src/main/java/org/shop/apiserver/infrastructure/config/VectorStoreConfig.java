package org.shop.apiserver.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    /**
     * 임베딩 모델 - 텍스트를 벡터로 변환
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new OpenAiEmbeddingModel(new OpenAiApi(openAiApiKey));
    }

    /**
     * 벡터 저장소 - 간단한 파일 기반 벡터 DB
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {

        File vectorStoreFile = new File("vector-store.json");

        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);

        // 기존 벡터 데이터가 있으면 로드
        if (vectorStoreFile.exists()) {
            vectorStore.load(vectorStoreFile);
        }

        return vectorStore;
    }
}
