package com.scholes.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class ExcelExport<T> {
	
	private WritableWorkbook workbook;
	
	public ExcelExport(String path) throws IOException {
		super();
		this.workbook = createWorkbook(path);
	}

	private WritableWorkbook createWorkbook(String path) throws IOException{
		WritableWorkbook book = null;
		File file = new File(path);
		if(file.exists()){
			file.delete();
		}
		book = Workbook.createWorkbook(file);
		return book;
	}
	
	public void closeWorkbook(){
		try {
			workbook.write();
			workbook.close();
		} catch (WriteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public WritableSheet createSheet(String sheetName , String[] headers , int index, List<T> entries) throws RowsExceededException, WriteException{
		WritableSheet sheet =  workbook.createSheet(sheetName, index);
		if(headers != null){
			for(int i=0 ; i < headers.length ;i++ ){
				Label label = new Label(i,0,headers[i]);
				sheet.addCell(label);
			}
		}
		
		if(entries != null){
			for(int i=0 ; i < entries.size() ; i++ ){
				T entry = entries.get(i);
				Field[] fields = entry.getClass().getFields();
				for(int k=0 ; k < fields.length ;k++ ){
					try {
						Field field = fields[k];
						Object value = field.get(entry);
						Label label = new Label(k,i+1,value+"");
						sheet.addCell(label);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return sheet;
	}

}
