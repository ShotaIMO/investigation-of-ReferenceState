# investigation-of-ReferenceState

## Introduction
This program is boiled for the following requests.

  ・Whether Reference State can include and verify in the transaction or not.
  
  ・Whether Reference State’s backchain can be confirmed in the transaction or not.
  
In order to investigate these matter, I added sort of process to https://github.com/sbir3japan/corda-dev-traning-sbir3japan.git.

## Note 
**This program focused only on IOU Issue and added the process of adding Reference State.**

**Reference State are not included in Transfer and Settle Flow.**

## Newly added files
### Put under "contracts\src\main\java\net\corda\training\states"
  AddressState.java: State corresponding to Ref.State.
  
### Put under "contracts\src\main\java\net\corda\training\contracts"
  AddressContract.java: Defined the Publish command that issues AddressState and the Move command that updates AddressState, and added restrictions on them.
    
### Put under "workflows\src\main\java\net\corda\training\flow"
  PublishFlow.java: Flow for publishing AddressState.
    
  MoveFlow.java: Flow for updating AddressState.
    
  
## Changes to existing files
### Put under "contracts\src\main\java\net\corda\training\contracts"
  IOUContract.java: Added process to include AddressState.
    
### Put under "workflows\src\main\java\net\corda\training\flow"
  IOUIssueFlow.java: Added constraints regarding AddressState.

## Procedure
  1. Run the nodes.
  2. Run PublishFlow.java.
  3. Run MoveFlow.java      ※You should confirm Transaction Hash.
  4. Run IOUIssueFlow.java  ※You should confirm Transaction Id
  5. Run the following command:

      run internalFindVerifiedTransaction txnId: Transaction Id
  6. Then looking at the item about Reference State, you will find that the transaction contains the latest Ref.State hash.
  
