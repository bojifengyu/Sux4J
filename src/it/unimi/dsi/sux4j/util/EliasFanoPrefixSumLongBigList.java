package it.unimi.dsi.sux4j.util;

/*		 
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2008 Sebastiano Vigna 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

import it.unimi.dsi.bits.BitVector;
import it.unimi.dsi.fastutil.bytes.ByteIterable;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.longs.AbstractLongIterator;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongIterators;
import it.unimi.dsi.fastutil.shorts.ShortIterable;

import java.util.NoSuchElementException;

/** A compressed big list of longs providing prefix sums; an element occupies a number of bits roughly equal to the logarithm of the average value.
 * 
 * <p>Instances of this class store in compacted form a list of longs. 
 * Values are provided either through an {@linkplain Iterable iterable object}.
 * As an additional service, this list provides access to the <em>{@linkplain #prefixSum(long) prefix sums}</em> of its values.
 * 
 * <h2>Implementation details</h2>
 * 
 * <p>Instances of this class are essentially a view over an instance of {@link EliasFanoMonotoneLongBigList}
 * storing the prefix sums. The {@link #getLong(long)} method has been optimised so to avoid two calls
 * to the {@link #getLong(long)} method of the {@link EliasFanoMonotoneLongBigList}.
 * 
 */
public class EliasFanoPrefixSumLongBigList extends EliasFanoMonotoneLongBigList {
	private static final long serialVersionUID = 1L;

	/** Wraps an iterator and returns prefix sums. */
	
	private final static class CumulativeLongIterable implements LongIterable {
		private final LongIterable iterable;
		
		public CumulativeLongIterable( final LongIterable iterable ) {
			this.iterable = iterable;
		}

		public LongIterator iterator() {
			return new AbstractLongIterator() {
				private final LongIterator iterator = iterable.iterator();
				private long prefixSum = 0;
				private boolean lastToDo = true;
				
				public boolean hasNext() {
					return iterator.hasNext() || lastToDo;
				}
				
				public long nextLong() {
					if ( ! hasNext() ) throw new NoSuchElementException();
					if ( ! iterator.hasNext() && lastToDo ) {
						lastToDo = false;
						return prefixSum;
					}
					final long result = prefixSum;
					prefixSum += iterator.nextLong();
					return result;
				}
			};
		}
		
	}

	private BitVector upperBits;
	
	/** Creates a new Elias&ndash;Fano prefix-sum long big list.
	 * 
	 * @param elements an iterable object.
	 */
	public EliasFanoPrefixSumLongBigList( final LongIterable elements ) {
		super( new CumulativeLongIterable( elements ) );
		this.upperBits = selectUpper.bitVector();
	}
	
	/** Creates a new Elias&ndash;Fano prefix-sum long big list.
	 * 
	 * @param elements an iterable object.
	 */
	public EliasFanoPrefixSumLongBigList( final IntIterable elements ) {
		this( new LongIterable() {
			public LongIterator iterator() {
				return LongIterators.wrap( elements.iterator() );
			}
		});
	}
	
	/** Creates a new Elias&ndash;Fano prefix-sum long big list.
	 * 
	 * @param elements an iterable object.
	 */
	public EliasFanoPrefixSumLongBigList( final ShortIterable elements ) {
		this( new LongIterable() {
			public LongIterator iterator() {
				return LongIterators.wrap( elements.iterator() );
			}
		});
	}
	
	/** Creates a new Elias&ndash;Fano prefix-sum long big list.
	 * 
	 * @param elements an iterable object.
	 */
	public EliasFanoPrefixSumLongBigList( final ByteIterable elements ) {
		this( new LongIterable() {
			public LongIterator iterator() {
				return LongIterators.wrap( elements.iterator() );
			}
		});
	}
	
	public long getLong( final long index ) {
		if ( index < 0 || index >= length - 1 ) throw new IndexOutOfBoundsException( Long.toString( index ) );
		final long pos = selectUpper.select( index + 1 );
		if ( upperBits.getBoolean( pos - 1 ) ) return lowerBits.getLong( index + 1 ) - lowerBits.getLong( index );
		else return ( pos - upperBits.previousOne( pos ) - 1 ) * ( 1L << l ) + lowerBits.getLong( index + 1 ) - lowerBits.getLong( index );
	}
	
	/** Returns the prefix sum of this list up to the given index.
	 * 
	 * @param index an index from 0 to the length of this list.
	 * @return the sum of the values with index between 0 (inclusive) and <code>index</code> (exclusive).
	 */
	public long prefixSum( final long index ) {
		return super.getLong( index );
	}

	public long length() {
		return length - 1;
	}
}