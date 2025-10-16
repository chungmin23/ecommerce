package org.shop.apiserver.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정 클래스
 * Outbox 패턴을 위한 Producer/Consumer 설정
 */
@Configuration
@EnableKafka
@EnableScheduling  // OutboxPublisher의 스케줄링을 위해 필요
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Jackson ObjectMapper 설정
     * LocalDateTime 등의 Java 8 시간 타입을 JSON으로 직렬화하기 위해 JavaTimeModule 등록
     */
    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // ============================================
    // Producer Configuration (Outbox 발행용)
    // ============================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 성능 및 신뢰성 최적화
        config.put(ProducerConfig.ACKS_CONFIG, "1");                 // 리더 브로커만 응답 확인
        config.put(ProducerConfig.RETRIES_CONFIG, 3);                // 실패 시 3회 재시도
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);         // 배치 크기 16KB
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);             // 10ms 대기 후 배치 전송
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Snappy 압축

        return new DefaultKafkaProducerFactory<>(
                config,
                new StringSerializer(),
                new JsonSerializer<>(kafkaObjectMapper())
        );
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ============================================
    // Consumer Configuration (이벤트 소비용)
    // ============================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "coupon-service-group");

        // 성능 최적화
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);     // 한 번에 최대 500개 레코드 조회
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);     // 최소 1KB 이상 데이터가 있을 때 가져오기
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);    // 최대 500ms 대기

        // JSON Deserializer 설정
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>(Object.class, kafkaObjectMapper());
        valueDeserializer.addTrustedPackages("org.shop.apiserver.*");  // 역직렬화 허용 패키지

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 동시 처리 스레드 수 (파티션 수에 맞춰 조정)
        return factory;
    }
}
