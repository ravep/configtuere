package com.unit16.z;

import com.google.common.base.MoreObjects;

public interface Triplet<A,B,C> {

	public A fst();
	public B snd();
	public C trd();
	
	public static interface Uniform<T> extends Triplet<T,T,T> {
		
		public static class I<T> extends Triplet.I<T, T, T> implements Uniform<T> {

			public I(T a, T b, T c) {
				super(a, b, c);
			}
			
	        public static <A> Triplet.Uniform<A> uniform(A a, A b, A c) {
	        	return new Triplet.Uniform.I<A>(a, b, c);
	        }
	          			
		}
		
		
	}
	
	
    public static class I<A,B,C> extends Pair.I<A, B> implements Triplet<A,B,C> {

        public C trd;
        
        public I(A a, B b, C c) {
        	super(a, b);
            trd = c;
        }
        
        public void copyFrom(Triplet<A, B, C> val) {
            fst = val.fst();
            snd = val.snd();
            trd = val.trd();
        }

        @Override
        public A fst() {
            return fst;
        }

        @Override
        public B snd() {
            return snd;
        }
        
        @Override
        public C trd() {
            return trd;
        }
        
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(Triplet.class)
                    .add("fst", fst)
                    .add("snd", snd)
                    .add("trd", trd)
                    .toString();
        }

        public static <A, B, C> Triplet.I<A, B, C> triplet(A a, B b, C c) {
            return new Triplet.I<A, B, C>(a, b, c);
        }
        
    
    }
    
	
}
