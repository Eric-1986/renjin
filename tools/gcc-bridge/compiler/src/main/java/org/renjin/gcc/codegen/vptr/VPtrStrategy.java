/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-${year} BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, a copy is available at
 *  https://www.gnu.org/licenses/gpl-2.0.txt
 *
 */

package org.renjin.gcc.codegen.vptr;

import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.array.ArrayTypeStrategy;
import org.renjin.gcc.codegen.condition.ConditionGenerator;
import org.renjin.gcc.codegen.expr.*;
import org.renjin.gcc.codegen.fatptr.FatPtrPair;
import org.renjin.gcc.codegen.fatptr.ValueFunction;
import org.renjin.gcc.codegen.type.*;
import org.renjin.gcc.codegen.type.voidt.VoidPtrExpr;
import org.renjin.gcc.codegen.type.voidt.VoidPtrReturnStrategy;
import org.renjin.gcc.codegen.type.voidt.VoidPtrStrategy;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleOp;
import org.renjin.gcc.gimple.GimpleVarDecl;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.gcc.gimple.type.GimplePointerType;
import org.renjin.gcc.gimple.type.GimpleType;
import org.renjin.gcc.runtime.Ptr;
import org.renjin.repackaged.asm.Type;
import org.renjin.repackaged.guava.base.Optional;

import java.lang.reflect.Field;

/**
 * Implements a C pointer using the {@link org.renjin.gcc.runtime.Pointer} interface.
 */
public class VPtrStrategy implements PointerTypeStrategy {

  private GimpleType baseType;
  private PointerType pointerType;

  public VPtrStrategy(GimpleType baseType) {
    this.baseType = baseType;
    this.pointerType = PointerType.ofType(baseType);
  }

  @Override
  public GExpr malloc(MethodGenerator mv, JExpr sizeInBytes) {

    String mallocDescriptor = Type.getMethodDescriptor(pointerType.alignedImpl(), Type.INT_TYPE);
    JExpr pointer = Expressions.staticMethodCall(pointerType.alignedImpl(), "malloc",
        mallocDescriptor, sizeInBytes);

    return new VPtrExpr(pointer);
  }

  @Override
  public GExpr pointerPlus(MethodGenerator mv, GExpr pointer, JExpr offsetInBytes) {
    return pointer.toVPtrExpr().plus(offsetInBytes);
  }

  @Override
  public GExpr nullPointer() {
    JExpr pointer = Expressions.staticField(pointerType.alignedImpl(), "NULL", pointerType.alignedImpl());

    return new VPtrExpr(pointer);
  }

  @Override
  public ConditionGenerator comparePointers(MethodGenerator mv, GimpleOp op, GExpr x, GExpr y) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public void memoryCopy(MethodGenerator mv, GExpr destination, GExpr source, JExpr length, boolean buffer) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public void memorySet(MethodGenerator mv, GExpr pointer, JExpr byteValue, JExpr length) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public GExpr unmarshallVoidPtrReturnValue(MethodGenerator mv, JExpr voidPointer) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public ParamStrategy getParamStrategy() {
    return new VPtrParamStrategy();
  }

  @Override
  public ReturnStrategy getReturnStrategy() {
    return new VoidPtrReturnStrategy();
  }

  @Override
  public ValueFunction getValueFunction() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public GExpr variable(GimpleVarDecl decl, VarAllocator allocator) {
    if(decl.isAddressable()) {
      GimplePointerType pointerType = this.baseType.pointerTo();
      JLValue unitArray = allocator.reserveUnitArray(decl.getName(), Type.getType(Ptr.class), Optional.<JExpr>absent());
      VPtrValueFunction valueFunction = new VPtrValueFunction(pointerType);
      FatPtrPair address = new FatPtrPair(valueFunction, unitArray, Expressions.constantInt(0));
      return address.valueOf(pointerType);

    } else {
      JLValue ref = allocator.reserve(decl.getName(), Type.getType(Ptr.class));
      return new VPtrExpr(ref);
    }
  }

  @Override
  public GExpr providedGlobalVariable(GimpleVarDecl decl, Field javaField) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public GExpr constructorExpr(ExprFactory exprFactory, MethodGenerator mv, GimpleConstructor value) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public FieldStrategy fieldGenerator(Type className, String fieldName) {
    return new VPtrFieldStrategy(className, fieldName);
  }

  @Override
  public FieldStrategy addressableFieldGenerator(Type className, String fieldName) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public PointerTypeStrategy pointerTo() {
    return new VPtrStrategy(baseType.pointerTo().pointerTo());
  }

  @Override
  public ArrayTypeStrategy arrayOf(GimpleArrayType arrayType) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public GExpr cast(MethodGenerator mv, GExpr value, TypeStrategy typeStrategy) throws UnsupportedCastException {
    if(typeStrategy instanceof VPtrStrategy) {
      return value;

    } else if(typeStrategy instanceof VoidPtrStrategy) {
      return new VoidPtrExpr(Expressions.cast(((VoidPtrExpr) value).unwrap(), Type.getType(Ptr.class)));
    }
    throw new UnsupportedOperationException("TODO: " + typeStrategy.getClass().getSimpleName());
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof VPtrStrategy;
  }

  @Override
  public int hashCode() {
    return 1;
  }
}