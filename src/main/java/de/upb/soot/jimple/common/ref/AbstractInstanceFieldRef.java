/* Soot - a J*va Optimization Framework
 * Copyright (C) 1999 Patrick Lam
 * Copyright (C) 2004 Ondrej Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package de.upb.soot.jimple.common.ref;

import de.upb.soot.StmtPrinter;
import de.upb.soot.core.SootField;
import de.upb.soot.jimple.PrecedenceTest;
import de.upb.soot.jimple.basic.Value;
import de.upb.soot.jimple.basic.ValueBox;
import de.upb.soot.jimple.common.type.Type;
import de.upb.soot.jimple.visitor.IRefVisitor;
import de.upb.soot.jimple.visitor.IVisitor;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class AbstractInstanceFieldRef implements FieldRef {
  protected SootFieldRef fieldRef;
  final ValueBox baseBox;

  protected AbstractInstanceFieldRef(ValueBox baseBox, SootFieldRef fieldRef) {
    if (fieldRef.isStatic()) {
      throw new RuntimeException("wrong static-ness");
    }
    this.baseBox = baseBox;
    this.fieldRef = fieldRef;
  }

  @Override
  public abstract Object clone();

  @Override
  public String toString() {
    return baseBox.getValue().toString() + "." + fieldRef.getSignature();
  }

  @Override
  public void toString(StmtPrinter up) {
    if (PrecedenceTest.needsBrackets(baseBox, this)) {
      up.literal("(");
    }
    baseBox.toString(up);
    if (PrecedenceTest.needsBrackets(baseBox, this)) {
      up.literal(")");
    }
    up.literal(".");
    up.fieldRef(fieldRef);
  }

  public Value getBase() {
    return baseBox.getValue();
  }

  public ValueBox getBaseBox() {
    return baseBox;
  }

  public void setBase(Value base) {
    baseBox.setValue(base);
  }

  @Override
  public SootFieldRef getFieldRef() {
    return fieldRef;
  }

  @Override
  public void setFieldRef(SootFieldRef fieldRef) {
    this.fieldRef = fieldRef;
  }

  @Override
  public SootField getField() {
    return fieldRef.resolve();
  }

  /**
   * Returns a list useBoxes of type ValueBox.
   */
  public final List<ValueBox> getUseBoxes() {
    List<ValueBox> useBoxes = new ArrayList<ValueBox>();

    useBoxes.addAll(baseBox.getValue().getUseBoxes());
    useBoxes.add(baseBox);

    return useBoxes;
  }

  @Override
  public Type getType() {
    return fieldRef.type();
  }

  @Override
  public void accept(IVisitor sw) {
    ((IRefVisitor) sw).caseInstanceFieldRef(this);
  }

  @Override
  public boolean equivTo(Object o) {
    if (o instanceof AbstractInstanceFieldRef) {
      AbstractInstanceFieldRef fr = (AbstractInstanceFieldRef) o;
      return fr.getField().equals(getField()) && fr.baseBox.getValue().equivTo(baseBox.getValue());
    }
    return false;
  }

  /** Returns a hash code for this object, consistent with structural equality. */
  @Override
  public int equivHashCode() {
    return getField().equivHashCode() * 101 + baseBox.getValue().equivHashCode() + 17;
  }

}
