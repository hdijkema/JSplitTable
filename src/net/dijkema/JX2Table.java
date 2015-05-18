/* ******************************************************************************
 *
 *       Copyright 2008-2010 Hans Dijkema
 *       This file is part of the JDesktop SwingX library
 *       and part of the SwingLabs project
 *
 *   SwingX is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as 
 *   published by the Free Software Foundation, either version 3 of 
 *   the License, or (at your option) any later version.
 *
 *   SwingX is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with SwingX.  If not, see <http://www.gnu.org/licenses/>.
 *   
 * ******************************************************************************/

package net.dijkema;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import net.dijkema.splittable.SplitTableDefaults;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.BorderHighlighter;

class MouseAdapt extends MouseAdapter {
	JX2Table _table;
	
	public void mouseClicked(MouseEvent e){
	      if (e.getClickCount() >= 2) {
	    	  _table.fireDoubleClick();
	      }	
	}
	
	public MouseAdapt(JX2Table t) {
		_table=t;
	}
}

class zc3MouseAdaptForHeader extends MouseAdapter {
	JX2Table _table;
	
	static public int ENTER=1;
	static public int LEAVE=2;
	static public int MOVE=3;
	static public int CLICKED=4;
	static public int DOUBLECLICK=5;
	static public int PRESSED=6;
	static public int RELEASED=7;
	
	public void mouseClicked(MouseEvent e){
	      if (e.getClickCount() >= 2) {
	    	  _table.fireMouseEventForHeader(e,DOUBLECLICK);
	      } else if (e.getClickCount()<2) {
	    	  _table.fireMouseEventForHeader(e,CLICKED);
	      }
	}
	
	public void mouseEntered(MouseEvent e) {
		_table.fireMouseEventForHeader(e,ENTER);
	}
	
	public void mouseExited(MouseEvent e) {
		_table.fireMouseEventForHeader(e,LEAVE);
	}
	
	public void mouseMoved(MouseEvent e) {
		_table.fireMouseEventForHeader(e,MOVE);
	}

	public void mousePressed(MouseEvent e) {
		_table.fireMouseEventForHeader(e,PRESSED);
	}
		
	public void mouseReleased(MouseEvent e) {
		_table.fireMouseEventForHeader(e,RELEASED);
	}
	
	public zc3MouseAdaptForHeader(JX2Table t) {
		_table=t;
	}
}


public class JX2Table extends JXTable {

	private static final long serialVersionUID = 1L;
	
	private String  _name;
	private String  _prgName;
	private boolean _selectTextOnFocus=false;
	
	public interface SelectionListener {
		public void selected(int row);
		public void choosen(int row);
		public void noSelection();
	}
	
	public interface MultipleSelectionListener {
		public void selected(Vector<Integer> rows);
	}
	
	public interface HeaderListener {
		public void choosen(int column);
		public void entered(int column);
		public void left(int column);
		public void clicked(int column);
		public void pressed(int column);
		public void released(int column);
	}
	
	public interface KeyListener {
		public void delete(int row);
		public void enter(int row);
		public void insert(int row);
	}
	
	private boolean			_prefsRead=false;
	
	ArrayList<ActionListener> listeners=new ArrayList<ActionListener>();
	private boolean _persistentRequested;

	// scrolling 
	
	public void scrollToSelected(JScrollPane pane) {
		scrollToRowCol(super.getSelectedRow(), super.getSelectedColumn(),pane);
	}
	
	public void scrollToRow(int row,JScrollPane pane) {
		scrollToRowCol(row,0,pane);
	}
	
	public void scrollToRowCol(int row,int col,JScrollPane pane) {
		Dimension d=pane.getViewport().getSize();
		Rectangle R=super.getCellRect(row, col, true);
		Rectangle E=super.getCellRect(super.getModel().getRowCount()-1,col,true);
		int H=d.height/2;
		if (R.y-H>0 && R.y+H<E.y) {
			R.y-=H;
			R.height=d.height;
		}
		super.scrollRectToVisible(R);
	}
	
	private void storeColumnAttributes(String prgName) {
		if (_name!=null) {
			Preferences prefs=Preferences.userRoot().node("jx2table");
			int i,N=super.getColumnCount();
			prefs.putInt(String.format("%s_N",_name),N);
			for(i=0;i<N;i++) {
				SortOrder o=super.getSortOrder(i);
				TableColumn col=super.getColumn(i);
				prefs.putInt(String.format("%s_col_width[%d]",_name,i), col.getWidth());
				Integer order=0;
				if (SortOrder.ASCENDING==o) { order=1; }
				else if (SortOrder.DESCENDING==o) { order=2; }
				prefs.putInt(String.format("%s_col_sort[%d]",_name,i), order);
				prefs.putInt(String.format("%s[%d].width",_name,i),col.getWidth());
			}
		}
	}
	
	private void readColumnAttributes(String prgName) {
		if (_name!=null) {
			Preferences prefs=Preferences.userRoot().node("jx2table");
			int N=prefs.getInt(String.format("%s_N",_name), -1);
			if (N!=-1) {
				try {
					int i; //, N;
					int icol = -1;
					SortOrder scol = null;
					if (N == super.getColumnCount()) {
						for (i = 0; i < N; i++) {
							int width = prefs.getInt(String.format("%s_col_width[%d]",_name,i), -1);
							if (width>-1) {
								int order=prefs.getInt(String.format("%s_col_sort[%d]",_name,i), 0);
								SortOrder o;
								if (order == 0) {
									o = SortOrder.UNSORTED;
								} else if (order == 1) {
									o = SortOrder.ASCENDING;
								} else {
									o = SortOrder.DESCENDING;
								}
	
								TableColumn col = super.getColumn(i);
								col.setPreferredWidth(width);
								if (order != 0) {
									icol = i;
									scol = o;
								}
							}
						}
					}
					if (icol >= 0) {
						super.setSortOrder(icol, scol);
					}
				} catch (Exception E) {
					E.printStackTrace();
				}
			}
			
			int i; //,N;
			for(i=0,N=super.getColumnCount();i<N;i++) {
				int w=prefs.getInt(String.format("%s[%d].width",_name,i), -1);
				TableColumn col=super.getColumn(i);
				if (w!=-1) {
					col.setPreferredWidth(w);
				}
			}
		}
	}
	
	public void persistPrefs() {
		storeColumnAttributes(_prgName);
	}
	
	public void readPrefs() {
		if (!_prefsRead) {
			readColumnAttributes(_prgName);
			_prefsRead=true;
		}
	}
	
	public void storeIfRequested() {
		if (_persistentRequested) {
			persistPrefs();
		}
	}
	
	/**
	 * If this is called with true, editable cells will be
	 * completely selected and therefore the text replaced on
	 * typing.  
	 * 
	 * @param yesSelectIt
	 */
	public void selectTextOnFocus(boolean yesSelectIt) {
		_selectTextOnFocus=yesSelectIt;
	}
	
	public boolean editCellAt(int row, int column, EventObject e)
	{
		boolean result = super.editCellAt(row, column, e);
		if (_selectTextOnFocus) { 
			selectAll(e);
		}
		return result;
	}

	private void selectAll(EventObject e) {
		final Component editor = getEditorComponent();
		
		if (editor == null || ! (editor instanceof JTextComponent)) {
			return;
		}

		if (e == null) {
			((JTextComponent)editor).selectAll();
			return;
		}

		if (_selectTextOnFocus) {
			((JTextComponent)editor).selectAll();
			return;
		}
	}
	
	private void addKey(KeyStroke k ,AbstractAction a) {
		InputMap im = this.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	    im.put(k, k);
	    getActionMap().put(im.get(k), a);
	}
	
	public void requestPersistent(boolean yes) {
		_persistentRequested = yes;
	}
	
	public JX2Table(String prgName) {
		_name=null;
		init();
	}
	
	public JX2Table(String prgName, AbstractTableModel m) {
		super(m);
		_name=null;
		_prgName = prgName;
		init();
	}
	
	public JX2Table(String prgName, String name) {
		_name=name;
		_prgName = prgName;
		init();
	}
	
	public JX2Table(String prgName, String name,AbstractTableModel m) {
		super(m);
		_name=name;
		_prgName = prgName;
		init();
	}
	
	@SuppressWarnings("unchecked")
	public JX2Table(String prgName, String name,Vector data,Vector headers) {
		super(data,headers);
		_name=name;
		_prgName = prgName;
		init();
	}
	
	public void init() {
		_persistentRequested = false;
		this.addMouseListener(new MouseAdapt(this));
		
		//super.setShowGrid(true);  --> Not reliable with Nimbus
		super.setShowGrid(false); // override default mode of any LAF 
		this.addHighlighter(new BorderHighlighter(null,SplitTableDefaults.innerCellBorder(),false));
		
		super.setSortOrderCycle(SortOrder.ASCENDING,SortOrder.DESCENDING,SortOrder.UNSORTED);
		
		JTableHeader h=super.getTableHeader();
		h.addMouseListener(new zc3MouseAdaptForHeader(this));
		h.addMouseMotionListener(new zc3MouseAdaptForHeader(this));
		h.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			public void columnAdded(TableColumnModelEvent e) {
				storeIfRequested();
			}

			public void columnRemoved(TableColumnModelEvent e) {
				storeIfRequested();
			}

			public void columnMoved(TableColumnModelEvent e) {
				storeIfRequested();
			}

			public void columnMarginChanged(ChangeEvent e) {
				storeIfRequested();
			}

			public void columnSelectionChanged(ListSelectionEvent e) {
				storeIfRequested();
			}
		});
		
		super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	
		super.getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (!e.getValueIsAdjusting()) {
						int index=getSelectedRow();
						int indices[]=getSelectedRows();
						if (index==-1) {
							informUnselect();
						} else if (indices.length>1) {
							Vector<Integer> rows=new Vector<Integer>();
							for (int i : indices) {
								rows.add(i);
							}
							informSelect(rows);
						} else {
							informSelect(index);
						}
					}
				}
			});
		
		_listeners=new HashSet<SelectionListener>();
		_mlisteners=new HashSet<MultipleSelectionListener>();
		_hlisteners=new HashSet<HeaderListener>();
		
	    addKey(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
	    	   new AbstractAction() {
					public void actionPerformed(ActionEvent e) {
						if (JX2Table.this.getSelectedRow()!=-1) {
							JX2Table.this.deleteEvent();
						}
					}
	    });
	    
	    addKey(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),
		    	   new AbstractAction() {
						public void actionPerformed(ActionEvent e) {
							if (JX2Table.this.getSelectedRow()!=-1) {
								JX2Table.this.insertEvent();
							}
						}
		    });
	    
	    addKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.VK_CONTROL),
		    	   new AbstractAction() {
						public void actionPerformed(ActionEvent e) {
							if (JX2Table.this.getSelectedRow()!=-1) {
								JX2Table.this.enterEvent();
							}
						}
		    });
	    

	    readPrefs();
	}
	
	public void fireDoubleClick() {
		int i,N;
		ActionEvent e=new ActionEvent(this,1,"doubleclick");
		
		for(i=0,N=listeners.size();i<N;i++) {
			listeners.get(i).actionPerformed(e);
		}
		
		if (super.getSelectedRow()>=0) {
			informChoosen(super.getSelectedRow());
		} else {
			informUnselect();
		}
	}
	
	public void fireDoubleClickForHeader(MouseEvent E) {
		int i,N;
        int column = columnAtPoint(E.getPoint());
		ActionEvent e=new ActionEvent(this,1,"header_doubleclick");
		for(i=0,N=listeners.size();i<N;i++) {
			listeners.get(i).actionPerformed(e);
		}
        if (column >= 0) {
			informHChoosen(column);
		} 
	}
	
	private int prevcol=-1;
	
	public void fireMouseEventForHeader(MouseEvent E,int type) {
        int column = columnAtPoint(E.getPoint());
        //System.out.println("type="+type+", column="+column+", prevcol="+prevcol);
        if (type==zc3MouseAdaptForHeader.ENTER) { 
        	informHEntered(column);
        	prevcol=column;
        } else if (type==zc3MouseAdaptForHeader.LEAVE) {
        	informHLeft(prevcol);
        	prevcol=-1;
        } else if (type==zc3MouseAdaptForHeader.MOVE && prevcol!=column) {
        	informHLeft(prevcol);
        	informHEntered(column);
        	prevcol=column;
        } else if (type==zc3MouseAdaptForHeader.CLICKED) {
        	informHClicked(column);
        } else if (type==zc3MouseAdaptForHeader.DOUBLECLICK) {
        	fireDoubleClickForHeader(E);
        } else if (type==zc3MouseAdaptForHeader.PRESSED) {
        	if (E.getButton()==MouseEvent.BUTTON1) {
        		informHPressed(column);
        	}
        } else if (type==zc3MouseAdaptForHeader.RELEASED) {
        	if (E.getButton()==MouseEvent.BUTTON1) {
        		informHReleased(column);
        	}
        }
	}
	
	public void addDoubleClickListener(ActionListener o) {
		listeners.add(o);
	}

	private Set<SelectionListener> _listeners;
	private Set<MultipleSelectionListener> _mlisteners;
	private Set<HeaderListener>    _hlisteners;
	
	public void addHeaderListener(HeaderListener l) {
		_hlisteners.add(l);
	}
	
	public void removeHeaderListener(HeaderListener l) {
		_hlisteners.remove(l);
	}
	
	public void informHChoosen(int col) {
		Iterator<HeaderListener> it=_hlisteners.iterator();
		while(it.hasNext()) {
			it.next().choosen(col);
		}
	}
	
	public void informHEntered(int col) {
		Iterator<HeaderListener> it=_hlisteners.iterator();
		while(it.hasNext()) {
			it.next().entered(col);
		}
	}
	
	public void informHLeft(int col) {
		Iterator<HeaderListener> it=_hlisteners.iterator();
		while(it.hasNext()) {
			it.next().left(col);
		}
	}
	
	public void informHClicked(int col) {
		Iterator<HeaderListener> it=_hlisteners.iterator();
		while(it.hasNext()) {
			it.next().clicked(col);
		}
	}
	
	public void informHPressed(int col) {
		Iterator<HeaderListener> it=_hlisteners.iterator();
		while(it.hasNext()) {
			it.next().pressed(col);
		}
	}
	
	public void informHReleased(int col) {
		Iterator<HeaderListener> it=_hlisteners.iterator();
		while(it.hasNext()) {
			it.next().released(col);
		}
	}
	
	public void addSelectionListener(SelectionListener l) {
		_listeners.add(l);
	}
	
	public void addSelectionListener(MultipleSelectionListener l) {
		_mlisteners.add(l);
	}
	
	public void removeSelectionListener(SelectionListener l) {
		_listeners.remove(l);
	}
	
	public void removeSelectionListener(MultipleSelectionListener l) {
		_mlisteners.remove(l);
	}
	
	public void informUnselect() {
		Iterator<SelectionListener> it=_listeners.iterator();
		while(it.hasNext()){
			it.next().noSelection();
		}
	}
	
	public void informSelect(int row) {
		Iterator<SelectionListener> it=_listeners.iterator();
		while(it.hasNext()){
			it.next().selected(convertRowIndexToModel(row));
		}
	}
	
	public void informSelect(Vector<Integer> rows) {
		Vector<Integer> converted=new Vector<Integer>();
		for(int r : rows) {
			converted.add(convertRowIndexToModel(r));
		}
		for(MultipleSelectionListener l : _mlisteners) {
			l.selected(converted);
		}
	}
	
	public void informChoosen(int row) {
		Iterator<SelectionListener> it=_listeners.iterator();
		while(it.hasNext()){
			it.next().choosen(convertRowIndexToModel(row));
		}
	}
	
	
	private Set<KeyListener> _keylisteners=new HashSet<KeyListener>();
	
	public void addKeyListener(KeyListener l) {
		_keylisteners.add(l);
	}
	
	public void removeKeyListener(KeyListener l) {
		_keylisteners.remove(l);
	}
	
	private void deleteEvent() {
		int row=this.getSelectedRow();
		Iterator<KeyListener> it=_keylisteners.iterator();
		while(it.hasNext()) { it.next().delete(row); }
	}

	private void insertEvent() {
		int row=this.getSelectedRow();
		Iterator<KeyListener> it=_keylisteners.iterator();
		while(it.hasNext()) { it.next().insert(row); }
	}

	private void enterEvent() {
		int row=this.getSelectedRow();
		Iterator<KeyListener> it=_keylisteners.iterator();
		while(it.hasNext()) { it.next().enter(row); }
	}

}
