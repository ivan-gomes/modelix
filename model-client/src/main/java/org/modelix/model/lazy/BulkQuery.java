package org.modelix.model.lazy;



import java.util.List;
import jetbrains.mps.baseLanguage.tuples.runtime.Tuples;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.HashMap;
import java.util.Iterator;
import jetbrains.mps.internal.collections.runtime.Sequence;
import jetbrains.mps.baseLanguage.tuples.runtime.MultiTuple;
import jetbrains.mps.internal.collections.runtime.ISelector;
import java.util.Collections;
import jetbrains.mps.baseLanguage.closures.runtime.Wrappers;

/**
 * Not thread safe
 */
public class BulkQuery implements IBulkQuery {

  private IDeserializingKeyValueStore store;
  private List<Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>>> queue = ListSequence.fromList(new ArrayList<Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>>>());
  private boolean processing = false;

  public BulkQuery(IDeserializingKeyValueStore store) {
    this.store = store;
  }

  protected Map<String, Object> executeBulkQuery(Iterable<String> keys, final Map<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>> deserializers) {
    Iterable<Object> values = store.getAll(keys, new _FunctionTypes._return_P2_E0<Object, String, String>() {
      public Object invoke(String key, String serialized) {
        return MapSequence.fromMap(deserializers).get(key).invoke(serialized);
      }
    });
    Map<String, Object> result = MapSequence.fromMap(new HashMap<String, Object>());
    {
      Iterator<String> key_it = Sequence.fromIterable(keys).iterator();
      Iterator<Object> value_it = Sequence.fromIterable(values).iterator();
      String key_var;
      Object value_var;
      while (key_it.hasNext() && value_it.hasNext()) {
        key_var = key_it.next();
        value_var = value_it.next();
        MapSequence.fromMap(result).put(key_var, value_var);
      }
    }
    return result;
  }

  public void query(String key, _FunctionTypes._return_P1_E0<? extends Object, ? super String> deserializer, _FunctionTypes._void_P1_E0<? super Object> callback) {
    ListSequence.fromList(queue).addElement(MultiTuple.<String,_FunctionTypes._return_P1_E0<? extends Object, ? super String>,_FunctionTypes._void_P1_E0<? super Object>>from(key, deserializer, callback));
  }

  public <T> IBulkQuery.Value<T> get(String hash, _FunctionTypes._return_P1_E0<? extends T, ? super String> deserializer) {
    final Value<T> result = new Value<T>();
    query(hash, deserializer, new _FunctionTypes._void_P1_E0<Object>() {
      public void invoke(Object value) {
        result.success((T) value);
      }
    });
    return result;
  }

  public <T> IBulkQuery.Value<T> constant(T value) {
    return new Value<T>(value);
  }

  public void process() {
    if (processing) {
      throw new RuntimeException("Already processing");
    }
    processing = true;
    try {
      while (!(ListSequence.fromList(queue).isEmpty())) {
        final List<Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>>> currentRequests = queue;
        queue = ListSequence.fromList(new ArrayList<Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>>>());

        Map<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>> deserializers = MapSequence.fromMap(new HashMap<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>>());
        for (Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>> request : ListSequence.fromList(currentRequests)) {
          MapSequence.fromMap(deserializers).put(request._0(), request._1());
        }

        final Map<String, Object> entries = executeBulkQuery(ListSequence.fromList(currentRequests).select(new ISelector<Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>>, String>() {
          public String select(Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>> it) {
            return it._0();
          }
        }).distinct(), deserializers);
        for (Tuples._3<String, _FunctionTypes._return_P1_E0<? extends Object, ? super String>, _FunctionTypes._void_P1_E0<? super Object>> request : ListSequence.fromList(currentRequests)) {
          request._2().invoke(MapSequence.fromMap(entries).get(request._0()));
        }
      }
    } finally {
      processing = false;
    }
  }

  public <I, O> IBulkQuery.Value<List<O>> map(Iterable<I> input_, _FunctionTypes._return_P1_E0<? extends IBulkQuery.Value<O>, ? super I> f) {
    List<I> input = Sequence.fromIterable(input_).toListSequence();
    if (ListSequence.fromList(input).isEmpty()) {
      return constant(Collections.<O>emptyList());
    }
    final Object[] output = new Object[ListSequence.fromList(input).count()];
    final boolean[] done = new boolean[ListSequence.fromList(input).count()];
    final Wrappers._int remaining = new Wrappers._int(ListSequence.fromList(input).count());
    final Value<List<O>> result = new Value();
    for (int i_ = 0; i_ < ListSequence.fromList(input).count(); i_++) {
      final int i = i_;
      f.invoke(ListSequence.fromList(input).getElement(i)).onSuccess(new _FunctionTypes._void_P1_E0<O>() {
        public void invoke(O value) {
          if (done[i]) {
            return;
          }
          output[i] = value;
          done[i] = true;
          remaining.value--;
          if (remaining.value == 0) {
            result.success(Sequence.fromIterable(Sequence.fromArray(output)).select(new ISelector<Object, O>() {
              public O select(Object it) {
                return (O) it;
              }
            }).toListSequence());
          }
        }
      });
    }
    return result;
  }

  public class Value<T> implements IBulkQuery.Value<T> {
    private List<_FunctionTypes._void_P1_E0<? super T>> handlers = ListSequence.fromList(new ArrayList<_FunctionTypes._void_P1_E0<? super T>>());
    private T value;
    private boolean done = false;

    public Value() {
    }

    public Value(T value) {
      this.value = value;
      done = true;
    }

    public synchronized void success(T value) {
      if (done) {
        throw new IllegalStateException("Value is already set");
      } else {
        this.value = value;
        this.done = true;
        for (_FunctionTypes._void_P1_E0<? super T> handler : ListSequence.fromList(handlers)) {
          handler.invoke(value);
        }
        handlers = null;
      }
    }

    public synchronized void onSuccess(_FunctionTypes._void_P1_E0<? super T> handler) {
      if (done) {
        handler.invoke(value);
      } else {
        ListSequence.fromList(handlers).addElement(handler);
      }
    }

    public T execute() {
      process();
      if (!(done)) {
        throw new RuntimeException("No value received");
      }
      return value;
    }

    public <R> IBulkQuery.Value<R> map(final _FunctionTypes._return_P1_E0<? extends R, ? super T> handler) {
      final Value<R> result = new Value();
      onSuccess(new _FunctionTypes._void_P1_E0<T>() {
        public void invoke(T v) {
          result.success(handler.invoke(v));
        }
      });
      return result;
    }

    public <R> IBulkQuery.Value<R> mapBulk(final _FunctionTypes._return_P1_E0<? extends IBulkQuery.Value<R>, ? super T> handler) {
      final Value<R> result = new Value();
      onSuccess(new _FunctionTypes._void_P1_E0<T>() {
        public void invoke(T v) {
          handler.invoke(v).onSuccess(new _FunctionTypes._void_P1_E0<R>() {
            public void invoke(R r) {
              result.success(r);
            }
          });
        }
      });
      return result;
    }
  }

}
