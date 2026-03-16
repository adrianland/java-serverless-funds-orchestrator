package com.adrianland.fundsorchestrator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Duration;

/**
 * DynamoDB client configuration.
 *
 * Credential strategy:
 *   Local / Docker: DYNAMODB_ENDPOINT is set -> uses static credentials
 *                   (access-key=local, secret-key=local) against DynamoDB Local.
 *   AWS (EC2/ECS):  DYNAMODB_ENDPOINT is empty -> uses DefaultCredentialsProvider
 *                   which automatically picks up the EC2 Instance Profile (IAM Role).
 */
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

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var httpClient = ApacheHttpClient.builder()
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(5))
                .socketTimeout(Duration.ofSeconds(30))
                .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                .build();

        boolean isLocal = endpoint != null && !endpoint.isBlank();

        // Local  -> static credentials against DynamoDB Local
        // AWS    -> DefaultCredentialsProvider picks up EC2 Instance Profile (IAM Role)
        AwsCredentialsProvider credentialsProvider;
        if (isLocal) {
            log.info("event=dynamodb_local endpoint={} credentials=static", endpoint);
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        } else {
            log.info("event=dynamodb_aws region={} credentials=instance-profile", region);
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .httpClient(httpClient);

        if (isLocal) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}