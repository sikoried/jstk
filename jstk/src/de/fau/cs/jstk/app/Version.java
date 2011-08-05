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
package de.fau.cs.jstk.app;

public class Version {
	// TODO, maybe: create a class with global settings, and put it there. Implement version number comparisons.
	public static final String Version = "0.1.1-0";

	public static final String SYNOPSIS =
		Version.class.getCanonicalName() + 
		": Print version string to stdout and exit\n\n";

	public static void main(String[] args) throws Exception {
		if (args.length == 1 && (
				args[0].equals("-h") || 
				args[0].equals("--help"))){
			System.err.println(SYNOPSIS);
			System.exit(0);
		}

		System.out.println(Version);
		System.exit(0);
	}
}