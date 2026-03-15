package com.adrianland.fundsorchestrator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Duration;


@Slf4j
@Configuration
public class DynamoDbConfig {

    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;

    @Value("${aws.dynamodb.region:us-east-1}")
    private String region;

    @Value("${aws.dynamodb.access-key:local}")
    private String accessKey;

    @Value("${aws.dynamodb.secret-key:local}")
    private String secretKey;

    /**
     * Tuned Apache HTTP client:
     * <ul>
     *   <li>maxConnections – caps the connection pool</li>
     *   <li>connectionTimeout – how long to wait for TCP handshake</li>
     *   <li>socketTimeout – how long to wait for data on an open socket</li>
     * </ul>
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        var httpClient = ApacheHttpClient.builder()
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(5))
                .socketTimeout(Duration.ofSeconds(30))
                .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                .build();

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .httpClient(httpClient);

        if (endpoint != null && !endpoint.isBlank()) {
            log.info("DynamoDB endpoint overridden → {}", endpoint);
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
