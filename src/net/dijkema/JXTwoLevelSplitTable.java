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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.dijkema.splittable.AbstractTwoLevelSplitTableModel;
import net.dijkema.splittable.SplitTableDefaults;
import net.dijkema.splittable.AbstractTwoLevelSplitTableModel.CNodeIndex;

import org.jdesktop.swingx.decorator.BorderHighlighter;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;
import org.jdesktop.swingx.renderer.JRendererLabel;


public class JXTwoLevelSplitTable extends JXSplitTable {
	
	class ExpandIcon implements Icon {
		
		static final int EXPANDED = 0;
		static final int CLOSED = 1;
		static final int NONE = 2;

		public int 		height=10;
		public int 		width=10;
		public int      kind=NONE;
		private boolean expanded = false;
		public Color    color=Color.gray;
		private Polygon _p=new Polygon();
		
		public ExpandIcon(int exp) {
			_p.addPoint(0,0);
			_p.addPoint(0,0);
			_p.addPoint(0,0);
			kind = exp;
			expanded = (kind == EXPANDED) ? true : false;
		}
		
		public void setExpanded(boolean b) {
			expanded=b;
		}
		
		public int getIconHeight() {
			return height; 
		}

		public int getIconWidth() {
			return width; 
		}

		private void drawRight(int x,int y, int w,int h, Graphics2D g) {
			g.setColor(color);
			_p.xpoints[0]=x;_p.ypoints[0]=y;
			_p.xpoints[1]=x+w;_p.ypoints[1]=y+(h/2);
			_p.xpoints[2]=x;_p.ypoints[2]=y+h;
			g.fillPolygon(_p);
		}

		private void drawDown(int x,int y, int w,int h, Graphics2D g) {
			g.setColor(color);
			_p.xpoints[0]=x;_p.ypoints[0]=y;
			_p.xpoints[1]=x+w;_p.ypoints[1]=y;
			_p.xpoints[2]=x+(w/2);_p.ypoints[2]=y+h;
			g.fillPolygon(_p);
		}
		
		private void drawNone(int x,int y, int w,int h, Graphics2D g) {
			//g.setColor(g.getBackground());
			//g.clearRect(x, y, w, h);
			//g.setColor(Color.white);;
			//_p.xpoints[0]=x;_p.ypoints[0]=y;
			//_p.xpoints[1]=x+w;_p.ypoints[1]=y;
			//_p.xpoints[2]=x+w;_p.ypoints[2]=y+h;
			//g.fillPolygon(_p);
		}
		
		public void paintIcon(Component c, Graphics _g, int x, int y) {
			
			Graphics2D g=(Graphics2D) _g;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			x=x-2;
			y=y-2;
			
			x+=2;
			y+=2;
			int w=width-2;
			int h=height-2;
			
			if (kind != NONE) {
				if (expanded) {
					drawDown(x,y,w,h,g);
				} else {
					drawRight(x,y,w,h,g);
				}
			} else {
				drawNone(x,y,w,h,g);
			}
		}
		
	}

	
	private static final long serialVersionUID = 1L;

	public interface SelectionListener {
		
		/**
		 * Indicates that nodeRow in nodeIndex at col has been choosen in the left or the right 
		 * part of the Split Table. If nodeRow==-1, then only the node has been choosen, no
		 * row within. (choosen==double click)
		 * 
		 * @param nodeIndex
		 * @param nodeRow
		 * @param col
		 * @param left
		 */
		public void choosen(int nodeIndex,int nodeRow,int col,boolean left);
		
		/**
		 * Indicates that nodeRow in nodeIndex at col has been selected in the left or the right 
		 * part of the Split Table. If nodeRow==-1, then only the node has been selected, no
		 * row within. (selected==single click)
		 * 
		 * @param nodeIndex
		 * @param nodeRow
		 * @param col
		 * @param left
		 */
		public void selected(int nodeIndex,int nodeRow,int col,boolean left);
		
		/**
		 * Indicates that a previous selection in the left (left==true) or the right part
		 * is unselected, i.e. after this, no selection is active anymore in the left or
		 * the right part.
		 *  
		 * @param left
		 */
		public void unSelected(boolean left);
		
	}
	
	
	private AbstractTwoLevelSplitTableModel _model;
	private Set<SelectionListener>          _listeners;
	

	public void addSelectionListener(SelectionListener l) {
		_listeners.add(l);
	}
	
	public void removeSelectionListener(SelectionListener l) {
		_listeners.remove(l);
	}

	
	
	/**
	 * Constructs the JXTowLevelSplitTable with given name, model and scrollbar policies. 
	 * 
	 * @param name
	 * @param model
	 * @param verticalScrollPolicy
	 * @param horizontalScrollPolicy
	 */
	public JXTwoLevelSplitTable(String prgName, String name, AbstractTwoLevelSplitTableModel model, int verticalScrollPolicy, int horizontalScrollPolicy) {
		super(prgName, name, model, verticalScrollPolicy, horizontalScrollPolicy);
		
		super.setShowGrid(false);
		super.setIntercellSpacing(new Dimension(0,1));
		super.setSortable(false);
		
		_model=model;
		_listeners=new HashSet<SelectionListener>();
		
		HighlightPredicate paintIconExpanded=new HighlightPredicate() {
			private int 	_row = -1;
			private boolean _prev = false;
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				if (adapter.column==0) {
					if (_row == adapter.row) {
						return _prev;
					} else {
						CNodeIndex cnode=_model.getCNodeIndex(adapter.row, 0);
						if (cnode.nodeRow==-1) {
							_prev = _model.getNodeExpanded(cnode.nodeIndex);
							return _prev;
						} else {
							_prev = false;
							return false;
						}
					}
				} else {
					return false;
				}
			}
		};
		HighlightPredicate paintIconClosed=new HighlightPredicate() {
			private int 	_row = -1;
			private boolean _prev = false;
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				if (adapter.column==0) {
					if (_row == adapter.row) {
						return _prev;
					} else {
						_row = adapter.row;
						CNodeIndex cnode=_model.getCNodeIndex(adapter.row, 0);
						if (cnode.nodeRow==-1) {
							_prev = !_model.getNodeExpanded(cnode.nodeIndex);
							return _prev;
						} else {
							_prev = false;
							return false;
						}
					}
				} else {
					return false;
				}
			}
		};
		
		HighlightPredicate paintIconNone=new HighlightPredicate() {
			private int 	_row = -1;
			private boolean _prev = false;
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				if (adapter.column==0) {
					if (_row == adapter.row) {
						return _prev;
					} else {
						_row = adapter.row;
						CNodeIndex cnode=_model.getCNodeIndex(adapter.row, 0);
						if (cnode.nodeRow==-1) {
							_prev = false;
							return false;
						} else {
							_prev = true;
							return true;
						}
					}
				} else {
					return true;
				}
			}
		};
		
		
		HighlightPredicate splitRow=new HighlightPredicate() {
			private int 		_row = -1;
			private boolean 	_prev = false;
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				if (adapter.row == _row) { 
					return _prev; 
				} else {
					_row = adapter.row;
					CNodeIndex cnode=_model.getCNodeIndex(adapter.row, adapter.column);
					if (cnode.nodeRow==-1) {
						_prev = true;
						return true;
					} else {
						_prev = false;
						return false;
					}
				}
			}
		};
		
		HighlightPredicate top=new HighlightPredicate() {
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				return (adapter.row==0);
			}
		};
		
		HighlightPredicate left=new HighlightPredicate() {
			public boolean isHighlighted(Component rederer, ComponentAdapter adapter) {
				return (adapter.column==0);
			}
		};
		
		
		super.addHighlighter(new IconHighlighter(paintIconExpanded,new ExpandIcon(ExpandIcon.EXPANDED)),true);
		super.addHighlighter(new IconHighlighter(paintIconClosed,new ExpandIcon(ExpandIcon.CLOSED)),true);
		super.addHighlighter(new IconHighlighter(paintIconNone,new ExpandIcon(ExpandIcon.NONE)),true);
		super.addHighlighter(new ColorHighlighter(splitRow,SplitTableDefaults.tablePartOfBg(),Color.black),true);
		super.addHighlighter(new ColorHighlighter(splitRow,SplitTableDefaults.tablePartOfBg(),Color.black),false);
		super.addHighlighter(new BorderHighlighter(top,SplitTableDefaults.topCellBorder()), true);
		super.addHighlighter(new BorderHighlighter(left,SplitTableDefaults.leftCellBorder()), false);
		
		
		super.addSelectionListener(new JXSplitTable.SelectionListener() {
			public void choosen(int row, int col, boolean left) {
				AbstractTwoLevelSplitTableModel.CNodeIndex cnode=_model.getCNodeIndex(row,col);
				if (cnode.isValid()) {
					Iterator<SelectionListener> it=_listeners.iterator();
					
					if (cnode.nodeRow==-1) {
						boolean expanded=_model.getNodeExpanded(cnode.nodeIndex);
						boolean newExpanded=_model.setNodeExpanded(cnode.nodeIndex, !expanded);
						if (newExpanded!=expanded) { _model.fireTableDataChanged(); }
					}
					
					while(it.hasNext()) {
						it.next().choosen(cnode.nodeIndex, cnode.nodeRow, cnode.column, left);
					}
				}
			}

			public void selected(int row, int col, boolean left) {
				AbstractTwoLevelSplitTableModel.CNodeIndex cnode=_model.getCNodeIndex(row,col);
				if (cnode.isValid()) {
					Iterator<SelectionListener> it=_listeners.iterator();
					while(it.hasNext()) {
						it.next().selected(cnode.nodeIndex,cnode.nodeRow,cnode.column,left);
					}
				}
			}

			public void unSelected(boolean left) {
				Iterator<SelectionListener> it=_listeners.iterator();
				while(it.hasNext()) {
					it.next().unSelected(left);
				}
			}
		});
	}
	
}
