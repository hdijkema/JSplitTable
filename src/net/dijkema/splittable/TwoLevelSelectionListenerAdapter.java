package net.dijkema.splittable;

import net.dijkema.JXTwoLevelSplitTable;

public abstract class TwoLevelSelectionListenerAdapter implements JXTwoLevelSplitTable.SelectionListener {

	public void choosen(int nodeIndex, int nodeRow, int col, boolean left) {
	}

	public void selected(int nodeIndex, int nodeRow, int col, boolean left) {
	}

	public void unSelected(boolean left) {
	}

}
