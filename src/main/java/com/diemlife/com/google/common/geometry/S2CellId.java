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

import static com.diemlife.com.google.common.geometry.S2Projections.PROJ;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.primitives.UnsignedLongs;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An S2CellId is a 64-bit unsigned integer that uniquely identifies a cell in the S2 cell
 * decomposition. It has the following format:
 *
 * <pre>
 * id = [face][face_pos]
 * </pre>
 *
 * <p>face: a 3-bit number (range 0..5) encoding the cube face.
 *
 * <p>face_pos: a 61-bit number encoding the position of the center of this cell along the Hilbert
 * curve over this face (see the Wiki pages for details).
 *
 * <p>Sequentially increasing cell ids follow a continuous space-filling curve over the entire
 * sphere. They have the following properties:
 *
 * <ul>
 *   <li>The id of a cell at level k consists of a 3-bit face number followed by k bit pairs that
 *       recursively select one of the four children of each cell. The next bit is always 1, and all
 *       other bits are 0. Therefore, the level of a cell is determined by the position of its
 *       lowest-numbered bit that is turned on (for a cell at level k, this position is 2 *
 *       (MAX_LEVEL - k).)
 *   <li>The id of a parent cell is at the midpoint of the range of ids spanned by its children (or
 *       by its descendants at any level).
 * </ul>
 *
 * <p>Leaf cells are often used to represent points on the unit sphere, and this class provides
 * methods for converting directly between these two representations. For cells that represent 2D
 * regions rather than discrete point, it is better to use the S2Cell class.
 *
 */
@Immutable
@GwtCompatible(emulated = true, serializable = true)
public final strictfp class S2CellId implements Comparable<S2CellId>, Serializable {
  // Although only 60 bits are needed to represent the index of a leaf
  // cell, we need an extra bit in order to represent the position of
  // the center of the leaf cell along the Hilbert curve.
  public static final int FACE_BITS = 3;
  public static final int NUM_FACES = 6;
  public static final int MAX_LEVEL = 30; // Valid levels: 0..MAX_LEVEL
  public static final int POS_BITS = 2 * MAX_LEVEL + 1;
  public static final int MAX_SIZE = 1 << MAX_LEVEL;

  /** The change in ST coordinates for each unit change in IJ coordinates. */
  private static final double IJ_TO_ST = 1.0 / MAX_SIZE;

  // Constant related to unsigned long's
  public static final long MAX_UNSIGNED = -1L; // Equivalent to 0xffffffffffffffffL

  // Used to encode the i, j, and orientation values into primitive longs.
  private static final int I_SHIFT = 33;
  private static final int J_SHIFT = 2;
  private static final long J_MASK = (1L << 31) - 1;
  private static final long ORIENTATION_MASK = (1L << 2) - 1;

  // Used to encode the si and ti values into primitive longs.
  private static final int SI_SHIFT = 32;
  private static final long TI_MASK = (1L << 32) - 1;

  // The following lookup tables are used to convert efficiently between an
  // (i,j) cell index and the corresponding position along the Hilbert curve.
  // "LOOKUP_POS" maps 4 bits of "i", 4 bits of "j", and 2 bits representing the
  // orientation of the current cell into 8 bits representing the order in which
  // that subcell is visited by the Hilbert curve, plus 2 bits indicating the
  // new orientation of the Hilbert curve within that subcell. (Cell
  // orientations are represented as combination of SWAP_MASK and INVERT_MASK.)
  //
  // "LOOKUP_IJ" is an inverted table used for mapping in the opposite
  // direction.
  //
  // We also experimented with looking up 16 bits at a time (14 bits of position
  // plus 2 of orientation) but found that smaller lookup tables gave better
  // performance. (2KB fits easily in the primary cache.)

  // Values for these constants are *declared* in the *.h file. Even though
  // the declaration specifies a value for the constant, that declaration
  // is not a *definition* of storage for the value. Because the values are
  // supplied in the declaration, we don't need the values here. Failing to
  // define storage causes link errors for any code that tries to take the
  // address of one of these values.
  private static final int LOOKUP_BITS = 4;
  private static final int SWAP_MASK = 0x01;
  private static final int INVERT_MASK = 0x02;
  private static final int LOOKUP_MASK = (1 << LOOKUP_BITS) - 1;

  private static final int[] LOOKUP_POS = new int[1 << (2 * LOOKUP_BITS + 2)];
  private static final int[] LOOKUP_IJ = new int[1 << (2 * LOOKUP_BITS + 2)];

  private static final S2CellId NONE = new S2CellId();
  private static final S2CellId SENTINEL = new S2CellId(MAX_UNSIGNED);

  /**
   * This is the offset required to wrap around from the beginning of the Hilbert curve to the end
   * or vice versa; see nextWrap() and prevWrap().
   */
  private static final long WRAP_OFFSET = ((long) NUM_FACES) << POS_BITS;

  static {
    initLookupCell(0, 0, 0, 0, 0, 0);
    initLookupCell(0, 0, 0, SWAP_MASK, 0, SWAP_MASK);
    initLookupCell(0, 0, 0, INVERT_MASK, 0, INVERT_MASK);
    initLookupCell(0, 0, 0, SWAP_MASK | INVERT_MASK, 0, SWAP_MASK | INVERT_MASK);
  }

  public static final S2CellId[] FACE_CELLS = new S2CellId[6];

  static {
    for (int face = 0; face < 6; face++) {
      FACE_CELLS[face] = fromFace(face);
    }
  }

  /** The id of the cell. */
  private final long id;

  public S2CellId(long id) {
    this.id = id;
  }

  public S2CellId() {
    this.id = 0;
  }

  /** The default constructor returns an invalid cell id. */
  public static S2CellId none() {
    return NONE;
  }

  /**
   * Returns an invalid cell id guaranteed to be larger than any valid cell id. Useful for creating
   * indexes.
   */
  public static S2CellId sentinel() {
    return SENTINEL; // -1
  }

  /** Returns the cell corresponding to a given S2 cube face. */
  public static S2CellId fromFace(int face) {
    return new S2CellId(fromFaceAsLong(face));
  }

  /**
   * Returns a cell given its face (range 0..5), Hilbert curve position within that face (an
   * unsigned integer with {@link #POS_BITS} bits), and level (range 0..MAX_LEVEL). The given
   * position will be modified to correspond to the Hilbert curve position at the center of the
   * returned cell. This is a static function rather than a constructor in order to indicate what
   * the arguments represent.
   */
  public static S2CellId fromFacePosLevel(int face, long pos, int level) {
    return new S2CellId(fromFacePosLevelAsLong(face, pos, level));
  }

  /**
   * Return the leaf cell containing the given point (a direction vector, not necessarily unit
   * length).
   */
  public static S2CellId fromPoint(S2Point p) {
    int face = S2Projections.xyzToFace(p);
    S2Projections.UvTransform t = S2Projections.faceToUvTransform(face);
    int i = S2Projections.stToIj(PROJ.uvToST(t.xyzToU(p.x, p.y, p.z)));
    int j = S2Projections.stToIj(PROJ.uvToST(t.xyzToV(p.x, p.y, p.z)));
    return fromFaceIJ(face, i, j);
  }

  /** Return the leaf cell containing the given S2LatLng. */
  public static S2CellId fromLatLng(S2LatLng ll) {
    return fromPoint(ll.toPoint());
  }

  /**
   * Returns the center of the cell in (u,v) coordinates. Note that the center of the cell is
   * defined as the point at which it is recursively subdivided into four children; in general, it
   * is not at the midpoint of the (u,v) rectangle covered by the cell.
   */
  public R2Vector getCenterUV() {
    long center = getCenterSiTi();
    return new R2Vector(
        PROJ.stToUV(S2Projections.siTiToSt(getSi(center))),
        PROJ.stToUV(S2Projections.siTiToSt(getTi(center))));
  }

  /** Returns the center of the cell in (s,t) coordinates. */
  public R2Vector getCenterST() {
    long center = getCenterSiTi();
    return new R2Vector(
        S2Projections.siTiToSt(getSi(center)), S2Projections.siTiToSt(getTi(center)));
  }

  /** Returns the bounds of this cell in (s,t)-space. */
  public R2Rect getBoundST() {
    double size = getSizeST();
    return R2Rect.fromCenterSize(getCenterST(), new R2Vector(size, size));
  }

  /** Returns the bounds of this cell in (u,v)-space. */
  public R2Rect getBoundUV() {
    long ijo = toIJOrientation();
    return ijLevelToBoundUv(getI(ijo), getJ(ijo), level());
  }

  public S2Point toPoint() {
    return S2Point.normalize(toPointRaw());
  }

  /**
   * Return the direction vector corresponding to the center of the given cell. The vector returned
   * by toPointRaw is not necessarily unit length.
   */
  public S2Point toPointRaw() {
    long center = getCenterSiTi();
    return PROJ.faceSiTiToXyz(face(), getSi(center), getTi(center));
  }

  /**
   * Returns a loop along the boundary of this cell, with vertices at intersections with the cell
   * grid at {@code level}. Equivalent to the union of new S2Polygon(new S2Cell(child)), for each
   * child in {@link #childrenAtLevel(int)} for the given level, but radically faster.
   */
  public S2Loop toLoop(int level) {
    S2Projections p = S2Projections.PROJ;
    int depth = level - level();
    Preconditions.checkState(depth >= 0);
    R2Rect rect = getBoundST();
    int face = face();
    int n = 1 << depth;
    double step = Math.scalb(1, -depth);
    List<S2Point> points = Lists.newArrayListWithCapacity(4 * n);
    R2Vector b = rect.getVertex(3);
    for (int corner = 0; corner < 4; corner++) {
      R2Vector a = b;
      b = rect.getVertex(corner);
      points.add(S2Point.normalize(S2Projections.faceUvToXyz(face, p.stToUV(a.x), p.stToUV(a.y))));
      for (double d = step; d < 1; d += step) {
        double s = (1 - d) * a.x + d * b.x;
        double t = (1 - d) * a.y + d * b.y;
        points.add(S2Point.normalize(S2Projections.faceUvToXyz(face, p.stToUV(s), p.stToUV(t))));
      }
    }
    return new S2Loop(points);
  }

  /**
   * Returns the (si, ti) coordinates of the center of the cell. The returned long packs the values
   * into one long, such that bits 32-63 contain si, and bits 0-31 contain ti.
   *
   * <p>Note that although (si, ti) coordinates span the range [0,2**31] in general, the cell center
   * coordinates are always in the range [1,2**31-1] and therefore can be represented using a signed
   * 32-bit integer.
   *
   * <p>Use {@link #getSi(long)} and {@link #getTi(long)} to extract integer values for si and ti,
   * respectively.
   */
  long getCenterSiTi() {
    // First we compute the discrete (i,j) coordinates of a leaf cell contained
    // within the given cell.  Given that cells are represented by the Hilbert
    // curve position corresponding at their center, it turns out that the cell
    // returned by ToFaceIJOrientation is always one of two leaf cells closest
    // to the center of the cell (unless the given cell is a leaf cell itself,
    // in which case there is only one possibility).
    //
    // Given a cell of size s >= 2 (i.e. not a leaf cell), and letting (imin,
    // jmin) be the coordinates of its lower left-hand corner, the leaf cell
    // returned by ToFaceIJOrientation() is either (imin + s/2, jmin + s/2) or
    // (imin + s/2 - 1, jmin + s/2 - 1).  The first case is the one we want.
    // We can distinguish these two cases by looking at the low bit of "i" or
    // "j".  In the second case the low bit is one, unless s == 2 (i.e. the
    // level just above leaf cells) in which case the low bit is zero.
    //
    // In the code below, the expression ((i ^ ((int) id >> 2)) & 1) is nonzero
    // if we are in the second case described above.
    long ijo = toIJOrientation();
    int i = getI(ijo);
    int j = getJ(ijo);
    int delta = isLeaf() ? 1 : (((i ^ (((int) id) >>> 2)) & 1) != 0) ? 2 : 0;
    // Note that (2 * {i,j} + delta) will never overflow a 32-bit integer. Thus,
    // we can embed both integers into a single primitive long. Bits 32-63 hold
    // the value for si, and bits 0-31 hold the value for ti.
    return (((long) (2 * i + delta)) << SI_SHIFT) | ((2 * j + delta) & TI_MASK);
  }

  /**
   * Returns the "si" coordinate from bits 32-63 in the given {@code center} primitive long returned
   * by {@link #getCenterSiTi()}.
   */
  static int getSi(long center) {
    return (int) (center >> SI_SHIFT);
  }

  /**
   * Returns the "ti" coordinate from bits 0-31 in the given {@code center} primitive long returned
   * by {@link #getCenterSiTi()}.
   */
  static int getTi(long center) {
    return (int) center;
  }

  /** Return the S2LatLng corresponding to the center of the given cell. */
  public S2LatLng toLatLng() {
    return new S2LatLng(toPointRaw());
  }

  /** The 64-bit unique identifier for this cell. */
  public long id() {
    return id;
  }

  /** Return true if id() represents a valid cell. */
  public boolean isValid() {
    return face() < NUM_FACES && ((lowestOnBit() & (0x1555555555555555L)) != 0);
  }

  /** Which cube face this cell belongs to, in the range 0..5. */
  public int face() {
    return (int) (id >>> POS_BITS);
  }

  /**
   * The position of the cell center along the Hilbert curve over this face, in the range
   * 0..(2**kPosBits-1).
   */
  public long pos() {
    return (id & (-1L >>> FACE_BITS));
  }

  /** Return the subdivision level of the cell (range 0..MAX_LEVEL). */
  public int level() {
    // Fast path for leaf cells (benchmarking shows this is worthwhile.)
    if (isLeaf()) {
      return MAX_LEVEL;
    }
    return MAX_LEVEL - (Long.numberOfTrailingZeros(id) >> 1);
  }

  /** As {@link #getSizeIJ(int)}, using the level of this cell. */
  public int getSizeIJ() {
    return getSizeIJ(level());
  }

  /** As {@link #getSizeST(int)}, using the level of this cell. */
  public double getSizeST() {
    return getSizeST(level());
  }

  /** Returns the edge length of cells at the given level in (i,j)-space. */
  public static int getSizeIJ(int level) {
    return 1 << (MAX_LEVEL - level);
  }

  /** Returns the edge length of cells at the given level in (s,t)-space. */
  public static double getSizeST(int level) {
    return S2Projections.ijToStMin(getSizeIJ(level));
  }

  /**
   * Return true if this is a leaf cell (more efficient than checking whether level() == MAX_LEVEL).
   */
  public boolean isLeaf() {
    return ((int) id & 1) != 0;
  }

  /**
   * Return true if this is a top-level face cell (more efficient than checking whether level() ==
   * 0).
   */
  public boolean isFace() {
    return (id & (lowestOnBitForLevel(0) - 1)) == 0;
  }

  /**
   * Return the child position (0..3) of this cell's ancestor at the given level, relative to its
   * parent. The argument should be in the range 1..MAX_LEVEL. For example, childPosition(1) returns
   * the position of this cell's level-1 ancestor within its top-level face cell.
   */
  public int childPosition(int level) {
    return (int) (id >>> (2 * (MAX_LEVEL - level) + 1)) & 3;
  }

  /**
   * Returns the start of the range of cell ids that are contained within this cell (including
   * itself.) The range is *inclusive* (i.e. test using >= and <=) and the return values of both
   * this method and {@link #rangeMax()} are valid leaf cell ids.
   */
  public S2CellId rangeMin() {
    return new S2CellId(rangeMinAsLong(id));
  }

  /**
   * Returns the end of the range of cell ids that are contained within this cell (including
   * itself.) The range is *inclusive* (i.e. test using >= and <=) and the return values of both
   * this method and {@link #rangeMin()} are valid leaf cell ids.
   *
   * <p>Note that because the range max is inclusive, care should be taken to iterate accordingly,
   * for example: <code>
   * for (S2CellId min = x.rangeMin(); min.compareTo(x.rangeMax()) <= 0; min = min.next()) {...}
   * </code> If you need to convert the range to a semi-open interval [min, limit), for example to
   * use a key-value store that only supports semi-open range queries, then do not attempt to define
   * "limit" as rangeMax.next(). The problem is that leaf S2CellIds are 2 units apart, so the
   * semi-open interval [min, limit) includes an additional value (rangeMax.id() + 1) which happens
   * to be a valid S2CellId about one-third of the time and is never contained by this cell. (It
   * always corresponds to a cell ID larger than this ID). You can define "limit" as {@code
   * rangeMax.id() + 1} if necessary; this is not always a valid S2CellId but can still be used with
   * fromToken/toToken. You may also convert rangeMax() to the key space of your key- value store
   * and define "limit" as the next larger key.
   *
   * <p>Note that sentinel().rangeMin(), sentinel.rangeMax(), and sentinel() are all equal.
   *
   * @see S2CellId#rangeMin
   * @see S2CellId#childBegin(int)
   * @see S2CellId#childEnd(int)
   * @see S2CellId#childrenAtLevel(int)
   */
  public S2CellId rangeMax() {
    return new S2CellId(rangeMaxAsLong(id));
  }

  /** Return true if the given cell is contained within this one. */
  public boolean contains(S2CellId other) {
    // assert (isValid() && other.isValid());
    return unsignedLongGreaterOrEquals(other.id, rangeMinAsLong(id))
        && unsignedLongLessOrEquals(other.id, rangeMaxAsLong(id));
  }

  /** Return true if the given cell intersects this one. */
  public boolean intersects(S2CellId other) {
    // assert (isValid() && other.isValid());
    return unsignedLongLessOrEquals(rangeMinAsLong(other.id), rangeMaxAsLong(id))
        && unsignedLongGreaterOrEquals(rangeMaxAsLong(other.id), rangeMinAsLong(id));
  }

  public S2CellId parent() {
    // assert (isValid() && level() > 0);
    return new S2CellId(parentAsLong(id));
  }

  /**
   * Return the cell at the previous level or at the given level (which must be less than or equal
   * to the current level).
   */
  public S2CellId parent(int level) {
    // assert (isValid() && level >= 0 && level <= this.level());
    return new S2CellId(parentAsLong(id, level));
  }

  /**
   * Returns the immediate child of this cell at the given traversal order position (in the range 0
   * to 3). Results are undefined if this is a leaf cell.
   */
  public S2CellId child(int position) {
    // assert (isValid());
    // assert (!isLeaf());
    // To change the level, we need to move the least-significant bit two positions downward. We do
    // this by subtracting (4 * new_lsb) and adding new_lsb. Then to advance to the given child
    // cell, we add (2 * position * new_lsb).
    long newLsb = lowestOnBit() >>> 2;
    return new S2CellId(id + (2 * position + 1 - 4) * newLsb);
  }

  public Iterable<S2CellId> children() {
    if (isLeaf()) {
      return ImmutableList.of();
    } else {
      return childrenAtLevel(level() + 1);
    }
  }

  public Iterable<S2CellId> childrenAtLevel(final int level) {
    Preconditions.checkState(isValid());
    Preconditions.checkArgument(level >= this.level() && level <= MAX_LEVEL);
    return new Iterable<S2CellId>() {
      @Override
      public Iterator<S2CellId> iterator() {
        return new UnmodifiableIterator<S2CellId>() {
          private S2CellId next = childBegin(level);
          private long childEnd = childEnd(level).id();

          @Override
          public boolean hasNext() {
            return next.id() != childEnd;
          }

          @Override
          public S2CellId next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            S2CellId oldNext = next;
            next = next.next();
            return oldNext;
          }
        };
      }
    };
  }

  // Iterator-style methods for traversing the immediate children of a cell or
  // all of the children at a given level (greater than or equal to the current
  // level). Note that the end value is exclusive, just like standard STL
  // iterators, and may not even be a valid cell id. You should iterate using
  // code like this:
  //
  // for (S2CellId c = id.childBegin(); !c.equals(id.childEnd()); c = c.next())
  // ...
  //
  // The convention for advancing the iterator is "c = c.next()", so be sure
  // to use 'equals()' in the loop guard, or compare 64-bit cell id's,
  // rather than "c != id.childEnd()".

  /**
   * Returns the first child in a traversal of the children of this cell, in Hilbert curve order.
   */
  public S2CellId childBegin() {
    // assert (isValid() && level() < MAX_LEVEL);
    return new S2CellId(childBeginAsLong(id));
  }

  /**
   * Returns the first cell in a traversal of children a given level deeper than this cell, in
   * Hilbert curve order. Requires that the given level be greater or equal to this cell level.
   */
  public S2CellId childBegin(int level) {
    // assert (isValid() && level >= this.level() && level <= MAX_LEVEL);
    return new S2CellId(childBeginAsLong(id, level));
  }

  /**
   * Returns the first cell after a traversal of the children of this cell in Hilbert curve order.
   * This cell can be invalid.
   */
  public S2CellId childEnd() {
    // assert (isValid() && level() < MAX_LEVEL);
    return new S2CellId(childEndAsLong(id));
  }

  /**
   * Returns the first cell after the last child in a traversal of children a given level deeper
   * than this cell, in Hilbert curve order. This cell can be invalid.
   */
  public S2CellId childEnd(int level) {
    // assert (isValid() && level >= this.level() && level <= MAX_LEVEL);
    return new S2CellId(childEndAsLong(id, level));
  }

  /**
   * Return the next cell at the same level along the Hilbert curve. Works correctly when advancing
   * from one face to the next, but does *not* wrap around from the last face to the first or vice
   * versa.
   */
  public S2CellId next() {
    return new S2CellId(id + (lowestOnBit() << 1));
  }

  /**
   * Return the previous cell at the same level along the Hilbert curve. Works correctly when
   * advancing from one face to the next, but does *not* wrap around from the last face to the first
   * or vice versa.
   */
  public S2CellId prev() {
    return new S2CellId(id - (lowestOnBit() << 1));
  }

  /**
   * Like next(), but wraps around from the last face to the first and vice versa. Should *not* be
   * used for iteration in conjunction with childBegin(), childEnd(), Begin(), or End().
   */
  public S2CellId nextWrap() {
    S2CellId n = next();
    if (unsignedLongLessThan(n.id, WRAP_OFFSET)) {
      return n;
    }
    return new S2CellId(n.id - WRAP_OFFSET);
  }

  /**
   * Like prev(), but wraps around from the last face to the first and vice versa. Should *not* be
   * used for iteration in conjunction with childBegin(), childEnd(), Begin(), or End().
   */
  public S2CellId prevWrap() {
    S2CellId p = prev();
    if (UnsignedLongs.compare(p.id, WRAP_OFFSET) < 0) {
      return p;
    }
    return new S2CellId(p.id + WRAP_OFFSET);
  }

  /**
   * Returns the first cell in an ordered traversal along the Hilbert curve at a given level (across
   * all 6 faces of the cube).
   */
  public static S2CellId begin(int level) {
    return new S2CellId(childBeginAsLong(fromFaceAsLong(0), level));
  }

  /**
   * Returns the first cell after an ordered traversal along the Hilbert curve at a given level
   * (across all 6 faces of the cube). The end value is exclusive, and is not a valid cell id.
   */
  public static S2CellId end(int level) {
    return new S2CellId(childEndAsLong(fromFaceAsLong(5), level));
  }

  /**
   * This method advances or retreats the indicated number of steps along the Hilbert curve at the
   * current level, and returns the new position. The position never advances past {@link #end(int)}
   * or before {@link #begin(int)}, and remains at the current level.
   */
  public S2CellId advance(long steps) {
    if (steps == 0) {
      return this;
    }

    // We clamp the number of steps if necessary to ensure that we do not advance past the end() or
    // before the begin() of this level.  Note that minSteps and maxSteps always fit in a signed
    // 64-bit integer.
    int stepShift = 2 * (MAX_LEVEL - level()) + 1;
    if (steps < 0) {
      long minSteps = -(id >>> stepShift);
      if (steps < minSteps) {
        steps = minSteps;
      }
    } else {
      long maxSteps = (WRAP_OFFSET + lowestOnBit() - id) >>> stepShift;
      if (steps > maxSteps) {
        steps = maxSteps;
      }
    }
    return new S2CellId(id + (steps << stepShift));
  }

  /**
   * This method advances or retreats the indicated number of steps along the Hilbert curve at the
   * current level, and returns the new position. The position wraps between the first and last
   * faces as necessary. The input must be a valid cell id.
   */
  public S2CellId advanceWrap(long steps) {
    // assert (isValid());
    if (steps == 0) {
      return this;
    }

    int stepShift = 2 * (MAX_LEVEL - level()) + 1;
    if (steps < 0) {
      long minSteps = -(id >>> stepShift);
      if (steps < minSteps) {
        long stepWrap = WRAP_OFFSET >>> stepShift;
        steps %= stepWrap;
        if (steps < minSteps) {
          steps += stepWrap;
        }
      }
    } else {
      // Unlike advance(), we don't want to return end(level).
      long maxSteps = (WRAP_OFFSET - id) >>> stepShift;
      if (steps > maxSteps) {
        long stepWrap = WRAP_OFFSET >>> stepShift;
        steps %= stepWrap;
        if (steps > maxSteps) {
          steps -= stepWrap;
        }
      }
    }
    return new S2CellId(id + (steps << stepShift));
  }

  /**
   * Returns the level of the "lowest common ancestor" of this cell and "other". Note that because
   * of the way that cell levels are numbered, this is actually the *highest* level of any shared
   * ancestor. Returns -1 if the two cells do not have any common ancestor (i.e., they are from
   * different faces).
   */
  public int getCommonAncestorLevel(S2CellId other) {
    // Basically we find the first bit position at which the two S2CellIds differ and convert that
    // to a level.  The max() below is necessary for the case where one S2CellId is a descendant of
    // the other.
    long bits = UnsignedLongs.max(id ^ other.id, lowestOnBit(), other.lowestOnBit());

    // Compute the position of the most significant bit, and then map
    // {0} -> 30, {1,2} -> 29, {3,4} -> 28, ... , {59,60} -> 0, {61,62,63} -> -1.
    return Math.max(Long.numberOfLeadingZeros(bits) - 3, -1) >> 1;
  }


  /**
   * Decodes the cell id from a compact text string suitable for display or indexing. Cells at lower
   * levels (i.e. larger cells) are encoded into fewer characters. The maximum token length is 16.
   *
   * @param token the token to decode
   * @return the S2CellId for that token
   * @throws NumberFormatException if the token is not formatted correctly
   */
  public static S2CellId fromToken(String token) {
    return fromTokenImpl(token, true);
  }

  /**
   * Returns the cell id for the given token, which will be implicitly zero-right-padded to length
   * 16 if 'implicitZeroes' is true.
   */
  private static S2CellId fromTokenImpl(String token, boolean implicitZeroes) {
    if (token == null) {
      throw new NumberFormatException("Null string in S2CellId.fromToken");
    }
    if (token.isEmpty()) {
      throw new NumberFormatException("Empty string in S2CellId.fromToken");
    }
    int length = token.length();
    if (length > 16 || "X".equals(token)) {
      return none();
    }

    long value = 0;
    for (int pos = 0; pos < length; pos++) {
      int digitValue = Character.digit(token.charAt(pos), 16);
      if (digitValue == -1) {
        throw new NumberFormatException(token);
      }
      value = value * 16 + digitValue;
    }

    if (implicitZeroes) {
      value = value << (4 * (16 - length));
    }

    return new S2CellId(value);
  }

  /**
   * Encodes the cell id to compact text strings suitable for display or indexing. Cells at lower
   * levels (i.e. larger cells) are encoded into fewer characters. The maximum token length is 16.
   *
   * <p>Simple implementation: convert the id to hex and strip trailing zeros. We could use base-32
   * or base-64, but assuming the cells used for indexing regions are at least 100 meters across
   * (level 16 or less), the savings would be at most 3 bytes (9 bytes hex vs. 6 bytes base-64).
   *
   * @return the encoded cell id
   */
  public String toToken() {
    if (id == 0) {
      return "X";
    }

    // Convert to a hex string with as many digits as necessary.
    String hex = Ascii.toLowerCase(Long.toHexString(id));
    // Prefix 0s to get a length 16 string.
    String padded = Strings.padStart(hex, 16, '0');
    // Trim zeroes off the end.
    return MATCHES_ZERO.trimTrailingFrom(padded);
  }

  /** Matches literal '0' characters. */
  private static final CharMatcher MATCHES_ZERO = CharMatcher.is('0');

  public String toTokenOld() {
    String hex = Ascii.toLowerCase(Long.toHexString(id));
    StringBuilder sb = new StringBuilder(16);
    for (int i = hex.length(); i < 16; i++) {
      sb.append('0');
    }
    sb.append(hex);
    for (int len = 16; len > 0; len--) {
      if (sb.charAt(len - 1) != '0') {
        return sb.substring(0, len);
      }
    }

    throw new RuntimeException("Shouldn't make it here");
  }

  /**
   * Return the four cells that are adjacent across the cell's four edges. Neighbors are returned in
   * the order defined by S2Cell::GetEdge. All neighbors are guaranteed to be distinct.
   *
   * <p>Requires that this cell is valid.
   */
  public void getEdgeNeighbors(S2CellId[] neighbors) {
    int level = this.level();
    int size = getSizeIJ(level);
    int face = face();

    long ijo = toIJOrientation();
    int i = getI(ijo);
    int j = getJ(ijo);

    // Edges 0, 1, 2, 3 are in the down, right, up, left directions.
    neighbors[0] = fromFaceIJSame(face, i, j - size, j - size >= 0).parent(level);
    neighbors[1] = fromFaceIJSame(face, i + size, j, i + size < MAX_SIZE).parent(level);
    neighbors[2] = fromFaceIJSame(face, i, j + size, j + size < MAX_SIZE).parent(level);
    neighbors[3] = fromFaceIJSame(face, i - size, j, i - size >= 0).parent(level);
  }

  /**
   * Return the neighbors of closest vertex to this cell at the given level, by appending them to
   * "output". Normally there are four neighbors, but the closest vertex may only have three
   * neighbors if it is one of the 8 cube vertices.
   *
   * <p>Requires that level < this.level(), so that we can determine which vertex is closest (in
   * particular, level == MAX_LEVEL is not allowed). Also requires that this cell is valid.
   */
  public void getVertexNeighbors(int level, Collection<S2CellId> output) {
    // "level" must be strictly less than this cell's level so that we can
    // determine which vertex this cell is closest to.
    // assert (level < this.level());
    long ijo = toIJOrientation();
    int i = getI(ijo);
    int j = getJ(ijo);

    // Determine the i- and j-offsets to the closest neighboring cell in each
    // direction. This involves looking at the next bit of "i" and "j" to
    // determine which quadrant of this->parent(level) this cell lies in.
    int halfsize = getSizeIJ(level + 1);
    int size = halfsize << 1;
    boolean isame;
    boolean jsame;
    int ioffset;
    int joffset;
    if ((i & halfsize) != 0) {
      ioffset = size;
      isame = (i + size) < MAX_SIZE;
    } else {
      ioffset = -size;
      isame = (i - size) >= 0;
    }
    if ((j & halfsize) != 0) {
      joffset = size;
      jsame = (j + size) < MAX_SIZE;
    } else {
      joffset = -size;
      jsame = (j - size) >= 0;
    }

    int face = face();
    output.add(parent(level));
    output.add(fromFaceIJSame(face, i + ioffset, j, isame).parent(level));
    output.add(fromFaceIJSame(face, i, j + joffset, jsame).parent(level));
    // If i- and j- edge neighbors are *both* on a different face, then this
    // vertex only has three neighbors (it is one of the 8 cube vertices).
    if (isame || jsame) {
      output.add(fromFaceIJSame(face, i + ioffset, j + joffset, isame && jsame).parent(level));
    }
  }

  /**
   * Append all neighbors of this cell at the given level to "output". Two cells X and Y are
   * neighbors if their boundaries intersect but their interiors do not. In particular, two cells
   * that intersect at a single point are neighbors.
   *
   * <p>Requires that nbrLevel >= this->level(). Note that for cells adjacent to a face vertex, the
   * same neighbor may be appended more than once. Also requires that this cell is valid.
   */
  public void getAllNeighbors(int nbrLevel, List<S2CellId> output) {
    long ijo = toIJOrientation();

    // Find the coordinates of the lower left-hand leaf cell. We need to
    // normalize (i,j) to a known position within the cell because nbrLevel
    // may be larger than this cell's level.
    int size = getSizeIJ();
    int face = face();
    int i = getI(ijo) & -size;
    int j = getJ(ijo) & -size;

    int nbrSize = getSizeIJ(nbrLevel);
    // assert (nbrSize <= size);

    // We compute the top-bottom, left-right, and diagonal neighbors in one pass.
    // The loop test is at the end of the loop to avoid 32-bit overflow.
    for (int k = -nbrSize; ; k += nbrSize) {
      boolean sameFace;
      if (k < 0) {
        sameFace = j + k >= 0;
      } else if (k >= size) {
        sameFace = j + k < MAX_SIZE;
      } else {
        sameFace = true;
        // Top and bottom neighbors.
        output.add(fromFaceIJSame(face, i + k, j - nbrSize, j - size >= 0).parent(nbrLevel));
        output.add(fromFaceIJSame(face, i + k, j + size, j + size < MAX_SIZE).parent(nbrLevel));
      }
      // Left, right, and diagonal neighbors.
      output.add(
          fromFaceIJSame(face, i - nbrSize, j + k, sameFace && i - size >= 0).parent(nbrLevel));
      output.add(
          fromFaceIJSame(face, i + size, j + k, sameFace && i + size < MAX_SIZE).parent(nbrLevel));
      if (k >= size) {
        break;
      }
    }
  }

  // ///////////////////////////////////////////////////////////////////
  // Low-level methods.

  /** Return a leaf cell given its cube face (range 0..5) and i- and j-coordinates (see s2.h). */
  public static S2CellId fromFaceIJ(int face, int i, int j) {
    // Optimization notes:
    // - Non-overlapping bit fields can be combined with either "+" or "|".
    // Generally "+" seems to produce better code, but not always.

    // gcc doesn't have very good code generation for 64-bit operations.
    // We optimize this by computing the result as two 32-bit integers
    // and combining them at the end. Declaring the result as an array
    // rather than local variables helps the compiler to do a better job
    // of register allocation as well. Note that the two 32-bits halves
    // get shifted one bit to the left when they are combined.
    long lsb = 0;
    long msb = ((long) face) << (POS_BITS - 33);

    // Alternating faces have opposite Hilbert curve orientations; this
    // is necessary in order for all faces to have a right-handed
    // coordinate system.
    int bits = (face & SWAP_MASK);

    // Each iteration maps 4 bits of "i" and "j" into 8 bits of the Hilbert
    // curve position. The lookup table transforms a 10-bit key of the form
    // "iiiijjjjoo" to a 10-bit value of the form "ppppppppoo", where the
    // letters [ijpo] denote bits of "i", "j", Hilbert curve position, and
    // Hilbert curve orientation respectively.

    for (int k = 7; k >= 4; --k) {
      bits = lookupBits(i, j, k, bits);
      msb = updateBits(msb, k, bits);
      bits = maskBits(bits);
    }
    for (int k = 3; k >= 0; --k) {
      bits = lookupBits(i, j, k, bits);
      lsb = updateBits(lsb, k, bits);
      bits = maskBits(bits);
    }

    return new S2CellId((((msb << 32) + lsb) << 1) + 1);
  }

  private static final int lookupBits(int i, int j, int k, int bits) {
    bits += (((i >> (k * LOOKUP_BITS)) & LOOKUP_MASK) << (LOOKUP_BITS + 2));
    bits += (((j >> (k * LOOKUP_BITS)) & LOOKUP_MASK) << 2);
    return LOOKUP_POS[bits];
  }

  private static final long updateBits(long sb, int k, int bits) {
    return sb | ((((long) bits) >> 2) << ((k & 0x3) * 2 * LOOKUP_BITS));
  }

  private static final int maskBits(int bits) {
    return bits & (SWAP_MASK | INVERT_MASK);
  }

  /**
   * Returns the (i, j) coordinates for the leaf cell corresponding to this cell id, and the
   * orientation the i- and j-axes follow at that level. The returned long packs the values into one
   * long, such that bits 33-63 contain i, bits 2-32 contain j, and bits 0-1 contain the
   * orientation.
   *
   * <p>Since cells are represented by the Hilbert curve position at the center of the cell, the
   * returned (i, j) for non-leaf cells will be a leaf cell adjacent to the cell center.
   *
   * <p>Use {@link #getI(long)}, {@link #getJ(long)}, and {@link #getOrientation(long)} to extract
   * integer values for i, j, and orientation, respectively.
   */
  long toIJOrientation() {
    int face = face();
    int bits = (face & SWAP_MASK);

    // Each iteration maps 8 bits of the Hilbert curve position into
    // 4 bits of "i" and "j". The lookup table transforms a key of the
    // form "ppppppppoo" to a value of the form "iiiijjjjoo", where the
    // letters [ijpo] represents bits of "i", "j", the Hilbert curve
    // position, and the Hilbert curve orientation respectively.
    //
    // On the first iteration we need to be careful to clear out the bits
    // representing the cube face.
    int i = 0;
    int j = 0;
    for (int k = 7; k >= 0; --k) {
      final int nbits = (k == 7) ? (MAX_LEVEL - 7 * LOOKUP_BITS) : LOOKUP_BITS;
      bits += (((int) (id >>> (k * 2 * LOOKUP_BITS + 1)) & ((1 << (2 * nbits)) - 1))) << 2;
      bits = LOOKUP_IJ[bits];
      i += (bits >> (LOOKUP_BITS + 2)) << (k * LOOKUP_BITS);
      j += (((bits >> 2) & LOOKUP_MASK)) << (k * LOOKUP_BITS);
      bits = maskBits(bits);
    }

    // The position of a non-leaf cell at level "n" consists of a prefix of
    // 2*n bits that identifies the cell, followed by a suffix of
    // 2*(MAX_LEVEL-n)+1 bits of the form 10*. If n==MAX_LEVEL, the suffix is
    // just "1" and has no effect. Otherwise, it consists of "10", followed
    // by (MAX_LEVEL-n-1) repetitions of "00", followed by "0". The "10" has
    // no effect, while each occurrence of "00" has the effect of reversing
    // the SWAP_MASK bit.
    // assert (S2.POS_TO_ORIENTATION[2] == 0);
    // assert (S2.POS_TO_ORIENTATION[0] == S2.SWAP_MASK);
    if ((lowestOnBit() & 0x1111111111111110L) != 0) {
      bits ^= S2.SWAP_MASK;
    }
    int orientation = bits;

    // Since i and j are non-negative ints, we only need 31 bits to represent
    // each value. Thus, bits 33-63 of the {@code ijo} primitive long hold the
    // value for i, and bits 2-32 hold the value for j. Bits 0-1 hold the value
    // of the 2-bit orientation.
    return (((long) i) << I_SHIFT) | (((long) j) << J_SHIFT) | orientation;
  }

  /** Returns the "i" coordinate of this S2 cell ID. */
  public int getI() {
    return getI(toIJOrientation());
  }

  /**
   * Returns the "i" coordinate from bits 33-63 in the given {@code ijo} primitive long returned by
   * {@link #toIJOrientation()}.
   */
  static int getI(long ijo) {
    return (int) (ijo >>> I_SHIFT);
  }

  /** Returns the "j" coordinate of this S2 cell ID. */
  public int getJ() {
    return getJ(toIJOrientation());
  }

  /**
   * Returns the "j" coordinate from bits 2-32 in the given {@code ijo} primitive long returned by
   * {@link #toIJOrientation()}.
   */
  static int getJ(long ijo) {
    return (int) ((ijo >>> J_SHIFT) & J_MASK);
  }

  /** Returns the orientation of this S2 cell ID. */
  public int getOrientation() {
    return getOrientation(toIJOrientation());
  }

  /**
   * Returns the orientation from bits 0-1 in the given {@code ijo} primitive long returned by
   * {@link #toIJOrientation()}.
   */
  static int getOrientation(long ijo) {
    return (int) (ijo & ORIENTATION_MASK);
  }

  /**
   * Returns the lowest-numbered bit that is on for this cell id, which is equal to {@code 1L << (2
   * * (MAX_LEVEL - level))}. So for example, a.lsb() <= b.lsb() if and only if a.level() >=
   * b.level(), but the first test is more efficient.
   */
  public long lowestOnBit() {
    return lowestOnBit(id);
  }

  /** Return the lowest-numbered bit that is on for cells at the given level. */
  public static long lowestOnBitForLevel(int level) {
    return 1L << (2 * (MAX_LEVEL - level));
  }

  /**
   * Returns the bound in (u,v)-space for the cell at the given level containing the leaf cell with
   * the given (i,j)-coordinates.
   */
  static R2Rect ijLevelToBoundUv(int i, int j, int level) {
    R2Rect bound = new R2Rect();
    int cellSize = getSizeIJ(level);
    setAxisRange(i, cellSize, bound.x());
    setAxisRange(j, cellSize, bound.y());
    return bound;
  }

  private static void setAxisRange(int ij, int cellSize, R1Interval interval) {
    interval.set(
        S2Projections.PROJ.ijToUV(ij, cellSize),
        S2Projections.PROJ.ijToUV(ij + cellSize, cellSize));
  }

  /**
   * Given a face and a point (i,j) where either i or j is outside the valid range [0..MAX_SIZE-1],
   * this function first determines which neighboring face "contains" (i,j), and then returns the
   * leaf cell on that face which is adjacent to the given face and whose distance from (i,j) is
   * minimal.
   */
  private static S2CellId fromFaceIJWrap(int face, int i, int j) {
    // Convert i and j to the coordinates of a leaf cell just beyond the
    // boundary of this face.  This prevents 32-bit overflow in the case
    // of finding the neighbors of a face cell.
    i = Math.max(-1, Math.min(MAX_SIZE, i));
    j = Math.max(-1, Math.min(MAX_SIZE, j));

    // We want to wrap these coordinates onto the appropriate adjacent face.
    // The easiest way to do this is to convert the (i,j) coordinates to (x,y,z)
    // (which yields a point outside the normal face boundary), and then call
    // S2::XYZtoFaceUV() to project back onto the correct face.
    //
    // The code below converts (i,j) to (si,ti), and then (si,ti) to (u,v) using
    // the linear projection (u=2*s-1 and v=2*t-1).  (The code further below
    // converts back using the inverse projection, s=0.5*(u+1) and t=0.5*(v+1).
    // Any projection would work here, so we use the simplest.)  We also clamp
    // the (u,v) coordinates so that the point is barely outside the
    // [-1,1]x[-1,1] face rectangle, since otherwise the reprojection step
    // (which divides by the new z coordinate) might change the other
    // coordinates enough so that we end up in the wrong leaf cell.
    final double kLimit = 1.0 + S2.DBL_EPSILON;
    double u = Math.max(-kLimit, Math.min(kLimit, IJ_TO_ST * ((i << 1) + 1 - MAX_SIZE)));
    double v = Math.max(-kLimit, Math.min(kLimit, IJ_TO_ST * ((j << 1) + 1 - MAX_SIZE)));

    // Find the leaf cell coordinates on the adjacent face, and convert
    // them to a cell id at the appropriate level.
    S2Projections.XyzTransform xyzTransform = S2Projections.faceToXyzTransform(face);
    double x = xyzTransform.uvToX(u, v);
    double y = xyzTransform.uvToY(u, v);
    double z = xyzTransform.uvToZ(u, v);
    face = S2Projections.xyzToFace(x, y, z);
    S2Projections.UvTransform uvTransform = S2Projections.faceToUvTransform(face);
    return fromFaceIJ(
        face,
        S2Projections.stToIj(0.5 * (1 + uvTransform.xyzToU(x, y, z))),
        S2Projections.stToIj(0.5 * (1 + uvTransform.xyzToV(x, y, z))));
  }

  /**
   * Public helper function that calls FromFaceIJ if sameFace is true, or FromFaceIJWrap if sameFace
   * is false.
   */
  public static S2CellId fromFaceIJSame(int face, int i, int j, boolean sameFace) {
    if (sameFace) {
      return S2CellId.fromFaceIJ(face, i, j);
    } else {
      return S2CellId.fromFaceIJWrap(face, i, j);
    }
  }

  @Override
  public boolean equals(Object that) {
    if (!(that instanceof S2CellId)) {
      return false;
    }
    S2CellId x = (S2CellId) that;
    return id() == x.id();
  }

  /** Returns true if x1 < x2, when both values are treated as unsigned. */
  public static boolean unsignedLongLessThan(long x1, long x2) {
    return (x1 + Long.MIN_VALUE) < (x2 + Long.MIN_VALUE);
  }

  /** Returns true if x1 <= x2, when both values are treated as unsigned. */
  public static boolean unsignedLongLessOrEquals(long x1, long x2) {
    return (x1 + Long.MIN_VALUE) <= (x2 + Long.MIN_VALUE);
  }

  /** Returns true if x1 > x2, when both values are treated as unsigned. */
  public static boolean unsignedLongGreaterThan(long x1, long x2) {
    return (x1 + Long.MIN_VALUE) > (x2 + Long.MIN_VALUE);
  }

  /** Returns true if x1 >= x2, when both values are treated as unsigned. */
  public static boolean unsignedLongGreaterOrEquals(long x1, long x2) {
    return (x1 + Long.MIN_VALUE) >= (x2 + Long.MIN_VALUE);
  }

  public boolean lessThan(S2CellId x) {
    return unsignedLongLessThan(id, x.id);
  }

  public boolean greaterThan(S2CellId x) {
    return unsignedLongGreaterThan(id, x.id);
  }

  public boolean lessOrEquals(S2CellId x) {
    return unsignedLongLessOrEquals(id, x.id);
  }

  public boolean greaterOrEquals(S2CellId x) {
    return unsignedLongGreaterOrEquals(id, x.id);
  }

  @Override
  public int hashCode() {
    return (int) ((id >>> 32) + id);
  }

  @Override
  public String toString() {
    return "(face=" + face() + ", pos=" + Long.toHexString(pos()) + ", level=" + level() + ")";
  }

  private static void initLookupCell(
      int level, int i, int j, int origOrientation, int pos, int orientation) {
    if (level == LOOKUP_BITS) {
      int ij = (i << LOOKUP_BITS) + j;
      LOOKUP_POS[(ij << 2) + origOrientation] = (pos << 2) + orientation;
      LOOKUP_IJ[(pos << 2) + origOrientation] = (ij << 2) + orientation;
    } else {
      level++;
      i <<= 1;
      j <<= 1;
      pos <<= 2;
      // Initialize each sub-cell recursively.
      for (int subPos = 0; subPos < 4; subPos++) {
        int ij = S2.posToIJ(orientation, subPos);
        int orientationMask = S2.posToOrientation(subPos);
        initLookupCell(
            level,
            i + (ij >>> 1),
            j + (ij & 1),
            origOrientation,
            pos + subPos,
            orientation ^ orientationMask);
      }
    }
  }

  @Override
  public int compareTo(S2CellId that) {
    return unsignedLongLessThan(this.id, that.id)
        ? -1
        : unsignedLongGreaterThan(this.id, that.id) ? 1 : 0;
  }

  private static long fromFaceAsLong(int face) {
    return (((long) face) << POS_BITS) + lowestOnBitForLevel(0);
  }

  private static long childBeginAsLong(long id) {
    long oldLsb = lowestOnBit(id);
    return id - oldLsb + (oldLsb >>> 2);
  }

  private static long childBeginAsLong(long id, int level) {
    return id - lowestOnBit(id) + lowestOnBitForLevel(level);
  }

  private static long childEndAsLong(long id) {
    long oldLsb = lowestOnBit(id);
    return id + oldLsb + (oldLsb >>> 2);
  }

  private static long childEndAsLong(long id, int level) {
    return id + lowestOnBit(id) + lowestOnBitForLevel(level);
  }

  private static long rangeMinAsLong(long id) {
    return id - (lowestOnBit(id) - 1);
  }

  private static long rangeMaxAsLong(long id) {
    return id + (lowestOnBit(id) - 1);
  }

  private static long lowestOnBit(long id) {
    return Long.lowestOneBit(id);
  }

  private static long parentAsLong(long id) {
    long newLsb = lowestOnBit(id) << 2;
    return (id & -newLsb) | newLsb;
  }

  private static long parentAsLong(long id, int level) {
    long newLsb = lowestOnBitForLevel(level);
    return (id & -newLsb) | newLsb;
  }

  private static long fromFacePosLevelAsLong(int face, long pos, int level) {
    return parentAsLong((((long) face) << POS_BITS) + (pos | 1), level);
  }

  /**
   * Returns a cell id decoded from a simple debug format. This function is reasonably efficient,
   * but is only intended for use in tests; no promises are made about the durability of the
   * encoding over time.
   */
  static S2CellId fromDebugString(String str) {
    int level = str.length() - 2;
    if (level < 0 || level > S2CellId.MAX_LEVEL) {
      return S2CellId.NONE;
    }
    int face = str.charAt(0) - '0';
    if (face < 0 || face > 5 || str.charAt(1) != '/') {
      return S2CellId.NONE;
    }
    S2CellId id = S2CellId.fromFace(face);
    for (int i = 2; i < str.length(); ++i) {
      int childPos = str.charAt(i) - '0';
      if (childPos < 0 || childPos > 3) {
        return S2CellId.NONE;
      }
      id = id.child(childPos);
    }
    return id;
  }
}
