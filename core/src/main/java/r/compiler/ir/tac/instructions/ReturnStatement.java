package r.compiler.ir.tac.instructions;

import java.util.Collections;
import java.util.Set;

import r.compiler.ir.tac.IRLabel;
import r.compiler.ir.tac.operand.Operand;
import r.compiler.ir.tac.operand.Variable;
import r.lang.Context;

public class ReturnStatement implements Statement {

  private final Operand value;

  public ReturnStatement(Operand value) {
    super();
    this.value = value;
  }
  
  public Operand getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return "return " + value;
  }

  @Override
  public Object interpret(Context context, Object[] temps) {
    return value.retrieveValue(context, temps);
  }
  
  @Override
  public Operand getRHS() {
    return value;
  }
  
  @Override
  public ReturnStatement withRHS(Operand newRHS) {
    return new ReturnStatement(newRHS);
  }

  @Override
  public Iterable<IRLabel> possibleTargets() {
    return Collections.emptySet();
  }

  @Override
  public Set<Variable> variables() {
    return value.variables();
  }
}
