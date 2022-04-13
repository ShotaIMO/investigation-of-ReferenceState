package net.corda.training.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.training.contracts.AddressContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * AddressState equals Reference State.
 * The Address State object, with the following properties:
 * - [issuer] The party who issue Address state.
 * - [address] The issuer's address
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *   the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *   except at issuance/termination.
 */

@BelongsToContract(AddressContract.class)
public class AddressState implements LinearState {

    private static int ID_AddressState=1;
    @NotNull
    private final Party issuer;
    @NotNull
    private final String address;
    @NotNull
    private final UniqueIdentifier linearId;

    //Constructor for already existing Ref.State.
    @ConstructorForDeserialization
    public AddressState(@NotNull Party issuer,
                        @NotNull String address,
                        @NotNull UniqueIdentifier linearId
                        ){
        this.issuer=issuer;
        this.address = address;
        this.linearId=linearId;

    }

    //Constructor for creating new Ref.State.
    public AddressState(@NotNull Party issuer,
                        @NotNull String address
                        ){
        this(issuer,address,new UniqueIdentifier());
    }

    @NotNull
    public UniqueIdentifier getLinearId(){return linearId;}

    @NotNull
    public List<AbstractParty> getParticipants(){return ImmutableList.of(issuer);}

    @NotNull
    public Party getIssuer(){return issuer;}

    @NotNull
    public String getAddress(){return address;}

    public static int getID_AddressState() { return ID_AddressState; }
}

