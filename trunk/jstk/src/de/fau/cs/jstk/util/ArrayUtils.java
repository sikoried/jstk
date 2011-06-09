package de.fau.cs.jstk.util;


/**
 * 
 * 
 * @author hoenig
 *
 */
public class ArrayUtils {
	/**
	 * an interface for classes that have a *public* clone() method
	 * @author hoenig
	 */
	public interface PubliclyCloneable {
		/**
		 * @return a deep copy of this object
		 */
		public PubliclyCloneable clone();
	}
	
	/**
	 * clone an array of PubliclyCloneable implementers by calling clone() for each element 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PubliclyCloneable> T[] arrayClone(T[] orig){
		// shallow copy:
		T [] ret = orig.clone();

		// deep copies:
		int i;
		for (i = 0; i < orig.length; i++)
			ret[i] = (T) orig[i].clone();
		return ret;
	}
}
