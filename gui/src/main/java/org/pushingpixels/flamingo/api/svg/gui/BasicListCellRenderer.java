/**
 * Copyright 2014 Emmanuel Bourg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pushingpixels.flamingo.api.svg.gui;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * ListCellRenderer implementation that delegates the rendering to the renderer
 * defined by the current look and feel (unlike DefaultCellRenderer).
 * 
 * @author Emmanuel Bourg
 */
abstract class BasicListCellRenderer<E> implements ListCellRenderer<E> {
    private ListCellRenderer delegate;
    private Class uiClass;

    @Override
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getUI().getClass() != uiClass) {
            uiClass = list.getUI().getClass();
            if ("ComboBox.list".equals(list.getName())) {
                delegate = new JComboBox().getRenderer();
            } else {
                delegate = new JList().getCellRenderer();
            }
        }
        
        return delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
}
