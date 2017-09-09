/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.vette.idea.neos.lang.fusion.psi.ext;


public interface FusionPathMixin {

    /**
     * Checks if there is only a prototype signature in this path
     *
     * @return boolean
     */
    boolean isPrototypeSignature();

    /**
     * Checks if this path is a @class property of a prototype signature
     * e.g. prototype(Foo:Bar).@class
     *
     * @return boolean
     */
    boolean isPrototypeClassProperty();
}
