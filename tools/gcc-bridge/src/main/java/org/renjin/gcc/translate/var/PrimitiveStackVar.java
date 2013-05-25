package org.renjin.gcc.translate.var;

import org.renjin.gcc.gimple.type.GimplePrimitiveType;
import org.renjin.gcc.jimple.Jimple;
import org.renjin.gcc.jimple.JimpleExpr;
import org.renjin.gcc.translate.FunctionContext;
import org.renjin.gcc.translate.PrimitiveAssignment;
import org.renjin.gcc.translate.expr.AbstractImExpr;
import org.renjin.gcc.translate.expr.ImExpr;
import org.renjin.gcc.translate.expr.ImLValue;
import org.renjin.gcc.translate.expr.PrimitiveLValue;
import org.renjin.gcc.translate.type.PrimitiveTypes;

/**
 * Writes jimple instructions to store and retrieve a single primitive numeric
 * value in a local JVM variable, allocated on the stack.
 */
public class PrimitiveStackVar extends AbstractImExpr implements Variable, PrimitiveLValue {

  private FunctionContext context;
  private String jimpleName;
  private GimplePrimitiveType type;

  public PrimitiveStackVar(FunctionContext context, GimplePrimitiveType type, String gimpleName) {
    this.context = context;
    this.type = type;
    this.jimpleName = Jimple.id(gimpleName);

    context.getBuilder().addVarDecl(PrimitiveTypes.get(type), jimpleName);
  }

  @Override
  public String toString() {
    return "stack:" + jimpleName;
  }

  @Override
  public void writePrimitiveAssignment(JimpleExpr expr) {
    context.getBuilder().addStatement(jimpleName + " = " + expr); 
  }

  @Override
  public void writeAssignment(FunctionContext context, ImExpr rhs) {
    PrimitiveAssignment.assign(context, this, rhs);
  }

  @Override
  public JimpleExpr translateToPrimitive(FunctionContext context) {
    return new JimpleExpr(jimpleName);
  }

  @Override
  public GimplePrimitiveType type() {
    return type;
  }
}