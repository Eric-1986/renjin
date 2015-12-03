package org.renjin.gcc.codegen.pointers;

import org.objectweb.asm.MethodVisitor;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.gimple.type.GimplePointerType;
import org.renjin.gcc.gimple.type.GimpleType;

import static org.objectweb.asm.Opcodes.ICONST_0;


public class AddressOfPrimitiveArray extends AbstractExprGenerator {
  
  private ExprGenerator array;

  public AddressOfPrimitiveArray(ExprGenerator array) {
    this.array = array;
  }

  @Override
  public GimpleType getGimpleType() {
    return new GimplePointerType(array.getGimpleType());
  }


  @Override
  public void emitPushPtrArrayAndOffset(MethodVisitor mv) {
    array.emitPushArray(mv);
    mv.visitInsn(ICONST_0);
  }

  @Override
  public ExprGenerator valueOf() {
    return array;
  }
}