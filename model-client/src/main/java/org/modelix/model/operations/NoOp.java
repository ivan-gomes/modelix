package org.modelix.model.operations;



import de.q60.mps.shadowmodels.runtime.model.persistent.IWriteTransaction;

public class NoOp extends AbstractOperation implements IAppliedOperation {


  @Override
  public IAppliedOperation apply(IWriteTransaction transaction) {
    return this;
  }

  @Override
  public IOperation transform(IOperation previous) {
    return null;
  }

  @Override
  public IOperation getOriginalOp() {
    return this;
  }

  @Override
  public IOperation invert() {
    return this;
  }

}
