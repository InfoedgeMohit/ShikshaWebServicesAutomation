package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class Practise {
	String subsId;

	public Practise() {
		// new Common().loadPropertiesFromConfig();
		subsId = "8184";
	}

	@DataProvider
	public String[][] testData() {

		// API params in sequence
		String[] params = new String[] { "email", "password" };

		String[][] testData = prepareTestData(params);

		return testData;
	}

	// prepare test data
	public String[][] prepareTestData(String[] params) {

		int noOfParam = params.length;

		// read data from excel
		HashMap<String, HashMap<String, String>> excelData = readExcel();
		int numOfTestData = excelData.size();

		// create a multi-dimensional array for test data {of size
		// [numberOfTestData][noOfParam]}
		String[][] testData = new String[numOfTestData][noOfParam];

		// keys of outer HashMap
		Set<String> keys = excelData.keySet();

		int counter = 0;
		// traverse for all keys of outer hashMap
		for (String key : keys) {
			System.out.println("Row Number >> " + key);
			// get inner hashMap for a particular key of outer hashMap
			HashMap<String, String> tempExcelData = excelData.get(key);
			// traverse for all keys of inner hashMap
			for (Map.Entry<String, String> entry : tempExcelData.entrySet()) {
				for (int i = 0; i < noOfParam; i++) {
					// Fetch appropriate value for key and assign to testData
					if (entry.getKey().equals(params[i]))
						testData[counter][i] = entry.getValue();
				}
			}
			System.out.println(Arrays.deepToString(testData[counter]));
			counter++;
		}
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1, enabled = false)
	public void testLogin(String email, String password) {
		Response response = RestAssured.given().header("SOURCE", "AndroidShiksha").header("authKey", "5656cd264aa2f")
				.param("email", email).param("password", password).when()
				.post("https://api.shiksha.com/api/v1/User/login").then().assertThat().statusCode(200).extract()
				.response();

		String userId = response.path("userId");
		Reporter.log(userId, true);
	}

	@Test(enabled = false)
	public void respnseTime() {
		ArrayList<Integer> responseTimes = new ArrayList<Integer>();
		responseTimes.add(20);
		responseTimes.add(10);
		responseTimes.add(30);
		responseTimes.add(15);
		Collections.sort(responseTimes);
		// Reporter.log(Arrays.toString(responseTimes.toArray()), true);
		int size = responseTimes.size();
		Reporter.log("Min time : " + responseTimes.get(0), true);
		Reporter.log("Max time : " + responseTimes.get(size - 1), true);
		long totalTime = 0;
		for (int item : responseTimes) {
			totalTime = totalTime + item;
		}
		Reporter.log("Total time : " + totalTime, true);
		Reporter.log("Average time : " + totalTime / size, true);

	}

	@Test(enabled = false)
	public HashMap<String, HashMap<String, String>> readExcel() {
		// create a outer hashMap
		HashMap<String, HashMap<String, String>> outerHashMap = new HashMap<String, HashMap<String, String>>();
		// create a inner hashMap
		HashMap<String, String> innerHashMap = new HashMap<String, String>();

		String filePath = System.getProperty("user.dir") + "\\src\\test\\resources\\info\\";
		String fileName = "test.xlsx";
		String sheetName = "Sheet1";
		File file = new File(filePath + fileName);
		FileInputStream inputStream = null;

		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		}

		Workbook workbook = null;
		String fileExt = fileName.substring(fileName.lastIndexOf("."));

		try {
			if (fileExt.equalsIgnoreCase(".xlsx"))
				workbook = new XSSFWorkbook(inputStream);
			else
				workbook = new HSSFWorkbook(inputStream);
		} catch (IOException e) {
			System.out.println("Unable to access file");
		}

		Sheet sheet = workbook.getSheet(sheetName);

		int totalRows = sheet.getLastRowNum();
		int totalColumns = sheet.getRow(0).getLastCellNum() - 1;

		// Loop to read all rows
		for (int i = 1; i <= totalRows; i++) {
			Row row = sheet.getRow(i);
			// key of outer hashMap
			String rowKey = row.getCell(1).getStringCellValue();
			innerHashMap.clear();
			// Loop to read all cells of a row
			for (int j = 2; j <= totalColumns; j++) {
				// key of inner hashMap
				String columnHeader = sheet.getRow(0).getCell(j).getStringCellValue();
				Cell cell = row.getCell(j);
				// value of inner hashMap
				String cellValue = cell.getStringCellValue();
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

	@Test(enabled = false)
	public void verifyPostiveCasesFromDb() {
		ResultSet rs = exceuteDbQuery(
				"SELECT BaseProductId FROM Subscription_Product_Mapping where SubscriptionId = " + subsId + ";",
				"SUMS");

		try {
			System.out.println(rs.getFetchSize());
			if (rs.getFetchSize() > 0) {
				System.out.println(rs.getInt(1));
			} else {
				System.out.println("Fail");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ResultSet exceuteDbQuery(String dbQuery, String db) {
		ResultSet rs = null;
		try {
			Connection con = DriverManager.getConnection("jdbc:mysql://172.16.3.111:3306/SUMS", "shiksha",
					"shiKm7Iv80l");
			Statement stmt = con.createStatement();
			rs = stmt.executeQuery(dbQuery);
			while (rs.next()) {
				System.out.println(rs.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}

	@Test(enabled = false)
	public void abc() throws ParseException {
		String date = "2016/01/01 00:00:dadf";
		DateFormat df = new SimpleDateFormat("yyyy/mm/dd hh:mm:ss");
		Date startDate = df.parse(date);
		System.out.println(startDate);
	}

	@Test(priority = 0, dependsOnMethods = { "testJenkins2", "testJenkins3" }, enabled = false)
	public void testJenkins1() {
		System.out.println("I'm in testJenkins1");
		// for(int i=1; i<=10; i++)
		// Reporter.log("<br> Hello>>"+i, true);
	}

	// @Test (priority = 2, enabled = false)
	@Test(priority = 1, enabled = false)
	public void testJenkins2() {
		System.out.println("I'm in testJenkins2");
		// Assert.fail("Getting failed explicitly");
		// for(int i=1; i<=10; i++) {
		// Reporter.log("<br> Hi>>"+i, true);
		// if(i==1) {
		//// Assert.fail("Getting failed explicitly");
		// }
		// }
	}

	@Test(priority = 0, enabled = false)
	public void testJenkins3() {
		System.out.println("I'm in testJenkins3");
		// Assert.fail("Getting failed explicitly");
		// for(int i=1; i<=10; i++) {
		// Reporter.log("<br> Hi>>"+i, true);
		// if(i==1) {
		//// Assert.fail("Getting failed explicitly");
		// }
		// }
	}

	@Test(enabled = true, priority = 2)
	public void checkSSLCertifictions() {
		Response respose = RestAssured.given().param("listing_id", "198720")
				.param("email_id", "1378026350828400gera@shiksha.com").param("listing_type", "course")
				.param("action_type", "actionAbc").when()
				.post("https://shikshatest04.infoedge.com/response/Response/createResponse").then().extract()
				.response();
		System.out.println(respose.getStatusCode());
	}

	@Test(enabled = true, priority = 1)
	public void setSsl() {}

	@Test(enabled = false)
	public void sortCharsInString() {
		HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
		hm.put(1, 10);
		hm.put(2, 10);
		hm.put(3, 10);
		System.out.println(hm);
		hm.replace(1, 5);
		System.out.println(hm);
		hm.replace(1, 15);
		System.out.println(hm);

	}

}
