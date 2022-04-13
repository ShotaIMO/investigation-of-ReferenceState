package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.training.contracts.AddressContract;
import net.corda.training.states.AddressState;

import java.util.Collections;

/**
 * This is the flow which handles updating already published AddressState on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */

public class MoveFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<StateAndRef<AddressState>>{
        private final String address;

        public Initiator(String address){
            this.address=address;
        }

        ProgressTracker.Step ADDING_PARTY_TO_LIST = new ProgressTracker.Step("Sanctioning Party: ");
        ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction");
        ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction."){
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };
        ProgressTracker progressTracker = new ProgressTracker(
                ADDING_PARTY_TO_LIST,
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        @Suspendable
        @Override
        public StateAndRef<AddressState> call() throws FlowException{
            //1. Get a reference to the notary service on our network.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //2. Find already published AddressState.
            StateAndRef<AddressState> oldState=getServiceHub().getVaultService().queryBy(AddressState.class).getStates().get(0);
            AddressState oldStateData=oldState.getState().getData();
            String newAddress=address;
            AddressState newAddressState=new AddressState(
                    oldStateData.getIssuer(),
                    newAddress,
                    oldStateData.getLinearId()
            );

            //3. Add inputState, outputState and Command into transaction.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            Command txCommand=new Command(new AddressContract.Commands.Move(),getServiceHub().getMyInfo().getLegalIdentities().get(0).getOwningKey());
            TransactionBuilder txBuilder=new TransactionBuilder(notary)
                    .addInputState(oldState)
                    .addOutputState(newAddressState,AddressContract.ADDRESS_CONTRACT_ID)
                    .addCommand(txCommand);
            progressTracker.setCurrentStep(ADDING_PARTY_TO_LIST);

            //3. Verify and sign
            txBuilder.verify(getServiceHub());
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            //4. Finalise the transaction.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(
                    new FinalityFlow(partSignedTx, Collections.emptyList(), FINALISING_TRANSACTION.childProgressTracker())
            ).getTx().outRefsOfType(AddressState.class).get(0);
        }
    }
}
