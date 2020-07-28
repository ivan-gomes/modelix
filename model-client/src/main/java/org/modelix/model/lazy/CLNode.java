package org.modelix.model.lazy;



import org.modelix.model.persistent.CPNode;
import org.modelix.model.persistent.CPElementRef;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import jetbrains.mps.internal.collections.runtime.ISequence;
import jetbrains.mps.internal.collections.runtime.Sequence;
import jetbrains.mps.internal.collections.runtime.ISelector;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import jetbrains.mps.internal.collections.runtime.ITranslator2;

public class CLNode extends CLElement {

  public CLNode(CLTree tree, CPNode data) {
    super(tree, data);
  }

  public CLNode(CLTree tree, long id, String concept, long parentId, String roleInParent, long[] childrenIds, String[] propertyRoles, String[] propertyValues, String[] referenceRoles, CPElementRef[] referenceTargets) {
    this(tree, CPNode.create(id, concept, parentId, roleInParent, childrenIds, propertyRoles, propertyValues, referenceRoles, referenceTargets));
  }

  @Override
  public CPNode getData() {
    return (CPNode) super.getData();
  }

  public IBulkQuery.Value<Iterable<CLNode>> getChildren(IBulkQuery bulkQuery) {
    return tree.resolveElements(getData().getChildrenIds(), bulkQuery).map(new _FunctionTypes._return_P1_E0<ISequence<CLNode>, Iterable<CLNode>>() {
      public ISequence<CLNode> invoke(Iterable<CLNode> elements) {
        return Sequence.fromIterable(elements).select(new ISelector<CLNode, CLNode>() {
          public CLNode select(CLNode it) {
            return (CLNode) it;
          }
        });
      }
    });
  }

  public IBulkQuery.Value<Iterable<CLNode>> getDescendants(final IBulkQuery bulkQuery, boolean includeSelf) {
    if (includeSelf) {
      return getDescendants(bulkQuery, false).map(new _FunctionTypes._return_P1_E0<ISequence<CLNode>, Iterable<CLNode>>() {
        public ISequence<CLNode> invoke(Iterable<CLNode> descendants) {
          return Sequence.fromIterable(Sequence.<CLNode>singleton(CLNode.this)).concat(Sequence.fromIterable(descendants));
        }
      });
    } else {
      return getChildren(bulkQuery).mapBulk(new _FunctionTypes._return_P1_E0<IBulkQuery.Value<Iterable<CLNode>>, Iterable<CLNode>>() {
        public IBulkQuery.Value<Iterable<CLNode>> invoke(Iterable<CLNode> children) {
          IBulkQuery.Value<Iterable<CLNode>> d = bulkQuery.map(children, new _FunctionTypes._return_P1_E0<IBulkQuery.Value<Iterable<CLNode>>, CLNode>() {
            public IBulkQuery.Value<Iterable<CLNode>> invoke(CLNode child) {
              return child.getDescendants(bulkQuery, true);
            }
          }).map(new _FunctionTypes._return_P1_E0<ISequence<CLNode>, List<Iterable<CLNode>>>() {
            public ISequence<CLNode> invoke(List<Iterable<CLNode>> it) {
              return ListSequence.fromList(it).translate(new ITranslator2<Iterable<CLNode>, CLNode>() {
                public Iterable<CLNode> translate(Iterable<CLNode> it) {
                  return it;
                }
              });
            }
          });
          return d;
        }
      });
    }
  }

  public String getConcept() {
    return getData().getConcept();
  }
}
