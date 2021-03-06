package org.renjin.primitives.packaging;

import com.google.common.io.InputSupplier;
import org.renjin.eval.Context;
import org.renjin.sexp.NamedValue;

import java.io.IOException;
import java.io.InputStream;

public class BasePackage extends Package {

  @Override
  public Iterable<NamedValue> loadSymbols(Context context) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputSupplier<InputStream> getResource(String name) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class getClass(String name) {
    throw new UnsupportedOperationException();
  }
}
