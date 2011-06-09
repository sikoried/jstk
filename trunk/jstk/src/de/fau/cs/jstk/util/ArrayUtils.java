package de.fau.cs.jstk.util;


public class ArrayUtils {
	// an interface for classes that have a public clone() method
	public interface PubliclyCloneable {
		public PubliclyCloneable clone();
	}

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
