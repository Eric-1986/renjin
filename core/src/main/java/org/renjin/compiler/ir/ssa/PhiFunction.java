package org.renjin.compiler.ir.ssa;

import java.util.List;

import com.google.common.base.Preconditions;
import org.objectweb.asm.MethodVisitor;
import org.renjin.compiler.emit.EmitContext;
import org.renjin.compiler.ir.tac.expressions.Expression;
import org.renjin.compiler.ir.tac.expressions.Variable;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class PhiFunction implements Expression {

  private List<Variable> arguments;
  private Class type;

  public PhiFunction(Variable variable, int count) {
    if(count < 2) {
      throw new IllegalArgumentException("variable=" + variable + ", count=" + count + " (count must be >= 2)");
    }
    this.arguments = Lists.newArrayList();
    for(int i=0;i!=count;++i) {
      arguments.add(variable);
    }
  }

  public PhiFunction(List<Variable> arguments) {
    this.arguments = arguments;
  }

  public List<Variable> getArguments() {
    return arguments;
  }

  @Override
  public String toString() {
    return "\u03A6(" + Joiner.on(", ").join(arguments) + ")";
  }

  @Override
  public boolean isDefinitelyPure() {
    return false; // not sure... have to think about this
  }

  @Override
  public void emitPush(EmitContext emitContext, MethodVisitor mv) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class getType() {
    Preconditions.checkNotNull(type, "type is not resolved yet");
    return type;
  }

  @Override
  public void resolveType() {
    Class type = arguments.get(0).getType();
    for(Expression argument : arguments) {
      if(!argument.getType().equals(type)) {
        throw new UnsupportedOperationException("polymorphic types not supported here");
      }
    }
    this.type = type;
  }

  @Override
  public boolean isTypeResolved() {
    return type != null;
  }

  public void setVersionNumber(int argumentIndex, int versionNumber) {
    arguments.set(argumentIndex, new SsaVariable(arguments.get(argumentIndex), versionNumber));
  }

  public Variable getArgument(int j) {
    return arguments.get(j);
  }

  @Override
  public void setChild(int childIndex, Expression child) {
    arguments.set(childIndex, (Variable)child);
  }

  @Override
  public int getChildCount() {
    return arguments.size();
  }

  @Override
  public Expression childAt(int index) {
    return arguments.get(index);
  }
}
