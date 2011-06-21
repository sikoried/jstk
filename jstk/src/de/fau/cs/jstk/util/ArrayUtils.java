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
	public interface PubliclyCloneable extends Cloneable{
		/**
		 * @return a deep copy of this object
		 */
		public PubliclyCloneable clone();// throws CloneNotSupportedException;
	}
	
	/**
	 * clone an array of PubliclyCloneable implementers by calling clone() for each element 
	 * @throws CloneNotSupportedException 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PubliclyCloneable> T[] arrayClone(T[] orig){// throws CloneNotSupportedException{
		// shallow copy:
		T [] ret = orig.clone();

		// deep copies:
		int i;
		for (i = 0; i < orig.length; i++)
			ret[i] = (T) orig[i].clone();
		return ret;
	}
}
