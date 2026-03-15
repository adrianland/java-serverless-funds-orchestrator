package com.adrianland.fundsorchestrator.web.dto.response;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Client details including current balance")
public record ClientResponse(
        String clientId,
        String name,
        String email,
        BigDecimal balance,
        NotificationType notificationPreference
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getClientId(), client.getName(),
                client.getEmail(), client.getBalance(), client.getNotificationPreference());
    }
}
