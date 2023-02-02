/*
 * Copyright 2005 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diemlife.com.google.common.geometry;

import com.google.common.annotations.GwtCompatible;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import javax.annotation.CheckReturnValue;

/**
 * S2Cap represents a disc-shaped region defined by a center and radius. Technically this shape is
 * called a "spherical cap" (rather than disc) because it is not planar; the cap represents a
 * portion of the sphere that has been cut off by a plane. The boundary of the cap is the circle
 * defined by the intersection of the sphere and the plane. For containment purposes, the cap is a
 * closed set, i.e. it contains its boundary.
 *
 * <p>For the most part, you can use a spherical cap wherever you would use a disc in planar
 * geometry. The radius of the cap is measured along the surface of the sphere (rather than the
 * straight-line distance through the interior). Thus a cap of radius Pi/2 is a hemisphere, and a
 * cap of radius Pi covers the entire sphere.
 *
 * <p>A cap can also be defined by its center point and height. The height is simply the distance
 * from the center point to the cutoff plane. There is also support for "empty" and "full" caps,
 * which contain no points and all points respectively.
 *
 */
@GwtCompatible(serializable = true)
public final strictfp class S2Cap implements S2Region, Serializable {
  private final S2Point axis;
  private final S1ChordAngle radius;

  // Caps may be constructed from either an axis and a height, or an axis and
  // an angle. To avoid ambiguity, there are no public constructors

  private S2Cap(S2Point axis, S1ChordAngle radius) {
    this.axis = axis;
    this.radius = radius;
    // assert (isValid());
  }

  /**
   * Creates a cap where the radius is expressed as an S1ChordAngle. This constructor is more
   * efficient than {@link #fromAxisAngle(S2Point, S1Angle)}.
   */
  public static S2Cap fromAxisChord(S2Point center, S1ChordAngle radius) {
    return new S2Cap(center, radius);
  }

  /**
   * Create a cap given its axis and the cap height, i.e. the maximum projected distance along the
   * cap axis from the cap center. 'axis' should be a unit-length vector.
   */
  public static S2Cap fromAxisHeight(S2Point axis, double height) {
    // assert (S2.isUnitLength(axis));
    return new S2Cap(axis, S1ChordAngle.fromLength2(2 * height));
  }

  /**
   * Create a cap given its axis and the cap opening angle, i.e. maximum angle between the axis and
   * a point on the cap. 'axis' should be a unit-length vector, and 'angle' should be between 0 and
   * 180 degrees.
   */
  public static S2Cap fromAxisAngle(S2Point axis, S1Angle angle) {
    // The "min" calculation below is necessary to handle S1Angle.INFINITY.
    // assert (S2.isUnitLength(axis));
    return fromAxisChord(
        axis, S1ChordAngle.fromS1Angle(S1Angle.radians(Math.min(angle.radians(), S2.M_PI))));
  }

  /**
   * Create a cap given its axis and its area in steradians. 'axis' should be a unit-length vector,
   * and 'area' should be between 0 and 4 * M_PI.
   */
  public static S2Cap fromAxisArea(S2Point axis, double area) {
    // assert (S2.isUnitLength(axis));
    return new S2Cap(axis, S1ChordAngle.fromLength2(area / S2.M_PI));
  }

  /** Return an empty cap, i.e. a cap that contains no points. */
  public static S2Cap empty() {
    return new S2Cap(S2Point.X_POS, S1ChordAngle.NEGATIVE);
  }

  /** Return a full cap, i.e. a cap that contains all points. */
  public static S2Cap full() {
    return new S2Cap(S2Point.X_POS, S1ChordAngle.STRAIGHT);
  }

  // Accessor methods.
  public S2Point axis() {
    return axis;
  }

  public S1ChordAngle radius() {
    return radius;
  }

  /** Returns the height of the cap, i.e. the distance from the center point to the cutoff plane. */
  public double height() {
    return 0.5 * radius.getLength2();
  }

  public double area() {
    return 2 * S2.M_PI * Math.max(0.0, height());
  }

  /**
   * Returns the cap radius as an S1Angle. Since the cap angle is stored internally as an
   * S1ChordAngle, this method requires a trigonometric operation and may yield a slightly different
   * result than the value passed to {@link #fromAxisAngle(S2Point, S1Angle)}.
   */
  public S1Angle angle() {
    return radius.toAngle();
  }

  /**
   * Returns true if the axis is {@link S2#isUnitLength unit length}, and the angle is less than Pi.
   *
   * <p>Negative angles or heights are valid, and represent empty caps.
   */
  public boolean isValid() {
    return S2.isUnitLength(axis) && radius.getLength2() <= 4;
  }

  /** Return true if the cap is empty, i.e. it contains no points. */
  public boolean isEmpty() {
    return radius.isNegative();
  }

  /** Return true if the cap is full, i.e. it contains all points. */
  public boolean isFull() {
    return S1ChordAngle.STRAIGHT.equals(radius);
  }

  /**
   * Return the complement of the interior of the cap. A cap and its complement have the same
   * boundary but do not share any interior points. The complement operator is not a bijection,
   * since the complement of a singleton cap (containing a single point) is the same as the
   * complement of an empty cap.
   */
  @CheckReturnValue
  public S2Cap complement() {
    // The complement of a full cap is an empty cap, not a singleton.
    // Also make sure that the complement of an empty cap is full.
    if (isFull()) {
      return empty();
    }
    if (isEmpty()) {
      return full();
    }
    return fromAxisChord(S2Point.neg(axis), S1ChordAngle.fromLength2(4 - radius.getLength2()));
  }

  /**
   * Return true if and only if this cap contains the given other cap (in a set containment sense,
   * e.g. every cap contains the empty cap).
   */
  public boolean contains(S2Cap other) {
    if (isFull() || other.isEmpty()) {
      return true;
    } else {
      S1ChordAngle axialDistance = new S1ChordAngle(axis, other.axis);
      return radius.compareTo(S1ChordAngle.add(axialDistance, other.radius)) >= 0;
    }
  }

  /**
   * Return true if and only if the interior of this cap intersects the given other cap. (This
   * relationship is not symmetric, since only the interior of this cap is used.)
   */
  public boolean interiorIntersects(S2Cap other) {
    // Interior(X) intersects Y if and only if Complement(Interior(X))
    // does not contain Y.
    return !complement().contains(other);
  }

  /**
   * Return true if and only if the given point is contained in the interior of the region (i.e. the
   * region excluding its boundary). 'p' should be a unit-length vector.
   */
  public boolean interiorContains(S2Point p) {
    // assert (S2.isUnitLength(p));
    return isFull() || new S1ChordAngle(axis, p).compareTo(radius) < 0;
  }

  /**
   * Increase the cap radius if necessary to include the given point. If the cap is empty the axis
   * is set to the given point, but otherwise it is left unchanged.
   *
   * @param p must be {@link S2#isUnitLength unit length}
   */
  @CheckReturnValue
  public S2Cap addPoint(S2Point p) {
    // assert (S2.isUnitLength(p));
    if (isEmpty()) {
      return new S2Cap(p, S1ChordAngle.ZERO);
    } else {
      // After adding p to this cap, we require that the result contains p. However we don't need to
      // do anything special to achieve this because contains() does exactly the same distance
      // calculation that we do here.
      return new S2Cap(
          axis, S1ChordAngle.fromLength2(Math.max(radius.getLength2(), axis.getDistance2(p))));
    }
  }

  /**
   * Increase the cap radius if necessary to include the given cap. If the current cap is empty, it
   * is set to the given other cap.
   */
  @CheckReturnValue
  public S2Cap addCap(S2Cap other) {
    if (isEmpty()) {
      return other;
    } else if (other.isEmpty()) {
      return this;
    } else {
      // We round up the distance to ensure that the cap is actually contained.
      // TODO(user): Do some error analysis in order to guarantee this.
      S1ChordAngle dist = S1ChordAngle.add(new S1ChordAngle(axis, other.axis), other.radius);
      S1ChordAngle roundedUp = dist.plusError(S2.DBL_EPSILON * dist.getLength2());
      return new S2Cap(axis, S1ChordAngle.max(radius, roundedUp));
    }
  }

  // //////////////////////////////////////////////////////////////////////
  // S2Region interface (see {@code S2Region} for details):
  @Override
  public S2Cap getCapBound() {
    return this;
  }

  @Override
  public S2LatLngRect getRectBound() {
    if (isEmpty()) {
      return S2LatLngRect.empty();
    }
    if (isFull()) {
      return S2LatLngRect.full();
    }

    // Convert the axis to a (lat,lng) pair, and compute the cap angle.
    S2LatLng axisLatLng = new S2LatLng(axis);
    double capAngle = angle().radians();

    boolean allLongitudes = false;
    double[] lat = new double[2];
    double[] lng = new double[2];
    lng[0] = -S2.M_PI;
    lng[1] = S2.M_PI;

    // Check whether cap includes the south pole.
    lat[0] = axisLatLng.lat().radians() - capAngle;
    if (lat[0] <= -S2.M_PI_2) {
      lat[0] = -S2.M_PI_2;
      allLongitudes = true;
    }
    // Check whether cap includes the north pole.
    lat[1] = axisLatLng.lat().radians() + capAngle;
    if (lat[1] >= S2.M_PI_2) {
      lat[1] = S2.M_PI_2;
      allLongitudes = true;
    }
    if (!allLongitudes) {
      // Compute the range of longitudes covered by the cap. We use the law
      // of sines for spherical triangles. Consider the triangle ABC where
      // A is the north pole, B is the center of the cap, and C is the point
      // of tangency between the cap boundary and a line of longitude. Then
      // C is a right angle, and letting a,b,c denote the sides opposite A,B,C,
      // we have sin(a)/sin(A) = sin(c)/sin(C), or sin(A) = sin(a)/sin(c).
      // Here "a" is the cap angle, and "c" is the colatitude (90 degrees
      // minus the latitude). This formula also works for negative latitudes.
      double sinA = S1ChordAngle.sin(radius);
      double sinC = Math.cos(axisLatLng.lat().radians());
      if (sinA <= sinC) {
        double angleA = Math.asin(sinA / sinC);
        lng[0] = Platform.IEEEremainder(axisLatLng.lng().radians() - angleA, 2 * S2.M_PI);
        lng[1] = Platform.IEEEremainder(axisLatLng.lng().radians() + angleA, 2 * S2.M_PI);
      }
    }
    return new S2LatLngRect(new R1Interval(lat[0], lat[1]), new S1Interval(lng[0], lng[1]));
  }

  @Override
  public boolean contains(S2Cell cell) {
    // If the cap does not contain all cell vertices, return false.
    // We check the vertices before taking the Complement() because we can't
    // accurately represent the complement of a very small cap (a height
    // of 2-epsilon is rounded off to 2).
    S2Point[] vertices = new S2Point[4];
    for (int k = 0; k < 4; ++k) {
      vertices[k] = cell.getVertex(k);
      if (!contains(vertices[k])) {
        return false;
      }
    }
    // Otherwise, return true if the complement of the cap does not intersect
    // the cell. (This test is slightly conservative, because technically we
    // want Complement().InteriorIntersects() here.)
    return !complement().intersects(cell, vertices);
  }

  @Override
  public boolean mayIntersect(S2Cell cell) {
    // If the cap contains any cell vertex, return true.
    S2Point[] vertices = new S2Point[4];
    for (int k = 0; k < 4; ++k) {
      vertices[k] = cell.getVertex(k);
      if (contains(vertices[k])) {
        return true;
      }
    }
    return intersects(cell, vertices);
  }

  /**
   * Return true if the cap intersects 'cell', given that the cap vertices have already been
   * checked.
   */
  public boolean intersects(S2Cell cell, S2Point[] vertices) {
    // Return true if this cap intersects any point of 'cell' excluding its
    // vertices (which are assumed to already have been checked).

    // If the cap is a hemisphere or larger, the cell and the complement of the
    // cap are both convex. Therefore since no vertex of the cell is contained,
    // no other interior point of the cell is contained either.
    if (radius.compareTo(S1ChordAngle.RIGHT) >= 0) {
      return false;
    }

    // We need to check for empty caps due to the axis check just below.
    if (isEmpty()) {
      return false;
    }

    // Optimization: return true if the cell contains the cap axis. (This
    // allows half of the edge checks below to be skipped.)
    if (cell.contains(axis)) {
      return true;
    }

    // At this point we know that the cell does not contain the cap axis,
    // and the cap does not contain any cell vertex. The only way that they
    // can intersect is if the cap intersects the interior of some edge.

    double sin2Angle = S1ChordAngle.sin2(radius);
    for (int k = 0; k < 4; ++k) {
      S2Point edge = cell.getEdgeRaw(k);
      double dot = axis.dotProd(edge);
      if (dot > 0) {
        // The axis is in the interior half-space defined by the edge. We don't
        // need to consider these edges, since if the cap intersects this edge
        // then it also intersects the edge on the opposite side of the cell
        // (because we know the axis is not contained with the cell).
        continue;
      }
      // The Norm2() factor is necessary because "edge" is not normalized.
      if (dot * dot > sin2Angle * edge.norm2()) {
        return false; // Entire cap is on the exterior side of this edge.
      }
      // Otherwise, the great circle containing this edge intersects
      // the interior of the cap. We just need to check whether the point
      // of closest approach occurs between the two edge endpoints.
      S2Point dir = S2Point.crossProd(edge, axis);
      if (dir.dotProd(vertices[k]) < 0 && dir.dotProd(vertices[(k + 1) & 3]) > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(S2Point p) {
    // The point 'p' should be a unit-length vector.
    // assert (S2.isUnitLength(p));
    return new S1ChordAngle(axis, p).compareTo(radius) <= 0;
  }

  /** Return true if two caps are identical. */
  @Override
  public boolean equals(Object that) {
    if (that instanceof S2Cap) {
      S2Cap other = (S2Cap) that;
      return (axis.equalsPoint(other.axis) && radius.equals(other.radius))
          || (isEmpty() && other.isEmpty())
          || (isFull() && other.isFull());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    if (isFull()) {
      return 17;
    } else if (isEmpty()) {
      return 37;
    } else {
      return 37 * (17 * 37 + axis.hashCode()) + radius.hashCode();
    }
  }

  // /////////////////////////////////////////////////////////////////////
  // The following static methods are convenience functions for assertions
  // and testing purposes only.

  /**
   * Returns true if the radian angle between axes of this and 'other' is at most 'maxError', and
   * the chord distance radius between this and 'other' is at most 'maxError'.
   */
  boolean approxEquals(S2Cap other, double maxError) {
    double r2 = radius.getLength2();
    double otherR2 = other.radius.getLength2();
    return (S2.approxEquals(axis, other.axis, maxError) && Math.abs(r2 - otherR2) <= maxError)
        || (isEmpty() && otherR2 <= maxError)
        || (other.isEmpty() && r2 <= maxError)
        || (isFull() && otherR2 >= 2 - maxError)
        || (other.isFull() && r2 >= 2 - maxError);
  }

  boolean approxEquals(S2Cap other) {
    return approxEquals(other, 1e-14);
  }

  @Override
  public String toString() {
    return "[Point = " + axis + " Radius = " + radius + "]";
  }

  /** Writes this cap to the given output stream. */
  public void encode(OutputStream os) throws IOException {
    encode(new LittleEndianOutput(os));
  }

  /** Writes this cap to the given little endian output stream. */
  void encode(LittleEndianOutput os) throws IOException {
    axis.encode(os);
    os.writeDouble(radius.getLength2());
  }

  /** Returns a new S2Cap decoded from the given input stream. */
  public static S2Cap decode(InputStream is) throws IOException {
    return decode(new LittleEndianInput(is));
  }

  /** Returns a new S2Cap decoded from the given little endian input stream. */
  static S2Cap decode(LittleEndianInput is) throws IOException {
    S2Point axis = S2Point.decode(is);
    S1ChordAngle chord = S1ChordAngle.fromLength2(is.readDouble());
    return S2Cap.fromAxisChord(axis, chord);
  }
}
