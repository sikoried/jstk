/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet

	This file is part of the Java Speech Toolkit (JSTK).

	The JSTK is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	The JSTK is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
*/
package de.fau.cs.jstk.sampled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Samples {
	public static double [] signedLinearLittleShorts2samples(byte [] buf){
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		
		double [] ret = new double[buf.length / 2];
		
		// decode the byte stream
		int i;
		for (i = 0; i < buf.length / 2; i++){
			short value = bb.getShort();
			if (value == 32768)
				value = 32767;
		
			ret[i] = (double) value / 32767.0;
		}
				
		
		return ret;		
	}
	
	public static byte [] samples2signedLinearLittleShorts(double [] buf){
		ByteBuffer bb = ByteBuffer.allocate(buf.length * 2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		for (double d : buf)
			bb.putShort((short)(d * 32767.0));
		
		return bb.array();		
	}

}
