/*
 * Copyright (c) 2022  Informatics Matters Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package squonk.jobs.cdk.util;

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.HashMap;
import java.util.Map;

/** A wrapper around a CDK IAtomContainer that allows the molecule to be used in different <i>Represention</i>s.
 * This allows the molecule to be used in different hydrogenation forms.
 * The original molecule is not modified, a copy is made to generate the different forms.
 * Representations are created when needed and cached.
 *
 * Also allows to set properties on the molecule, with the property being set on the original molecule and any
 * representations that have been created.
 */
public class MoleculeObject {

    public enum Representation {
        Original, ExplicitH, ImplicitH
    }

    private final IAtomContainer mol;
    private final Map<Representation, IAtomContainer> representations = new HashMap<>();

    public MoleculeObject(IAtomContainer mol) {
        assert mol != null;
        this.mol = mol;
    }

    public IAtomContainer getMol() {
        return mol;
    }

    /** Get the specified representation. It is created if needed.
     *
     * @param key
     * @return
     */
    public IAtomContainer getRepresentation(Representation key) {
        if (key == Representation.Original) {
            return mol;
        } else if (representations.containsKey(key)) {
            return representations.get(key);
        } else {
            IAtomContainer m = MoleculeUtils.createRepresentation(mol, key);
            representations.put(key, m);
            return m;
        }
    }

    public boolean hasRepresentations(Representation key) {
        return representations.containsKey(key);
    }

    public Map<Object, Object> getProperties() {
        return mol.getProperties();
    }

    public Object getProperty(Object name) {
        return mol.getProperty(name);
    }

    /** Set this property to the original molecule and any representations that have been created.
     *
     * @param name
     * @param value
     */
    public void setProperty(Object name, Object value) {
        mol.setProperty(name, value);
        for (IAtomContainer m : representations.values()) {
            m.setProperty(name, value);
        }
    }
}
