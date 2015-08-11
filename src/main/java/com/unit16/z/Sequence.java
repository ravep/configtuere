package com.unit16.z;

import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * Like {@link Iterator}, but immutable (and hence, simpler).
 * @param <A>
 */
public abstract class Sequence<A> implements Iterable<A> {
	
	public abstract A next();
	public abstract boolean hasNext();
	
	public static <B> Sequence<B> fromIterator(final Iterator<B> source)
	{
		return new Sequence<B>(){

			@Override
			public B next() { return source.next(); }

			@Override
			public boolean hasNext() { return source.hasNext(); }
		};
	}
	
	public static <B> Sequence<B> fromIterable(final Iterable<B> source)
	{
		return fromIterator(source.iterator());
	}
	
	public final <C> Sequence<Pair<A, C>> zip(final Sequence<C> snd)
	{
		final Sequence<A> fst = this;
		return new Sequence<Pair<A, C>>(){

			@Override
			public Pair<A, C> next() {
				return new Pair.I<>(fst.next(), snd.next());
			}

			@Override
			public boolean hasNext() {
				return fst.hasNext() && snd.hasNext();
			}};
	}
	
	public final <D> Sequence<D> onResultOf(Function<A, D> f)
	{
		return fromIterator(Iterators.transform(iterator(), f));
	}
	
	public final Sequence<A> filter(Predicate<A> p)
	{
		return fromIterator(Iterators.filter(iterator(), p));
	}
	
	@Override
	public final Iterator<A> iterator()
	{
		return new UnmodifiableIterator<A>() {

			@Override
			public boolean hasNext() { return Sequence.this.hasNext(); }

			@Override
			public A next() { return Sequence.this.next(); }
		};
	}
	
	public final Sequence<A> append(final Iterable<A> other) {
				
		final Iterator<A> mine = iterator();
		
		return new Sequence<A>() {

			Iterator<A> fst, snd;			
			{
				fst = mine;
				snd = other.iterator();
				advance();
			}

			void advance() { 
				if(!fst.hasNext()) {
					fst = snd;
				}
			}
			
			@Override
			public A next() {
				A ret = fst.next();
				advance();
				return ret;
			}

			@Override
			public boolean hasNext() {
				return fst.hasNext();
			}
			
		};
	}
	
}
