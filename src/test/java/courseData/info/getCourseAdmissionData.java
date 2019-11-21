package courseData.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getCourseAdmissionData extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseAdmission";
	List<Integer> dbExamList = null;
	HashMap<String, HashMap<String, String>> dbImportantDates = null;
	HashMap<String, String> innerMap = null;
	String sqlQuery = null;
	String ExpectedActualListingId = null;
	String path = "data.importantDates";

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/courseapi/v1/info/getCourseData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "courseId", "cityId", "localityId",
				"apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//courseData//info//GetCourseData.xlsx";
		PrintWriter pw = new PrintWriter(file + ".txt");
		pw.close();

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same
		// no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetCoursedatagetCourseAdmissionDataApi(String courseId, String cityId,
			String localityId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		//courseId ="294736";
		// pass api params and hit apicompareCourseId
		if (courseId.equals("ignoreHeader")) {
			return;
			// apiResponse = RestAssured.given().param("userId",
			// userId).param("compareCourseIds[]",compareCourseId
			// ).when().post(api).then().extract().response();
		} else if (courseId.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();
		} else if (!courseId.equals("ignoreInput")) {
			if (!cityId.equals("ignoreInput")) {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId).then()
							.extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId+"&localityId="+localityId)
							.then().extract().response();
				}
			} else {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId)
							.then().extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&localityId="+localityId)
							.then().extract().response();
				}
			}
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();
		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(courseId, cityId, localityId, jsonPath);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);
		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(String courseId, String cityId,
			String localityId, JsonPath jsonPath) {
		// verify status is success
		String statusResponse = jsonPath.getString("status");
		if (statusResponse.contentEquals("success")) {
			Reporter.log(", Correct status [success]", true);
		} else {
			Reporter.log(", InCorrect status [" + statusResponse + "]", true);
			Assert.fail("InCorrect status [" + statusResponse + "]");
		}

		// verify message is null
		String messageResponse = jsonPath.getString("message");
		if (messageResponse == null) {
			Reporter.log(", Correct message [null]", true);
		} else {
			Reporter.log(", InCorrect message [" + messageResponse + "]", true);
			Assert.fail("InCorrect message [" + messageResponse + "]");
		}
		dbExamList = new ArrayList<Integer>();
		Date date = new Date();
		SimpleDateFormat dateformat = new SimpleDateFormat(
				"yyyy-MM-dd");
		String current = dateformat.format(date);
		String[] curDate = current.split("-");
		try {
			ResultSet rs = DatabaseCommon.getCourseImportantDate(courseId);
			dbExamList = DatabaseCommon.getExamListOfCourse(courseId);
			if (dbExamList.size() > 0 || getCountOfResultSet(rs) > 0) {	
				dbImportantDates = new HashMap<String, HashMap<String, String>>();
				if (dbExamList.size() > 0) {
					for (int i = 0; i < dbExamList.size(); i++) {
						int examId = dbExamList.get(i);
						ResultSet rsExamDate = DatabaseCommon
								.getCourseExamDates(examId);
						System.out.println(getCountOfResultSet(rsExamDate) );
						if (getCountOfResultSet(rsExamDate) > 0) {
							while (rsExamDate.next()) {
								innerMap = new HashMap<String, String>();
								
								String dbEventName = rsExamDate
										.getString("event_name");
								String dbExamName =rsExamDate.getString("name");
								
								if(!dbEventName.contains(dbExamName)){
									dbEventName = dbExamName+"-"+dbEventName;
								}
								String[] dbStartDate = String.valueOf(
										rsExamDate.getDate("start_date"))
										.split("-");
								String[] dbEndDate = String.valueOf(
										rsExamDate.getDate("end_date")).split(
										"-");
								String showComingUp = String
										.valueOf(getshowComingUp(dbEndDate[0],
												dbEndDate[1], dbEndDate[2],
												dbStartDate[0], dbStartDate[1],
												dbStartDate[2]));
								String dbDisplaystring = getDisplayString(
										dbEndDate[0], dbEndDate[1],
										dbEndDate[2].replaceFirst("^0+(?!$)", ""), dbStartDate[0],
										dbStartDate[1], dbStartDate[2].replaceFirst("^0+(?!$)", ""));
								innerMap.put("eventName", dbEventName);
								innerMap.put("startDate", dbStartDate[2].replaceFirst("^0+(?!$)", ""));
								innerMap.put("startMonth", dbStartDate[1].replaceFirst("^0+(?!$)", ""));
								innerMap.put("startYear", dbStartDate[0]);
								innerMap.put("endDate", dbEndDate[2].replaceFirst("^0+(?!$)", ""));
								innerMap.put("endMonth", dbEndDate[1].replaceFirst("^0+(?!$)", ""));
								innerMap.put("endYear", dbEndDate[0]);
								innerMap.put("type", "exam");
								innerMap.put("examId", String.valueOf(examId));
								innerMap.put("examName",
										dbExamName);
								innerMap.put("showUpcoming", showComingUp);
								innerMap.put("displayString", dbDisplaystring);
								int difference = getDateDifferenceinMonths(dbStartDate[0]
										+ "-" + dbStartDate[1] + "-"
										+ dbStartDate[2].replaceFirst("^0+(?!$)", ""));
								if(difference<8){
									dbImportantDates.put(dbEventName, innerMap);
								}
								else{
									if(Integer.parseInt(dbEndDate[0])>=Integer.parseInt(curDate[0])&&Integer.parseInt(dbEndDate[1])>=Integer.parseInt(curDate[1])){
										if(Integer.parseInt(dbEndDate[1])>=Integer.parseInt(curDate[1])){
											if(Integer.parseInt(dbEndDate[1])>Integer.parseInt(curDate[1])){
											dbImportantDates.put(dbEventName, innerMap);
											}
											else
											if(Integer.parseInt(dbEndDate[1])==Integer.parseInt(curDate[1])){
		 										if(dbEndDate[2].equals(0)){
													if(Integer.parseInt(dbEndDate[2])>=Integer.parseInt(curDate[2])){
														dbImportantDates.put(dbEventName, innerMap);
													}
												}
											}
										}
									}
								}
								
								
							}
						}
					}
				}
				if (getCountOfResultSet(rs) > 0) {
					while (rs.next()) {
						innerMap = new HashMap<String, String>();
						String dbEventName = rs.getString("event_name");
						String dbStartDate = String.valueOf(rs.getInt("start_date")).replaceFirst("^0+(?!$)", "");
						String dbStartMonth = String.valueOf(rs.getInt("start_month"));
						String dbStartYear = String.valueOf(rs.getInt("start_year"));
						String diffStartDate = dbStartDate;
						String startDateString = dbStartDate;
						if(diffStartDate.equals("0")){
							diffStartDate = "01";
						}
						if(startDateString.equals("0")){
							startDateString="null";
						}
						String dbEndDate = String.valueOf(rs.getInt("end_date")).replaceFirst("^0+(?!$)", "");
						String diffEndDate = dbEndDate;
						if(diffEndDate.equals("0")){
							diffEndDate="null";
						}						
						String dbEndMonth = String.valueOf(rs.getInt("end_month"));
						String diffEndMonth = dbEndMonth;
						if(diffEndMonth.equals("0")){
							diffEndMonth="null";
						}
						String dbEndYear = String.valueOf(rs.getInt("end_year"));
						String diffEndYear = dbEndYear;
						if(diffEndYear.equals("0")){
							diffEndYear="null";
						}
						String showComingUp = String.valueOf(getshowComingUp(
								dbEndYear, dbEndMonth, dbEndDate, dbStartYear,
								dbStartMonth, dbStartDate));
						String dbDisplaystring = getDisplayString(dbEndYear,
								dbEndMonth, dbEndDate, dbStartYear,
								dbStartMonth, dbStartDate);

						innerMap.put("eventName", dbEventName);
						innerMap.put("startDate", startDateString);
						innerMap.put("startMonth", dbStartMonth);
						innerMap.put("startYear", dbStartYear);
						innerMap.put("endDate", diffEndDate.replaceFirst("^0+(?!$)", ""));
						innerMap.put("endMonth", diffEndMonth);
						innerMap.put("endYear", diffEndYear);
						
						innerMap.put("type", "others");
						innerMap.put("examId", "null");
						innerMap.put("examName", "null");
						innerMap.put("showUpcoming", showComingUp);
						innerMap.put("displayString", dbDisplaystring);
						int difference = getDateDifferenceinMonths(dbStartYear
								+ "-" + dbStartMonth + "-"
								+ diffStartDate);
						if(difference<8){
							dbImportantDates.put(dbEventName, innerMap);
						}
						else{
							if(Integer.parseInt(dbEndYear)>=Integer.parseInt(curDate[0])&&Integer.parseInt(dbEndMonth)>=Integer.parseInt(curDate[1])){
								if(Integer.parseInt(dbEndMonth)>=Integer.parseInt(curDate[1])){
									if(Integer.parseInt(dbEndMonth)>Integer.parseInt(curDate[1])){
									dbImportantDates.put(dbEventName, innerMap);
									}
									else
									if(Integer.parseInt(dbEndMonth)==Integer.parseInt(curDate[1])){
 										if(dbEndDate.equals(0)){
											if(Integer.parseInt(dbEndDate)>=Integer.parseInt(curDate[2])){
												dbImportantDates.put(dbEventName, innerMap);
											}
										}
									}
								}
							}
						}
						
					}

				}
				if(dbImportantDates.size()>0){
				List<HashMap<String, String>> jsonDateList = new ArrayList<>(jsonPath.getList("data.importantDates.importantDates"));
				for(int i=0; i<jsonDateList.size();i++){
					HashMap<String, String> jsonTempMap = jsonDateList.get(i);
					String getEventName = jsonTempMap.get("eventName");
					HashMap<String, String> dbTempMap = dbImportantDates.get(getEventName);
					
					for(Map.Entry<String, String> entry: jsonTempMap.entrySet()){
						String keyName = entry.getKey();
						String jsonValue = String.valueOf(entry.getValue());
						String dbValue = dbTempMap.get(keyName);
						if(StringUtils.equals(jsonValue, dbValue)){
							Reporter.log("Value are appear correct for Important Dates for CourseId: "+courseId, true);
						}
						else{
							Reporter.log("Value are not appear correct for important dates for CourseId: "+courseId+" keyName: "+keyName, true);
							filewriter("Value are not appear correct for important dates for CourseId: "+courseId+" keyName: "+keyName, file);
							filewriter("Actual Value: "+jsonValue+" Expected Value: "+dbValue, file);
							filewriter("jsonMap: "+jsonTempMap, file);
							filewriter("dbMap: "+dbTempMap, file);
							Assert.assertTrue(false, "important Date data is not appear correct for courseId: "+courseId);
						}
					}
					if(jsonDateList.size()>3){
						if(jsonPath.getString("data.importantDates.showImportantViewMore").equals("true")){
							Reporter.log("showImportantDateviewmore is appear correct for courseId: "+courseId, true);
						}
						else{
							Reporter.log("showImportantDateviewmore is not appear correct for courseId: "+courseId, true);
							filewriter("showImportantDateviewmore is not appear correct for courseId: "+courseId, file);
							filewriter("Actual Value: "+jsonPath.getString("data.importantDates.showImportantViewMore"), file);
							Assert.assertTrue(false, "showImportantDateviewmore is not appear correct for courseId: "+courseId);
						}
					}
					else{
						if(jsonPath.getString("data.importantDates.showImportantViewMore").equals("false")){
							Reporter.log("showImportantDateviewmore is appear correct for courseId: "+courseId, true);
						}
						else{
							Reporter.log("showImportantDateviewmore is not appear correct for courseId: "+courseId, true);
							filewriter("showImportantDateviewmore is not appear correct for courseId: "+courseId, file);
							filewriter("Actual Value: "+jsonPath.getString("data.importantDates.showImportantViewMore"), file);
							Assert.assertTrue(false, "showImportantDateviewmore is not appear correct for courseId: "+courseId);
						}
					}
					
				}
			}
				else {
					if (jsonPath.getString(path + ".source").equals("layer")
							&& jsonPath.getString(path + ".isCourseDates").equals(
									"false")
							&& jsonPath.getString(path + ".importantDates") == null
							&& jsonPath.getString(path + ".showImportantViewMore")
									.equals("false")
							&& jsonPath.getMap(path + ".examsHavingDates").size() == 0) {
						Reporter.log(
								"important Date is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"important Date is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"important Date is not appear correct for courseId: "
										+ courseId, file);
						filewriter("importantDates List: " + jsonPath.getMap(path),
								file);
					}
				}
			} else {
				if (jsonPath.getString(path + ".source").equals("layer")
						&& jsonPath.getString(path + ".isCourseDates").equals(
								"false")
						&& jsonPath.getString(path + ".importantDates") == null
						&& jsonPath.getString(path + ".showImportantViewMore")
								.equals("false")
						&& jsonPath.getMap(path + ".examsHavingDates").size() == 0) {
					Reporter.log(
							"important Date is appear correct for courseId: "
									+ courseId, true);
				} else {
					Reporter.log(
							"important Date is not appear correct for courseId: "
									+ courseId, true);
					filewriter(
							"important Date is not appear correct for courseId: "
									+ courseId, file);
					filewriter("importantDates List: " + jsonPath.getMap(path),
							file);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try{
			ResultSet rs = DatabaseCommon.getAdmissionProcess(courseId);
			if(getCountOfResultSet(rs)>0){
				dbImportantDates=new HashMap<>();
				while(rs.next()){
					innerMap = new HashMap<String, String>();
					String processName = rs.getString("admission_name");
					if(processName.equalsIgnoreCase("Others")){
						processName = rs.getString("admission_name_other");
					}
					String admissiondesc = rs.getString("admission_description");
					String StageOrder = String.valueOf(rs.getInt("stage_order"));
					innerMap.put("admissionName", processName);
					innerMap.put("description", admissiondesc);
					dbImportantDates.put(StageOrder,innerMap);
				}
				HashMap<String, HashMap<String, String>> jsonAdmissionMap = new HashMap<>(jsonPath.getMap("data.admissionProcess"));
				if(dbImportantDates.size()==jsonAdmissionMap.size()){
					for(Map.Entry<String, HashMap<String, String>> entry: jsonAdmissionMap.entrySet()){
						String orderNum= entry.getKey();
						HashMap<String, String> jsonTempMap = entry.getValue();
						HashMap<String, String> dbTempMap = dbImportantDates.get(orderNum);
						for(Map.Entry<String, String> tempEntry: jsonTempMap.entrySet()){
							String keyString=tempEntry.getKey();
							String jsonValue = tempEntry.getValue();
							String dbValue = dbTempMap.get(keyString);
							if(jsonValue.equals(dbValue)){
								Reporter.log("Admission process value for: "+keyString+" is appear correct for courseId: "+courseId, true);
							}
							else{
								Reporter.log("Admission process value for: "+keyString+" is not appear correct for courseId: "+courseId, true);
								filewriter("Admission process value for: "+keyString+" is not appear correct for courseId: "+courseId, file);
								filewriter("KeyName: "+keyString+" Actual Value: "+jsonValue+" Expected Value: "+dbValue, file);
								Assert.assertTrue(false);
							}
						}
					}
				}
			}
			else{
				if(jsonPath.getString("data.admissionProcess")==null){
					Reporter.log("Admission process is appear null that is correct for courseId: "+courseId, true);
				}
				else{
					Reporter.log("Admission process is appear null that is not correct for courseId: "+courseId, true);
					filewriter("Admission process is appear null that is not correct for courseId: "+courseId, file);
					filewriter("Actual Value: "+jsonPath.getMap("data.admissionProcess"), file);
					Assert.assertTrue(false);
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetCourseFeesApi() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}

}