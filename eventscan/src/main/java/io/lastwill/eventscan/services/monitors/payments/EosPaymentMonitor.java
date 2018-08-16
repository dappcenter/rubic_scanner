package io.lastwill.eventscan.services.monitors.payments;

import io.lastwill.eventscan.events.model.UserPaymentEvent;
import io.lastwill.eventscan.events.model.contract.erc223.Erc223TransferEvent;
import io.lastwill.eventscan.model.CryptoCurrency;
import io.lastwill.eventscan.model.NetworkType;
import io.lastwill.eventscan.model.UserProfile;
import io.lastwill.eventscan.repositories.UserProfileRepository;
import io.lastwill.eventscan.services.TransactionProvider;
import io.mywish.scanner.model.NewBlockEvent;
import io.mywish.scanner.services.EventPublisher;
import io.mywish.wrapper.WrapperOutput;
import io.mywish.wrapper.WrapperTransaction;
import io.mywish.wrapper.WrapperTransactionReceipt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class EosPaymentMonitor {
    @Value("${io.lastwill.eventscan.eos.token-contract-action}")
    private String eosTokenAction;
    @Value("${io.lastwill.eventscan.eos.target-address}")
    private String targetAddress;

    @Autowired
    private TransactionProvider transactionProvider;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EventPublisher eventPublisher;

    @EventListener
    public void newBlockEvent(final NewBlockEvent newBlockEvent) {
        if (newBlockEvent.getNetworkType() != NetworkType.EOS_MAINNET) {
            return;
        }

        List<WrapperTransaction> transactions = newBlockEvent.getTransactionsByAddress().get(eosTokenAction);
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        for (WrapperTransaction transaction: transactions) {
            for (WrapperOutput output : transaction.getOutputs()) {
                if (!output.getAddress().equalsIgnoreCase(eosTokenAction)) {
                    continue;
                }

                WrapperTransactionReceipt receipt;
                try {
                    receipt = transactionProvider.getTransactionReceipt(
                            newBlockEvent.getNetworkType(),
                            transaction
                    );
                }
                catch (Exception e) {
                    log.error("Error on getting receipt tx {}.", transaction, e);
                    continue;
                }

                receipt.getLogs()
                        .stream()
                        .filter(event -> event instanceof Erc223TransferEvent)
                        .map(event -> (Erc223TransferEvent) event)
                        .forEach(transferEvent -> {
                            if (!targetAddress.equalsIgnoreCase(transferEvent.getTo())) {
                                return;
                            }

                            String memo = new String(transferEvent.getData(), StandardCharsets.US_ASCII);
                            UserProfile userProfile = userProfileRepository.findByMemo(memo);
                            if (userProfile == null) {
                                String from = transaction.isSingleInput() ? transaction.getSingleInputAddress() : "?unknown";
                                log.warn("Transfer received, but with wrong memo {} from {}.", memo, from);
                                return;
                            }

                            eventPublisher.publish(new UserPaymentEvent(
                                    newBlockEvent.getNetworkType(),
                                    transaction,
                                    transferEvent.getTokens(),
                                    CryptoCurrency.EOS,
                                    receipt.isSuccess(),
                                    userProfile
                            ));
                        });
            }
        }
    }
}