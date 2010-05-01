package it.unimi.dsi.sux4j.bits;

/*		 
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2007-2010 Sebastiano Vigna 
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

/** An abstract implementation of {@link Rank} providing a few obvious derived methods. */

public abstract class AbstractRank implements Rank {

	public long count() {
		return rank( bitVector().length() );
	}
	
	public long rank( final long from, final long to ) {
		return rank( to ) - rank( from );
	}

	public long rankZero( final long pos ) {
		return pos - rank( pos );
	}

	public long rankZero( final long from, final long to ) {
		return to - from - rank( from, to );
	}
}
