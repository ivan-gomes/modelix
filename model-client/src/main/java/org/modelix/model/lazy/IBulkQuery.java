package org.modelix.model.lazy;



import java.util.List;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;

public interface IBulkQuery {
  <I, O> Value<List<O>> map(Iterable<I> input_, _FunctionTypes._return_P1_E0<? extends Value<O>, ? super I> f);
  <T> Value<T> constant(T value);
  <T> Value<T> get(String hash, _FunctionTypes._return_P1_E0<? extends T, ? super String> deserializer);


  interface Value<T> {
    T execute();
    <R> Value<R> mapBulk(final _FunctionTypes._return_P1_E0<? extends Value<R>, ? super T> handler);
    <R> Value<R> map(_FunctionTypes._return_P1_E0<? extends R, ? super T> handler);
    void onSuccess(_FunctionTypes._void_P1_E0<? super T> handler);
  }
}
