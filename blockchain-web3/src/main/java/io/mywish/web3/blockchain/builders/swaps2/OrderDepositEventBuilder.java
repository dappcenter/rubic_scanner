package io.mywish.web3.blockchain.builders.swaps2;

import io.lastwill.eventscan.events.model.contract.swaps2.DepositEvent;
import io.mywish.web3.blockchain.builders.Web3ContractEventBuilder;
import io.mywish.web3.blockchain.model.Web3ContractEventDefinition;
import io.mywish.web3.blockchain.model.WrapperType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Bytes32;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Getter
@Component
@NoArgsConstructor
public class OrderDepositEventBuilder extends Web3ContractEventBuilder<DepositEvent> {

    private final Web3ContractEventDefinition definition = new Web3ContractEventDefinition(
            "Deposit",
            Arrays.asList(
                    WrapperType.create(Bytes32.class, false),
                    WrapperType.create(Address.class, false),
                    WrapperType.create(Address.class, false),
                    WrapperType.create(Uint.class, false),
                    WrapperType.create(Uint.class, false)
            )
    );

    @Override
    public DepositEvent build(String address, List<Object> values) {
        return new DepositEvent(
                definition,
                "0x" + TypeEncoder.encode(new Bytes32((byte[]) values.get(0))),
                (String) values.get(1),
                (String) values.get(2),
                (BigInteger) values.get(4),
                (BigInteger) values.get(5),
                address
                );
    }
}
