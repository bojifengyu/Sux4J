package it.unimi.dsi.sux4j.mph;

import static it.unimi.dsi.sux4j.bits.Fast.ceilLog2;
import static it.unimi.dsi.sux4j.bits.Fast.length;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.io.FileLinesCollection;
import it.unimi.dsi.mg4j.io.LineIterator;
import it.unimi.dsi.mg4j.util.Fast;
import it.unimi.dsi.sux4j.bits.BitVector;
import it.unimi.dsi.sux4j.bits.BitVectors;
import it.unimi.dsi.sux4j.bits.LongArrayBitVector;
import it.unimi.dsi.sux4j.bits.RankAndSelect;
import it.unimi.dsi.sux4j.bits.SimpleRankAndSelect;
import it.unimi.dsi.sux4j.bits.BitVector.TransformationStrategy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.ForNameStringParser;

public class HollowTrie<T> implements Serializable {
	private static final Logger LOGGER = Fast.getLogger( HollowTrie.class );
	private static final long serialVersionUID = 0L;

	private static final boolean ASSERTS = true;
	
	public LongArrayBitVector skips;
	public LongArrayBitVector delimiters;
	public RankAndSelect delimitersRankAndSelect;
	public BitVector trie;
	public long lastOne;
	public RankAndSelect rankAndSelect;
	//public IntArrayList temp;
	
	private final static class Node {
		Node left, right;
		int skip;
		
		public Node( final Node left, final Node right, final int skip ) {
			this.left = left;
			this.right = right;
			this.skip = skip;
			if ( ASSERTS ) assert skip >= 0;
		}
	}
	
	private transient Node root;
	private long numNodes;
	private LongArrayBitVector borders;
	private SimpleRankAndSelect skipLocator;
	private final TransformationStrategy<? super T> transform;
	
	public long getLeafIndex( final T object ) {
		//System.err.println( bitVector );
		final BitVector bitVector = transform.toBitVector( object );
		long p = 0, r = 0, length = bitVector.length(), index = 0, a = 0, b = 0, t;
		int s = 0;
		long leftBorder, rightBorder;
		
		for(;;) {
			leftBorder = 0;
			if ( r > 0 ) leftBorder = skipLocator.select( r ) + 1;
			rightBorder = skips.length();
			if ( r < numNodes ) rightBorder = skipLocator.select( r + 1 ) + 1;
		
/*			System.err.println( skips + " " + borders + " " + temp + " " + r+" " +leftBorder + " " + rightBorder );
			if ( (int)skips.extract( leftBorder, rightBorder )
				!= temp.getInt( (int) r ) ) throw new AssertionError((int)skips.extract( leftBorder, rightBorder )
						+"!="+ temp.getInt( (int) r ));*/
			if ( ( s += (int)skips.extract( leftBorder, rightBorder ) ) >= length ) return -1;

			if ( bitVector.getBoolean( s ) ) p = 2 * r + 2;
			else p = 2 * r + 1;

			t = 2 * rankAndSelect.rank( a, b );
			a = b + 1;
			b += t;
			
			index += p - a - rankAndSelect.rank( a, p - 1 );

			//System.err.println( a + " " + b + " " + p + " " + index );
			
			if ( ASSERTS ) assert p < trie.length();
			if ( ! trie.getBoolean( p ) ) break;

			r = rankAndSelect.rank( p ) - 1;
			
			s++;
		}
		
		// Complete computation of leaf index
		
		for(;;) {
			p = rankAndSelect.select( r = rankAndSelect.rank( p ) );
			if ( p < a ) break;
			p = r * 2;
			
			t = 2 * rankAndSelect.rank( a, b );
			a = b + 1;
			b += t;
			
			index += p - a + 1 - rankAndSelect.rank( a, p );
			
			//System.err.println( a + " " + b + " " + p + " " + index );
		}

		return index;
	}
	
	public void succinct() {
		final BitVector bitVector = LongArrayBitVector.getInstance( 2 * numNodes + 1 );
		if ( numNodes == 0 ) return;
		final ObjectArrayList<Node> queue = new ObjectArrayList<Node>();
		final IntArrayList skips = new IntArrayList();
		int p = 0, maxSkip = Integer.MIN_VALUE;
		long skipsLength = 0;
		bitVector.add( 1 );
		queue.add( root );
		root = null;
		Node n;
		
		while( p < queue.size() ) {
			n = queue.get( p );
			if ( maxSkip < n.skip ) maxSkip = n.skip;
			skips.add( n.skip );
			skipsLength += length( n.skip );
			bitVector.add( n.left != null );
			bitVector.add( n.right != null );
			if ( n.left != null ) queue.add( n.left );
			if ( n.right != null ) queue.add( n.right );
			p++;
		}
		
		trie = bitVector;
		rankAndSelect = new SimpleRankAndSelect( bitVector, 2 );
		lastOne = rankAndSelect.lastOne();
		final int skipWidth = ceilLog2( maxSkip ) + 1;
		final int skipLongWidth = ceilLog2( skipWidth ) + 1;
		LOGGER.info( "Skip width: " + skipWidth );
		LOGGER.info( "Skip log width: " + skipWidth );
		this.skips = LongArrayBitVector.getInstance( skipsLength );
		this.borders = LongArrayBitVector.getInstance( skipsLength );
		int s = skips.size();
		int x;
		for( IntIterator i = skips.iterator(); s-- != 0; ) {
			x = i.nextInt();
			this.skips.append( x, length( x ) );
			borders.append( 1, length( x ) );
		}
		//this.temp = skips;
		if ( this.skips.trim() ) throw new AssertionError();
		if ( borders.trim() ) throw new AssertionError();
		skipLocator = new SimpleRankAndSelect( borders, 2 );
	}
	
	
	public HollowTrie( final Iterable<? extends T> iterable, final BitVector.TransformationStrategy<? super T> transform ) {
		this( iterable.iterator(), transform );
	}
		
	public HollowTrie( final Iterator<? extends T> iterator, final BitVector.TransformationStrategy<? super T> transform ) {

		this.transform = transform;
		if ( ! iterator.hasNext() ) return;
		
		BitVector prev = transform.toBitVector( iterator.next() ).copy(), curr;
		int prefix;
		Node p, parent;
		
		while( iterator.hasNext() ) {
			curr = transform.toBitVector( iterator.next() );
			if ( prev.compareTo( curr ) >= 0 ) throw new IllegalArgumentException( "The input bit vectors are not lexicographically sorted" );
			prefix = (int)curr.maximumCommonPrefixLength( prev );
			if ( prefix == prev.length() ) throw new IllegalArgumentException( "The input bit vectors are not prefix-free" );
			
			p = root;
			parent = null;
			Node n = null;
			while( p != null ) {
				if ( prefix < p.skip ) {
					n = new Node( p, null, prefix );
					numNodes++;
					if ( parent == null ) {
						root.skip -= prefix + 1;
						if ( ASSERTS ) assert root.skip >= 0;
						root = n;
					}
					else {
						parent.right = n;
						p.skip -= prefix + 1;
						if ( ASSERTS ) assert p.skip >= 0;
					}
					break;
				}
			
				prefix -= p.skip + 1;
				parent = p;
				p = p.right;
			}
			
			if ( p == null ) {
				if ( parent == null ) root = new Node( null, null, prefix );
				else parent.right = new Node( null, null, prefix );
				numNodes++;
			}
			
			if ( ASSERTS ) {
				long s = 0;
				Node m = root;
				while( m != null ) {
					s += m.skip;
					if ( curr.getBoolean( s ) ) {
						if ( m.right == null ) break;
					}
					else if ( m.left == null ) break;
					m = curr.getBoolean( s ) ? m.right : m.left;
					s++;
				}
				assert parent == null || ( p == null ? m == parent.right : m == n );
			}
			
			prev = curr.copy();
		}
	}
	
	private void recToString( final Node n, final StringBuilder printPrefix, final StringBuilder result, final StringBuilder path, final int level ) {
		if ( n == null ) return;
		
		result.append( printPrefix ).append( '(' ).append( level ).append( ')' );
		
		if ( n.skip >= 0 ) result.append( " skip: " ).append( n.skip );

		result.append( '\n' );
		
		path.append( '0' );
		recToString( n.left, printPrefix.append( '\t' ).append( "0 => " ), result, path, level + 1 );
		path.setCharAt( path.length() - 1, '1' ); 
		recToString( n.right, printPrefix.replace( printPrefix.length() - 5, printPrefix.length(), "1 => "), result, path, level + 1 );
		path.delete( path.length() - 1, path.length() ); 
		printPrefix.delete( printPrefix.length() - 6, printPrefix.length() );
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		recToString( root, new StringBuilder(), s, new StringBuilder(), 0 );
		return s.toString();
	}
	
	public static void main( final String[] arg ) throws NoSuchMethodException, IOException, JSAPException {

		final SimpleJSAP jsap = new SimpleJSAP( HollowTrie.class.getName(), "Builds a hollow trie reading a newline-separated list of terms.",
				new Parameter[] {
					new FlaggedOption( "bufferSize", JSAP.INTSIZE_PARSER, "64Ki", JSAP.NOT_REQUIRED, 'b',  "buffer-size", "The size of the I/O buffer used to read terms." ),
					//new FlaggedOption( "class", MG4JClassParser.getParser(), klass.getName(), JSAP.NOT_REQUIRED, 'c', "class", "A subclass of MinimalPerfectHash to be used when creating the table." ),
					new FlaggedOption( "encoding", ForNameStringParser.getParser( Charset.class ), "UTF-8", JSAP.NOT_REQUIRED, 'e', "encoding", "The term file encoding." ),
					new Switch( "zipped", 'z', "zipped", "The term list is compressed in gzip format." ),
					new Switch( "huTucker", 'h', "hu-tucker", "Use Hu-Tucker coding to increase entropy." ),
					new FlaggedOption( "termFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "offline", "Read terms from this file (without loading them into core memory) instead of standard input." ),
					new UnflaggedOption( "trie", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The filename for the serialised hollow trie." )
		});
		
		JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) return;
		
		final int bufferSize = jsapResult.getInt( "bufferSize" );
		final String trieName = jsapResult.getString( "trie" );
		final String termFile = jsapResult.getString( "termFile" );
		//final Class<?> tableClass = jsapResult.getClass( "class" );
		final Charset encoding = (Charset)jsapResult.getObject( "encoding" );
		final boolean zipped = jsapResult.getBoolean( "zipped" );
		final boolean huTucker = jsapResult.getBoolean( "huTucker" );
		
		final HollowTrie<CharSequence> hollowTrie;
		
		LOGGER.info( "Building trie..." );

		if ( termFile == null ) {
			hollowTrie = new HollowTrie<CharSequence>( new LineIterator( new FastBufferedReader( new InputStreamReader( System.in, encoding ), bufferSize ) ), BitVectors.utf16() );
			hollowTrie.succinct();
		}
		else {
			FileLinesCollection collection = new FileLinesCollection( termFile, encoding.toString(), zipped );
			hollowTrie = new HollowTrie<CharSequence>( collection, huTucker ? new HuTuckerTransformationStrategy( collection ) : BitVectors.utf16() );
			hollowTrie.succinct();
		}
		
		LOGGER.info( "Writing to file..." );		
		BinIO.storeObject( hollowTrie, trieName );
		LOGGER.info( "Completed." );
	}
}