package org.fedorahosted.flies.webtrans.editor;

import java.util.ArrayList;

import org.fedorahosted.flies.webtrans.model.TransUnit;


import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModelHelper.Request;
import com.google.gwt.gen2.table.client.TableModelHelper.SerializableResponse;

public class WebTransTableModel extends MutableTableModel<TransUnit> {

	@Override
	protected boolean onRowInserted(int beforeRow) {
		return false;
	}

	@Override
	protected boolean onRowRemoved(int row) {
		return true;
	}

	@Override
	protected boolean onSetRowValue(int row, TransUnit rowValue) {
		return true;
	}

	@Override
	public void requestRows(
			Request request,
			com.google.gwt.gen2.table.client.TableModel.Callback<TransUnit> callback) {
		int numRows = request.getNumRows();
		
		Log.info("Requesting " + numRows + " rows");
		
		ArrayList<TransUnit> units = generateSampleData(numRows);
		SerializableResponse<TransUnit> response = new SerializableResponse<TransUnit>(
				units);
		callback.onRowsReady(request, response);
	}

	private ArrayList<TransUnit> generateSampleData(int numRows) {
		ArrayList<TransUnit> units = new ArrayList<TransUnit>();
		for(int i=0;i<numRows; i++) {
			TransUnit unit = new TransUnit("<hellow num=\"" + i+"\" />", "<world> \"" + i+"\"</world>");
			unit.setFuzzy(Math.random() > 0.7);
			units.add(unit);
		}
		return units;
	}

}