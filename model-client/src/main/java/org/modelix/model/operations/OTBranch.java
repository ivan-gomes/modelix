package org.modelix.model.operations;



import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import de.q60.mps.shadowmodels.runtime.model.persistent.IIdGenerator;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import jetbrains.mps.baseLanguage.tuples.runtime.Tuples;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITree;
import jetbrains.mps.baseLanguage.tuples.runtime.MultiTuple;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranchListener;
import de.q60.mps.shadowmodels.runtime.model.persistent.IReadTransaction;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITransaction;
import de.q60.mps.shadowmodels.runtime.model.persistent.IWriteTransaction;

public class OTBranch implements IBranch {

  private IBranch branch;
  private IIdGenerator idGenerator;
  private List<IAppliedOperation> operations = ListSequence.fromList(new ArrayList<IAppliedOperation>());
  private Object operationsLock = new Object();

  public OTBranch(IBranch branch, IIdGenerator idGenerator) {
    this.branch = branch;
    this.idGenerator = idGenerator;
  }

  public void operationApplied(IAppliedOperation op) {
    synchronized (operationsLock) {
      ListSequence.fromList(operations).addElement(op);
    }
  }

  public List<IAppliedOperation> getNewOperations() {
    synchronized (operationsLock) {
      List<IAppliedOperation> result = operations;
      operations = ListSequence.fromList(new ArrayList<IAppliedOperation>());
      return result;
    }
  }

  public Tuples._2<List<IAppliedOperation>, ITree> getOperationsAndTree() {
    synchronized (operationsLock) {
      return MultiTuple.<List<IAppliedOperation>,ITree>from(getNewOperations(), computeRead(new _FunctionTypes._return_P0_E0<ITree>() {
        public ITree invoke() {
          return getTransaction().getTree();
        }
      }));
    }
  }

  @Override
  public void addListener(IBranchListener l) {
    branch.addListener(l);
  }
  @Override
  public void removeListener(IBranchListener l) {
    branch.removeListener(l);
  }
  @Override
  public boolean canRead() {
    return branch.canRead();
  }
  @Override
  public boolean canWrite() {
    return branch.canWrite();
  }
  @Override
  public <T> T computeRead(_FunctionTypes._return_P0_E0<? extends T> computable) {
    checkNotEDT();
    return branch.computeRead(computable);
  }
  @Override
  public <T> T computeWrite(_FunctionTypes._return_P0_E0<? extends T> computable) {
    checkNotEDT();
    return branch.computeWrite(computable);
  }
  @Override
  public IReadTransaction getReadTransaction() {
    return branch.getReadTransaction();
  }
  @Override
  public ITransaction getTransaction() {
    return wrap(branch.getTransaction());
  }
  @Override
  public IWriteTransaction getWriteTransaction() {
    return wrap(branch.getWriteTransaction());
  }
  @Override
  public void runRead(_FunctionTypes._void_P0_E0 runnable) {
    checkNotEDT();
    branch.runRead(runnable);
  }
  @Override
  public void runWrite(_FunctionTypes._void_P0_E0 runnable) {
    checkNotEDT();
    branch.runWrite(runnable);
  }

  public ITransaction wrap(ITransaction t) {
    return (t instanceof IWriteTransaction ? wrap((IWriteTransaction) t) : t);
  }

  public IWriteTransaction wrap(IWriteTransaction t) {
    return new OTWriteTransaction(t, this, idGenerator);
  }

  protected void checkNotEDT() {
  }

}
