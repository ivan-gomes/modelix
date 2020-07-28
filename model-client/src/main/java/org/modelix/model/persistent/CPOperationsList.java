package org.modelix.model.persistent;



import org.modelix.model.operations.IOperation;
import jetbrains.mps.internal.collections.runtime.IterableUtils;
import jetbrains.mps.internal.collections.runtime.Sequence;
import jetbrains.mps.internal.collections.runtime.ISelector;
import jetbrains.mps.internal.collections.runtime.IWhereFilter;

public class CPOperationsList {
  public final IOperation[] operations;

  public CPOperationsList(IOperation[] operations) {
    this.operations = operations;
  }

  public String serialize() {
    return IterableUtils.join(Sequence.fromIterable(Sequence.fromArray(operations)).select(new ISelector<IOperation, String>() {
      public String select(IOperation it) {
        return OperationSerializer.INSTANCE.serialize(it);
      }
    }), ",");
  }

  public static CPOperationsList deserialize(String input) {
    return new CPOperationsList(Sequence.fromIterable(Sequence.fromArray(input.split(","))).where(new IWhereFilter<String>() {
      public boolean accept(String it) {
        return (it != null && it.length() > 0);
      }
    }).select(new ISelector<String, IOperation>() {
      public IOperation select(String it) {
        return OperationSerializer.INSTANCE.deserialize(it);
      }
    }).toGenericArray(IOperation.class));
  }

  public String getHash() {
    return HashUtil.sha256(this.serialize());
  }

}
