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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * This is the flow which handles publishing of new AddressState on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */

public class PublishFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<StateAndRef<AddressState>>{

        ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction");
        ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction."){
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        @NotNull
        private final Party issuer;
        @NotNull
        private final String address;

        public Initiator(Party issuer, String address) {
            this.issuer = issuer;
            this.address = address;
        }

        @Suspendable
        @Override
        public StateAndRef<AddressState> call() throws FlowException {
            //1. Get a reference to the notary service on our network.
            Party notary=getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            //2. Add outputState and Command into TX.
            AddressState state=new AddressState(getServiceHub().getMyInfo().getLegalIdentities().get(0), address);
            Command txCommand=new Command(new AddressContract.Commands.Publish(),getServiceHub().getMyInfo().getLegalIdentities().get(0).getOwningKey());
            TransactionBuilder txBuilder=new TransactionBuilder(notary)
                    .addOutputState(state, AddressContract.ADDRESS_CONTRACT_ID)
                    .addCommand(txCommand);

            //3. Verify and sign.
            txBuilder.verify(getServiceHub());
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction partSignedTx=getServiceHub().signInitialTransaction(txBuilder);

            //4. Finalise the transaction.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(partSignedTx, Collections.emptyList(),FINALISING_TRANSACTION.childProgressTracker()))
                    .getTx().outRefsOfType(AddressState.class).get(0);
        }
    }
}
