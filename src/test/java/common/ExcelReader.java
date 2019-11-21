package common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Reporter;

public class ExcelReader {

	private static XSSFWorkbook ExcelWBook;
	private static XSSFSheet ExcelWSheet;
	private static XSSFRow Row;
	private static XSSFCell Cell;	

	public static HashMap<String, HashMap<String, String>> readExcel(String FilePath){
		String SheetName = "Sheet1";
		// create a outer hashMap
		HashMap<String, HashMap<String, String>> outerHashMap = new HashMap<String, HashMap<String, String>>();
		// create a inner hashMap
		HashMap<String, String> innerHashMap = new HashMap<String, String>();

		FileInputStream ExcelFile = null;

		try {
			ExcelFile = new FileInputStream(FilePath);
		} catch (FileNotFoundException e) {
			Reporter.log("File not found", true);
		}

		// Access the required test data file
		try {
			ExcelWBook = new XSSFWorkbook(ExcelFile);
		} catch (IOException e) {
			Reporter.log("Unable to access file", true);
		}

		// 	Access the required sheet
		ExcelWSheet = ExcelWBook.getSheet(SheetName);

		int totalRows = ExcelWSheet.getLastRowNum();
		int totalColumns = ExcelWSheet.getRow(0).getLastCellNum() - 1;
		
		// Loop to read all rows
		for(int i=1 ; i <= totalRows; i++){
			Row = ExcelWSheet.getRow(i);
			// key of outer hashMap
			String rowKey = Row.getCell(1).getStringCellValue();
			innerHashMap.clear();
			// Loop to read all cells of a row
			for(int j = 2; j <= totalColumns; j++){
				// key of inner hashMap
				String columnHeader = ExcelWSheet.getRow(0).getCell(j).getStringCellValue();
				Cell = Row.getCell(j);
				// value of inner hashMap
				String cellValue;
				if(!Cell.getStringCellValue().isEmpty()||Cell.getStringCellValue()!=null)
					cellValue = Cell.getStringCellValue();
				else
					cellValue = "";
				//				String cellValue = Cell.getStringCellValue();
				// put key-value in inner hashMap
				innerHashMap.put(columnHeader, cellValue);
			}

			// tempHashMap required to copy innerHashMap into outerHashMap
			HashMap<String, String> tempHashMap = new HashMap<String, String>();
			// copy innerHashMap into tempHashMap
			tempHashMap.putAll(innerHashMap);
			// copy tempHashMap into outerHashMap
			outerHashMap.put(rowKey, tempHashMap);
		}
		return outerHashMap;
	}

	// not in use
	public static String[][] readExcelOldNotInUse(String FilePath, String SheetName) throws Exception {   
		String[][] testDataObjArr = null;

		try {
			FileInputStream ExcelFile = new FileInputStream(FilePath);

			// Access the required test data sheet
			ExcelWBook = new XSSFWorkbook(ExcelFile);
			ExcelWSheet = ExcelWBook.getSheet(SheetName);

			int startRow = 1;
			int startCol = 1;

			int ci,cj;

			int totalRows = ExcelWSheet.getLastRowNum();
			int totalCols = ExcelWSheet.getRow(startRow).getLastCellNum()-1;

			testDataObjArr = new String[totalRows][totalCols];

			ci=0;
			for (int i=startRow; i<=totalRows; i++, ci++) {           	   
				Row = ExcelWSheet.getRow(i);
				cj=0;
				for (int j=startCol; j<=totalCols; j++, cj++){
					Cell = Row.getCell(j);
					if(!Cell.getStringCellValue().isEmpty())
						testDataObjArr[ci][cj] = Cell.getStringCellValue();
					else
						testDataObjArr[ci][cj] = "";
				}
			}
		}

		catch (FileNotFoundException e){
			System.out.println("Could not find the Excel sheet");
			e.printStackTrace();
		}

		catch (IOException e){
			System.out.println("Could not read the Excel sheet");
			e.printStackTrace();
		}
		return testDataObjArr;
	}
}
