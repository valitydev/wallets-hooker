package dev.vality.wallets.hooker.model;

import dev.vality.fistful.withdrawal.WithdrawalState;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageGenParams {

    private String sourceId;

    private Long eventId;

    private Long parentId;

    private String createdAt;

    private String externalId;

    private WithdrawalState withdrawalState;

}
