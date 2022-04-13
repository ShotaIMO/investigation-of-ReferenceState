package net.corda.training.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.training.states.AddressState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * This is the contract code which defines how the [AddressState] behaves.
 */

public class AddressContract implements Contract{
    public static String ADDRESS_CONTRACT_ID="net.corda.training.contracts.AddressContract";

    public interface Commands extends CommandData{
        //Publish command means creating a new Ref.State
        class Publish implements Commands{}

        //Move command means updating Party's address.
        class Move implements  Commands{}
    }

    @Override
    public void verify(@NotNull final LedgerTransaction tx)throws IllegalArgumentException{

        /**
         * The AddressContract can handle two transaction types involving [AddressState]s.
         * - Publish: Issuing a new [AddressState] on the ledger.
         * - Move: Updating AddressState which means transfer to the new location.
         */

        final CommandWithParties<Commands> command=requireSingleCommand(tx.getCommands(), Commands.class);
        final AddressContract.Commands commandData = command.getValue();

        if(commandData.equals(new Commands.Publish())){
            requireThat(require->{
                //add constraints regarding Publish command.
                //1. About InputState
                require.using("No Address InputState should be consumed when publishing Address State",
                        tx.inputsOfType(AddressState.class).isEmpty());

                //2. About OutputState
                final List<AddressState> outList=tx.outputsOfType(AddressState.class);
                require.using("Only one OutputState should be created",
                        outList.size()==1);

                //3. About Sign
                final AddressState state=outList.get(0);
                List<PublicKey> signers=tx.getCommands().get(0).getSigners();
                require.using("The issuer must be the signer",
                        signers.size()==1 &&
                                signers.contains(state.getIssuer().getOwningKey()));

                return null;
            });
        }else if(commandData.equals(new Commands.Move())){
            requireThat(require->{

                List<AddressState> inputState=tx.inputsOfType(AddressState.class);
                List<AddressState> outputState=tx.outputsOfType(AddressState.class);

                //add constraints regarding Move command.
                //1. About InputState and OutputState
                require.using("Move transaction should only consume and create one InputState and One OutputState.",
                        inputState.size()==1 &&
                                outputState.size()==1);

                //2. About Address information
                require.using("Address information should be changed in Move transaction",
                        !(inputState.get(0).getAddress()).equals(outputState.get(0).getAddress()));

                //3. Other fields should not be changed.
                require.using("Only address field should be changed in Move transaction",
                        inputState.get(0).getIssuer().equals(outputState.get(0).getIssuer()) &&
                        inputState.get(0).getLinearId().equals(outputState.get(0).getLinearId()));

                //4. About sign
                require.using("Move transaction should be signed same issuer",
                        inputState.get(0).getIssuer().getOwningKey().equals(outputState.get(0).getIssuer().getOwningKey()));

                return  null;
            });
        }
    }
}
