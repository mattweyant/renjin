package org.renjin.gcc.translate;

import java.util.List;

import org.renjin.gcc.gimple.ins.GimpleAssign;
import org.renjin.gcc.gimple.expr.GimpleExpr;
import org.renjin.gcc.gimple.type.GimpleBooleanType;
import org.renjin.gcc.gimple.type.GimpleIntegerType;
import org.renjin.gcc.gimple.type.GimpleRealType;
import org.renjin.gcc.gimple.type.GimpleType;
import org.renjin.gcc.jimple.JimpleExpr;
import org.renjin.gcc.translate.expr.ImExpr;
import org.renjin.gcc.translate.expr.ImLValue;
import org.renjin.gcc.translate.expr.PrimitiveLValue;

import com.google.common.collect.Lists;

public class AssignmentTranslator {
  private FunctionContext context;

  public AssignmentTranslator(FunctionContext context) {
    this.context = context;
  }

  public void translate(GimpleAssign assign) {
    ImExpr lhs = context.resolveExpr(assign.getLHS());
    List<ImExpr> operands = resolveOps(assign.getOperands());

    switch(assign.getOperator()) {
    case INTEGER_CST:
    case MEM_REF:
    case ADDR_EXPR:
    case REAL_CST:
    case VAR_DECL:
    case FLOAT_EXPR:
    case NOP_EXPR:
    case ARRAY_REF:
    case PAREN_EXPR:
    case COMPONENT_REF:
      assign(lhs, operands.get(0));
      return;
      
    case POINTER_PLUS_EXPR:
      assign(lhs, operands.get(0).pointerPlus(operands.get(1)));
      return;
     
    case EQ_EXPR:
    case NE_EXPR:
    case LE_EXPR:
    case LT_EXPR:
    case GT_EXPR:
    case GE_EXPR:
      assignComparison(lhs, new Comparison(assign.getOperator(), operands.get(0), operands.get(1)));
      break;
      
    case MULT_EXPR:
      assignBinaryOp(lhs, "*", operands);
      break;
      
    case PLUS_EXPR:
      assignBinaryOp(lhs, "+", operands);
      break;

    case MINUS_EXPR:
      assignBinaryOp(lhs, "-", operands);
      break;

    case RDIV_EXPR:
    case TRUNC_DIV_EXPR:
      assignDiv(lhs, operands);
      break;
      
    case TRUNC_MOD_EXPR:
      assignBinaryOp(lhs, "%", operands);
      break;

    case BIT_NOT_EXPR:
      assignBitNot(lhs, operands.get(0));
      break;
      
    case NEGATE_EXPR:
      assignNegated(lhs, operands.get(0));
      break;

    case ABS_EXPR:
      assignAbs(lhs, operands.get(0));
      break;

    case MAX_EXPR:
      assignMax(lhs, operands);
      break;

    case UNORDERED_EXPR:
      assignUnordered(lhs, operands);
      break;

    case TRUTH_NOT_EXPR:
      assignTruthNot(lhs, operands.get(0));
      break;

    
    case TRUTH_OR_EXPR:
      assignTruthOr(lhs, operands);
      break;
      
    default:
      throw new UnsupportedOperationException(assign.getOperator().toString());
    }
  }

  private void assignDiv(ImExpr lhs, List<ImExpr> operands) {
    ImExpr x = operands.get(0);
    ImExpr y = operands.get(1);

    if(!x.type().equals(y.type())) {
      throw new UnsupportedOperationException();
    }

    if(! (x.type() instanceof GimpleRealType || x.type() instanceof GimpleIntegerType) ) {
      throw new UnsupportedOperationException("unsupported type for div " + x.type());
    }
    
    assignBinaryOp(lhs, "/", operands);
  }


  private void assignNegated(ImExpr lhs, ImExpr expr) {
    TypeChecker.assertSameType(lhs, expr);
    
    assignPrimitive(lhs, new JimpleExpr("neg " + expr.translateToPrimitive(context)));
  }

  private void assignBinaryOp(ImExpr lhs, String operator, List<ImExpr> operands) {

    TypeChecker.assertSameType(lhs, operands.get(0), operands.get(1));
    
    JimpleExpr a = operands.get(0).translateToPrimitive(context);
    JimpleExpr b = operands.get(1).translateToPrimitive(context);

    assignPrimitive(lhs, JimpleExpr.binaryInfix(operator, a, b));
  }
  
  private List<ImExpr> resolveOps(List<GimpleExpr> operands) {
    List<ImExpr> exprs = Lists.newArrayList();
    for(GimpleExpr op : operands) {
      exprs.add(context.resolveExpr(op));
    }
    return exprs;
  }

  private void assignComparison(ImExpr lhs, Comparison comparison) {
    assignIfElse(lhs, comparison.toCondition(context), JimpleExpr.integerConstant(1), JimpleExpr.integerConstant(0));
  }
  
  private void assignPrimitive(ImExpr lhs, JimpleExpr jimpleExpr) {
    ((PrimitiveLValue)lhs).writePrimitiveAssignment(jimpleExpr);
  }

  private void assignTruthNot(ImExpr lhs, ImExpr op) {
    JimpleExpr expr = op.translateToPrimitive(context);
    JimpleExpr condition = new JimpleExpr(expr + " == 0");
    assignBoolean(lhs, condition);
  }

  private void assignTruthOr(ImExpr lhs, List<ImExpr> ops) {
    if(! (ops.get(0).type() instanceof GimpleBooleanType &&
          ops.get(1).type() instanceof GimpleBooleanType)) {
      throw new UnsupportedOperationException();
    }



    JimpleExpr a = ops.get(0).translateToPrimitive(context);
    JimpleExpr b = ops.get(1).translateToPrimitive(context);
    
    String checkB = context.newLabel();
    String noneIsTrue = context.newLabel();
    String doneLabel = context.newLabel();


    context.getBuilder().addStatement("if " + a + " == 0 goto " + checkB);
    assignPrimitive(lhs, JimpleExpr.integerConstant(1));
    context.getBuilder().addStatement("goto " + doneLabel);
    
    context.getBuilder().addLabel(checkB);
    context.getBuilder().addStatement("if " + b + " == 0 goto " + noneIsTrue);
    assignPrimitive(lhs, JimpleExpr.integerConstant(1));
    context.getBuilder().addStatement("goto " + doneLabel);

    context.getBuilder().addLabel(noneIsTrue);
    assignPrimitive(lhs, JimpleExpr.integerConstant(0));

    context.getBuilder().addLabel(doneLabel);

  }
  

  private void assignBoolean(ImExpr lhs, JimpleExpr booleanExpr) {
    assignIfElse(lhs, booleanExpr, JimpleExpr.integerConstant(1), JimpleExpr.integerConstant(0));
  }

  private void assignBitNot(ImExpr lhs, ImExpr op) {
    TypeChecker.assertSameType(lhs, op);

    assignPrimitive(lhs, JimpleExpr.binaryInfix("^", op.translateToPrimitive(context), JimpleExpr.integerConstant(-1)));
  }

  private void assignIfElse(ImExpr lhs, JimpleExpr booleanExpr, JimpleExpr ifTrue, JimpleExpr ifFalse) {
    String trueLabel = context.newLabel();
    String doneLabel = context.newLabel();

    context.getBuilder().addStatement("if " + booleanExpr + " goto " + trueLabel);

    assignPrimitive(lhs, ifFalse);
    context.getBuilder().addStatement("goto " + doneLabel);

    context.getBuilder().addLabel(trueLabel);
    assignPrimitive(lhs, ifTrue);
    context.getBuilder().addStatement("goto " + doneLabel);

    context.getBuilder().addLabel(doneLabel);
  }


  private void assignUnordered(ImExpr lhs, List<ImExpr> operands) {
    ImExpr x = operands.get(0);
    ImExpr y = operands.get(1);

    TypeChecker.assertSameType(x, y);

    if(TypeChecker.isDouble(x.type())) {
      //assignPrimitive(lhs, JimpleExpr.integerConstant(0));
      assignPrimitive(lhs, new JimpleExpr(String.format(
              "staticinvoke <org.renjin.gcc.runtime.Builtins: boolean unordered(double, double)>(%s, %s)",
              x.translateToPrimitive(context),
              y.translateToPrimitive(context))));
    } else {
      throw new UnsupportedOperationException();
    }
  }


  private void assignAbs(ImExpr lhs, ImExpr expr) {
    
    TypeChecker.assertSameType(lhs, expr);
    
    assignPrimitive(lhs, new JimpleExpr(String.format("staticinvoke <java.lang.Math: %s>(%s)",
            absMethodForType(expr.type()),
            expr.translateToPrimitive(context))));
    
  }

  private String absMethodForType(GimpleType type) {
    if (type instanceof GimpleRealType) {
      if (((GimpleRealType) type).getPrecision() == 64) {
        return "double abs(double)";
      }
    }
    if (type instanceof GimpleIntegerType) {
      if (((GimpleIntegerType) type).getPrecision() == 32) {
        return "int abs(int)";
      }
    }
    throw new UnsupportedOperationException("abs on type " + type.toString());
  }

  private void assignMax(ImExpr lhs, List<ImExpr> operands) {
    TypeChecker.assertSameType(lhs, operands.get(0), operands.get(1));

    String signature = "{t} max({t}, {t})"
            .replace("{t}", TypeChecker.primitiveJvmTypeName(lhs.type()));
    
    JimpleExpr a = operands.get(0).translateToPrimitive(context);
    JimpleExpr b = operands.get(1).translateToPrimitive(context);

    assignPrimitive(lhs, new JimpleExpr(String.format(
            "staticinvoke <java.lang.Math: %s>(%s, %s)",
            signature, a.toString(), b.toString())));


  }
  
  private void assign(ImExpr lhs, ImExpr rhs) {
    if(lhs instanceof PrimitiveLValue) {
      PrimitiveAssignment.assign(context, lhs, rhs);
    } else if(lhs instanceof ImLValue) {
      ((ImLValue) lhs).writeAssignment(context, rhs);
    } else {
      throw new UnsupportedOperationException("Unsupported assignment of " + rhs.toString() + " to " + lhs.toString());
    }
  }

}