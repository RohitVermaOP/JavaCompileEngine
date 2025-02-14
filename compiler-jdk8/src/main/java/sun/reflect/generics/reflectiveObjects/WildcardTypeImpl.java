/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.reflect.generics.reflectiveObjects;


import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.visitor.Reifier;


/**
 * Implementation of WildcardType interface for core reflection.
 */
public class WildcardTypeImpl extends LazyReflectiveObjectGenerator
        implements WildcardType {
    // upper bounds - evaluated lazily
    private Type[] upperBounds;
    // lower bounds - evaluated lazily
    private Type[] lowerBounds;
    // The ASTs for the bounds. We are required to evaluate the bounds
    // lazily, so we store these at least until we are first asked
    // for the bounds. This also neatly solves the
    // problem with F-bounds - you can't reify them before the formal
    // is defined.
    private FieldTypeSignature[] upperBoundASTs;
    private FieldTypeSignature[] lowerBoundASTs;

    // constructor is private to enforce access through static factory
    private WildcardTypeImpl(FieldTypeSignature[] ubs,
                             FieldTypeSignature[] lbs,
                             GenericsFactory f) {
        super(f);
        upperBoundASTs = ubs;
        lowerBoundASTs = lbs;
    }

    /**
     * Factory method.
     *
     * @param ubs - an array of ASTs representing the upper bounds for the type
     *            variable to be created
     * @param lbs - an array of ASTs representing the lower bounds for the type
     *            variable to be created
     * @param f   - a factory that can be used to manufacture reflective
     *            objects that represent the bounds of this wildcard type
     * @return a wild card type with the requested bounds and factory
     */
    public static WildcardTypeImpl make(FieldTypeSignature[] ubs,
                                        FieldTypeSignature[] lbs,
                                        GenericsFactory f) {
        return new WildcardTypeImpl(ubs, lbs, f);
    }

    // Accessors

    // accessor for ASTs for upper bounds. Must not be called after upper
    // bounds have been evaluated, because we might throw the ASTs
    // away (but that is not thread-safe, is it?)
    private FieldTypeSignature[] getUpperBoundASTs() {
        // check that upper bounds were not evaluated yet
        assert (upperBounds == null);
        return upperBoundASTs;
    }

    // accessor for ASTs for lower bounds. Must not be called after lower
    // bounds have been evaluated, because we might throw the ASTs
    // away (but that is not thread-safe, is it?)
    private FieldTypeSignature[] getLowerBoundASTs() {
        // check that lower bounds were not evaluated yet
        assert (lowerBounds == null);
        return lowerBoundASTs;
    }

    /**
     * Returns an array of <tt>Type</tt> objects representing the  upper
     * bound(s) of this type variable.  Note that if no upper bound is
     * explicitly declared, the upper bound is <tt>Object</tt>.
     *
     * <p>For each upper bound B :
     * <ul>
     *  <li>if B is a parameterized type or a type variable, it is created,
     *  (see  for the details of the creation
     *  process for parameterized types).
     *  <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the upper bound(s) of this
     * type variable
     * @throws <tt>TypeNotPresentException</tt>             if any of the
     *                                                      bounds refers to a non-existent type declaration
     * @throws <tt>MalformedParameterizedTypeException</tt> if any of the
     *                                                      bounds refer to a parameterized type that cannot be instantiated
     *                                                      for any reason
     */
    public Type[] getUpperBounds() {
        // lazily initialize bounds if necessary
        if (upperBounds == null) {
            FieldTypeSignature[] fts = getUpperBoundASTs(); // get AST

            // allocate result array; note that
            // keeping ts and bounds separate helps with threads
            Type[] ts = new Type[fts.length];
            // iterate over bound trees, reifying each in turn
            for (int j = 0; j < fts.length; j++) {
                Reifier r = getReifier();
                fts[j].accept(r);
                ts[j] = r.getResult();
            }
            // cache result
            upperBounds = ts;
            // could throw away upper bound ASTs here; thread safety?
        }
        return upperBounds.clone(); // return cached bounds
    }

    /**
     * Returns an array of <tt>Type</tt> objects representing the
     * lower bound(s) of this type variable.  Note that if no lower bound is
     * explicitly declared, the lower bound is the type of <tt>null</tt>.
     * In this case, a zero length array is returned.
     *
     * <p>For each lower bound B :
     * <ul>
     *   <li>if B is a parameterized type or a type variable, it is created,
     *   (see  for the details of the creation
     *   process for parameterized types).
     *   <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the lower bound(s) of this
     * type variable
     * @throws <tt>TypeNotPresentException</tt>             if any of the
     *                                                      bounds refers to a non-existent type declaration
     * @throws <tt>MalformedParameterizedTypeException</tt> if any of the
     *                                                      bounds refer to a parameterized type that cannot be instantiated
     *                                                      for any reason
     */
    public Type[] getLowerBounds() {
        // lazily initialize bounds if necessary
        if (lowerBounds == null) {
            FieldTypeSignature[] fts = getLowerBoundASTs(); // get AST
            // allocate result array; note that
            // keeping ts and bounds separate helps with threads
            Type[] ts = new Type[fts.length];
            // iterate over bound trees, reifying each in turn
            for (int j = 0; j < fts.length; j++) {
                Reifier r = getReifier();
                fts[j].accept(r);
                ts[j] = r.getResult();
            }
            // cache result
            lowerBounds = ts;
            // could throw away lower bound ASTs here; thread safety?
        }
        return lowerBounds.clone(); // return cached bounds
    }

    public String toString() {
        Type[] lowerBounds = getLowerBounds();
        Type[] bounds = lowerBounds;
        StringBuilder sb = new StringBuilder();

        if (lowerBounds.length > 0)
            sb.append("? super ");
        else {
            Type[] upperBounds = getUpperBounds();
            if (upperBounds.length > 0 && !upperBounds[0].equals(Object.class)) {
                bounds = upperBounds;
                sb.append("? extends ");
            } else
                return "?";
        }

        assert bounds.length > 0;

        boolean first = true;
        for (Type bound : bounds) {
            if (!first)
                sb.append(" & ");

            first = false;
            sb.append(bound.toString());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WildcardType) {
            WildcardType that = (WildcardType) o;
            return
                    Arrays.equals(this.getLowerBounds(),
                            that.getLowerBounds()) &&
                            Arrays.equals(this.getUpperBounds(),
                                    that.getUpperBounds());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        Type[] lowerBounds = getLowerBounds();
        Type[] upperBounds = getUpperBounds();

        return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
    }
}
