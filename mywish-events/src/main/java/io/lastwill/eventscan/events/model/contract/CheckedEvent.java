package io.lastwill.eventscan.events.model.contract;

import io.mywish.wrapper.ContractEvent;
import io.mywish.wrapper.ContractEventDefinition;
import io.mywish.wrapper.WrapperTransactionReceipt;
import lombok.Getter;

@Getter
public class CheckedEvent extends ContractEvent {
    private final boolean isAccident;
    public CheckedEvent(ContractEventDefinition definition, boolean isAccident, String address) {
        super(definition, address);
        this.isAccident = isAccident;
    }
}
