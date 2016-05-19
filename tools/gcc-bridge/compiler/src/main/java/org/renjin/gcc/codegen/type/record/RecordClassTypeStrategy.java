package org.renjin.gcc.codegen.type.record;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.objectweb.asm.Type;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.RecordClassGenerator;
import org.renjin.gcc.codegen.array.ArrayTypeStrategy;
import org.renjin.gcc.codegen.expr.*;
import org.renjin.gcc.codegen.fatptr.AddressableField;
import org.renjin.gcc.codegen.fatptr.FatPtrExpr;
import org.renjin.gcc.codegen.fatptr.FatPtrStrategy;
import org.renjin.gcc.codegen.type.*;
import org.renjin.gcc.codegen.type.record.unit.RecordUnitPtrStrategy;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleVarDecl;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.expr.GimpleFieldRef;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.gcc.gimple.type.GimpleField;
import org.renjin.gcc.gimple.type.GimpleRecordTypeDef;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for variables and values of type {@code GimpleRecordType} that employs JVM classes
 */
public class RecordClassTypeStrategy extends RecordTypeStrategy<SimpleExpr> {

  private Type jvmType;
  private boolean provided;
  private boolean unitPointer;

  private Map<String, FieldStrategy> nameMap = null;
  private Map<Integer, FieldStrategy> offsetMap = null;

  public RecordClassTypeStrategy(GimpleRecordTypeDef recordTypeDef) {
    super(recordTypeDef);
  }

  public Type getJvmType() {
    if(jvmType == null) {
      throw new IllegalStateException("Type name of record " + getRecordType().getName() + " has not been initialized.");
    }
    return jvmType;
  }

  public void setJvmType(Type jvmType) {
    this.jvmType = jvmType;
  }

  /**
   *
   * @return true if the class backing this record type is already provided by an existing JVM class
   */
  public boolean isProvided() {
    return provided;
  }

  public void setProvided(boolean provided) {
    this.provided = provided;
  }

  public boolean isUnitPointer() {
    return unitPointer;
  }

  public void setUnitPointer(boolean unitPointer) {
    this.unitPointer = unitPointer;
  }

  @Override
  public void linkFields(TypeOracle typeOracle) {
    nameMap = new HashMap<>();
    offsetMap = new HashMap<>();
    for (GimpleField gimpleField : getRecordTypeDef().getFields()) {
      FieldStrategy fieldStrategy = typeOracle.forField(getJvmType(), gimpleField);
      nameMap.put(gimpleField.getName(), fieldStrategy);
      offsetMap.put(gimpleField.getOffset(), fieldStrategy);
    }
  }

  @Override
  public final ParamStrategy getParamStrategy() {
    return new SimpleParamStrategy(jvmType);
  }

  @Override
  public ReturnStrategy getReturnStrategy() {
    return new SimpleReturnStrategy(jvmType);
  }

  @Override
  public SimpleExpr variable(GimpleVarDecl decl, VarAllocator allocator) {

    SimpleLValue instance = allocator.reserve(decl.getName(), jvmType, new RecordConstructor(this));

    if(decl.isAddressable()) {
      if (isUnitPointer()) {
        // If we are using the RecordUnitPtr strategy, then the record value is also it's address
        return new SimpleAddressableExpr(instance, instance);
        
      } else {
        
        SimpleLValue unitArray = allocator.reserveUnitArray(decl.getName(), jvmType, Optional.of((SimpleExpr)instance));
        FatPtrExpr address = new FatPtrExpr(unitArray);
        SimpleExpr value = Expressions.elementAt(address.getArray(), 0);
        return new SimpleAddressableExpr(value, address);      
      }
    
    } else {
      
      return instance;
    }
  }

  @Override
  public FieldStrategy fieldGenerator(Type className, String fieldName) {
    return new RecordFieldStrategy(this, fieldName);
  }

  @Override
  public FieldStrategy addressableFieldGenerator(Type className, String fieldName) {
    return new AddressableField(getJvmType(), fieldName, new RecordClassValueFunction(this));
  }
  
  @Override
  public SimpleExpr constructorExpr(ExprFactory exprFactory, GimpleConstructor value) {
    Map<GimpleFieldRef, Expr> fields = Maps.newHashMap();
    for (GimpleConstructor.Element element : value.getElements()) {
      Expr fieldValue = exprFactory.findGenerator(element.getValue());
      fields.put((GimpleFieldRef) element.getField(), fieldValue);
    }
    return new RecordConstructor(this, fields);
  }


  @Override
  public void writeClassFiles(File outputDirectory) throws IOException {
    if(isProvided()) {
      return;
    }

    RecordClassGenerator classGenerator = new RecordClassGenerator(jvmType, nameMap.values());
    classGenerator.writeClassFile(outputDirectory);
  }


  @Override
  public Expr memberOf(SimpleExpr instance, GimpleFieldRef fieldRef) {
    if(nameMap == null) {
      throw new IllegalStateException("Fields map is not yet initialized.");
    }
    FieldStrategy fieldStrategy = nameMap.get(fieldRef.getName());
    if(fieldStrategy != null) {
      return fieldStrategy.memberExprGenerator(instance);
    }
    
    // Field names are not really taken seriously in Gimple
    // If we can't find a field by name, then try by offset.
    fieldStrategy = offsetMap.get(fieldRef.getOffset());
    
    if(fieldStrategy != null) {
      return fieldStrategy.memberExprGenerator(instance);
    }
    
    throw new InternalCompilerException(
          String.format("No field named '%s' in record type '%s'", fieldRef.getName(), jvmType));
  }

  @Override
  public ArrayTypeStrategy arrayOf(GimpleArrayType arrayType) {
    return new ArrayTypeStrategy(arrayType, new RecordClassValueFunction(this));
  }

  @Override
  public PointerTypeStrategy pointerTo() {
    if(unitPointer) {
      return new RecordUnitPtrStrategy(this);
    } else {
      return new FatPtrStrategy(new RecordClassValueFunction(this));
    }
  }

  public RecordUnitPtrStrategy pointerToUnit() {
    return new RecordUnitPtrStrategy(this);
  }
}